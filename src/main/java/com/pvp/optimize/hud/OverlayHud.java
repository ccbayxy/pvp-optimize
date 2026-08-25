package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class OverlayHud {

    private static boolean hudVisible = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();
            while (PvPOptimizeConfig.OPEN_HUD.wasPressed()) {
                hudVisible = !hudVisible;
            }
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

        // 1. Full-screen red filter (红色滤镜)
        if (cfg.redOverlayEnabled) {
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int color = cfg.overlayColor;
            int alpha = (int) (cfg.overlayOpacity * 255.0f);
            int argb = (alpha << 24) | (color & 0x00FFFFFF);
            ctx.fill(0, 0, w, h, argb);
        }

        // 2. Status panel
        if (!hudVisible) return;
        int x = 4;
        int y = 4;
        int lineHeight = 12;
        int padding = 4;

        Text[] lines = {
                Text.literal("PvP-Optimize"),
                Text.literal("粒子: " + onOff(cfg.particlesEnabled)
                        + "  (暴击=" + onOff(cfg.keepCritParticles)
                        + " 受击=" + onOff(cfg.keepDamageParticles)
                        + " 药水=" + onOff(cfg.keepPotionParticles)
                        + " 经验=" + onOff(cfg.keepXpParticles) + ")"),
                Text.literal("实体剔除: " + onOff(cfg.entityCullingEnabled)
                        + "  半径=" + ((int) cfg.cullDistance) + "格"),
                Text.literal("红色滤镜: " + onOff(cfg.redOverlayEnabled)
                        + "  透明度=" + String.format("%.2f", cfg.overlayOpacity)),
        };

        int width = 0;
        for (Text t : lines) {
            int w = mc.textRenderer.getWidth(t);
            if (w > width) width = w;
        }
        int boxH = lines.length * lineHeight + padding * 2;

        ctx.fill(x, y, x + width + padding * 2, y + boxH, 0x90000000);
        for (int i = 0; i < lines.length; i++) {
            ctx.drawText(mc.textRenderer, lines[i], x + padding, y + padding + i * lineHeight, 0xFFFFFFFF, false);
        }
    }

    private static String onOff(boolean b) { return b ? "开" : "关"; }
}