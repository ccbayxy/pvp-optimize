package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.perf.AdaptivePerformance;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 粒子过滤 v1.0.5
 *
 * Fabric Loader 默认使用 intermediary 命名空间, 运行时 particle.getClass().getName()
 * 返回的是 "net.minecraft.class_657" 这种形式, 而不是 yarn 名 ("DamageParticle")
 * 或 Mojang 混淆短名 ("gae")。本实现以 intermediary 名为准做匹配, 同时 fallback
 * 检查 yarn 命名, 兼容不同运行环境。
 *
 * 任何无法识别的粒子都过滤掉, 这是 PvP 优化 mod 需要的"少粒子"行为。
 */
public final class ParticleFilter {

    private ParticleFilter() {}

    // ===== 类名查询缓存 (避免每粒子每帧调用 Class.getName() 分配新字符串) =====
    private static final Map<Class<?>, String> NAME_CACHE = new ConcurrentHashMap<>();

    private static String nameOf(Particle p) {
        return NAME_CACHE.computeIfAbsent(p.getClass(), Class::getName);
    }

    

    // ===== intermediary 命名空间 (Fabric 默认) =====
    public static final String DAMAGE_PARTICLE_INTERMEDIARY   = "net.minecraft.class_657";   // DamageParticle
    public static final String SWEEP_PARTICLE_INTERMEDIARY    = "net.minecraft.class_645";   // SweepAttackParticle
    public static final String NOTE_PARTICLE_INTERMEDIARY     = "net.minecraft.class_698";   // NoteParticle
    public static final String SPELL_PARTICLE_INTERMEDIARY    = "net.minecraft.class_711";   // SpellParticle
    public static final String SONIC_BOOM_PARTICLE_INTERMEDIARY = "net.minecraft.class_7452"; // SonicBoomParticle

    // ===== yarn 命名空间 (loom 编译期/部分运行环境) =====
    public static final String DAMAGE_PARTICLE_YARN   = "net.minecraft.client.particle.DamageParticle";
    public static final String SWEEP_PARTICLE_YARN    = "net.minecraft.client.particle.SweepAttackParticle";
    public static final String NOTE_PARTICLE_YARN     = "net.minecraft.client.particle.NoteParticle";
    public static final String SPELL_PARTICLE_YARN    = "net.minecraft.client.particle.SpellParticle";

    // ===== Sprite 反射缓存 =====
    private static volatile Field SPRITE_FIELD;
    private static volatile boolean SPRITE_FIELD_RESOLVED = false;

    private static Field spriteField() {
        if (SPRITE_FIELD_RESOLVED) return SPRITE_FIELD;
        synchronized (ParticleFilter.class) {
            if (SPRITE_FIELD_RESOLVED) return SPRITE_FIELD;
            try {
                Field f = SpriteBillboardParticle.class.getDeclaredField("sprite");
                f.setAccessible(true);
                SPRITE_FIELD = f;
            } catch (Throwable t) {
                SPRITE_FIELD = null;
            }
            SPRITE_FIELD_RESOLVED = true;
        }
        return SPRITE_FIELD;
    }

