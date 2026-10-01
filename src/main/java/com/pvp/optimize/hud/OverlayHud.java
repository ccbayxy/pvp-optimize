package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class OverlayHud {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
            while (PvPOptimizeConfig.TOGGLE_PARTICLES.wasPressed()) {
                cfg.particlesEnabled = !cfg.particlesEnabled;
                PvPOptimizeConfig.save();
            }
            while (PvPOptimizeConfig.TOGGLE_CULL.wasPressed()) {
                cfg.entityCullingEnabled = !cfg.entityCullingEnabled;
                PvPOptimizeConfig.save();
            }
            while (PvPOptimizeConfig.TOGGLE_RED_OVERLAY.wasPressed()) {
                cfg.redOverlayEnabled = !cfg.redOverlayEnabled;
                PvPOptimizeConfig.save();
            }
        });

        HudRenderCallback.EVENT.register(OverlayHud::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

        if (cfg.redOverlayEnabled) {
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int alpha = (int) (cfg.overlayOpacity * 255.0f);
            int argb = (alpha << 24) | (cfg.overlayColor & 0x00FFFFFF);
            ctx.fill(0, 0, w, h, argb);
        }

        // 药水时间面板 (1.0.1 功能, 1.0.0 没有)
        if (cfg.potionHudEnabled) {
            PotionHudRenderer.render(ctx, mc);
        }
    }
}