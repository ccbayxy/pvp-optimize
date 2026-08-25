package com.pvp.optimize.mixin;

import com.pvp.optimize.particle.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters out particles we don't want to render.
 *
 *  - Block all particles by default.
 *  - Re-allow particles matched by {@link ParticleFilter}.
 *
 * Strategy: rather than poking private fields (which differ between yarn
 * build numbers), we redirect the per-particle geometry call. Anything that
 * would have been drawn is dropped if the filter rejects it. This is the
 * same trick NoRender+ uses on a per-particle-type basis, applied
 * indiscriminately to every particle.
 *
 * Note: in 1.20.6 yarn, the per-particle draw method is named
 * {@code buildGeometry(VertexConsumer, Camera, float)} (not {@code render}
 * and not {@code method_3073}).
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

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

    /**
     * Belt-and-braces: also kill the "new particles" list at the end of
     * {@link ParticleManager#renderParticles} so that any particles spawned
     * during rendering (or any that slipped past the redirect above) are
     * cleared before the next tick. We use reflection here on purpose to
     * avoid binding to the exact field name / generic shape, which changes
     * between yarn builds.
     *
     * The actual signature in 1.20.6 yarn is
     * {@code renderParticles(LightmapTextureManager, Camera, float)}.
     */
    @Inject(method = "renderParticles", at = @At("TAIL"))
    private void pvpoptimize$clearStragglers(net.minecraft.client.render.LightmapTextureManager lightmap,
                                             Camera camera,
                                             float tickDelta,
                                             CallbackInfo ci) {
        try {
            java.lang.reflect.Field particles = ParticleManager.class.getDeclaredField("particles");
            particles.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.client.particle.ParticleTextureSheet, java.util.Queue<Particle>> map =
                    (java.util.Map<net.minecraft.client.particle.ParticleTextureSheet, java.util.Queue<Particle>>) particles.get(this);
            for (java.util.Queue<Particle> q : map.values()) {
                q.removeIf(p -> !ParticleFilter.shouldRender(p));
            }
        } catch (Throwable ignored) {
            // field renamed in this yarn build - redirect above still active
        }
        try {
            java.lang.reflect.Field newParticles = ParticleManager.class.getDeclaredField("newParticles");
            newParticles.setAccessible(true);
            Object q = newParticles.get(this);
            if (q instanceof java.util.Collection<?> coll) {
                coll.removeIf(o -> o instanceof Particle p && !ParticleFilter.shouldRender(p));
            }
        } catch (Throwable ignored) {
            // field renamed in this yarn build - redirect above still active
        }
    }
}