    public static String tryGetSpriteId(Particle particle) {
        try {
            Field f = spriteField();
            if (f == null) return null;
            if (!(particle instanceof SpriteBillboardParticle sbp)) return null;
            Sprite sprite = (Sprite) f.get(sbp);
            if (sprite == null) return null;
            return sprite.getAtlasId().toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 是否应当保留这个粒子 (返回 true 则渲染)。
     *
     * 策略 — 命中以下任一规则则保留:
     *   - 伤害红心 (DamageParticle)
     *   - 横扫弧光 (SweepAttackParticle)
     *   - 音符 (NoteParticle)
     *   - SpellParticle 中 critical_hit (暴击星) / effect (药效果) / enchant (附魔命中) 纹理
     *   - SonicBoomParticle ( Warden 冲击波, PvP 也常见)
     *
     * 其余全部过滤。
     */
    public static boolean shouldKeep(Particle particle) {
        if (particle == null) return false;

        // 总开关关闭 -> 全部放行, 不过滤任何粒子
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.particlesEnabled) return true;

        try {
            String name = nameOf(particle);

            // 1) 伤害红心 - DamageParticle
            if (name.equals(DAMAGE_PARTICLE_INTERMEDIARY) || name.equals(DAMAGE_PARTICLE_YARN)) {
                return cfg.keepDamageParticles;
            }

            // 2) 横扫弧光 - SweepAttackParticle (与"保留暴击粒子"开关联动)
            if (name.equals(SWEEP_PARTICLE_INTERMEDIARY) || name.equals(SWEEP_PARTICLE_YARN)) {
                return cfg.keepCritParticles;
            }

            // 3) 音符 - NoteParticle (经验球附近)
            if (name.equals(NOTE_PARTICLE_INTERMEDIARY) || name.equals(NOTE_PARTICLE_YARN)) {
                return cfg.keepXpParticles;
            }

            // 4) SpellParticle - 暴击星 / 药效果 / 附魔命中 / 环境效果都通过这里渲染
            //    在 1.20.6 中药水效果粒子的 Sprite ID 不一定含 effect/mob_effect 字串,
            //    直接全部保留以确保 PvP 场景下所有状态可视粒子都能正常显示。
            if (name.equals(SPELL_PARTICLE_INTERMEDIARY) || name.equals(SPELL_PARTICLE_YARN)) {
                // SpellParticle 同时承载暴击星 + 药效果两类 PvP 粒子,
                // 默认放行 (PvP 友好). 关闭任何一个开关联动时也保留, 避免误判.
                return cfg.keepCritParticles || cfg.keepPotionParticles;
            }

            // 5) SonicBoom 冲击波 (PvP 也常见)
            if (name.equals(SONIC_BOOM_PARTICLE_INTERMEDIARY)) {
                return true;
            }

            // 其余全部过滤
            return false;
        } catch (Throwable t) {
            // 任何异常一律过滤 (安全默认, 减少粒子)
            return false;
        }
    }

    // ==================== 1.1.0 追加门槛 ====================
    // 说明: 上面的类型白名单是本 mod 的核心机制, 这里刻意不做任何修改。
    // 距离剔除与每帧预算只是叠加在白名单之外的第二层过滤, 两者相互独立。

    private static final AtomicInteger RENDERED_THIS_FRAME = new AtomicInteger();
    private static final AtomicLong LAST_RESET_NS = new AtomicLong();

    /** 由 HudRenderCallback 每帧调用一次，为下一帧清零计数。 */
    public static void beginFrame() {
        RENDERED_THIS_FRAME.set(0);
        LAST_RESET_NS.set(System.nanoTime());
    }

    /**
     * 白名单判定通过后再调用：距离门槛 + 每帧粒子预算。
     *
     * @param camera 当前相机（buildGeometry 自带参数，无需额外注入）
     * @param x/y/z  粒子世界坐标（由 Mixin 的 @Shadow 传入）
     */
    public static boolean allowRender(Particle particle, Camera camera, double x, double y, double z) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        // 粒子总开关关闭时与 shouldKeep 保持一致：完全放行
        if (!cfg.particlesEnabled) return true;

        double scale = AdaptivePerformance.scale();

        if (cfg.particleDistanceCullEnabled && camera != null) {
            Vec3d cam = camera.getPos();
            double dx = x - cam.x;
            double dy = y - cam.y;
            double dz = z - cam.z;
            double max = cfg.particleCullDistance * scale;
            if (dx * dx + dy * dy + dz * dz > max * max) return false;
        }

        if (cfg.particleBudgetEnabled && cfg.particleMaxPerFrame > 0) {
            ensureFrameWindow();
            int budget = Math.max(16, (int) Math.round(cfg.particleMaxPerFrame * scale));
            if (RENDERED_THIS_FRAME.getAndIncrement() >= budget) return false;
        }
        return true;
    }

    /**
     * 兜底：万一 HUD 回调被其它 mod 提前取消，计数不会无限累加，
     * 否则超预算后的所有粒子会被永久丢弃。
     */
    private static void ensureFrameWindow() {
        long now = System.nanoTime();
        long last = LAST_RESET_NS.get();
        if (now - last > 1_000_000_000L && LAST_RESET_NS.compareAndSet(last, now)) {
            RENDERED_THIS_FRAME.set(0);
        }
    }
}