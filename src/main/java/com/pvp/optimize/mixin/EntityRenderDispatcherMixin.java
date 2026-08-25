package com.pvp.optimize.mixin;

import com.pvp.optimize.entity.EntityFilter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$skipCullableEntities(Entity entity,
                                                  double x, double y, double z,
                                                  float yaw,
                                                  float tickDelta,
                                                  MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers,
                                                  int light,
                                                  CallbackInfo ci) {
        if (EntityFilter.shouldCull(entity)) {
            ci.cancel();
        }
    }
}