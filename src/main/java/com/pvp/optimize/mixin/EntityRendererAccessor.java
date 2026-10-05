package com.pvp.optimize.mixin;

import com.pvp.optimize.util.RenderContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 EntityRenderer.render 入口处把当前渲染的 Entity 写入 ThreadLocal,
 * 渲染流程结束时清理, 避免引用泄漏到下一帧。
 *
 * 用 @Inject + @At("HEAD") + finally 清理模式, 任何异常路径都释放 ThreadLocal。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererAccessor {

    /**
     * render(Entity, float yaw, float tickDelta, MatrixStack, VertexConsumerProvider, int light)
     * 中间形参类型: Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;
     *              Lnet/minecraft/client/render/VertexConsumerProvider;I
     * 注意: Fabric 织入时把第一个 Entity 参数也认作捕获参数, 我们不显式捕获, 走 ThreadLocal 共享。
     */
    @Inject(method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void pvpoptimize$beginRender(Entity entity, float yaw, float tickDelta,
                                          net.minecraft.client.util.math.MatrixStack matrices,
                                          net.minecraft.client.render.VertexConsumerProvider consumers,
                                          int light, CallbackInfo ci) {
        RenderContext.set(entity);
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void pvpoptimize$endRender(Entity entity, float yaw, float tickDelta,
                                        net.minecraft.client.util.math.MatrixStack matrices,
                                        net.minecraft.client.render.VertexConsumerProvider consumers,
                                        int light, CallbackInfo ci) {
        RenderContext.clear();
    }
}