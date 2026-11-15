package com.pvp.optimize.hud;

import com.pvp.optimize.PvPOptimizeConfig;
import com.pvp.optimize.dango.DangoBridge;
import com.pvp.optimize.dango.DangoHudFeed;
import com.pvp.optimize.dango.DangoMusicArbiter;
import com.pvp.optimize.gui.ControlPanelScreen;
import com.pvp.optimize.gui.HudEditorScreen;
import com.pvp.optimize.particle.ParticleFilter;
import com.pvp.optimize.perf.AdaptivePerformance;
import com.pvp.optimize.perf.HudCost;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;

/**
 * 每帧同步点：把 HUD 渲染交给 DangoMusic 官方管线，并继续维护优化功能本身。
 *
 * <p>这里没有任何自绘音乐 UI：灵动岛、歌曲卡片、水波纹音浪全部由 Dango 的
 * HudRenderEvent 监听器用它的 GLSL 液态玻璃画布绘制，数据源是
 * {@link DangoHudFeed} 写进去的本地曲目。
 */
public final class OverlayHud {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

            // 本地 / Dango 播放器互斥（两首歌同时响、灵动岛显示错曲目）
            DangoMusicArbiter.tick();
            // 把本地播放器的曲目 / 进度喂给 Dango 灵动岛（仅写 6 个字段，零绘制）
            DangoHudFeed.tick();

            // H：呼出 / 收起 DangoMusic 样式的综合主控板
            while (PvPOptimizeConfig.OPEN_HUD.wasPressed()) {
                if (client.currentScreen instanceof ControlPanelScreen) {
                    client.currentScreen.close();
                } else if (client.currentScreen == null
                        || client.currentScreen instanceof TitleScreen) {
                    ControlPanelScreen.open(ControlPanelScreen.TAB_OVERVIEW);
                }
            }

            // U：打开 HUD 布局编辑器（透明浮层，拖动 / 缩放药水面板）
            while (PvPOptimizeConfig.OPEN_HUD_EDITOR.wasPressed()) {
                if (client.currentScreen == null || client.currentScreen instanceof TitleScreen) {
                    HudEditorScreen.open();
                }
            }

            // 优化开关快捷键（M 归官方 Dango 界面，V/N/B 由 MusicPlayer 处理）
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
        PvPOptimizeConfig.Data cfg = PvPOptimizeConfig.get();

        // HUD 回调是稳定的每帧同步点：采样帧时间、为下一帧重置粒子预算计数。
        AdaptivePerformance.onFrame();
        ParticleFilter.beginFrame();

        // F1 隐藏 HUD 时，本 mod 的所有覆盖层一起让路（否则会盖在「干净截图」上）
        boolean hudHidden = mc.options.hudHidden;
        boolean ownScreen = mc.currentScreen instanceof ControlPanelScreen
                || mc.currentScreen instanceof HudEditorScreen;

        // 官方灵动岛 / 歌曲通知：由 Dango 自己画（我们的面板打开时让路，避免两层叠在一起）
        if (!ownScreen && !hudHidden) {
            HudCost.begin(0);
            try {
                DangoBridge.hud(ctx);
            } finally {
                HudCost.end();
            }
            // 歌词面板：和药水 HUD 同款可调外观，画在灵动岛正下方（位置/缩放可在 HUD 编辑器里改）
            if (mc.world != null) {
                HudCost.begin(1);
                try {
                    LyricsHud.render(ctx, mc, cfg);
                } finally {
                    HudCost.end();
                }
            }
        }

        if (mc.player != null && mc.world != null && !hudHidden) {
            if (cfg.redOverlayEnabled) {
                int w = mc.getWindow().getScaledWidth();
                int h = mc.getWindow().getScaledHeight();
                int alpha = (int) (cfg.overlayOpacity * 255.0f / 100.0f);
                int argb = (alpha << 24) | (cfg.overlayColor & 0x00FFFFFF);
                ctx.fill(0, 0, w, h, argb);
            }

            // 药水时间面板：优化功能本体，保留（位置/大小仍可按配置自定义）。
            // 布局编辑器打开时只画它的预览框，避免两份面板叠在一起。
            if (cfg.potionHudEnabled && !(mc.currentScreen instanceof HudEditorScreen)) {
                HudCost.begin(2);
                try {
                    PotionHudRenderer.render(ctx, mc);
                } finally {
                    HudCost.end();
                }
            }
        }

        // 收口：本帧三段耗时进 2 秒窗口
        HudCost.frameDone();
    }
}
