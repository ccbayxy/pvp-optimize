package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体渲染优化:
 * 1) 名字背景透明 (transparentNametagBg) -- Redirect GameOptions.getTextBackgroundOpacity
 * 2) 距离 LOD: 玩家超过 hideNametagsDistance 时跳过名字渲染 -- Inject HEAD cancellable
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Redirect(
            method = "renderLabelIfPresent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getTextBackgroundOpacity(F)F"
            )
    )
    private float pvpoptimize$transparentNametagBg(GameOptions instance, float fallback) {
        if (PvPOptimizeConfig.get().transparentNametagBg) {
            return 0.0f;
        }
        return fallback;
    }

    /**
     * 距离 LOD: 玩家距离相机超过阈值时直接取消 renderLabelIfPresent,
     * 不绘制名字 (含背景 + 文字)。装备/皮肤层走别的路径, 不受影响。
     */
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$skipFarNametag(Entity entity,
                                            net.minecraft.text.Text text,
                                            net.minecraft.client.util.math.MatrixStack matrices,
                                            net.minecraft.client.render.VertexConsumerProvider consumers,
                                            int light, float tickDelta,
                                            CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (cfg.hideNametagsDistance <= 0.0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        double distSq = mc.player.squaredDistanceTo(entity);
        double maxDistSq = cfg.hideNametagsDistance * cfg.hideNametagsDistance;
        if (distSq > maxDistSq) {
            ci.cancel();
        }
    }
}