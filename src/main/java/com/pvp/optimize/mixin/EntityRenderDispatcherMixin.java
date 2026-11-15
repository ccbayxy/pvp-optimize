package com.pvp.optimize.mixin;

import com.pvp.optimize.entity.EntityFilter;
import com.pvp.optimize.util.RenderContext;
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
        if (!EntityFilter.shouldCull(entity) && !EntityFilter.shouldFullyHide(entity)) {
            RenderContext.set(entity);
        } else {
            // HEAD 取消后 TAIL 的 clear() 不会执行, 必须在此清理, 否则 ThreadLocal
            // 会残留上一个实体的引用, 让皮肤层 LOD 用错判断对象。
            RenderContext.clear();
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvpoptimize$endRender(Entity entity,
                                       double x, double y, double z,
                                       float yaw,
                                       float tickDelta,
                                       MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       int light,
                                       CallbackInfo ci) {
        RenderContext.clear();
    }
}
