package com.pvp.optimize.config;

import com.pvp.optimize.gui.ControlPanelScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * Mod Menu 的设置入口指向 DangoMusic 样式的综合主控板，而不是 Cloth Config。
 *
 * <p>主控板里包含全部优化开关 / 滑块 / 颜色 / 枚举、快捷键改键、在线搜歌下载、
 * 本地播放器、HUD 布局编辑器，以及打开内置 DangoMusic 官方音乐界面的入口。
 */
public class PvPOptimizeModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> new ControlPanelScreen(parent);
    }
}
