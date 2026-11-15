package com.pvp.optimize.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.gui.Ui;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 在线音乐：搜索、取直链、下载音频与同名 LRC、外部下载目录导入。
 *
 * <p>LenoMusic 是 LenoLang + SDL3 的独立桌面程序，没有 Java 库也没有 HTTP 服务，
 * 无法作为依赖塞进 Fabric mod。这里按它的引擎思路用 JDK 自带的 HttpClient
 * 复刻「多音源搜索 → 取直链 → 分片落盘 + 歌词」这条链路：
 * <ul>
 *   <li>搜索 / 取链走公开的聚合接口（yinyueku.cn 的 MKOnlinePlayer api.php），
 *       源为 netease，返回可直接解码的 MP3；</li>
 *   <li>歌词走网易云公开 lyric 接口，落盘成同名 .lrc；</li>
 *   <li>「直链导入」可粘贴任意下载器（包括 LenoMusic）给出的 MP3/WAV/OGG 直链；</li>
 *   <li>「目录导入」可把 LenoMusic 的 downloads 目录整批复制进 mod 的音乐目录。</li>
 * </ul>
 *
 * <p>所有网络与磁盘 IO 都在 daemon 线程上执行，渲染帧只读 volatile 状态，
 * 不阻塞游戏主线程。功能默认可通过 onlineMusicEnabled 关闭，且不含任何内容分发。
 * 音频版权归各平台与权利人所有，请仅用于个人学习与已购内容的备份。
 */
public final class OnlineMusic {

    private OnlineMusic() {}

    public enum Phase { IDLE, SEARCHING, RESULTS, DOWNLOADING, DONE, ERROR }

    public static final class Song {
        public final String name, artist, album, source, id, sign;
        Song(String name, String artist, String album, String source, String id, String sign) {
            this.name = name; this.artist = artist; this.album = album;
            this.source = source; this.id = id; this.sign = sign;
        }
        public String display() {
            return (artist.isEmpty() ? name : artist + " - " + name);
        }
    }

    public static final class State {
        public volatile Phase phase = Phase.IDLE;
        public volatile String message = "";
        public volatile String detail = "";
        public volatile double progress = 0.0;
        public volatile List<Song> songs = new ArrayList<>();
        public volatile long stamp = 0L;
    }

