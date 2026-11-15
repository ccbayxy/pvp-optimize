package com.pvp.optimize.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pvp.optimize.PvPOptimize;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 本地音乐库：目录扫描、播放队列、LRC 歌词、播放状态持久化。
 * 全部是纯文件 IO，只在切歌/启动时跑；渲染帧里只做内存查询。
 */
public final class MusicLibrary {

    private MusicLibrary() {}

    public enum Repeat { OFF, ALL, ONE }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM = new Random();
    private static final List<Path> TRACKS = new ArrayList<>();
    private static final Map<Path, Lrc> LRC_CACHE = new HashMap<>();
    private static int index = -1;
    private static boolean shuffle = false;
    private static Repeat repeat = Repeat.ALL;

    /**
     * 生效的音乐目录。
     *
     * <p>默认值由 {@code FabricLoader.getConfigDir()} 拼出来，也就是<b>当前游戏实例</b>下的
     * {@code config/pvp_optimize/music} —— 不是任何绝对路径常量，所以把这个 mod 分享给别人，
     * 对方的实例在 D 盘、E 盘、甚至另一个启动器下都各算各的，不存在“死读作者机器路径”。
     *
     * <p>想放到别处（比如歌库在 D 盘）时在配置里填 {@code musicDir}：绝对路径直接用，
     * 相对路径相对游戏目录解析；解析失败自动回落到默认目录，不会扫不到歌。
     */
    public static Path musicDir() {
        String custom = com.pvp.optimize.PvPOptimizeConfig.get().musicDir;
        if (custom != null && !custom.isBlank()) {
            try {
                Path p = Paths.get(custom.trim());
                return (p.isAbsolute() ? p : FabricLoader.getInstance().getGameDir().resolve(p))
                        .toAbsolutePath().normalize();
            } catch (Exception e) {
                PvPOptimize.LOGGER.warn("[PvP-Optimize] 音乐目录覆盖值无效: {}", custom, e);
            }
        }
        return FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize").resolve("music");
    }

