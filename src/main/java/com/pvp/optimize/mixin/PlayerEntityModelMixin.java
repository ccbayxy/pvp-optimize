package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the multi-layer skin overlay parts on player models when the
 * {@code hidePlayerSkinLayers} toggle is on:
 *   jacket, leftSleeve, rightSleeve, leftPants, rightPants.
 *
 * The base (head, hat, body, arms, legs) and equipment rendering still work
 * because {@link net.minecraft.client.render.entity.feature.ArmorFeatureRenderer}
 * and {@link net.minecraft.client.render.entity.PlayerEntityRenderer} manage
 * those separately.
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {

    @Shadow public net.minecraft.client.model.ModelPart leftSleeve;
    @Shadow public net.minecraft.client.model.ModelPart rightSleeve;
    @Shadow public net.minecraft.client.model.ModelPart leftPants;
    @Shadow public net.minecraft.client.model.ModelPart rightPants;
    @Shadow public net.minecraft.client.model.ModelPart jacket;

    /**
     * Runs AFTER the vanilla body of {@code setVisible}. By the time the
     * model is drawn, our TAIL injection has hidden the layers we don't
     * care about. Only acts when {@code visible == true} was passed in,
     * so we don't fight with the body going invisible.
     */
    @Inject(method = "setVisible(Z)V", at = @At("TAIL"))
    private void pvpoptimize$hideSkinLayers(boolean visible, CallbackInfo ci) {
        if (!visible) return;
        if (!PvPOptimizeConfig.get().hidePlayerSkinLayers) return;
        this.leftSleeve.visible  = false;
        this.rightSleeve.visible = false;
        this.leftPants.visible   = false;
        this.rightPants.visible  = false;
        this.jacket.visible      = false;
    }
}