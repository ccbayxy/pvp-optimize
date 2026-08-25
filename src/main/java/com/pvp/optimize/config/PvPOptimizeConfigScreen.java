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
}