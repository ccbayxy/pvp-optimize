package com.pvp.optimize.mixin;

import com.pvp.optimize.PvPOptimizeConfig;
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
 * 距离 LOD: 玩家超过 hideSkinLayersDistance 时隐藏多层皮肤 (jacket / sleeves / pants),
 * 保留装备/武器渲染 (ArmorFeatureRenderer 独立路径)。
 *
 * setVisible 拿不到当前渲染的 entity, 通过 RenderContext (由 EntityRenderer.render
 * 入口写入的 ThreadLocal) 获取。
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

        // 距离 LOD 检查
        if (cfg.hideSkinLayersDistance > 0.0) {
            net.minecraft.entity.Entity entity = RenderContext.get();
            if (entity instanceof PlayerEntity) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && mc.player != entity) {
                    double distSq = mc.player.squaredDistanceTo(entity);
                    double maxDistSq = cfg.hideSkinLayersDistance * cfg.hideSkinLayersDistance;
                    if (distSq > maxDistSq) {
                        return; // 距离超阈值, 啥也不做 (setVisible(true) 已经把基体 body 标 visible, 我们的图层保持原样)
                    }
                }
            }
        }

        // 距离内 (或开关关闭) -> 隐藏多层皮肤, 保留基体
        this.leftSleeve.visible  = false;
        this.rightSleeve.visible = false;
        this.leftPants.visible   = false;
        this.rightPants.visible  = false;
        this.jacket.visible      = false;
    }
}