package com.pvp.optimize.dango;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.gui.Ui;
import com.pvp.optimize.music.MusicLibrary;
import com.pvp.optimize.music.MusicPlayer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 把本地播放器「喂」给 DangoMusic 官方 HUD。
 *
 * <p>不再自绘任何播放器卡片：Dango 的灵动岛本来就有一个「外部音源」通道 ——
 * {@code MusicPlayer.getShownTrack()} 在自身未播放时会回落到
 * {@code SystemMedia.INSTANCE.getTrack()}（官方用来接管 Windows SMTC 的系统媒体）。
 * 我们直接往 {@code SystemMedia} 的静态字段里写本地曲目的元数据与播放位置，
 * 于是灵动岛 / 歌曲通知 / 歌曲卡片全部由 Dango 用它自己的液态玻璃管线画出来：
 * 封面、曲名、歌手、进度条、跳动音浪、切歌展开动画，一个都不用我们画。
 *
 * <p>为什么用 {@code nativeId = "smtc:local:..."}：Dango 的 {@code LyricsModule}
 * 看到 {@code smtc:} 前缀会跳过在线歌词请求（它拿不到本地文件的歌词），
 * 这条前缀同时让我们的 key 与真实网易云曲目完全隔离，不会串台。
 *
 * <p>性能：每个客户端 tick（20 Hz）写 6 个 volatile 字段 + 一次 {@code System.currentTimeMillis()}，
 * 没有任何分配（曲目对象与封面只在切歌时重建一次）。渲染成本完全等于 Dango 本身。
 */
public final class DangoHudFeed {

    private DangoHudFeed() {
    }

    /** 本地曲目的假封面地址：非空即可让 CoverCache 走「已有图片、只排队上传」的分支。 */
    private static final String COVER_URL = "pvp-local";
    /** 封面最大边长，超过就缩放，避免 4K 专辑图占显存。 */
    private static final int COVER_MAX = 256;

    private static boolean ready;
    private static boolean failedOnce;

    private static Field fTrack;
    private static Field fPlaying;
    private static Field fBasePos;
    private static Field fBaseAt;
    private static Field fEndMs;
    private static Field fEnabled;
    private static Constructor<?> ctorTrack;
    private static Object sourceNetease;
    private static Method mTrackKey;
    private static Field fCoverSources;

    // 灵动岛模块的外观开关（主控板「音乐」页直接写 Dango 自己的模块值）
    private static Object islandModule;
    private static int islandRetry;
    private static boolean islandBroken;
    private static Method mBoolSet;
    private static Method mFloatSetValue;
    private static Field fIslandGlass;
    private static Field fIslandNotify;
    private static Field fIslandNotifyTime;
    private static Field fIslandScale;
    private static boolean optApplied;
    private static boolean optGlass;
    private static boolean optNotify;
    private static int optSeconds = -1;
    private static int optScale = -1;

    // 当前注入的曲目
    private static Path curPath;
    private static Object curTrack;
    private static long curDurationMs = -1;
    private static boolean injected;

