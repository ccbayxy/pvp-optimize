package com.pvp.optimize.mixin;

import com.pvp.optimize.particle.ParticleFilter;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截每个 BillboardParticle 的 buildGeometry 调用。
 *
 * Sodium 0.5+ 完全重写了粒子渲染管线, 不调用 ParticleManager.renderParticles,
 * 但每个粒子自己的 buildGeometry() 仍然会被调用 (这是 BillboardParticle 抽象方法)。
 * 因此这是与 Sodium 兼容的拦截点。
 *
 * 通过 ParticleFilter.shouldKeep() 判定哪些粒子应该保留, 黑名单直接 cancel。
 * shouldKeep 对任何无法识别的粒子都默认放行 (返回 true), 因此该 mixin 不会
 * 误屏蔽不认识的粒子, 最坏情况是不过滤。
 */
@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin {

    @Inject(method = "buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V",
            at = @At("HEAD"),
            cancellable = true)
    private void pvpoptimize$filterBuildGeometry(VertexConsumer vertexConsumer,
                                                  Camera camera,
                                                  float tickDelta,
                                                  CallbackInfo ci) {
        Particle self = (Particle) (Object) this;
        // 默认 shouldKeep=true (放行), 只对明确黑名单返回 false (过滤)
        if (!ParticleFilter.shouldKeep(self)) {
            ci.cancel();
        }
    }
}