package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes entity nametags (player names above head) transparent in the
 * background while keeping the text itself opaque. Hooks the single point
 * where Minecraft samples the player's text-background-opacity setting in
 * {@code EntityRenderer.renderLabelIfPresent}.
 *
 * Vanilla computes:
 *   int bgColor = (int)(GameOptions.getTextBackgroundOpacity(0.25f) * 255) << 24;
 * and passes it as the background-color parameter of {@code TextRenderer.draw}.
 * Returning 0.0f here drives {@code bgColor} to 0, which makes the text
 * renderer skip drawing the rounded rectangle behind the name.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Redirect(
            method = "renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getTextBackgroundOpacity(F)F"
            )
    )
    private float pvpoptimize$transparentNametagBg(GameOptions instance, float fallback) {
        if (PvPOptimizeConfig.get().transparentNametagBg) {
            return 0.0f;
        }
        return fallback;
    }
}