package com.pvp.optimize.perf;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 群体检测器:
 *   - 每 10 个 tick 统计一次 32 格内玩家数
 *   - 实时读取 MinecraftClient.getCurrentFps() 判断是否掉帧
 *
 * 用于"近距离 6+ 玩家聚集 + 掉帧"时自动降低渲染复杂度 (隐藏皮肤层, 保留名字)。
 *
 * 线程安全: 全部字段都是 volatile / Atomic*, Render thread 读写安全。
 */
public final class CrowdDetector {

    private CrowdDetector() {}

    /** 32 格内玩家数 (不含自己) */
    private static final AtomicInteger NEARBY_PLAYER_COUNT = new AtomicInteger(0);

    /** 上次更新时的 tick 数, 用于节流 */
    private static volatile int lastUpdateTick = -1;

    public static int getNearbyPlayerCount() {
        return NEARBY_PLAYER_COUNT.get();
    }

    /**
     * 当前帧是否处于掉帧状态 (fps < 阈值)。
     * 直接读取 MinecraftClient.getCurrentFps(), Render thread 同步执行, 无锁。
     */
    public static boolean isLagging(double fpsThreshold) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        int fps = mc.getCurrentFps();
        return fps > 0 && fps < fpsThreshold;
    }

    /**
     * 综合判断: 是否触发"群体隐身"模式 (隐藏皮肤层, 保留名字)。
     *
     * @return true 当 6+ 玩家聚集 且 当前帧 fps 低于阈值
     */
    public static boolean shouldHideSkinInCrowd(int playerThreshold, double fpsThreshold) {
        return NEARBY_PLAYER_COUNT.get() >= playerThreshold && isLagging(fpsThreshold);
    }

    /**
     * 注册 tick 监听 (在 mod 入口调用一次)。
     * 每 10 tick 统计一次, 避免每 tick 都遍历玩家列表。
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.world == null) return;
            int tick = client.player.age; // age 每 tick +1, 作为节流时钟
            if (tick - lastUpdateTick < 10) return;
            lastUpdateTick = tick;

            World world = client.world;
            double selfX = client.player.getX();
            double selfY = client.player.getY();
            double selfZ = client.player.getZ();
            double r = 32.0;
            double rSq = r * r;

            int count = 0;
            for (PlayerEntity p : world.getPlayers()) {
                if (p == client.player) continue;
                double dx = p.getX() - selfX;
                double dy = p.getY() - selfY;
                double dz = p.getZ() - selfZ;
                if (dx*dx + dy*dy + dz*dz <= rSq) {
                    count++;
                }
            }
            NEARBY_PLAYER_COUNT.set(count);
        });
    }
}