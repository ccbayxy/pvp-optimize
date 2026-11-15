package com.pvp.optimize.mixin;

import com.pvp.optimize.perf.SmartTextureAnimations;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 智能纹理动画（Sodium 兼容版）注入点。
 *
 * 只拦截图集动画 tick 的 HEAD，不触碰区块网格 / GL 状态，
 * 因此与 Sodium、ImmediatelyFast 的渲染管线互不干扰。
 */
@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {

    @Inject(method = "tickAnimatedSprites", at = @At("HEAD"), cancellable = true)
    private void pvpoptimize$throttleAnimatedSprites(CallbackInfo ci) {
        SpriteAtlasTexture self = (SpriteAtlasTexture) (Object) this;
        if (!SmartTextureAnimations.shouldTick(self.getId())) {
            ci.cancel();
        }
    }
}
