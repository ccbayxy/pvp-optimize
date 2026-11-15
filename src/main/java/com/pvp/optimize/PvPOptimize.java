package com.pvp.optimize;

import com.pvp.optimize.dango.DangoBridge;
import com.pvp.optimize.dango.DangoHudFeed;
import com.pvp.optimize.dango.DangoLyrics;
import com.pvp.optimize.dango.DangoMusicArbiter;
import com.pvp.optimize.gui.ControlPanelScreen;
import com.pvp.optimize.gui.HudEditorScreen;
import com.pvp.optimize.hud.OverlayHud;
import com.pvp.optimize.music.MusicPlayer;
import com.pvp.optimize.perf.CrowdDetector;
import com.pvp.optimize.perf.RuntimeGuard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * pvp-optimize 客户端入口。
 *
 * <p>设置界面是一块按 DangoMusic 官方视觉规格复刻的综合主控板
 * （{@link ControlPanelScreen}）：全部优化功能、快捷键改键、在线搜歌下载、
 * 本地播放器与 HUD 布局入口都在里面，不使用 Cloth Config。
 *
 * <p>内置的 DangoMusic 官方客户端（{@link DangoBridge}）提供原汁原味的
 * 液态玻璃音乐界面与灵动岛，可从主控板内的入口、主菜单按钮或 M 键打开。
 */
public class PvPOptimize implements ClientModInitializer {
    public static final String MOD_ID = "pvp_optimize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        PvPOptimizeConfig.register();
        OverlayHud.register();
        CrowdDetector.register();
        // 本地播放器：歌单扫描、播放控制快捷键、自动续播、退出释放音频设备
        MusicPlayer.register();
        // 官方 DangoMusic UI：GLSL 液态玻璃、Inter 字体、灵动岛；M 键由它自己注册
        DangoBridge.init();
        // 把本地播放器接进 Dango 灵动岛的「外部音源」通道（必须在 Dango 启动之后）
        DangoHudFeed.init();
        // 本地播放器 / Dango 播放器互斥：避免两首歌同时响、以及灵动岛显示错曲目
        DangoMusicArbiter.init();
        // Dango 在线歌词 → 本模组的 2D 歌词面板（它的 3D 世界歌词在 1.20.6 上画不出来）
        DangoLyrics.init();
        registerTitleScreenEntry();
        RuntimeGuard.logStartup();
        LOGGER.info("[PvP-Optimize] Initialized. H = 主控板, U = HUD布局, M = Dango音乐, "
                + "主菜单/ModMenu = 主控板.");
    }

    /** 主菜单左上角三个原版按钮：综合主控板 / Dango 音乐 / HUD 布局编辑器。 */
    private static void registerTitleScreenEntry() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(
                            Text.translatable("gui.pvp_optimize.open_settings"),
                            b -> ControlPanelScreen.open(ControlPanelScreen.TAB_OVERVIEW))
                    .dimensions(4, 4, 116, 20)
                    .build());
            Screens.getButtons(screen).add(ButtonWidget.builder(
                            Text.translatable("gui.pvp_optimize.open_dango"),
                            b -> DangoBridge.open())
                    .dimensions(4, 28, 116, 20)
                    .build());
            Screens.getButtons(screen).add(ButtonWidget.builder(
                            Text.translatable("gui.pvp_optimize.open_hud_editor"),
                            b -> HudEditorScreen.open())
                    .dimensions(4, 52, 116, 20)
                    .build());
        });
    }
}