    private static final State STATE = new State();
    private static volatile boolean cancelled;
    private static final String API = "https://yinyueku.cn/api.php";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) PvPOptimize/1.3";
    private static final char[] ILLEGAL = {'<', '>', ':', '"', '/', '\\', '|', '?', '*'};

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static State state() { return STATE; }

    private static void set(Phase p, String msg) {
        STATE.phase = p;
        STATE.message = msg;
        STATE.stamp = System.nanoTime();
    }

    public static void cancel() {
        cancelled = true;
        set(Phase.IDLE, Ui.tr("gui.pvp_optimize.online.cancelled"));
    }

    /** 关键词搜索（异步）。 */
    public static void search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) { set(Phase.IDLE, Ui.tr("gui.pvp_optimize.online.need_keyword")); return; }
        if (!PvPOptimizeConfig.get().onlineMusicEnabled) {
            set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.disabled")); return;
        }
        if (STATE.phase == Phase.SEARCHING || STATE.phase == Phase.DOWNLOADING) return;
        cancelled = false;
        set(Phase.SEARCHING, Ui.tr("gui.pvp_optimize.online.searching"));
        STATE.songs = new ArrayList<>();
        thread(() -> {
            try {
                int limit = PvPOptimizeConfig.get().onlineSearchLimit;
                String url = API + "?" + params(
                        "types", "search", "name", q, "count", String.valueOf(limit),
                        "pages", "1", "source", "netease");
                String body = fetch(url, 15);
                JsonElement el = JsonParser.parseString(body);
                List<Song> out = new ArrayList<>();
                if (el.isJsonArray()) {
                    for (JsonElement e : el.getAsJsonArray()) {
                        if (!e.isJsonObject()) continue;
                        JsonObject o = e.getAsJsonObject();
                        String name = str(o, "name");
                        String id = str(o, "url_id");
                        if (id.isEmpty()) id = str(o, "id");
                        if (name.isEmpty() || id.isEmpty()) continue;
                        String artist = joinArtists(o.get("artist"));
                        out.add(new Song(name, artist, str(o, "album"), "netease", id, str(o, "sign")));
                    }
                }
                STATE.songs = out;
                STATE.progress = 0;
                set(out.isEmpty() ? Phase.ERROR : Phase.RESULTS,
                        out.isEmpty() ? Ui.tr("gui.pvp_optimize.online.no_result")
                                : String.format(Locale.ROOT, "%s %d",
                                        Ui.tr("gui.pvp_optimize.online.found"), out.size()));
            } catch (Exception ex) {
                fail("search", ex);
            }
        });
    }

    /** 下载搜索结果中的一首（异步）。 */
    public static void download(Song s) {
        if (s == null || STATE.phase == Phase.DOWNLOADING || STATE.phase == Phase.SEARCHING) return;
        cancelled = false;
        STATE.progress = 0;
        set(Phase.DOWNLOADING, Ui.tr("gui.pvp_optimize.online.resolving"));
        thread(() -> {
            try {
                String url = API + "?" + params(
                        "types", "url", "source", "netease", "id", s.id,
                        "sign", s.sign, "br", "128");
                String body = fetch(url, 15);
                JsonObject o = JsonParser.parseString(body).getAsJsonObject();
                String direct = o.has("url") && o.get("url").isJsonPrimitive()
                        ? o.get("url").getAsString() : "";
                if (!direct.startsWith("http")) {
                    throw new IllegalStateException(Ui.tr("gui.pvp_optimize.online.no_url") + " " + direct);
                }
                save(direct, (s.artist.isEmpty() ? "" : s.artist + " - ") + s.name, s.id);
            } catch (Exception ex) {
                fail("download", ex);
            }
        });
    }

    /** 直链导入：可粘贴 LenoMusic / 任意下载器给出的音频直链。 */
    public static void downloadUrl(String url) {
        String u = url == null ? "" : url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.bad_url")); return;
        }
        if (STATE.phase == Phase.DOWNLOADING || STATE.phase == Phase.SEARCHING) return;
        cancelled = false;
        STATE.progress = 0;
        set(Phase.DOWNLOADING, Ui.tr("gui.pvp_optimize.online.downloading"));
        thread(() -> {
            try {
                String name = baseNameFromUrl(u);
                save(u, name.isEmpty() ? "online-track" : name, null);
            } catch (Exception ex) {
                fail("url", ex);
            }
        });
    }

    /** 把 LenoMusic（或任意下载器）的输出目录里的音频与歌词整批导入。 */
    public static void importFolder(String dir) {
        if (STATE.phase == Phase.DOWNLOADING || STATE.phase == Phase.SEARCHING) return;
        String text = dir == null ? "" : dir.trim();
        if (text.isEmpty()) text = PvPOptimizeConfig.get().lenoMusicDir;
        if (text.isEmpty()) {
            set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.need_dir")); return;
        }
        Path src;
        try {
            src = Paths.get(text.replace("\"", ""));
        } catch (Exception ex) {
            set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.bad_dir")); return;
        }
        if (!Files.isDirectory(src)) {
            set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.bad_dir")); return;
        }
        PvPOptimizeConfig.get().lenoMusicDir = text;
        PvPOptimizeConfig.save();
        cancelled = false;
        set(Phase.DOWNLOADING, Ui.tr("gui.pvp_optimize.online.importing"));
        thread(() -> {
            int copied = 0, skipped = 0;
            try {
                Path dest = MusicLibrary.musicDir();
                Files.createDirectories(dest);
                List<Path> files = new ArrayList<>();
                try (Stream<Path> walk = Files.walk(src, 4)) {
                    walk.filter(Files::isRegularFile).filter(OnlineMusic::isSupportedPath)
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .forEach(files::add);
                }
                for (Path p : files) {
                    if (cancelled || copied + skipped > 300) break;
                    Path target = dest.resolve(p.getFileName().toString());
                    if (Files.exists(target)) { skipped++; continue; }
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    String stem = p.getFileName().toString();
                    int dot = stem.lastIndexOf('.');
                    if (dot > 0) {
                        Path lrc = p.resolveSibling(stem.substring(0, dot) + ".lrc");
                        if (Files.isRegularFile(lrc)) {
                            Files.copy(lrc, dest.resolve(stem.substring(0, dot) + ".lrc"),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    copied++;
                    STATE.progress = Math.min(1.0, (copied + skipped) / (double) Math.max(1, files.size()));
                }
                MusicPlayer.refreshLibrary();
                STATE.progress = 1;
                set(Phase.DONE, String.format(Locale.ROOT, "%s %d / %s %d",
                        Ui.tr("gui.pvp_optimize.online.imported"), copied,
                        Ui.tr("gui.pvp_optimize.online.skipped"), skipped));
            } catch (Exception ex) {
                fail("import", ex);
            }
        });
    }

    // ===== 内部 =====

    private static void save(String url, String rawName, String lyricId) throws Exception {
        String ext = extension(url);
        if (!ext.equals("mp3") && !ext.equals("wav") && !ext.equals("ogg")) {
            throw new IllegalStateException(Ui.tr("gui.pvp_optimize.online.unsupported") + " ." + ext);
        }
        Path dir = MusicLibrary.musicDir();
        Files.createDirectories(dir);
        String base = sanitize(rawName);
        if (base.isEmpty()) base = "online-" + System.currentTimeMillis();
        if (base.length() > 90) base = base.substring(0, 90);
        Path target = dir.resolve(base + "." + ext);
        Path tmp = dir.resolve(base + "." + ext + ".part");
        STATE.detail = base + "." + ext;
        set(Phase.DOWNLOADING, Ui.tr("gui.pvp_optimize.online.downloading"));

        HttpRequest req = request(url, Duration.ofSeconds(90));
        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        long total = resp.headers().firstValueAsLong("content-length").orElse(-1L);
        long read = 0;
        byte[] buf = new byte[16384];
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tmp)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                if (cancelled) break;
                out.write(buf, 0, n);
                read += n;
                STATE.progress = total > 0 ? Math.min(1.0, read / (double) total)
                        : Math.min(0.95, STATE.progress + 0.002);
            }
        }
        if (cancelled || read < 2048) {
            Files.deleteIfExists(tmp);
            if (cancelled) { set(Phase.IDLE, Ui.tr("gui.pvp_optimize.online.cancelled")); return; }
            throw new IllegalStateException(Ui.tr("gui.pvp_optimize.online.too_small"));
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

        // 歌词：网易云公开接口，转标准 LRC 落盘（失败不影响音频）
        if (lyricId != null && !lyricId.isEmpty()) {
            try {
                String lrcUrl = "https://music.163.com/api/song/lyric?id="
                        + URLEncoder.encode(lyricId, StandardCharsets.UTF_8) + "&lv=1&kv=1&tv=-1";
                String lrcJson = fetch(lrcUrl, 12);
                JsonObject o = JsonParser.parseString(lrcJson).getAsJsonObject();
                if (o.has("lrc") && o.get("lrc").isJsonObject()) {
                    String lrc = str(o.getAsJsonObject("lrc"), "lyric");
                    if (!lrc.isBlank()) {
                        Files.writeString(dir.resolve(base + ".lrc"), lrc, StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception ignored) {
                PvPOptimize.LOGGER.debug("[PvP-Optimize] 在线歌词获取失败（不影响下载）");
            }
        }
        MusicPlayer.refreshLibrary();
        STATE.progress = 1;
        set(Phase.DONE, Ui.tr("gui.pvp_optimize.online.saved") + "  " + base + "." + ext);
    }

    private static String fetch(String url, int seconds) throws Exception {
        HttpRequest req = request(url, Duration.ofSeconds(seconds));
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    private static HttpRequest request(String url, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", UA)
                .header("Accept", "*/*")
                .header("Referer", "https://yinyueku.cn/")
                .GET().build();
    }

    private static String params(String... kv) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (i > 0) b.append('&');
            b.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return b.toString();
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
        return o.get(key).getAsString().trim();
    }

    private static String joinArtists(JsonElement e) {
        if (e == null || !e.isJsonArray()) return "";
        JsonArray a = e.getAsJsonArray();
        StringBuilder b = new StringBuilder();
        for (JsonElement x : a) {
            if (!x.isJsonPrimitive()) continue;
            if (b.length() > 0) b.append(" / ");
            b.append(x.getAsString());
        }
        return b.toString();
    }

    private static String extension(String url) {
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot < path.lastIndexOf('/')) return "";
        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isSupportedPath(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg");
    }

    private static String baseNameFromUrl(String url) {
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        return name;
    }

    private static String sanitize(String name) {
        String s = name.replace('\u0000', ' ').trim();
        for (char c : ILLEGAL) s = s.replace(c, '_');
        s = s.replaceAll("[\\x00-\\x1F]", " ").trim();
        while (s.endsWith(".") || s.endsWith(" ")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static void fail(String where, Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) msg = ex.getClass().getSimpleName();
        PvPOptimize.LOGGER.warn("[PvP-Optimize] 在线音乐 {} 失败: {}", where, msg);
        STATE.progress = 0;
        set(Phase.ERROR, Ui.tr("gui.pvp_optimize.online.failed") + "  " + msg);
    }

    private static void thread(Runnable r) {
        Thread t = new Thread(r, "pvp-optimize-online-music");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
