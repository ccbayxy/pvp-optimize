package com.pvp.optimize.particle;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

/**
 * 粒子过滤 (2026-09-27 v5).
 *
 * <p><b>核心原则</b>: 默认 master = false (即不过滤, 让 Sodium 接管渲染).
 * 仅当用户在 ConfigScreen 打开 master 且关闭某些细类时, 才按细类筛选.</p>
 *
 * <p>分类规则 (按粒子类名/Sheet/特征判断):</p>
 * <pre>
 *   COMBAT      : Crit / SweepAttack / Note / AngryVillager / FireworkSpark
 *   DAMAGE      : Damage (伤害红心, 一般是 HeartParticle / DamageParticle)
 *   BLOCK       : Block / FallingDust / Dig (方块破坏/放置)
 *   SMOKE       : Smoke / LargeSmoke / Campfire
 *   EXPLOSION   : Explosion / HugeExplosion
 *   POTION      : Effect (药水喷溅)
 *   PORTAL      : Portal / ReversePortal (下界传送门)
 *   BUBBLE      : Bubble / BubbleColumn / CurrentDown / Underwater
 *   FIREWORK    : Firework (烟花爆裂)
 *   FIRE        : Flame / LavaEmber / Lava (火焰/岩浆)
 *   AMBIENT     : Totem / Ash / Sculk / Cherry / Drip / SporeBlossom
 *   EXPERIENCE  : Experience (经验球)
 * </pre>
 */
public final class ParticleFilter {

    private ParticleFilter() {}

    /** 不屏蔽 (即正常渲染) */
    public static boolean shouldRender(Particle particle) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        // Master 关闭 = 完全不过滤, 直接放行
        if (!cfg.particlesEnabled) return true;

        Category cat = categorize(particle);
        return switch (cat) {
            case COMBAT      -> cfg.particleCombat;
            case DAMAGE      -> cfg.particleDamage;
            case BLOCK       -> cfg.particleBlock;
            case SMOKE       -> cfg.particleSmoke;
            case EXPLOSION   -> cfg.particleExplosion;
            case POTION      -> cfg.particlePotion;
            case PORTAL      -> cfg.particlePortal;
            case BUBBLE      -> cfg.particleBubble;
            case FIREWORK    -> cfg.particleFirework;
            case FIRE        -> cfg.particleFire;
            case AMBIENT     -> cfg.particleAmbient;
            case EXPERIENCE  -> cfg.particleExperience;
            case OTHER       -> true; // 兜底: 未识别粒子默认放行
        };
    }

    public static boolean shouldRender(Particle particle, Entity source) {
        if (shouldRender(particle)) return true;
        if (source == null) return false;

        // PvP 投射物源实体的粒子默认全放行 (玩家关注度高)
        if (source instanceof EnderPearlEntity) return true;
        if (source instanceof SnowballEntity)   return true;
        if (source instanceof PotionEntity)     return true;
        if (source instanceof ExperienceBottleEntity) return true;
        if (source instanceof ArrowEntity)      return true;

        return false;
    }

    // ============ 分类逻辑 ============

    public enum Category {
        COMBAT, DAMAGE, BLOCK, SMOKE, EXPLOSION, POTION,
        PORTAL, BUBBLE, FIREWORK, FIRE, AMBIENT, EXPERIENCE, OTHER
    }

    public static Category categorize(Particle p) {
        if (p == null) return Category.OTHER;

        // 经验球在 1.20.6 是 TRANSLUCENT sheet, 类名通常含 experience / xp / orb
        ParticleTextureSheet sheet = p.getType();
        String name = p.getClass().getName().toLowerCase();

        // 1. 经验球: 优先识别, 因为经验球类名会被混淆
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT
                && (name.contains("experience") || name.contains("xp"))) {
            return Category.EXPERIENCE;
        }

        // 2. 通过类名包含的标识子串识别 (Mojang 混淆下仍然保留下来的语义字段)
        //    —— 注意 1.20.6 的类名是 class_xxxx, 所以这里主要靠 sheet + 字段特征
        if (containsAny(name, "crit", "sweep", "note", "firework", "angry")) return Category.COMBAT;
        if (containsAny(name, "damage", "heart"))                            return Category.DAMAGE;
        if (containsAny(name, "block", "falling_dust", "fallingdust"))      return Category.BLOCK;
        if (containsAny(name, "smoke", "campfire"))                          return Category.SMOKE;
        if (containsAny(name, "explosion", "huge_explosion"))                return Category.EXPLOSION;
        if (containsAny(name, "effect", "splash", "potion"))                 return Category.POTION;
        if (containsAny(name, "portal", "reverse_portal"))                   return Category.PORTAL;
        if (containsAny(name, "bubble", "current", "underwater"))            return Category.BUBBLE;
        if (containsAny(name, "flame", "lavaember", "lava"))                 return Category.FIRE;
        if (containsAny(name, "totem", "ash", "sculk", "cherry", "drip", "spore")) return Category.AMBIENT;

        // 3. 通过 Sheet 兜底
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
            return Category.FIRE; // OPAQUE 大部分是火焰/岩浆火星等
        }
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
            return Category.POTION;
        }
        return Category.OTHER;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }
}