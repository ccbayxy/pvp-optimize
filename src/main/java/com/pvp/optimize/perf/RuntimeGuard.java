package com.pvp.optimize.perf;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.hud.HudLayout;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * 运行时稳定性守卫: 同类 mod 让位 + 内存/GC 压力采样 + 诊断日志。
 *
 * <p>不注册额外事件、不新增任何 Mixin 注入点。采样由现有渲染热路径
 * ({@code AggressiveOptimizer.isEnabled} / 实体与方块实体渲染 Mixin) 惰性驱动,
 * 内部按时间限频, 绝大多数调用只做一次 {@code System.nanoTime()} 比较便返回。
 */
public final class RuntimeGuard {

    private RuntimeGuard() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("PvP-Optimize");

    // 重叠 mod 只在类加载时检测一次
    private static final boolean ENTITY_CULLING_MOD = loaded("entityculling");
    private static final boolean TILE_ENTITY_MOD =
            loaded("enhancedblockentities") || loaded("moreculling");
    private static final boolean BAD_OPTIMIZATIONS_MOD = loaded("badoptimizations");

    private static final MemoryMXBean MEM = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> GCS =
            ManagementFactory.getGarbageCollectorMXBeans();

    private static final long SAMPLE_INTERVAL_NS = 1_000_000_000L;   // 1 秒采样一次
    private static final long LOG_INTERVAL_NS = 60_000_000_000L;      // 60 秒或压力档变化时写日志
    private static final int PRESSURE_THRESHOLD = 80;                  // 堆占用百分比

    private static long lastSampleNs;
    private static long lastLogNs;
    private static long lastGcTimeMs;
    private static int pressure;          // 0 正常 / 1 偏高 / 2 高压
    private static boolean sampled;
    private static boolean started;

