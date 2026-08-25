package com.pvp.optimize.entity;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Set;

/**
 * Decides whether an entity should be culled when it is farther than the
 * configured distance from the local player. Modelled after EntityCulling
 * and "PVP Optimizations" type mods: rather than culling at the renderer
 * (which still costs CPU for AI / collision), we cull server-side via the
 * world iterator, so the entity is never sent to the client in the first
 * place for hostile mobs etc.
 *
 * The whitelist is intentionally tiny because the mod is meant for the
 * MCPVP / crystal-pvp scene:
 *   - Player, Villager
 *   - Snowball, EnderPearl, Arrow
 *   - Smelted-mineral items (e.g. iron_ingot, gold_ingot, diamond, ...)
 *
 * Everything else (item frames, armor stands, paintings, hostile mobs,
 * passive animals, projectiles we don't care about, etc.) is dropped past
 * the configured distance.
 */
public final class EntityFilter {

    private EntityFilter() {}

    /** Smelted mineral drops the player actually cares about in PVP. */
    private static final Set<Item> ALLOWED_MINERALS = Set.of(
            Items.COAL,
            Items.IRON_INGOT,
            Items.GOLD_INGOT,
            Items.DIAMOND,
            Items.EMERALD,
            Items.NETHERITE_INGOT,
            Items.QUARTZ,
            Items.LAPIS_LAZULI,
            Items.REDSTONE,
            Items.COPPER_INGOT
    );

    /**
     * Returns true if the entity should be culled (i.e. *removed*) right now.
     */
    public static boolean shouldCull(Entity entity) {
        if (!PvPOptimizeConfig.get().entityCullingEnabled) return false;

        // Always keep the local player
        if (entity instanceof PlayerEntity) return false;
        // Whitelisted mob
        if (entity instanceof VillagerEntity) return false;
        // Whitelisted projectiles
        if (entity instanceof SnowballEntity) return false;
        if (entity instanceof EnderPearlEntity) return false;
        if (entity instanceof ArrowEntity) return false;
        if (entity instanceof PotionEntity) return false;
        if (entity instanceof ExperienceBottleEntity) return false;
        if (entity instanceof EggEntity) return false;

        // Smelted-mineral item entity: keep regardless of distance.
        if (isMineralItem(entity)) return false;

        // Bosses are always kept - players need them on screen.
        if (entity instanceof WitherEntity) return false;
        if (entity instanceof EnderDragonEntity) return false;

        // Everything else (item frames, armor stands, paintings, mobs,
        // hostile entities, animals, ...) is culled past the distance.
        return !isWithinRange(entity);
    }

    private static boolean isMineralItem(Entity entity) {
        if (!(entity instanceof net.minecraft.entity.ItemEntity item)) return false;
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return false;
        return ALLOWED_MINERALS.contains(stack.getItem());
    }

    private static boolean isWithinRange(Entity entity) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return true;
        double dist = mc.player.squaredDistanceTo(entity);
        double max = PvPOptimizeConfig.get().cullDistance;
        return dist <= max * max;
    }
}