    public static void init() {
        try {
            Class<?> systemMedia = Class.forName("me.sorihiko.dango.music.SystemMedia");
            fTrack = field(systemMedia, "track");
            fPlaying = field(systemMedia, "playing");
            fBasePos = field(systemMedia, "basePos");
            fBaseAt = field(systemMedia, "baseAt");
            fEndMs = field(systemMedia, "endMs");
            fEnabled = field(systemMedia, "enabled");
            // 官方 SMTC 轮询会每 2 秒用 PowerShell 抓系统媒体，和我们的注入抢同一个字段。
            // 这里关一次（进程会被正常停掉），之后每 tick 只写布尔字段，不再重启它。
            try {
                Object instance = systemMedia.getField("INSTANCE").get(null);
                systemMedia.getMethod("setEnabled", boolean.class).invoke(instance, false);
            } catch (Throwable ignored) {
                // 拿不到就靠每 tick 的 enabled=false 兜底
            }

            Class<?> trackClass = Class.forName("me.sorihiko.dango.music.NeteaseTrack");
            Class<?> sourceClass = Class.forName("me.sorihiko.dango.music.MusicSource");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object source = Enum.valueOf((Class<? extends Enum>) sourceClass.asSubclass(Enum.class), "NETEASE");
            sourceNetease = source;
            ctorTrack = trackClass.getConstructor(long.class, String.class, List.class,
                    long.class, String.class, sourceClass, String.class);
            mTrackKey = trackClass.getMethod("getKey");

            try {
                Class<?> coverCache = Class.forName("me.sorihiko.dango.music.CoverCache");
                fCoverSources = field(coverCache, "sources");
            } catch (Throwable ignored) {
                fCoverSources = null;   // 拿不到封面缓存就只显示占位块，不影响曲名/进度
            }

            resolveIsland();

            ready = true;
            PvPOptimize.LOGGER.info("[PvP-Optimize] 本地播放器已接入 DangoMusic 灵动岛（官方渲染管线）。");
        } catch (Throwable t) {
            ready = false;
            PvPOptimize.LOGGER.warn("[PvP-Optimize] 本地播放器接入 Dango HUD 失败（不影响游戏）: {}", t.toString());
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /** 每客户端 tick 调用一次（在 MusicPlayer 更新播放位置之后）。 */
    public static void tick() {
        if (!ready) return;
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        applyIslandOptions(cfg);

        if (!cfg.musicPlayerEnabled || !cfg.musicHudEnabled) {
            clear();
            return;
        }

        Path path = MusicPlayer.trackPath();
        MusicPlayer.Status st = MusicPlayer.status();
        if (path == null || st == MusicPlayer.Status.STOPPED) {
            clear();
            return;
        }

        long durationMs = Math.max(0L, Math.round(MusicPlayer.duration() * 1000.0));
        // 同一首歌的时长解析完成时会从 0 跳到真实值，差得多了才重建曲目对象
        if (!path.equals(curPath) || curTrack == null || Math.abs(durationMs - curDurationMs) > 1500L) {
            build(path, durationMs);
        }
        if (curTrack == null) {
            clear();
            return;
        }

        long positionMs = Math.max(0L, Math.round(MusicPlayer.position() * 1000.0));
        boolean playing = st == MusicPlayer.Status.PLAYING;
        try {
            fEnabled.setBoolean(null, false);    // 关掉官方 SMTC 轮询，避免它把我们的曲目清掉
            fTrack.set(null, curTrack);
            fPlaying.setBoolean(null, playing);
            fBasePos.setLong(null, positionMs);
            fBaseAt.setLong(null, System.currentTimeMillis());
            fEndMs.setLong(null, durationMs);
            injected = true;
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** 停止 / 关闭本地播放器：把注入的曲目撤掉，灵动岛回到官方待机态。 */
    private static void clear() {
        curPath = null;
        curTrack = null;
        curDurationMs = -1;
        if (!injected) return;
        injected = false;
        try {
            fTrack.set(null, null);
            fPlaying.setBoolean(null, false);
            fBasePos.setLong(null, 0L);
            fEndMs.setLong(null, 0L);
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void build(Path path, long durationMs) {
        try {
            curPath = path;
            curDurationMs = durationMs;

            String raw = MusicLibrary.displayTitle(path);
            String title = raw;
            String artist = "";
            int dash = raw.indexOf(" - ");
            if (dash > 0 && dash + 3 < raw.length()) {
                artist = raw.substring(0, dash).trim();
                title = raw.substring(dash + 3).trim();
            }
            if (title.isEmpty()) title = raw;
            List<String> artists = artist.isEmpty()
                    ? List.of(Ui.tr("gui.pvp_optimize.music.local_artist"))
                    : List.of(artist);

            String nativeId = "smtc:local:" + Integer.toHexString(path.toString().hashCode());
            Object track = ctorTrack.newInstance(0L, title, artists, durationMs,
                    COVER_URL, sourceNetease, nativeId);
            curTrack = track;
            pushCover(path, (String) mTrackKey.invoke(track));
        } catch (Throwable t) {
            curTrack = null;
            fail(t);
        }
    }

    // ==================== 封面 ====================

    /** 把本地同名封面 / 文件夹封面塞进 Dango 的封面缓存，灵动岛就会画真封面而不是灰块。 */
    private static void pushCover(Path track, String key) {
        if (fCoverSources == null || key == null) return;
        try {
            BufferedImage image = loadCover(track);
            if (image == null) return;
            Object map;
            try {
                map = fCoverSources.get(null);
            } catch (IllegalAccessException e) {
                return;
            }
            if (!(map instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<Object, Object> sources = (Map<Object, Object>) map;
            // CoverCache.track() 查找用的就是 "t:" + track.getKey()
            sources.put("t:" + key, image);
        } catch (Throwable ignored) {
            // 封面只是锦上添花，失败就让 Dango 画占位块
        }
    }

    private static BufferedImage loadCover(Path track) {
        Path dir = track.getParent();
        if (dir == null) return null;
        String base = track.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        String[] stems = {base, "cover", "folder", "front", "album"};
        String[] exts = {".jpg", ".jpeg", ".png"};
        for (String stem : stems) {
            for (String ext : exts) {
                Path candidate = dir.resolve(stem + ext);
                if (!Files.isRegularFile(candidate)) continue;
                try {
                    BufferedImage src = ImageIO.read(candidate.toFile());
                    if (src != null) return scale(src);
                } catch (Throwable ignored) {
                    // 换下一个候选
                }
            }
        }
        return null;
    }

    private static BufferedImage scale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= COVER_MAX && h <= COVER_MAX) return src;
        double k = (double) COVER_MAX / Math.max(w, h);
        int nw = Math.max(1, (int) Math.round(w * k));
        int nh = Math.max(1, (int) Math.round(h * k));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    // ==================== 灵动岛外观 ====================

    /**
     * 主控板「音乐」页的灵动岛开关直接写到 Dango 自己的模块值上，
     * 这样面板改了立刻生效，也不必再维护第二套渲染。
     */
    private static void applyIslandOptions(PvPOptimizeConfig.Data cfg) {
        if (islandModule == null) {
            // 模块管理器可能在启动最初几 tick 才建好，这里低频重试，避免永久失去面板联动
            if (islandBroken) return;
            if ((++islandRetry % 100) == 0) resolveIsland();
            if (islandModule == null) return;
        }
        boolean glass = !Boolean.FALSE.equals(cfg.musicIslandGlass);
        boolean notify = !Boolean.FALSE.equals(cfg.musicIslandExpand);
        int seconds = Math.max(1, Math.min(10, cfg.musicIslandNotifySeconds));
        int scale = Math.max(60, Math.min(160, cfg.islandScale));
        if (optApplied && glass == optGlass && notify == optNotify
                && seconds == optSeconds && scale == optScale) {
            return;
        }
        try {
            if (!optApplied || glass != optGlass) mBoolSet.invoke(fIslandGlass.get(islandModule), glass);
            if (!optApplied || notify != optNotify) mBoolSet.invoke(fIslandNotify.get(islandModule), notify);
            if (!optApplied || seconds != optSeconds) {
                mFloatSetValue.invoke(fIslandNotifyTime.get(islandModule), (float) seconds);
            }
            if (!optApplied || scale != optScale) {
                mFloatSetValue.invoke(fIslandScale.get(islandModule), scale / 100.0f);
            }
            optGlass = glass;
            optNotify = notify;
            optSeconds = seconds;
            optScale = scale;
            optApplied = true;
        } catch (Throwable t) {
            islandBroken = true;
            islandModule = null;   // 结构不合预期就彻底放弃，不每帧重试
            PvPOptimize.LOGGER.warn("[PvP-Optimize] 灵动岛外观同步失败: {}", t.toString());
        }
    }

    private static void resolveIsland() {
        try {
            Class<?> dangoMusic = Class.forName("me.sorihiko.dango.DangoMusic");
            Object companion = dangoMusic.getField("Companion").get(null);
            Object instance = companion.getClass().getMethod("getInstance").invoke(companion);
            Object manager = instance.getClass().getMethod("getModuleManager").invoke(instance);
            Class<?> islandClass = Class.forName("me.sorihiko.dango.module.impls.music.DynamicIslandModule");
            Object module = manager.getClass().getMethod("get", Class.class).invoke(manager, islandClass);
            if (module == null) return;

            Class<?> boolValue = Class.forName("me.sorihiko.dango.module.values.BooleanValue");
            Class<?> floatValue = Class.forName("me.sorihiko.dango.module.values.FloatValue");
            mBoolSet = boolValue.getMethod("set", boolean.class);
            mFloatSetValue = floatValue.getMethod("setValue", float.class);
            fIslandGlass = field(islandClass, "glass");
            fIslandNotify = field(islandClass, "songNotify");
            fIslandNotifyTime = field(islandClass, "notifyTime");
            fIslandScale = field(islandClass, "scale");
            islandModule = module;
        } catch (Throwable ignored) {
            islandModule = null;   // 面板上的外观开关退化为无效，不影响其它功能
        }
    }

    private static void fail(Throwable t) {
        if (failedOnce) return;
        failedOnce = true;
        PvPOptimize.LOGGER.warn("[PvP-Optimize] Dango HUD 注入异常（已停止重试）: {}", t.toString());
        ready = false;
    }

    /** 供启动诊断。 */
    public static String status() {
        if (!ready) return "未接入";
        return injected ? "已接入/" + (curPath == null ? "?" : curPath.getFileName()) : "已接入/待播放";
    }
}
