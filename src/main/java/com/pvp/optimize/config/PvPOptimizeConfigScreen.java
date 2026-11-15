package com.pvp.optimize.config;

import com.pvp.optimize.PvPOptimize;
import com.pvp.optimize.PvPOptimizeConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Cloth-Config backed settings screen shown via Mod Menu.
 *
 * All labels are in Chinese because the mod targets zh-CN users. The
 * title/label/texts go through {@link Text#translatable} with keys in
 * {@code assets/pvp_optimize/lang/zh_cn.json} so the file can be
 * translated without recompiling.
 */
public final class PvPOptimizeConfigScreen {

    private PvPOptimizeConfigScreen() {}

    public static Screen create(Screen parent) {
        PvPOptimizeConfig.Data data = PvPOptimizeConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.pvp_optimize.title"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ============== 粒子过滤 ==============
        ConfigCategory particles = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.particles"));

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particlesEnabled"),
                        data.particlesEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particlesEnabled = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepCritParticles"),
                        data.keepCritParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepCritParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepDamageParticles"),
                        data.keepDamageParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepDamageParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepPotionParticles"),
                        data.keepPotionParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepPotionParticles = v)
                .build());

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.keepXpParticles"),
                        data.keepXpParticles)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.keepXpParticles = v)
                .build());


        // ============== HUD 增强 (药水时间, 1.0.1) ==============
        ConfigCategory hud = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.hud"));

        // 自定义布局：既能在这里填坐标，也能按 U / 主菜单按钮打开可视化拖动编辑器。
        // 音乐灵动岛由内置 DangoMusic 官方界面渲染，不受下面这些坐标影响。
        hud.addEntry(eb.startTextDescription(
                Text.translatable("config.pvp_optimize.hudEditorHint")).build());

        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.hudCustomLayoutEnabled"),
                        !Boolean.FALSE.equals(data.hudCustomLayoutEnabled))
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.hudCustomLayoutEnabled = v)
                .build());

        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudX"),
                        data.potionHudX)
                .setDefaultValue(-1)
                .setSaveConsumer(v -> data.potionHudX = v)
                .build());

        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudY"),
                        data.potionHudY)
                .setDefaultValue(8)
                .setSaveConsumer(v -> data.potionHudY = v)
                .build());

        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudEnabled"),
                        data.potionHudEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudEnabled = v)
                .build());

        hud.addEntry(eb.startEnumSelector(
                        Text.translatable("config.pvp_optimize.potionHudPosition"),
                        HudPosition.class,
                        parsePos(data.potionHudPosition))
                .setDefaultValue(HudPosition.TOP_RIGHT)
                .setSaveConsumer(v -> data.potionHudPosition = v.name())
                .build());

        hud.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.potionHudScale"),
                        data.potionHudScale, 50, 200)
                .setDefaultValue(100)
                .setSaveConsumer(v -> data.potionHudScale = v)
                .build());

        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudMaxLines"),
                        data.potionHudMaxLines)
                .setDefaultValue(6)
                .setMin(0).setMax(20)
                .setSaveConsumer(v -> data.potionHudMaxLines = v)
                .build());

        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudHideWhenEmpty"),
                        data.potionHudHideWhenEmpty)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudHideWhenEmpty = v)
                .build());

        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudShowTitle"),
                        data.potionHudShowTitle)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudShowTitle = v)
                .build());
        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudShowAmplifier"),
                        data.potionHudShowAmplifier)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudShowAmplifier = v)
                .build());
        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudShowDuration"),
                        data.potionHudShowDuration)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudShowDuration = v)
                .build());

        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudColorByCategory"),
                        data.potionHudColorByCategory)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudColorByCategory = v)
                .build());

        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudBgColor"),
                        data.potionHudBgColor)
                .setDefaultValue(0x90000000)
                .setSaveConsumer(v -> data.potionHudBgColor = v)
                .build());
        hud.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.potionHudBgOpacity"),
                        data.potionHudBgOpacity, 0, 100)
                .setDefaultValue(90)
                .setSaveConsumer(v -> data.potionHudBgOpacity = v)
                .build());
        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudBeneficialColor"),
                        data.potionHudBeneficialColor)
                .setDefaultValue(0xFF55FF55)
                .setSaveConsumer(v -> data.potionHudBeneficialColor = v)
                .build());
        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudHarmfulColor"),
                        data.potionHudHarmfulColor)
                .setDefaultValue(0xFFFF5555)
                .setSaveConsumer(v -> data.potionHudHarmfulColor = v)
                .build());
        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudNeutralColor"),
                        data.potionHudNeutralColor)
                .setDefaultValue(0xFFFFFFFF)
                .setSaveConsumer(v -> data.potionHudNeutralColor = v)
                .build());
        hud.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.potionHudTitleColor"),
                        data.potionHudTitleColor)
                .setDefaultValue(0xFFFFAA00)
                .setSaveConsumer(v -> data.potionHudTitleColor = v)
                .build());
        // ============== 实体剔除 ==============
        ConfigCategory culling = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.culling"));

        culling.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.entityCullingEnabled"),
                        data.entityCullingEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.entityCullingEnabled = v)
                .build());

        culling.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.cullDistance"),
                        data.cullDistance)
                .setDefaultValue(16.0)
                .setMin(1.0).setMax(64.0)
                .setSaveConsumer(v -> data.cullDistance = v)
                .build());

        // ============== 玩家渲染 ==============
        ConfigCategory player = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.player"));

        player.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.hidePlayerSkinLayers"),
                        data.hidePlayerSkinLayers)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.hidePlayerSkinLayers = v)
                .build());
        player.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.hideSkinLayersDistance"),
                        data.hideSkinLayersDistance)
                .setDefaultValue(16.0)
                .setMin(0.0).setMax(64.0)
                .setSaveConsumer(v -> data.hideSkinLayersDistance = v)
                .build());
        player.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.transparentNametagBg"),
                        data.transparentNametagBg)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.transparentNametagBg = v)
                .build());
        player.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.hideNametagsDistance"),
                        data.hideNametagsDistance)
                .setDefaultValue(16.0)
                .setMin(0.0).setMax(64.0)
                .setSaveConsumer(v -> data.hideNametagsDistance = v)
                .build());
        player.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.crowdPlayerThreshold"),
                        data.crowdPlayerThreshold, 3, 20)
                .setDefaultValue(6)
                .setSaveConsumer(v -> data.crowdPlayerThreshold = v)
                .build());
        player.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.crowdFpsThreshold"),
                        data.crowdFpsThreshold)
                .setDefaultValue(50.0)
                .setMin(10.0).setMax(120.0)
                .setSaveConsumer(v -> data.crowdFpsThreshold = v)
                .build());
        player.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.hideFarEmptyPlayers"),
                        data.hideFarEmptyPlayers)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.hideFarEmptyPlayers.tooltip"))
                .setSaveConsumer(v -> data.hideFarEmptyPlayers = v)
                .build());

        // ============== 屏幕滤镜 ==============
        ConfigCategory overlay = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.overlay"));

        overlay.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.redOverlayEnabled"),
                        data.redOverlayEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.redOverlayEnabled = v)
                .build());

        overlay.addEntry(eb.startEnumSelector(
                        Text.translatable("config.pvp_optimize.overlayColor"),
                        OverlayColorPreset.class,
                        OverlayColorPreset.fromArgb(data.overlayColor))
                .setDefaultValue(OverlayColorPreset.DARK_RED)
                .setSaveConsumer(v -> data.overlayColor = v.argb)
                .build());

        overlay.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.overlayOpacity"),
                        data.overlayOpacity, 0, 100)
                .setDefaultValue(15)
                .setSaveConsumer(v -> data.overlayOpacity = v)
                .build());

        // ============== 激进优化 ==============
        ConfigCategory agg = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.aggressive"));

        agg.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.aggressiveEnabled"),
                        data.aggressiveEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.aggressiveEnabled = v)
                .build());

        agg.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.tileEntityCullingEnabled"),
                        data.tileEntityCullingEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.tileEntityCullingEnabled = v)
                .build());

        agg.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.tileEntityCullDistance"),
                        data.tileEntityCullDistance)
                .setDefaultValue(24.0)
                .setMin(4.0).setMax(64.0)
                .setSaveConsumer(v -> data.tileEntityCullDistance = v)
                .build());

        agg.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.animationFreezeEnabled"),
                        data.animationFreezeEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.animationFreezeEnabled = v)
                .build());

        agg.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.animationFreezeDistance"),
                        data.animationFreezeDistance)
                .setDefaultValue(16.0)
                .setMin(4.0).setMax(64.0)
                .setSaveConsumer(v -> data.animationFreezeDistance = v)
                .build());

        agg.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.soundThrottleEnabled"),
                        data.soundThrottleEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.soundThrottleEnabled = v)
                .build());

        agg.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.soundThrottleDistance"),
                        data.soundThrottleDistance)
                .setDefaultValue(32.0)
                .setMin(4.0).setMax(64.0)
                .setSaveConsumer(v -> data.soundThrottleDistance = v)
                .build());

        // ====== 1.0.6-v1 运行时稳定性 ======
        ConfigCategory stability = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.stability"));

        stability.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.overlapGuardEnabled"),
                        data.overlapGuardEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.overlapGuardEnabled.tooltip"))
                .setSaveConsumer(v -> data.overlapGuardEnabled = v)
                .build());

        stability.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.entityCullingGuardEnabled"),
                        Boolean.TRUE.equals(data.entityCullingGuardEnabled))
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.pvp_optimize.entityCullingGuardEnabled.tooltip"))
                .setSaveConsumer(v -> data.entityCullingGuardEnabled = v)
                .build());

        stability.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.memoryAdaptiveEnabled"),
                        data.memoryAdaptiveEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.pvp_optimize.memoryAdaptiveEnabled.tooltip"))
                .setSaveConsumer(v -> data.memoryAdaptiveEnabled = v)
                .build());

        stability.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.diagnosticsLogEnabled"),
                        data.diagnosticsLogEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.diagnosticsLogEnabled.tooltip"))
                .setSaveConsumer(v -> data.diagnosticsLogEnabled = v)
                .build());

        // ====== 1.1.0 新增优化 (借鉴 OpenFine 思路, 独立实现) ======
        ConfigCategory advanced = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.advanced"));

        advanced.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.adaptiveEnabled"),
                        data.adaptiveEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.adaptiveEnabled.tooltip"))
                .setSaveConsumer(v -> data.adaptiveEnabled = v)
                .build());

        advanced.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.adaptiveMinScale"),
                        data.adaptiveMinScale)
                .setDefaultValue(0.6)
                .setMin(0.4).setMax(1.0)
                .setSaveConsumer(v -> data.adaptiveMinScale = v)
                .build());

        advanced.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleDistanceCullEnabled"),
                        data.particleDistanceCullEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.particleDistanceCullEnabled.tooltip"))
                .setSaveConsumer(v -> data.particleDistanceCullEnabled = v)
                .build());

        advanced.addEntry(eb.startDoubleField(
                        Text.translatable("config.pvp_optimize.particleCullDistance"),
                        data.particleCullDistance)
                .setDefaultValue(24.0)
                .setMin(4.0).setMax(64.0)
                .setSaveConsumer(v -> data.particleCullDistance = v)
                .build());

        advanced.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleBudgetEnabled"),
                        data.particleBudgetEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.particleBudgetEnabled.tooltip"))
                .setSaveConsumer(v -> data.particleBudgetEnabled = v)
                .build());

        advanced.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.particleMaxPerFrame"),
                        data.particleMaxPerFrame)
                .setDefaultValue(200)
                .setMin(0).setMax(2000)
                .setSaveConsumer(v -> data.particleMaxPerFrame = v)
                .build());

        advanced.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.smartTextureAnimationsEnabled"),
                        data.smartTextureAnimationsEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.smartTextureAnimationsEnabled.tooltip"))
                .setSaveConsumer(v -> data.smartTextureAnimationsEnabled = v)
                .build());

        // ====== 暂停界面 (1.2.0 新增) ======
        ConfigCategory pauseMenu = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.pause_menu"));

        pauseMenu.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.pauseMenuRelocateEnabled"),
                        data.pauseMenuRelocateEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.pauseMenuRelocateEnabled.tooltip"))
                .setSaveConsumer(v -> data.pauseMenuRelocateEnabled = v)
                .build());

        pauseMenu.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.pauseMenuExitButtonX"),
                        data.pauseMenuExitButtonX)
                .setDefaultValue(4)
                .setMin(0).setMax(2000)
                .setTooltip(Text.translatable("config.pvp_optimize.pauseMenuPosition.tooltip"))
                .setSaveConsumer(v -> data.pauseMenuExitButtonX = v)
                .build());

        pauseMenu.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.pauseMenuExitButtonY"),
                        data.pauseMenuExitButtonY)
                .setDefaultValue(4)
                .setMin(0).setMax(2000)
                .setTooltip(Text.translatable("config.pvp_optimize.pauseMenuPosition.tooltip"))
                .setSaveConsumer(v -> data.pauseMenuExitButtonY = v)
                .build());

        pauseMenu.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.pauseMenuExitButtonWidth"),
                        data.pauseMenuExitButtonWidth)
                .setDefaultValue(0)
                .setMin(0).setMax(2000)
                .setTooltip(Text.translatable("config.pvp_optimize.pauseMenuExitButtonWidth.tooltip"))
                .setSaveConsumer(v -> data.pauseMenuExitButtonWidth = v)
                .build());

        // ====== 游戏内音乐播放器 (1.3.0 新增) ======
        ConfigCategory music = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.music"));

        music.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.musicPlayerEnabled"),
                        data.musicPlayerEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.musicPlayerEnabled.tooltip"))
                .setSaveConsumer(v -> data.musicPlayerEnabled = v)
                .build());

        music.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.musicHudEnabled"),
                        data.musicHudEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.musicHudEnabled = v)
                .build());

        music.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.musicVolume"),
                        data.musicVolume, 0, 100)
                .setDefaultValue(70)
                .setSaveConsumer(v -> {
                    data.musicVolume = v;
                    com.pvp.optimize.music.MusicPlayer.setVolume(v);
                })
                .build());

        music.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.musicAutoPlay"),
                        data.musicAutoPlay)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.pvp_optimize.musicAutoPlay.tooltip"))
                .setSaveConsumer(v -> data.musicAutoPlay = v)
                .build());

        music.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.musicDuckVanilla"),
                        data.musicDuckVanilla)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.musicDuckVanilla.tooltip"))
                .setSaveConsumer(v -> data.musicDuckVanilla = v)
                .build());

        builder.setSavingRunnable(PvPOptimizeConfig::save);

        return builder.build();
    }

    private static HudPosition parsePos(String s) {
        try { return HudPosition.valueOf(s); }
        catch (IllegalArgumentException e) { return HudPosition.TOP_RIGHT; }
    }

    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public enum OverlayColorPreset {
        DARK_RED(0xFFFF1010, "config.pvp_optimize.overlayColor.darkRed"),
        RED(0xFFFF0000, "config.pvp_optimize.overlayColor.red"),
        ORANGE(0xFFFF8800, "config.pvp_optimize.overlayColor.orange"),
        YELLOW(0xFFFFFF00, "config.pvp_optimize.overlayColor.yellow"),
        GREEN(0xFF33FF33, "config.pvp_optimize.overlayColor.green"),
        CYAN(0xFF00FFFF, "config.pvp_optimize.overlayColor.cyan"),
        BLUE(0xFF2244FF, "config.pvp_optimize.overlayColor.blue"),
        PURPLE(0xFF8800FF, "config.pvp_optimize.overlayColor.purple"),
        BLACK(0xFF000000, "config.pvp_optimize.overlayColor.black"),
        WHITE(0xFFFFFFFF, "config.pvp_optimize.overlayColor.white");

        public final int argb;
        private final String key;

        OverlayColorPreset(int argb, String key) {
            this.argb = argb;
            this.key = key;
        }

        @Override
        public String toString() {
            return Text.translatable(key).getString();
        }

        public static OverlayColorPreset fromArgb(int argb) {
            for (OverlayColorPreset p : values()) {
                if (p.argb == argb) return p;
            }
            return DARK_RED;
        }
    }
}