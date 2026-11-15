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
 * 两道门槛:
 *   1) 类型白名单 ParticleFilter.shouldKeep() —— 原有机制, 保持不变
 *   2) 距离剔除 + 每帧预算 ParticleFilter.allowRender() —— 1.1.0 新增, 独立叠加
 *
 * 粒子坐标通过 ParticleAccessor 读取（@Shadow 无法解析父类字段）。
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

        // 1) 原有类型白名单：黑名单直接过滤
        if (!ParticleFilter.shouldKeep(self)) {
            ci.cancel();
            return;
        }

        // 2) 新增：距离门槛与每帧预算（白名单通过后才检查）
        ParticleAccessor pos = (ParticleAccessor) self;
        if (!ParticleFilter.allowRender(self, camera,
                pos.pvpoptimize$getX(), pos.pvpoptimize$getY(), pos.pvpoptimize$getZ())) {
            ci.cancel();
        }
    }
}
