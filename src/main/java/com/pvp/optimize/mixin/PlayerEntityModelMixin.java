package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.perf.CrowdDetector;
import com.pvp.optimize.util.RenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 皮肤层隐藏策略 (1.0.5):
 *
 *   - 默认: 玩家距离 > 16 格时隐藏多层皮肤 (保留装备/武器)
 *   - 群体隐身: 32 格内 6+ 玩家 + 当前帧 fps 低于阈值 -> 隐藏皮肤层, 名字保留
 *   - hidePlayerSkinLayers 开关关闭 -> 不走任何隐藏逻辑
 *
 * 装备 / 武器由 ArmorFeatureRenderer 独立路径渲染, 不受本 mixin 影响。
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {

    @Shadow public net.minecraft.client.model.ModelPart leftSleeve;
    @Shadow public net.minecraft.client.model.ModelPart rightSleeve;
    @Shadow public net.minecraft.client.model.ModelPart leftPants;
    @Shadow public net.minecraft.client.model.ModelPart rightPants;
    @Shadow public net.minecraft.client.model.ModelPart jacket;

    @Inject(method = "setVisible(Z)V", at = @At("TAIL"))
    private void pvpoptimize$hideSkinLayers(boolean visible, CallbackInfo ci) {
        if (!visible) return;
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
        if (!cfg.hidePlayerSkinLayers) return;

        boolean shouldHide = false;

        // 1) 距离 LOD: > 16 格默认隐藏
        if (cfg.hideSkinLayersDistance > 0.0) {
            net.minecraft.entity.Entity entity = RenderContext.get();
            if (entity instanceof PlayerEntity) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && mc.player != entity) {
                    double distSq = mc.player.squaredDistanceTo(entity);
                    double maxDistSq = cfg.hideSkinLayersDistance * cfg.hideSkinLayersDistance;
                    if (distSq > maxDistSq) {
                        shouldHide = true;
                    }
                }
            }
        }

        // 2) 群体隐身: 6+ 玩家聚集 + 掉帧 -> 隐藏皮肤层 (名字仍保留)
        if (!shouldHide && CrowdDetector.shouldHideSkinInCrowd(cfg.crowdPlayerThreshold, cfg.crowdFpsThreshold)) {
            shouldHide = true;
        }

        if (!shouldHide) return;

        this.leftSleeve.visible  = false;
        this.rightSleeve.visible = false;
        this.leftPants.visible   = false;
        this.rightPants.visible  = false;
        this.jacket.visible      = false;
    }
}