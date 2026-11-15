package com.pvp.optimize.perf;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.MinecraftClient;

/**
 * 激进优化统一调度控制器。
 *
 * 1) 总开关 {@code aggressiveEnabled}: 关闭则所有激进优化一律失效。
 * 2) 不做帧率自适应门控: 各优化只要自己开关打开就始终生效。
 *
 * 距离判断统一走"本地玩家到目标"的平方距离, 与实体剔除保持一致。
 */
public final class AggressiveOptimizer {

    private AggressiveOptimizer() {}

    public static boolean isActive() {
        return PvPOptimizeConfig.get().aggressiveEnabled;
    }

    public static boolean isEnabled(boolean toggle) {
        RuntimeGuard.sampleIfNeeded();
        return toggle && isActive();
    }

    public static double squaredDistanceTo(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return 0.0;
        return mc.player.squaredDistanceTo(x, y, z);
    }

    /** 距离是否超出阈值; 帧率自适应 + 内存/GC 压力都会缩小阈值。 */
    public static boolean isBeyond(double distSq, double configuredDistance) {
        double d = configuredDistance * AdaptivePerformance.scale() * RuntimeGuard.distanceScale();
        return distSq > d * d;
    }
}
