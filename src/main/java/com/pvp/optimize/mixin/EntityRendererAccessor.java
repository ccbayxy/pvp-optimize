package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.util.RenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EntityRenderer.render 入口/出口:
 *   1) 在 HEAD 把当前 Entity 写入 RenderContext (ThreadLocal)
 *   2) 在 HEAD 之前判断: 距离 > 阈值 + 玩家装备全空 -> 直接取消渲染 (隐身态)
 *   3) TAIL 清理 ThreadLocal
 *
 * "玩家装备全空"指 HEAD/CHEST/LEGS/FEET/MAINHAND/OFFHAND 6 个槽位均无物品。
 * 没装备的玩家距离远时整体不可见, 只看到装备玩家 (避免没装备的玩家干扰视线)。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererAccessor {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$beginRender(Entity entity, float yaw, float tickDelta,
                                          net.minecraft.client.util.math.MatrixStack matrices,
                                          net.minecraft.client.render.VertexConsumerProvider consumers,
                                          int light, CallbackInfo ci) {
        // 1) 距离 LOD + 玩家装备全空 -> 完全隐身 (不渲染整个玩家)
        if (shouldHideFarEmptyPlayer(entity)) {
            // 不写入 RenderContext, 也无需 TAIL 清理
            ci.cancel();
            return;
        }
        // 2) 正常路径: 把当前 Entity 写入 ThreadLocal 给 PlayerEntityModelMixin 用
        RenderContext.set(entity);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvpoptimize$endRender(Entity entity, float yaw, float tickDelta,
                                        net.minecraft.client.util.math.MatrixStack matrices,
                                        net.minecraft.client.render.VertexConsumerProvider consumers,
                                        int light, CallbackInfo ci) {
        RenderContext.clear();
    }

    private static boolean shouldHideFarEmptyPlayer(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        if (player == mc.player) return false; // 不隐藏自己
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.hidePlayerSkinLayers) return false;
        if (cfg.hideSkinLayersDistance <= 0.0) return false;
        if (!cfg.hideFarEmptyPlayers) return false;

        // 距离检查
        double distSq = mc.player.squaredDistanceTo(player);
        double maxDistSq = cfg.hideSkinLayersDistance * cfg.hideSkinLayersDistance;
        if (distSq <= maxDistSq) return false;

        // 装备检查: 6 个槽位全空才算"无装备玩家"
        return isEquipmentEmpty(player);
    }

    private static boolean isEquipmentEmpty(PlayerEntity p) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!p.getEquippedStack(slot).isEmpty()) return false;
        }
        return true;
    }
}