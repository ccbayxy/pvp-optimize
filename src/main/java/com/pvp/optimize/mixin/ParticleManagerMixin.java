package com.pvp.optimize.mixin;

import com.pvp.optimize.particle.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Minecraft 渲染粒子的两个稳定拦截点:
 *
 *   1. REDIRECT buildGeometry  — 单粒子绘制时的兜底拦截。
 *      触发时机: 真正绘制某粒子前一刻, 此时 Particle 对象已生成,
 *                getType() / getClass() 都可用, 判断最准。
 *
 *   2. TAIL tickParticles     — 每帧 tick 结束后清理队列。
 *      触发时机: 同一帧所有粒子的 tick() 完成之后。
 *      用途: 把不通过的粒子从队列中移走, 避免下一帧又尝试绘制。
 *
 * 不再用 @Shadow 访问 particles / newParticles (1.20.6 是 private final,
 * 不必依赖具体字段名, yarn 升级时更不易崩)。
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    /** 单粒子绘制拦截 — 最关键的关卡 */
    @Redirect(
            method = "renderParticles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V")
    )
    private void pvpoptimize$redirectBuildGeometry(Particle particle,
                                                  VertexConsumer vertexConsumer,
                                                  Camera camera,
                                                  float tickDelta) {
        if (ParticleFilter.shouldRender(particle)) {
            particle.buildGeometry(vertexConsumer, camera, tickDelta);
        }
    }

    /** tick 阶段: 把不该渲染的粒子从存活队列中移除, 防止下帧还来尝试画 */
    @Inject(method = "tickParticles", at = @At("TAIL"))
    private void pvpoptimize$purgeAfterTickParticles(CallbackInfo ci) {
        try {
            // 通过反射拿到 particles / newParticles 字段 (yarn 字段名会变, reflection 才稳)
            java.lang.reflect.Field f = ParticleManager.class.getDeclaredField("particles");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.client.particle.ParticleTextureSheet, java.util.Queue<Particle>> map =
                    (java.util.Map<net.minecraft.client.particle.ParticleTextureSheet, java.util.Queue<Particle>>) f.get(this);
            for (java.util.Queue<Particle> q : map.values()) {
                if (q != null) q.removeIf(p -> !ParticleFilter.shouldRender(p));
            }
        } catch (Throwable ignored) { /* 字段改名则放弃这一步, REDIRECT 仍然生效 */ }

        try {
            java.lang.reflect.Field f = ParticleManager.class.getDeclaredField("newParticles");
            f.setAccessible(true);
            Object q = f.get(this);
            if (q instanceof java.util.Collection<?> coll) {
                coll.removeIf(o -> o instanceof Particle p && !ParticleFilter.shouldRender(p));
            }
        } catch (Throwable ignored) { }
    }
}