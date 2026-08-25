package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

public final class ParticleFilter {

    private ParticleFilter() {}

    public static boolean shouldRender(Particle particle) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.particlesEnabled) return true;

        String name = particle.getClass().getName().toLowerCase();

        // 暴击星形粒子: 玩家对生物造成暴击时, 命中点出现的青色星星
        // Yarn 类名: net.minecraft.client.particle.CritParticle
        if (name.contains("critparticle")) {
            return cfg.keepCritParticles;
        }

        // 伤害红心粒子: 生物受到伤害时, 生物身上飞出的红心
        // Yarn 类名: net.minecraft.client.particle.DamageParticle
        if (name.contains("damageparticle")) {
            return cfg.keepDamageParticles;
        }

        // 药水/状态效果粒子 (药水云、围绕实体的光环)
        if (name.contains("effectparticle")
                || name.contains("entityeffectparticle")) {
            return cfg.keepPotionParticles;
        }

        // 经验球粒子
        if (name.contains("experienceorbparticle")) {
            return cfg.keepXpParticles;
        }

        // 其余全部屏蔽
        // 注意: heartparticle 是动物繁殖爱心 (非受击红心);
        //       sweepattackparticle 是剑的横扫特效 (非暴击) — 都不再保留
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