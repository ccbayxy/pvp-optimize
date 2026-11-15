package com.pvp.optimize.music;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.gui.ControlPanelScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Path;

/**
 * 播放引擎。解码与音频输出全部跑在一条 daemon 线程上，主线程只读写 volatile 状态，
 * 因此低配机器上不会因为解码或 SourceDataLine.write 卡住渲染帧。
 */
public final class MusicPlayer {

    private MusicPlayer() {}

    public enum Status { STOPPED, PLAYING, PAUSED }

    private static final Object LOCK = new Object();
    private static final int BUFFER_BYTES = 16384;

    private static volatile Thread worker;
    private static volatile boolean stopRequested;
    private static volatile boolean paused;
    private static volatile boolean switchRequested;
    private static volatile double pendingSeek = -1.0;
    private static volatile double resumeOnStart = -1.0;
    private static volatile long generation;
    private static volatile int volume = 70;
    private static volatile Status status = Status.STOPPED;
    private static volatile Path currentPath;
    private static volatile double cachedPosition;
    private static volatile double cachedDuration;
    private static volatile String lastError;
    private static volatile int deferredTicks = -1;

    // ==================== 对外 API ====================

    public static void register() {
        MusicLibrary.refresh();
        MusicLibrary.State s = MusicLibrary.loadState();
        // 音量以 pvp_optimize.json 为唯一来源：迁移后的感知曲线值不会被旧播放记忆覆盖。
        volume = clampVolume(PvPOptimizeConfig.get().musicVolume);
        if (s != null) {
            MusicLibrary.setShuffle(s.shuffle);
            try { MusicLibrary.setRepeat(MusicLibrary.Repeat.valueOf(s.repeat)); }
            catch (Exception ignored) { }
            if (s.autoPlay && s.lastTrack != null) {
                for (int i = 0; i < MusicLibrary.tracks().size(); i++) {
                    if (MusicLibrary.tracks().get(i).getFileName().toString().equals(s.lastTrack)) {
                        MusicLibrary.setIndex(i);
                        resumeOnStart = Math.max(0, s.position);
                        deferredTicks = 20;   // 等音频设备与游戏就绪
                        break;
                    }
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (deferredTicks > 0 && --deferredTicks == 0) {
                startWorker();
                switchRequested = true;
                status = Status.PLAYING;
            }
            handleKeys();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    private static void handleKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!PvPOptimizeConfig.get().musicPlayerEnabled) return;

        while (PvPOptimizeConfig.OPEN_MUSIC.wasPressed()) {
            if (mc.currentScreen == null) openGui();
        }
        while (PvPOptimizeConfig.MUSIC_PLAY_PAUSE.wasPressed()) toggle();
        while (PvPOptimizeConfig.MUSIC_NEXT.wasPressed()) next();
        while (PvPOptimizeConfig.MUSIC_PREV.wasPressed()) previous();
    }

    /** 打开统一控制台并直接跳到音乐页（M 键 / 底部「音乐」按钮共用）。 */
    public static void openGui() {
        ControlPanelScreen.open(ControlPanelScreen.TAB_MUSIC);
    }

    public static void playIndex(int i) {
        MusicLibrary.setIndex(i);
        Path p = MusicLibrary.current();
        if (p == null) return;
        currentPath = p;
        pendingSeek = -1.0;
        cachedPosition = 0;
        paused = false;
        startWorker();
        switchRequested = true;
        status = Status.PLAYING;
        synchronized (LOCK) { LOCK.notifyAll(); }
        // 与 DangoMusic 内置播放器互斥：本地开播立刻停掉它，否则两首歌一起响，
        // 而且灵动岛会一直显示 Dango 那一首（见 DangoMusicArbiter）。
        com.pvp.optimize.dango.DangoMusicArbiter.notifyLocalPlay();
    }

    public static void playCurrent() {
        if (MusicLibrary.current() == null && !MusicLibrary.tracks().isEmpty()) {
            MusicLibrary.setIndex(0);
        }
        Path p = MusicLibrary.current();
        if (p != null) playIndex(MusicLibrary.currentIndex());
    }

    public static void toggle() {
        if (status == Status.STOPPED) { playCurrent(); return; }
        setPaused(status == Status.PLAYING);
    }

    public static void setPaused(boolean v) {
        if (status == Status.STOPPED) return;
        paused = v;
        status = v ? Status.PAUSED : Status.PLAYING;
        synchronized (LOCK) { LOCK.notifyAll(); }
        if (!v) com.pvp.optimize.dango.DangoMusicArbiter.notifyLocalPlay();
    }

    public static void next() {
        MusicLibrary.advance(true);
        playIndex(MusicLibrary.currentIndex());
    }

    public static void previous() {
        MusicLibrary.previous();
        playIndex(MusicLibrary.currentIndex());
    }

    public static void stop() {
        generation++;                 // 让旧 worker 自然退出，不影响之后新建的 worker
        stopRequested = true;
        paused = false;
        status = Status.STOPPED;
        currentPath = null;
        synchronized (LOCK) { LOCK.notifyAll(); }
        Thread t = worker;
        if (t != null) t.interrupt();
        worker = null;
    }

    public static void seek(double seconds) {
        if (status == Status.STOPPED) return;
        pendingSeek = Math.max(0, seconds);
        cachedPosition = pendingSeek;
        synchronized (LOCK) { LOCK.notifyAll(); }
    }

    public static void seekFraction(double f) {
        double d = duration();
        if (d > 0) seek(Math.max(0, Math.min(1, f)) * d);
    }

    public static int volume() { return volume; }

    public static void setVolume(int v) {
        volume = clampVolume(v);
        PvPOptimizeConfig.get().musicVolume = volume;
    }

    private static int clampVolume(int v) { return Math.max(0, Math.min(100, v)); }

    public static Status status() { return status; }
    public static boolean isPlaying() { return status == Status.PLAYING; }
    /** 自动让路原版背景音乐用：真正在出声才算。 */
    public static boolean isActive() {
        return PvPOptimizeConfig.get().musicPlayerEnabled
                && status == Status.PLAYING && currentPath != null;
    }
    public static double position() { return cachedPosition; }
    public static double duration() { return cachedDuration; }
    public static String trackName() { return MusicLibrary.displayTitle(currentPath); }
    public static Path trackPath() { return currentPath; }
    public static String error() { return lastError; }
    public static void clearError() { lastError = null; }

    public static MusicLibrary.Lrc lyrics() { return MusicLibrary.lyricsFor(currentPath); }

    public static void refreshLibrary() { MusicLibrary.refresh(); }

    public static String statusText() {
        return switch (status) {
            case PLAYING -> "▶";
            case PAUSED -> "❚❚";
            default -> "■";
        };
    }

    // ==================== 播放线程 ====================

    private static void startWorker() {
        Thread t = worker;
        if (t != null && t.isAlive()) return;
        long g = ++generation;
        stopRequested = false;
        t = new Thread(() -> run(g), "PvP-Optimize-Music");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        worker = t;
        t.start();
    }

    private static void run(long myGeneration) {
        AudioStream stream = null;
        SourceDataLine line = null;
        byte[] buf = new byte[BUFFER_BYTES];

        try {
            while (!stopRequested && myGeneration == generation) {
                if (paused) {
                    if (line != null) line.stop();
                    synchronized (LOCK) {
                        while (paused && !stopRequested && !switchRequested
                                && myGeneration == generation) {
                            try { LOCK.wait(200); } catch (InterruptedException ignored) { }
                        }
                    }
                    if (line != null) line.start();
                    continue;
                }

                double seekTo = pendingSeek;
                if (seekTo >= 0) {
                    pendingSeek = -1.0;
                    if (stream != null) {
                        try { stream.seek(seekTo); } catch (Exception ignored) { }
                    }
                }

                if (switchRequested || stream == null) {
                    switchRequested = false;
                    closeStream(stream);
                    closeLine(line);
                    stream = null; line = null;
                    Path p = MusicLibrary.current();
                    if (p == null) { idleWait(); continue; }
                    currentPath = p;
                    try {
                        stream = AudioStream.open(p);
                        AudioFormat fmt = stream.format();
                        line = openLine(fmt);
                        cachedDuration = stream.durationSeconds();
                        cachedPosition = stream.positionSeconds();
                        lastError = null;
                        PvPOptimize.LOGGER.info("[音乐] 播放: {}", p.getFileName());
                    } catch (Exception e) {
                        lastError = e.getMessage();
                        PvPOptimize.LOGGER.warn("[音乐] 无法播放 {}: {}", p.getFileName(), e.toString());
                        closeStream(stream); closeLine(line);
                        stream = null; line = null;
                        // 跳过坏文件，避免卡死循环
                        if (!MusicLibrary.tracks().isEmpty()) MusicLibrary.advance(true);
                        switchRequested = true;
                        continue;
                    }
                    // 播放记忆：只在启动后第一次开流时恢复进度
                    double resume = resumeOnStart;
                    if (resume > 0) {
                        resumeOnStart = -1.0;
                        try { stream.seek(resume); cachedPosition = resume; }
                        catch (Exception ignored) { }
                    }
                }

                int n;
                try { n = stream.read(buf); }
                catch (Exception e) { n = -1; }

                if (n < 0) {
                    Path next = MusicLibrary.advance(false);
                    if (next == null) {
                        status = Status.STOPPED;
                        currentPath = null;
                        idleWait();
                    } else {
                        currentPath = next;
                    }
                    switchRequested = true;
                    continue;
                }
                if (n > 0) {
                    applyVolume(buf, n, volume, stream.format());
                    try { line.write(buf, 0, n); }
                    catch (Exception e) { break; }
                    cachedPosition = stream.positionSeconds();
                }
            }
        } finally {
            closeStream(stream);
            closeLine(line);
            if (myGeneration == generation) status = Status.STOPPED;
        }
    }

    private static SourceDataLine openLine(AudioFormat fmt) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
        int buffer = (int) (fmt.getFrameRate() * Math.max(1, fmt.getFrameSize()) * 0.25);
        buffer = Math.max(4096, Math.min(65536, buffer));
        l.open(fmt, buffer);
        l.start();
        return l;
    }

    /**
     * 软件音量：只对 16-bit 小端 PCM 生效，跨格式统一、不依赖声卡控件。
     *
     * <p>采用「感知 / 音频电位器」曲线 {@code gain = MASTER * (v/100)^2}，并带总上限
     * {@link PvPOptimizeConfig#MUSIC_MASTER_GAIN}。旧版是线性 {@code v/100}，音乐母带
     * 接近 0 dBFS，而游戏音效远低于此，导致 70% 就完全盖住游戏、只有 3% 能正常游玩。
     * 现在 100% 也只有 0.30 增益（约 -10.5 dB），常用区间 20~60% 细腻可控。
     */
    private static void applyVolume(byte[] buf, int len, int vol, AudioFormat fmt) {
        if (fmt.getSampleSizeInBits() != 16 || fmt.isBigEndian()) return;
        float t = Math.max(0f, Math.min(1f, vol / 100.0f));
        float gain = PvPOptimizeConfig.MUSIC_MASTER_GAIN * t * t;
        if (gain <= 0f) {
            java.util.Arrays.fill(buf, 0, len, (byte) 0);
            return;
        }
        if (gain >= 1f) return;
        for (int i = 0; i + 1 < len; i += 2) {
            int s = (buf[i] & 0xFF) | (buf[i + 1] << 8);
            if (s > 32767) s -= 65536;
            s = (int) (s * gain);
            s = Math.max(-32768, Math.min(32767, s));
            buf[i] = (byte) (s & 0xFF);
            buf[i + 1] = (byte) ((s >> 8) & 0xFF);
        }
    }

    private static void idleWait() {
        synchronized (LOCK) {
            try { LOCK.wait(500); } catch (InterruptedException ignored) { }
        }
    }

    private static void closeStream(AudioStream s) {
        if (s != null) { try { s.close(); } catch (Exception ignored) { } }
    }

    private static void closeLine(SourceDataLine l) {
        if (l == null) return;
        try { l.stop(); } catch (Exception ignored) { }
        try { l.flush(); } catch (Exception ignored) { }
        try { l.close(); } catch (Exception ignored) { }
    }

    public static void shutdown() {
        MusicLibrary.State s = new MusicLibrary.State();
        s.lastTrack = currentPath == null ? null : currentPath.getFileName().toString();
        s.position = cachedPosition;
        s.volume = volume;
        s.shuffle = MusicLibrary.isShuffle();
        s.repeat = MusicLibrary.repeat().name();
        s.autoPlay = PvPOptimizeConfig.get().musicAutoPlay;
        MusicLibrary.saveState(s);
        stop();
    }
}
