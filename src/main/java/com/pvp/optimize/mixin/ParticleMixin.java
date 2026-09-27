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
 * Sodium 兼容补丁 (fix-2026-09-19 v3.2).
 *
 * <p>关键定位: Minecraft 1.20.6 yarn 映射中, 粒子绘制方法仍叫
 * {@code buildGeometry(VertexConsumer, Camera, float)} (从 1.21.2 才改名为
 * {@code render})。{@code Particle.buildGeometry} 是抽象方法, 无方法体;
 * {@link BillboardParticle} 是第一个有具体方法体的层级, 是注入点。</p>
 *
 * <p>所有 PvP 战斗粒子 (Crit / Damage / SweepAttack / FireworkSpark)
 * 都继承 BillboardParticle, 这里注入就能覆盖。同时兼顾原版
 * ParticleManager 和 Sodium 重写的粒子渲染器 (它们都最终调到
 * BillboardParticle.buildGeometry)。</p>
 */
@Mixin(BillboardParticle.class)
public abstract class ParticleMixin {

    @Inject(method = "buildGeometry", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$filterBuildGeometry(VertexConsumer vertexConsumer,
                                                 Camera camera,
                                                 float tickDelta,
                                                 CallbackInfo ci) {
        if (!ParticleFilter.shouldRender((Particle)(Object)this)) {
            ci.cancel();
        }
    }
}