    private static boolean loaded(String id) {
        try {
            return FabricLoader.getInstance().isModLoaded(id);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 启动时立即写一条诊断, 不必等首次渲染采样。 */
    public static void logStartup() {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        LOGGER.info("[诊断] 启动: 重叠mod entityculling={} ebe/moreculling={} badoptimizations={} "
                        + "| 让位={} 实体剔除让位={} 内存自适应={} 日志诊断={}",
                ENTITY_CULLING_MOD, TILE_ENTITY_MOD, BAD_OPTIMIZATIONS_MOD,
                cfg.overlapGuardEnabled, cfg.entityCullingGuardEnabled,
                cfg.memoryAdaptiveEnabled, cfg.diagnosticsLogEnabled);
        // 直接把"哪些剔除当前真的生效"写进日志, 以后不用反编译就能判断是否失效
        LOGGER.info("[诊断] 生效状态: 实体距离剔除={} 方块实体剔除={} 激进优化={}",
                cfg.entityCullingEnabled && !shouldSkipEntityCulling(),
                cfg.aggressiveEnabled && cfg.tileEntityCullingEnabled && !shouldSkipTileEntityCulling(),
                cfg.aggressiveEnabled);
        LOGGER.info("[诊断] 新增优化: 动态自适应={} 粒子距离剔除={} 粒子预算={} 智能纹理动画={}",
                cfg.adaptiveEnabled, cfg.particleDistanceCullEnabled,
                cfg.particleBudgetEnabled, cfg.smartTextureAnimationsEnabled);
        LOGGER.info("[诊断] 暂停界面: 断开连接按钮重定位={} 位置=({},{}) 宽度={}",
                cfg.pauseMenuRelocateEnabled, cfg.pauseMenuExitButtonX, cfg.pauseMenuExitButtonY,
                cfg.pauseMenuExitButtonWidth == 0
                        ? "原版204" : String.valueOf(cfg.pauseMenuExitButtonWidth));
        LOGGER.info("[诊断] DangoMusic: 内置={} 状态={} 界面=官方MusicScreen(M键) 键位=官方ModernKeyBindings | "
                        + "HUD层={}",
                com.pvp.optimize.dango.DangoBridge.DANGO_VERSION,
                com.pvp.optimize.dango.DangoBridge.status(),
                "Dango官方灵动岛(GLSL玻璃) <- 本地曲目由 DangoHudFeed 注入, 与 Dango 播放器互斥");
        LOGGER.info("[诊断] 界面入口: 主控板=H键(纯色横带压暗, 零后处理) HUD布局编辑器=U键(药水+歌词) "
                + "音乐界面=M键(官方Dango)");
        LOGGER.info("[诊断] 歌词面板: 开关={} 行数={} 底板={} Dango在线歌词={}",
                !Boolean.FALSE.equals(cfg.lyricsHudEnabled), cfg.lyricsHudLines,
                !Boolean.FALSE.equals(cfg.lyricsHudBg),
                com.pvp.optimize.dango.DangoLyrics.status());
        LOGGER.info("[诊断] 本地播放器接入灵动岛: 开关={} 状态={} 播放器={} 药水面板={} | 可视化编辑=U键",
                cfg.musicHudEnabled, com.pvp.optimize.dango.DangoHudFeed.status(),
                cfg.musicPlayerEnabled, cfg.potionHudEnabled);
        LOGGER.info("[诊断] 主控板背景: 压暗档={}(0无/1轻/2深) 面板不透明度={}% 播放器仲裁={}",
                cfg.guiBackdrop, cfg.guiGlassOpacity,
                com.pvp.optimize.dango.DangoMusicArbiter.status());
        LOGGER.info("[诊断] 原版背景音乐让路: 配置={} 判定=Dango VanillaMusicHold.active",
                cfg.musicDuckVanilla);
        if (ENTITY_CULLING_MOD && Boolean.TRUE.equals(cfg.entityCullingGuardEnabled)) {
            LOGGER.warn("[诊断] 实体剔除让位已开启且检测到 EntityCulling, 本 mod 的距离剔除已停用; "
                    + "若远处实体仍在渲染, 请关闭「实体剔除让位」。");
        }
    }

    /** 渲染热路径上调用。内部惰性采样, 未到间隔直接返回。 */
    public static void sampleIfNeeded() {
        long now = System.nanoTime();
        if (now - lastSampleNs < SAMPLE_INTERVAL_NS) return;
        lastSampleNs = now;

        MemoryUsage heap = MEM.getHeapMemoryUsage();
        long used = heap.getUsed();
        long max = Math.max(heap.getMax(), 1L);
        int usedPct = (int) (used * 100L / max);

        long gcCount = 0;
        long gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : GCS) {
            gcCount += Math.max(gc.getCollectionCount(), 0);
            gcTimeMs += Math.max(gc.getCollectionTime(), 0);
        }

        // 首次采样只建立基线, 避免把累计 GC 时间误判成一次高压
        long gcDeltaMs = sampled ? (gcTimeMs - lastGcTimeMs) : 0L;
        sampled = true;
        lastGcTimeMs = gcTimeMs;

        int newPressure = usedPct >= PRESSURE_THRESHOLD ? 1 : 0;
        if (gcDeltaMs > 250L) newPressure = 2;   // 1 秒窗口内 GC 停顿超过 250ms
        boolean changed = newPressure != pressure;
        pressure = newPressure;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!Boolean.TRUE.equals(cfg.diagnosticsLogEnabled)) return;
        if (!started || changed || now - lastLogNs >= LOG_INTERVAL_NS) {
            started = true;
            lastLogNs = now;
            LOGGER.info("[诊断] 堆={}/{}MB ({}%) GC累计={}次/{}ms 窗口GC耗时={}ms 内存压力档={} "
                            + "重叠mod=[entityculling:{} ebe/moreculling:{} badoptimizations:{}] "
                            + "让位={} 内存自适应={} | 帧率档={} 帧均={}ms 收缩系数={} | HUD耗时={}",
                    used >> 20, max >> 20, usedPct, gcCount, gcTimeMs, gcDeltaMs, pressure,
                    ENTITY_CULLING_MOD, TILE_ENTITY_MOD, BAD_OPTIMIZATIONS_MOD,
                    cfg.overlapGuardEnabled, cfg.memoryAdaptiveEnabled,
                    AdaptivePerformance.level(),
                    (long) (AdaptivePerformance.avgFrameMs() * 10) / 10.0,
                    AdaptivePerformance.scale(),
                    HudCost.summary());
        }
    }

    /**
     * 实体距离剔除是否与外部 mod 重复。
     *
     * <p>EntityCulling 做的是"视锥 / 遮挡"剔除, 本 mod 做的是"超过配置距离 +
     * PvP 白名单"剔除, 两者互补而非重复 —— 装了 EntityCulling 并不能替代距离剔除。
     * 1.0.6-v1 曾在这里无条件让位, 结果只要整合包装了 EntityCulling, 本 mod 的
     * 实体剔除就 100% 失效 (表现为远处实体照常渲染、帧率无改善)。
     *
     * <p>因此默认不再让位; 只有用户显式开启 entityCullingGuardEnabled 才恢复旧行为。
     */
    public static boolean shouldSkipEntityCulling() {
        return Boolean.TRUE.equals(PvPOptimizeConfig.get().entityCullingGuardEnabled)
                && ENTITY_CULLING_MOD;
    }

    /** 方块实体裁剪是否与外部 mod 重复。 */
    public static boolean shouldSkipTileEntityCulling() {
        return Boolean.TRUE.equals(PvPOptimizeConfig.get().overlapGuardEnabled) && TILE_ENTITY_MOD;
    }

    /**
     * 内存压力自适应后的距离系数。压力大时缩小剔除距离 = 剔除更多对象 =
     * 减少渲染与实体处理, 从而压低分配速率和 GC 频率。自适应关闭时恒为 1.0。
     */
    public static double distanceScale() {
        if (!Boolean.TRUE.equals(PvPOptimizeConfig.get().memoryAdaptiveEnabled)) return 1.0;
        return switch (pressure) {
            case 2 -> 0.5;
            case 1 -> 0.75;
            default -> 1.0;
        };
    }
}
