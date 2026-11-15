package com.pvp.optimize.perf;

/**
 * HUD 各部分的实测耗时（毫秒），用来判断「掉帧」到底是谁花的。
 *
 * <p>只做一件事：渲染线程上每帧对 {@code System.nanoTime()} 取两次差值累加，
 * 2 秒窗口滑动平均，零分配、无锁（单线程读写，字段用普通 long 即可）。
 *
 * <p>为什么需要它：{@code 灵动岛（Dango 官方 GLSL 玻璃）}、{@code 歌词面板}、
 * {@code 药水面板} 三块都是每帧绘制的，光看帧率无法区分谁的占比大；把这三项写进
 * 诊断日志 / 主控板，就能判断「是不是该关灵动岛玻璃」「是不是歌词行数太宽」，
 * 而不是靠猜。
 */
public final class HudCost {

    private HudCost() {}

    private static final int WINDOW = 40;   // 约 2 秒 @20 次/秒的采样点（每帧只更新当前桶）

    // 三段：0 灵动岛 / 1 歌词 / 2 药水（含其它自绘 HUD）
    private static final long[][] BUCKET_NS = new long[3][WINDOW];
    private static final int[] CURSOR = new int[3];
    private static final int[] COUNT = new int[3];
    private static final long[] SUM_NS = new long[3];
    private static final long[] FRAME_NS = new long[3];

    // 当前帧正在计时的段（-1 = 没在计）
    private static int open = -1;
    private static long openAt;

    /** 开始计时某一段。 */
    public static void begin(int slot) {
        if (slot < 0 || slot >= 3) return;
        open = slot;
        openAt = System.nanoTime();
    }

    /** 结束计时，把耗时记进这一段。 */
    public static void end() {
        int slot = open;
        if (slot < 0) return;
        open = -1;
        long now = System.nanoTime();
        long delta = now - openAt;
        if (delta < 0L) delta = 0L;
        if (delta > 200_000_000L) delta = 200_000_000L;   // 超过 200ms 视为卡顿/切屏，不计入平均
        FRAME_NS[slot] += delta;
    }

    /** 每帧 HUD 回调结尾调用：把本帧累计值收进 2 秒窗口（每段只写一个样本）。 */
    public static void frameDone() {
        for (int i = 0; i < 3; i++) {
            long v = FRAME_NS[i];
            FRAME_NS[i] = 0L;
            SUM_NS[i] += v - BUCKET_NS[i][CURSOR[i]];
            BUCKET_NS[i][CURSOR[i]] = v;
            CURSOR[i] = (CURSOR[i] + 1) % WINDOW;
            if (COUNT[i] < WINDOW) COUNT[i]++;
        }
    }

    /** 该段的平均耗时（毫秒）。 */
    public static double ms(int slot) {
        if (slot < 0 || slot >= 3 || COUNT[slot] == 0) return 0.0;
        return SUM_NS[slot] / 1_000_000.0 / COUNT[slot];
    }

    /** 三段总耗时（毫秒）。 */
    public static double totalMs() {
        return ms(0) + ms(1) + ms(2);
    }

    /** 一行诊断文本，例如 {@code 灵动岛 0.42ms 歌词 0.06ms 药水 0.03ms}。 */
    public static String summary() {
        return String.format(java.util.Locale.ROOT, "灵动岛 %.2fms 歌词 %.2fms 药水 %.2fms",
                ms(0), ms(1), ms(2));
    }

    public static void reset() {
        for (int i = 0; i < 3; i++) {
            java.util.Arrays.fill(BUCKET_NS[i], 0L);
            CURSOR[i] = 0;
            COUNT[i] = 0;
            SUM_NS[i] = 0L;
            FRAME_NS[i] = 0L;
        }
        open = -1;
    }
}
