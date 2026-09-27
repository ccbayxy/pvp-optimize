package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

/**
 * 粒子过滤核心 (fix-2026-09-27 v4).
 *
 * <p>关键发现: Minecraft 1.20.6 运行时, 反编译
 * {@code minecraft-merged-1.20.6.jar} 确认 {@code DamageParticle}、
 * {@code NoteParticle}、{@code FlameParticle} 等用的就是
 * {@code PARTICLE_SHEET_OPAQUE}; 未重写 {@code getType()} 的子类
 * (CritParticle / SweepAttackParticle 等) 也都是 OPAQUE 默认。</p>
 *
 * <p>之前 v2 写 "OPAQUE 默认屏蔽" 会把这些 PvP 战斗粒子全部干掉。
 * 现改为 <b>默认全部放行</b> (白名单模式): 关掉
 * {@code particlesEnabled} 即恢复原版; 打开时只在极个别需要时按需微调。</p>
 */
public final class ParticleFilter {

    private ParticleFilter() {}

    public static boolean shouldRender(Particle particle) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        // 总开关: 关掉过滤就全部放行 (彻底恢复原版)
        if (!cfg.particlesEnabled) return true;

        // 默认白名单模式: 不区分 sheet, 全部放行.
        // 任何进一步细分留给未来按需追加 (例如 block_break 等特化过滤),
        // 当前的目标是 "绝不再误杀 PvP 战斗粒子".
        return true;
    }

    public static boolean shouldRender(Particle particle, Entity source) {
        // 粒子过滤开启时, 任何源实体的投射物粒子都应该正常显示
        // (EnderPearl / Snowball / Potion / ExperienceBottle / Arrow)
        // ——这些都是 PvP 中玩家关注的实体. 默认全放行.
        if (source == null) return shouldRender(particle);

        if (source instanceof EnderPearlEntity) return true;
        if (source instanceof SnowballEntity)   return true;
        if (source instanceof PotionEntity)     return true;
        if (source instanceof ExperienceBottleEntity) return true;
        if (source instanceof ArrowEntity)      return true;

        return shouldRender(particle);
    }
}