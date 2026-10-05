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

        // ============== 屏幕滤镜 ==============
        ConfigCategory overlay = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.overlay"));

        overlay.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.redOverlayEnabled"),
                        data.redOverlayEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.redOverlayEnabled = v)
                .build());

        overlay.addEntry(eb.startIntField(
                        Text.translatable("config.pvp_optimize.overlayColor"),
                        data.overlayColor)
                .setDefaultValue(0x10FF1010)
                .setSaveConsumer(v -> data.overlayColor = v)
                .build());

        overlay.addEntry(eb.startFloatField(
                        Text.translatable("config.pvp_optimize.overlayOpacity"),
                        data.overlayOpacity)
                .setDefaultValue(0.15f)
                .setMin(0.0f).setMax(1.0f)
                .setSaveConsumer(v -> data.overlayOpacity = v)
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
}