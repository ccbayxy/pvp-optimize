package com.pvp.optimize.dango;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.music.MusicPlayer;

import java.lang.reflect.Method;

/**
 * 本地播放器 与 DangoMusic 内置播放器 的<b>互斥仲裁</b>。
 *
 * <p>两个播放器各有自己的音频线程，谁也不认识谁，所以必须显式协调，否则会出现：
 * <ul>
 *   <li><b>两首歌同时响</b> —— 一个在放 Dango 在线曲目，另一个在放本地文件；</li>
 *   <li><b>本地音乐上不了岛</b> —— Dango 的 {@code MusicPlayer.getShownTrack()} 优先返回
 *       它自己的 {@code current}（只要状态是 PLAYING / PAUSED / LOADING 就算「活跃」），
 *       {@code SystemMedia} 外部音源通道只有在它<b>完全不活跃</b>时才会被采用。
 *       于是「暂停 Dango 音乐后再放本地音乐」时，灵动岛仍然显示那首被暂停的 Dango 歌。</li>
 * </ul>
 *
 * <p>解法（优雅降级，全部反射，Dango 不在就什么都不做）：
 * <ol>
 *   <li>本地开始出声 → 调 Dango 的 {@code MusicPlayer.INSTANCE.stop()}。
 *       {@code stop()} 会把 {@code current} 置空、状态置 STOPPED，于是
 *       {@code getActive()} 变 false，外部音源通道立刻接管，灵动岛换成我们的本地曲目；
 *       同时音频行被关掉，不会两首歌一起响。</li>
 *   <li>用户在 Dango 界面点了播放（状态由非 PLAYING 变成 PLAYING）→ 暂停本地播放器，
 *       把声音让给 Dango，灵动岛自然显示 Dango 的歌。</li>
 * </ol>
 * 两条都是<b>边沿触发</b>，不会每 tick 互相打架。
 */
public final class DangoMusicArbiter {

    private DangoMusicArbiter() {
    }

    private static boolean ready;
    private static Object player;
    private static Method mGetState;
    private static Method mGetActive;
    private static Method mStop;

    private static boolean prevLocal;
    private static boolean prevDango;

    public static void init() {
        try {
            Class<?> cls = Class.forName("me.sorihiko.dango.music.MusicPlayer");
            player = cls.getField("INSTANCE").get(null);
            mGetState = cls.getMethod("getState");
            mGetActive = cls.getMethod("getActive");
            mStop = cls.getMethod("stop");
            ready = true;
            PvPOptimize.LOGGER.info("[PvP-Optimize] 本地/Dango 播放器互斥仲裁已就绪。");
        } catch (Throwable t) {
            ready = false;
            PvPOptimize.LOGGER.warn("[PvP-Optimize] 播放器仲裁不可用（Dango 未加载？）: {}", t.toString());
        }
    }

    /** 每客户端 tick 调用一次。 */
    public static void tick() {
        if (!ready) return;
        boolean local = MusicPlayer.isPlaying();
        boolean dangoPlaying;
        boolean dangoActive;
        try {
            dangoPlaying = "PLAYING".equals(stateName());
            dangoActive = (Boolean) mGetActive.invoke(player);
        } catch (Throwable t) {
            ready = false;
            return;
        }

        // 本地刚开始出声但上一 tick 还没在放：补一次抢占（正常路径已由 notifyLocalPlay 处理，
        // 这里兜住「Dango 是在本地开播之后才加载完成」的竞态）
        if (local && !prevLocal && dangoActive) {
            stopDango();
        }

        // 用户在 Dango 界面按了播放：让本地让路，避免两首歌叠在一起
        if (dangoPlaying && !prevDango && local) {
            MusicPlayer.setPaused(true);
            PvPOptimize.LOGGER.info("[PvP-Optimize] DangoMusic 开始播放，本地播放器已暂停。");
            local = false;
        }

        prevLocal = local;
        prevDango = dangoPlaying;
    }

    /**
     * 本地播放器开播时立即调用（由 {@code MusicPlayer.playIndex} 触发），
     * 让 Dango 的音频行当场关掉 —— 而不是等下一 tick，省掉几十毫秒的重叠。
     */
    public static void notifyLocalPlay() {
        if (!ready) return;
        try {
            if ((Boolean) mGetActive.invoke(player)) stopDango();
        } catch (Throwable t) {
            ready = false;
        }
    }

    private static void stopDango() {
        try {
            mStop.invoke(player);
            PvPOptimize.LOGGER.info("[PvP-Optimize] 本地音乐开始播放，DangoMusic 播放器已停止（灵动岛交还本地曲目）。");
        } catch (Throwable t) {
            ready = false;
        }
    }

    private static String stateName() throws Exception {
        Object state = mGetState.invoke(player);
        return state == null ? "STOPPED" : state.toString();
    }

    /** 供启动诊断。 */
    public static String status() {
        if (!ready) return "不可用";
        try {
            return stateName() + (Boolean.TRUE.equals(mGetActive.invoke(player)) ? "/活跃" : "/空闲");
        } catch (Throwable t) {
            return "异常";
        }
    }

    /** 状态边沿缓存清空（退出世界 / 重载时用，避免把上一次的边沿当成新事件）。 */
    public static void reset() {
        prevLocal = false;
        prevDango = false;
    }
}
