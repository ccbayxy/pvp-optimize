package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.dango.DangoBridge;
import com.pvp.optimize.music.MusicPlayer;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 播放本 mod 的音乐时不再调度原版背景音乐（只让路 MUSIC 类别，音效不受影响）。 */
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$duckVanillaMusic(CallbackInfo ci) {
        // Dango 播放时由 ModernPlatform.holdVanillaMusic 置位 VanillaMusicHold.active
        if (PvPOptimizeConfig.get().musicDuckVanilla
                && (MusicPlayer.isActive() || DangoBridge.isDuckingVanillaMusic())) {
            ci.cancel();
        }
    }
}
