package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.perf.AggressiveOptimizer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 远处音效节流: 超过配置距离、且非相对/非强制播放的音效直接跳过。
 * 注入点对应老版本 refmap 的 class_1140.method_4852。
 */
@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V",
            at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$throttleFarSounds(SoundInstance sound, int delay, CallbackInfo ci) {
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (AggressiveOptimizer.isEnabled(cfg.soundThrottleEnabled)) {
            if (!sound.isRelative() && !sound.shouldAlwaysPlay()) {
                double distSq = AggressiveOptimizer.squaredDistanceTo(sound.getX(), sound.getY(), sound.getZ());
                if (AggressiveOptimizer.isBeyond(distSq, cfg.soundThrottleDistance)) {
                    ci.cancel();
                }
            }
        }
    }
}
