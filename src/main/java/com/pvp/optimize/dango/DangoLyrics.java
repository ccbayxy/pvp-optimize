package com.pvp.optimize.dango;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.music.MusicLibrary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 读取 DangoMusic 当前曲目的歌词，交给 {@code hud/LyricsHud} 用 2D 面板画出来。
 *
 * <h2>为什么不能只读 Dango 的歌词缓存</h2>
 * Dango 的 {@code LyricsModule} 里确实有 {@code cache}（key = 曲目 key），但它的填充路径是
 * <b>唯一的</b>：{@code LyricsModule.render(WorldRenderEndEvent)} → {@code updateClock()}
 * → {@code requestLyrics()}。也就是说：
 * <ul>
 *   <li>模块必须是启用状态；</li>
 *   <li>{@code WorldRenderEndEvent} 必须真的被派发 —— 而它在 1.20.6 兼容补丁里对应的
 *       世界渲染入口是空实现，所以这个事件根本不会来；</li>
 *   <li>因此 {@code cache} 永远是空的，<b>歌词抓到了也画不出来</b>。</li>
 * </ul>
 * 这就是「Dango music 的歌词无法显示」的真正原因，跟 HUD 面板本身无关。
 *
 * <h2>现在的做法</h2>
 * 三级取词，全部非阻塞（渲染线程绝不发网络请求）：
 * <ol>
 *   <li>先看 {@link #CACHE}（本曲已拿到过，直接返回）；</li>
 *   <li>再看 Dango 自己的模块缓存（万一用户那边模块是启用的、缓存里已经有，零成本复用）；</li>
 *   <li>都没有就丢到一条 daemon 线程去抓 —— 抓取用的是 <b>Dango 自己的实现</b>
 *       {@code MusicSources.api(source).lyrics(track)}，所以鉴权、编码、翻译合并、
 *       YRC/LRC 解析全部与官方一致，本项目没有重复实现一套网络协议。</li>
 * </ol>
 * 抓不到（版权曲 / 纯音乐）会记 60 秒冷却，避免每帧重试打网络。
 */
public final class DangoLyrics {

    private DangoLyrics() {}

    /** 抓取失败后的冷却：60 秒内不再为同一首歌重试。 */
    private static final long MISS_COOLDOWN_NS = 60_000_000_000L;
    /** 读 Dango 模块缓存的限频（它只在别处被写，没必要每帧反射）。 */
    private static final long MODULE_PROBE_NS = 500_000_000L;

    // ===== 当前曲目通道 =====
    private static boolean ready;
    private static Object player;
    private static Method mShownTrack;
    private static Method mShownPos;
    private static Method mShownActive;
    private static Method mGetKey;
    private static Method mGetNativeId;
    private static Method mGetSource;

    // ===== Dango 自己的歌词模块缓存（可选快路径） =====
    private static Object lyricsModule;
    private static Field fCache;
    private static Method mGetLines;
    private static int moduleRetry;
    private static boolean moduleBroken;
    private static long lastProbeNs;

    // ===== 自己抓取用的官方实现 =====
    private static Object musicSources;
    private static Method mApi;
    private static Method mApiLyrics;

    // ===== 逐行解析（首次用到时解析一次方法句柄） =====
    private static Method mLineTime;
    private static Method mLineText;
    private static Method mLineWords;
    private static Method mWordText;

    private static final Map<String, MusicLibrary.Lrc> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> MISS_AT = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();
    private static ExecutorService fetcher;

    /** 诊断用：最近一次抓取的结果说明。 */
    private static volatile String lastResult = "未开始";

    public static void init() {
        try {
            Class<?> playerCls = Class.forName("me.sorihiko.dango.music.MusicPlayer");
            player = playerCls.getField("INSTANCE").get(null);
            mShownTrack = playerCls.getMethod("getShownTrack");
            mShownPos = playerCls.getMethod("getShownPositionMs");
            try {
                mShownActive = playerCls.getMethod("getShownActive");
            } catch (NoSuchMethodException ignored) {
                mShownActive = null;
            }

            Class<?> trackCls = Class.forName("me.sorihiko.dango.music.NeteaseTrack");
            mGetKey = trackCls.getMethod("getKey");
            mGetNativeId = trackCls.getMethod("getNativeId");
            mGetSource = trackCls.getMethod("getSource");

            Class<?> lyricLine = Class.forName("me.sorihiko.dango.lyrics.LyricLine");
            mLineTime = lyricLine.getMethod("getTimeMs");
            mLineText = lyricLine.getMethod("getText");
            mLineWords = lyricLine.getMethod("getWords");
            mWordText = Class.forName("me.sorihiko.dango.lyrics.LyricWord").getMethod("getText");

            // 官方取词入口：MusicSources.api(source).lyrics(track)
            Class<?> sources = Class.forName("me.sorihiko.dango.music.MusicSources");
            musicSources = sources.getField("INSTANCE").get(null);
            mApi = sources.getMethod("api", Class.forName("me.sorihiko.dango.music.MusicSource"));
            mApiLyrics = Class.forName("me.sorihiko.dango.music.MusicSourceApi")
                    .getMethod("lyrics", trackCls);
            lastResult = "待机";

            ready = true;
        } catch (Throwable t) {
            ready = false;
            PvPOptimize.LOGGER.warn("[PvP-Optimize] Dango 歌词桥不可用: {}", t.toString());
            return;
        }
        resolveModule();
    }

    private static void resolveModule() {
        try {
            Class<?> dangoMusic = Class.forName("me.sorihiko.dango.DangoMusic");
            Object companion = dangoMusic.getField("Companion").get(null);
            Object instance = companion.getClass().getMethod("getInstance").invoke(companion);
            Object manager = instance.getClass().getMethod("getModuleManager").invoke(instance);
            Class<?> lyricClass = Class.forName("me.sorihiko.dango.module.impls.music.LyricsModule");
            Object module = manager.getClass().getMethod("get", Class.class).invoke(manager, lyricClass);
            if (module == null) return;
            Field f = lyricClass.getDeclaredField("cache");
            f.setAccessible(true);
            fCache = f;
            lyricsModule = module;
        } catch (Throwable ignored) {
            lyricsModule = null;
        }
    }

    // ==================== 对外接口 ====================

    /** Dango 侧当前歌曲的进度（秒）；拿不到返回 0。 */
    public static double shownPositionSec() {
        if (!ready) return 0;
        try {
            Object pos = mShownPos.invoke(player);
            return pos instanceof Long ? ((Long) pos) / 1000.0 : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Dango 现在是否在「显示」某首曲目（含暂停态）。 */
    public static boolean isShowing() {
        if (!ready) return false;
        try {
            if (mShownActive != null) {
                Object active = mShownActive.invoke(player);
                if (active instanceof Boolean) return (Boolean) active;
            }
            return mShownTrack.invoke(player) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 当前 Dango 曲目的歌词；没有就返回 {@code null}（面板这一帧不画）。
     * 首次调用会异步发起抓取，后续帧命中缓存即返回。
     */
    public static MusicLibrary.Lrc currentLrc() {
        if (!ready) return null;
        Object track = shownTrack();
        if (track == null) return null;
        String key = str(invoke(mGetKey, track));
        if (key == null) return null;

        MusicLibrary.Lrc lrc = CACHE.get(key);
        if (lrc != null) return lrc;

        lrc = fromModuleCache(key);
        if (lrc != null) {
            CACHE.put(key, lrc);
            lastResult = "Dango缓存命中";
            return lrc;
        }

        requestFetch(track, key);
        return null;
    }

    /** 手动清掉某首歌的失败记录（面板上「重新抓取歌词」按钮用）。 */
    public static void retry(String key) {
        if (key == null) return;
        CACHE.remove(key);
        MISS_AT.remove(key);
        lastResult = "已请求重试";
    }

    /** 供启动诊断。 */
    public static String status() {
        if (!ready) return "不可用";
        Object track = shownTrack();
        if (track == null) return "无曲目";
        String key = str(invoke(mGetKey, track));
        MusicLibrary.Lrc lrc = key == null ? null : CACHE.get(key);
        if (lrc != null) return "有歌词(" + lrc.size() + " 行)";
        if (key != null && PENDING.containsKey(key)) return "抓取中";
        if (key != null && MISS_AT.containsKey(key)) return "本曲无歌词";
        return lastResult;
    }

    // ==================== 内部 ====================

    private static Object shownTrack() {
        try {
            return mShownTrack.invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 读 Dango 模块缓存（限频 + 结构异常即永久放弃）。 */
    private static MusicLibrary.Lrc fromModuleCache(String key) {
        if (moduleBroken) return null;
        if (lyricsModule == null) {
            if ((++moduleRetry % 200) == 0) resolveModule();
            if (lyricsModule == null) return null;
        }
        long now = System.nanoTime();
        if (now - lastProbeNs < MODULE_PROBE_NS) return null;
        lastProbeNs = now;
        try {
            Object map = fCache.get(lyricsModule);
            if (!(map instanceof Map)) return null;
            return toLrc(((Map<?, ?>) map).get(key));
        } catch (Throwable t) {
            moduleBroken = true;
            lyricsModule = null;
            return null;
        }
    }

    private static void requestFetch(Object track, String key) {
        if (mApiLyrics == null) return;
        String nativeId = str(invoke(mGetNativeId, track));
        // smtc: 前缀是 Dango 对「本地 / 系统媒体」的标记，官方同样跳过在线歌词
        if (nativeId != null && nativeId.startsWith("smtc:")) return;
        Long miss = MISS_AT.get(key);
        if (miss != null && System.nanoTime() - miss < MISS_COOLDOWN_NS) return;
        if (PENDING.putIfAbsent(key, Boolean.TRUE) != null) return;
        try {
            executor().execute(() -> {
                try {
                    Object source = invoke(mGetSource, track);
                    Object api = source == null ? null : mApi.invoke(musicSources, source);
                    Object lyrics = api == null ? null : mApiLyrics.invoke(api, track);
                    MusicLibrary.Lrc lrc = toLrc(lyrics);
                    if (lrc != null && !lrc.isEmpty()) {
                        CACHE.put(key, lrc);
                        MISS_AT.remove(key);
                        lastResult = "已抓取(" + lrc.size() + " 行)";
                    } else {
                        MISS_AT.put(key, System.nanoTime());
                        lastResult = "官方无此曲歌词";
                    }
                } catch (Throwable t) {
                    MISS_AT.put(key, System.nanoTime());
                    lastResult = "抓取失败: " + t.getClass().getSimpleName();
                } finally {
                    PENDING.remove(key);
                }
            });
        } catch (Throwable t) {
            PENDING.remove(key);
        }
    }

    private static synchronized ExecutorService executor() {
        if (fetcher == null) {
            fetcher = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "pvp-optimize-lyrics");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
        }
        return fetcher;
    }

    /** {@code me.sorihiko.dango.lyrics.Lyrics} → 本项目的歌词模型（空行会被丢掉）。 */
    private static MusicLibrary.Lrc toLrc(Object lyrics) {
        if (lyrics == null) return null;
        try {
            Object linesObj = lyrics.getClass().getMethod("getLines").invoke(lyrics);
            if (!(linesObj instanceof List<?> lines) || lines.isEmpty()) return null;
            List<Long> times = new ArrayList<>(lines.size());
            List<String> texts = new ArrayList<>(lines.size());
            for (Object line : lines) {
                if (line == null) continue;
                String text = str(mLineText.invoke(line));
                if (text == null || text.isBlank()) text = joinWords(line);
                if (text == null) text = "";
                text = text.trim();
                if (text.isEmpty()) continue;                 // 纯伴奏行不占行高
                Object t = mLineTime.invoke(line);
                long ms = t instanceof Long ? (Long) t : 0L;
                times.add(Math.max(0L, ms));
                texts.add(text);
            }
            if (times.isEmpty()) return null;
            long[] ts = new long[times.size()];
            for (int i = 0; i < ts.length; i++) ts[i] = times.get(i);
            return MusicLibrary.lrcOf(ts, texts.toArray(new String[0]));
        } catch (Throwable t) {
            return null;
        }
    }

    /** 逐字歌词（YRC）的 text 可能为空，文本全在 words 里。 */
    private static String joinWords(Object line) {
        try {
            Object wordsObj = mLineWords.invoke(line);
            if (!(wordsObj instanceof List<?> words) || words.isEmpty()) return null;
            StringBuilder sb = new StringBuilder(words.size() * 2);
            for (Object w : words) {
                if (w == null) continue;
                String s = str(mWordText.invoke(w));
                if (s != null) sb.append(s);
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invoke(Method m, Object target) {
        if (m == null || target == null) return null;
        try {
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
