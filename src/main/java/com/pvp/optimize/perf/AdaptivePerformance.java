package com.pvp.optimize.perf;

import com.pvp.optimize.PvPOptimizeConfig;

/**
 * 动态性能自适应。
 *
 * <p>思路借鉴 OpenFine 的 {@code Lagometer}（环形帧时间采样）与 SmoothFPS（分档调节），
 * 但实现完全独立、不碰渲染管线：只输出一个 0..1 的系数, 由各优化功能自己决定怎么用。
 * 这是必要的取舍 —— OpenFine 是 Forge / MC 26.2 / 新 GPU 渲染架构，其区块网格、
 * GL 状态相关优化在本项目（Fabric 1.20.6 + Sodium 0.5.11）里会直接冲突。
 *
 * <p>每帧由 {@code OverlayHud} 的 HudRenderCallback 驱动 {@link #onFrame()}，采样帧间隔
 * 写入环形缓冲，用窗口平均值判定压力档。档位切换带滞回 + 1 秒冷却，避免在临界帧率
 * 上反复抖动导致剔除距离忽大忽小。
 *
 * <p>压力越大 scale 越小 → 剔除距离缩短（剔除更多远处实体/粒子）、每帧粒子预算降低、
 * 动画纹理 tick 变稀。所有消费方都把它当作乘数，互不影响。
 */
public final class AdaptivePerformance {

    private AdaptivePerformance() {}

    private static final int WINDOW = 120;                    // 约 2 秒 @60fps
    private static final long[] FRAME_NS = new long[WINDOW];
    private static final long IGNORE_OVER_NS = 500_000_000L; // 切屏/重载造成的长间隔不计入
    private static final long CHANGE_COOLDOWN_NS = 1_000_000_000L;

    private static int cursor;
    private static int count;
    private static long sumNs;          // 滑动窗口的和，避免每帧 O(WINDOW) 重算
    private static long lastFrameNs;
    private static long lastChangeNs;
    private static int level;          // 0 正常 / 1 轻度掉帧 / 2 重度掉帧
    private static double avgFrameMs;
    private static double scale = 1.0;

    /** 每渲染帧调用一次（HUD 回调是稳定的帧同步点）。 */
    public static void onFrame() {
        long now = System.nanoTime();
        if (lastFrameNs != 0L) {
            long delta = now - lastFrameNs;
            if (delta > 0L && delta < IGNORE_OVER_NS) {
                sumNs += delta - FRAME_NS[cursor];   // 覆盖环形缓冲里的旧样本
                FRAME_NS[cursor] = delta;
                cursor = (cursor + 1) % WINDOW;
                if (count < WINDOW) count++;
            }
        }
        lastFrameNs = now;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.adaptiveEnabled) {
            level = 0;
            scale = 1.0;
            return;
        }
        if (count < 30) return;   // 样本不足，保持默认档

        avgFrameMs = sumNs / 1_000_000.0 / count;

        // 滞回：进入阈值高于退出阈值，中间区间保持当前档
        int target;
        if (avgFrameMs >= 30.0) target = 2;                        // < 33 fps
        else if (avgFrameMs >= 20.0) target = 1;                   // < 50 fps
        else if (level == 2 && avgFrameMs > 22.0) target = 2;
        else if (level >= 1 && avgFrameMs > 15.0) target = 1;
        else target = 0;

        if (target != level && now - lastChangeNs >= CHANGE_COOLDOWN_NS) {
            level = target;
            lastChangeNs = now;
        }

        double min = Math.max(0.4, Math.min(1.0, cfg.adaptiveMinScale));
        scale = switch (level) {
            case 2 -> min;
            case 1 -> (1.0 + min) / 2.0;
            default -> 1.0;
        };
    }

    /** 距离 / 预算的全局乘数，1.0 表示不收缩。 */
    public static double scale() {
        return PvPOptimizeConfig.get().adaptiveEnabled ? scale : 1.0;
    }

    public static int level() { return level; }

    public static double avgFrameMs() { return avgFrameMs; }

    /** 世界切换 / 资源重载后清空历史，避免把加载卡顿带进新世界。 */
    public static void reset() {
        cursor = 0;
        count = 0;
        sumNs = 0L;
        java.util.Arrays.fill(FRAME_NS, 0L);
        lastFrameNs = 0L;
        lastChangeNs = 0L;
        level = 0;
        avgFrameMs = 0.0;
        scale = 1.0;
    }
}