    /** 用系统文件管理器打开音乐目录（按钮用；失败只记日志，不抛给界面）。 */
    public static void openFolder() {
        Path dir = musicDir();
        try {
            Files.createDirectories(dir);
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(dir.toFile());
                return;
            }
            // 无 AWT 桌面环境时的兜底
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("explorer.exe", dir.toString())
                    : os.contains("mac")
                        ? new ProcessBuilder("open", dir.toString())
                        : new ProcessBuilder("xdg-open", dir.toString());
            pb.start();
        } catch (Exception e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] 打开音乐目录失败: {}", dir, e);
        }
    }

    private static Path stateFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize_music_state.json");
    }

    /** 扫描 music 目录（含子目录），只在启动和手动刷新时调用。 */
    public static synchronized void refresh() {
        String previous = currentName();
        TRACKS.clear();
        Path dir = musicDir();
        try {
            Files.createDirectories(dir);
            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(Files::isRegularFile)
                        .filter(MusicLibrary::isAudio)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(TRACKS::add);
            }
        } catch (Exception e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] 音乐目录扫描失败: {}", dir, e);
        }
        index = -1;
        if (previous != null) {
            for (int i = 0; i < TRACKS.size(); i++) {
                if (TRACKS.get(i).getFileName().toString().equals(previous)) { index = i; break; }
            }
        }
        if (index < 0 && !TRACKS.isEmpty()) index = 0;
    }

    private static boolean isAudio(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg");
    }

    public static synchronized List<Path> tracks() { return new ArrayList<>(TRACKS); }

    public static synchronized Path current() {
        return index >= 0 && index < TRACKS.size() ? TRACKS.get(index) : null;
    }

    public static synchronized String currentName() {
        Path p = current();
        return p == null ? null : p.getFileName().toString();
    }

    public static synchronized int currentIndex() { return index; }

    public static synchronized void setIndex(int i) {
        if (i >= 0 && i < TRACKS.size()) index = i;
    }

    public static synchronized boolean isShuffle() { return shuffle; }
    public static synchronized void setShuffle(boolean v) { shuffle = v; }
    public static synchronized Repeat repeat() { return repeat; }
    public static synchronized void setRepeat(Repeat v) { repeat = v == null ? Repeat.ALL : v; }

    /** @param userInitiated true = 玩家按了下一首；false = 本曲自然播完。 */
    public static synchronized Path advance(boolean userInitiated) {
        if (TRACKS.isEmpty()) return null;
        if (!userInitiated && repeat == Repeat.ONE) return current();
        if (shuffle && TRACKS.size() > 1) {
            int n;
            do { n = RANDOM.nextInt(TRACKS.size()); } while (n == index);
            index = n;
            return current();
        }
        int next = index + 1;
        if (next >= TRACKS.size()) {
            if (repeat == Repeat.OFF && !userInitiated) { index = -1; return null; }
            next = 0;
        }
        index = next;
        return current();
    }

    public static synchronized Path previous() {
        if (TRACKS.isEmpty()) return null;
        if (shuffle && TRACKS.size() > 1) {
            int n;
            do { n = RANDOM.nextInt(TRACKS.size()); } while (n == index);
            index = n;
        } else {
            index = (index <= 0 ? TRACKS.size() : index) - 1;
        }
        return current();
    }

    /** 从 "歌手 - 标题.mp3" 里拆出便于显示的文本。 */
    public static String displayTitle(Path p) {
        if (p == null) return "—";
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        return n.trim().isEmpty() ? p.getFileName().toString() : n;
    }

    public static String formatTime(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) seconds = 0;
        int t = (int) seconds;
        return String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60);
    }

    // ==================== LRC 歌词 ====================

    /**
     * 直接用「时间(毫秒) + 文本」构建歌词模型。
     *
     * <p>给 DangoMusic 的在线歌词复用同一套 HUD 渲染用：Dango 那边已经解析好了
     * {@code Lyrics.getLines()}（timeMs / text），转过来就不用再走 LRC 文本解析。
     */
    public static Lrc lrcOf(long[] timesMs, String[] texts) {
        Lrc lrc = new Lrc();
        if (timesMs == null || texts == null) return lrc;
        int n = Math.min(timesMs.length, texts.length);
        for (int i = 0; i < n; i++) {
            lrc.times.add(new double[]{Math.max(0L, timesMs[i]) / 1000.0});
            lrc.texts.add(texts[i] == null ? "" : texts[i]);
        }
        return lrc;
    }

    public static final class Lrc {
        private static final Pattern TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
        private final List<double[]> times = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();

        public boolean isEmpty() { return times.isEmpty(); }

        /** 歌词总行数。 */
        public int size() { return times.size(); }

        /** 该行的时间戳（秒），越界返回 -1。 */
        public double timeAt(int line) {
            return line >= 0 && line < times.size() ? times.get(line)[0] : -1;
        }

        public int lineAt(double seconds) {
            int lo = 0, hi = times.size() - 1, ans = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (times.get(mid)[0] <= seconds) { ans = mid; lo = mid + 1; }
                else hi = mid - 1;
            }
            return ans;
        }

        public String textAt(int line) {
            return line >= 0 && line < texts.size() ? texts.get(line) : "";
        }

        static Lrc parse(List<String> lines) {
            Lrc lrc = new Lrc();
            // TreeMap 天然按时间排序，避免「时间排了序、歌词没跟着排」的错位。
            java.util.TreeMap<Double, String> ordered = new java.util.TreeMap<>();
            for (String raw : lines) {
                Matcher m = TAG.matcher(raw);
                double at = -1;
                int end = 0;
                if (m.find()) {
                    try {
                        int min = Integer.parseInt(m.group(1));
                        int sec = Integer.parseInt(m.group(2));
                        double frac = m.group(3) == null ? 0
                                : Double.parseDouble("0." + m.group(3));
                        at = min * 60 + sec + frac;
                        end = m.end();
                    } catch (NumberFormatException ignored) { }
                }
                if (at < 0) continue;
                ordered.putIfAbsent(at, raw.substring(end).trim());
            }
            for (Map.Entry<Double, String> e : ordered.entrySet()) {
                lrc.times.add(new double[]{e.getKey()});
                lrc.texts.add(e.getValue());
            }
            return lrc;
        }
    }

    /** 空歌词单例：没有 .lrc 时复用同一个对象，HUD 每帧取歌词不会产生分配。 */
    private static final Lrc EMPTY = Lrc.parse(List.of());

    /** 读取与曲目同名的 .lrc，结果缓存；找不到返回空歌词。 */
    public static Lrc lyricsFor(Path track) {
        if (track == null) return EMPTY;
        Lrc cached = LRC_CACHE.get(track);
        if (cached != null) return cached;
        Lrc result;
        try {
            String base = track.getFileName().toString();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            Path lrcPath = track.resolveSibling(base + ".lrc");
            if (Files.isRegularFile(lrcPath)) {
                List<String> lines;
                try {
                    lines = Files.readAllLines(lrcPath);           // UTF-8
                } catch (Exception utf8) {
                    lines = Files.readAllLines(lrcPath, java.nio.charset.Charset.forName("GBK"));
                }
                result = Lrc.parse(lines);
            } else {
                result = EMPTY;
            }
        } catch (Exception e) {
            result = EMPTY;
        }
        LRC_CACHE.put(track, result);
        return result;
    }

    // ==================== 播放记忆 ====================

    public static final class State {
        public String lastTrack;
        public double position;
        public int volume;
        public boolean shuffle;
        public String repeat;
        public boolean autoPlay;
    }

    public static State loadState() {
        Path p = stateFile();
        if (!Files.isRegularFile(p)) return null;
        try (Reader r = Files.newBufferedReader(p)) {
            return GSON.fromJson(r, State.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveState(State s) {
        try (Writer w = Files.newBufferedWriter(stateFile())) {
            GSON.toJson(s, w);
        } catch (Exception ignored) { }
    }
}
