package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
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
 * Sodium 兼容补丁 (2026-09-27 v5).
 *
 * <p>注入 {@link BillboardParticle#buildGeometry}. 默认 master = false
 * 时直接返回 (不过滤任何粒子, 让 Sodium/原版渲染器接管). 用户开启 master
 * 后, 再按 {@link ParticleFilter} 的细类判断.</p>
 *
 * <p>所有 PvP 战斗粒子 (Crit / Damage / SweepAttack / FireworkSpark) 都
 * 继承 BillboardParticle, 这里注入能覆盖. 原版 ParticleManager 和 Sodium
 * 重写的粒子渲染器最终都会调到 BillboardParticle.buildGeometry.</p>
 */
@Mixin(BillboardParticle.class)
public abstract class ParticleMixin {

    @Inject(method = "buildGeometry", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$filterBuildGeometry(VertexConsumer vertexConsumer,
                                                 Camera camera,
                                                 float tickDelta,
                                                 CallbackInfo ci) {
        // Master 关闭 = 不做事, 完全放行
        if (!PvPOptimizeConfig.get().particlesEnabled) return;
        if (!ParticleFilter.shouldRender((Particle)(Object)this)) {
            ci.cancel();
        }
    }
}