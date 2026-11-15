package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.perf.AggressiveOptimizer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 远景实体动画冻结: 超过配置距离的生物跳过 animateModel。
 *
 * 注意: 老版本 refmap 中该注入点是 class_572.method_17086, 即
 * BipedEntityModel.animateModel, 而不是 LivingEntityRenderer。
 * 这里严格按老成品的映射落地, 避免注入失败。
 */
@Mixin(BipedEntityModel.class)
public class LivingEntityRendererMixin {

    @Inject(method = "animateModel(Lnet/minecraft/entity/LivingEntity;FFF)V",
            at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$freezeFarAnimation(LivingEntity entity, float f, float g, float tickDelta,
                                                 CallbackInfo ci) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (AggressiveOptimizer.isEnabled(cfg.animationFreezeEnabled)) {
            double distSq = AggressiveOptimizer.squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
            if (AggressiveOptimizer.isBeyond(distSq, cfg.animationFreezeDistance)) {
                ci.cancel();
            }
        }
    }
}
