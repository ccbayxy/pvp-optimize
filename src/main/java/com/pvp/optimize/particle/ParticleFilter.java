package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

import java.lang.reflect.Field;

/**
 * 粒子过滤 v1.0.2 修复版
 *
 * 1.20.6 Mojang 混淆后, {@code particle.getClass().getName()} 返回的是
 * 默认包路径下的短类名（如 "gae"、"fzv"），yarn 名字 ("CritParticle"、
 * "DamageParticle") 运行时根本拿不到。因此旧版基于子串匹配 ("critparticle"
 * 等) 的过滤逻辑运行时永远不命中。
 *
 * 本实现改用反射 + Class.forName() 解析具体类, 并配合 Sprite 纹理
 * (critical_hit / damage / note / sweep) 作为兜底识别, 确保 PvP 场景里
 * 暴击星、伤害红心、药水效果、经验球、附魔命中粒子能正常显示。
 */
public final class ParticleFilter {

    private ParticleFilter() {}

    // ===== 缓存通过 Class.forName 解析出的目标 Class 对象 =====
    // 在 1.20.6 yarn 中：
    //   DamageParticle          = gae
    //   SweepAttackParticle     = fzv
    //   NoteParticle            = gbd
    //   SpellParticle (附魔/暴击) = gbw
    //   SonicBoomParticle       = gbu
    //   PortalParticle          = gbk
    //   FlameParticle           = gap
    private static Class<?> DAMAGE_PARTICLE_CLS;
    private static Class<?> SWEEP_PARTICLE_CLS;
    private static Class<?> NOTE_PARTICLE_CLS;
    private static Class<?> SPELL_PARTICLE_CLS;
    private static Class<?> SONIC_BOOM_CLS;
    private static Class<?> FLAME_PARTICLE_CLS;
    private static Class<?> PORTAL_PARTICLE_CLS;
    private static boolean RESOLVED = false;

    private static synchronized void resolveClasses() {
        if (RESOLVED) return;
        RESOLVED = true;
        DAMAGE_PARTICLE_CLS   = tryResolve("gae");
        SWEEP_PARTICLE_CLS    = tryResolve("fzv");
        NOTE_PARTICLE_CLS     = tryResolve("gbd");
        SPELL_PARTICLE_CLS    = tryResolve("gbw");
        SONIC_BOOM_CLS        = tryResolve("gbu");
        FLAME_PARTICLE_CLS    = tryResolve("gap");
        PORTAL_PARTICLE_CLS   = tryResolve("gbk");
    }

    private static Class<?> tryResolve(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 提取 Sprite 资源 ID (例如 "minecraft:critical_hit"), 用于在
     * 1.20.6 没有 CritParticle 类的情况下, 仍能识别暴击粒子。
     */
    private static String tryGetSpriteId(Particle particle) {
        if (!(particle instanceof SpriteBillboardParticle sbp)) return null;
        try {
            Field f = SpriteBillboardParticle.class.getDeclaredField("sprite");
            f.setAccessible(true);
            Sprite sprite = (Sprite) f.get(sbp);
            if (sprite == null) return null;
            return sprite.getAtlasId().toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean shouldRender(Particle particle) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.particlesEnabled) return true;

        resolveClasses();

        // 1) 暴击星 (蓝/青色星星 - 暴击时出现)
        //    1.20.6 中 CritParticle 类已被移除, 改为使用 SpellParticle (gbw)
        //    + critical_hit 纹理渲染
        if (SPELL_PARTICLE_CLS != null && SPELL_PARTICLE_CLS.isInstance(particle)) {
            String spriteId = tryGetSpriteId(particle);
            if (spriteId != null && spriteId.contains("critical_hit")) {
                return cfg.keepCritParticles;
            }
        }

        // 2) 伤害红心 (DamageParticle / gae) - 受击飞出的红心
        if (DAMAGE_PARTICLE_CLS != null && DAMAGE_PARTICLE_CLS.isInstance(particle)) {
            return cfg.keepDamageParticles;
        }

        // 3) 药水效果粒子 - 围绕实体的光环 (SpellParticle + effect/mob_effect 纹理)
        if (SPELL_PARTICLE_CLS != null && SPELL_PARTICLE_CLS.isInstance(particle)) {
            String spriteId = tryGetSpriteId(particle);
            if (spriteId != null && (spriteId.contains("effect") || spriteId.contains("mob_effect"))) {
                return cfg.keepPotionParticles;
            }
        }

        // 4) 经验球音符粒子 (NoteParticle / gbd) - 经验球附近漂浮的音符
        if (NOTE_PARTICLE_CLS != null && NOTE_PARTICLE_CLS.isInstance(particle)) {
            return cfg.keepXpParticles;
        }

        // 5) 横扫弧光 (SweepAttackParticle / fzv) - 剑的横扫特效, 与"保留暴击粒子"开关联动
        if (SWEEP_PARTICLE_CLS != null && SWEEP_PARTICLE_CLS.isInstance(particle)) {
            return cfg.keepCritParticles;
        }

        // 其余全部屏蔽
        return false;
    }

    public static boolean shouldRender(Particle particle, Entity source) {
        if (shouldRender(particle)) return true;
        if (source == null) return false;

        if (source instanceof EnderPearlEntity) return true;
        if (source instanceof SnowballEntity)   return true;
        if (source instanceof PotionEntity)     return true;
        if (source instanceof ExperienceBottleEntity) return true;
        if (source instanceof ArrowEntity)      return true;

        return false;
    }
}