package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.texture.Sprite;

import java.lang.reflect.Field;

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
            String name = particle.getClass().getName();

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
}