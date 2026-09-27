package com.pvp.optimize.config;

import com.pvp.optimize.PvPOptimizeConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;


import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

/**
 * Mod Menu 配置屏幕 (2026-09-27 v5: Sodium 风格重写).
 *
 * <p>4 个大区:</p>
 * <ol>
 *   <li><b>粒子过滤</b> —— master + 12 个细类 (模拟 Sodium 粒子渲染模式分类)</li>
 *   <li><b>HUD 增强</b> —— 药水时间面板的位置/缩放/颜色/字体 全套</li>
 *   <li><b>实体剔除</b> —— 16 块半径的 PvP 优化</li>
 *   <li><b>屏幕滤镜</b> —— 红色受击遮罩</li>
 * </ol>
 *
 * <p>默认 master = false, 即 mod 自身不做任何粒子过滤, 让 Sodium / 原版
 * 渲染器接管. UI 上 12 个细类选项保留 (用户可开 master 后逐类精调).</p>
 */
public final class PvPOptimizeConfigScreen {

    private PvPOptimizeConfigScreen() {}

    private static final List<String> POSITION_OPTIONS = Arrays.asList(
            "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT");

    public static Screen create(Screen parent) {
        PvPOptimizeConfig.Data data = PvPOptimizeConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.pvp_optimize.title"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ============================================================
        // 区域 1: 粒子过滤 (12 类 + master)
        // ============================================================
        ConfigCategory particles = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.particles"));

        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particlesEnabled"),
                        data.particlesEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.pvp_optimize.particlesEnabled.tooltip"))
                .setSaveConsumer(v -> data.particlesEnabled = v)
                .build());

        // ---- 子组: 战斗 ----
        particles.addEntry(subHeader(eb, "config.pvp_optimize.particleGroup.combat"));
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleCombat"),
                        data.particleCombat)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.particleCombat.tooltip"))
                .setSaveConsumer(v -> data.particleCombat = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleDamage"),
                        data.particleDamage)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.pvp_optimize.particleDamage.tooltip"))
                .setSaveConsumer(v -> data.particleDamage = v)
                .build());

        // ---- 子组: 方块/破坏 ----
        particles.addEntry(subHeader(eb, "config.pvp_optimize.particleGroup.world"));
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleBlock"),
                        data.particleBlock)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleBlock = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleSmoke"),
                        data.particleSmoke)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleSmoke = v)
                .build());

        // ---- 子组: 战斗/特效 ----
        particles.addEntry(subHeader(eb, "config.pvp_optimize.particleGroup.combatFx"));
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleExplosion"),
                        data.particleExplosion)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleExplosion = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particlePotion"),
                        data.particlePotion)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particlePotion = v)
                .build());

        // ---- 子组: 环境 ----
        particles.addEntry(subHeader(eb, "config.pvp_optimize.particleGroup.ambient"));
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particlePortal"),
                        data.particlePortal)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particlePortal = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleBubble"),
                        data.particleBubble)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleBubble = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleFirework"),
                        data.particleFirework)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleFirework = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleFire"),
                        data.particleFire)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleFire = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleAmbient"),
                        data.particleAmbient)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleAmbient = v)
                .build());
        particles.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.particleExperience"),
                        data.particleExperience)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.particleExperience = v)
                .build());

        // ============================================================
        // 区域 2: HUD 增强 (位置 + 外观 + 颜色)
        // ============================================================
        ConfigCategory hud = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.hud"));

        // 总开关
        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudEnabled"),
                        data.potionHudEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudEnabled = v)
                .build());

        // 位置 + 缩放 + 隐藏行为
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

        // 显示项开关
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

        // 颜色分类
        hud.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.potionHudColorByCategory"),
                        data.potionHudColorByCategory)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.potionHudColorByCategory = v)
                .build());

        // 颜色
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

        // ============================================================
        // 区域 3: 实体剔除
        // ============================================================
        ConfigCategory culling = builder.getOrCreateCategory(
                Text.translatable("config.pvp_optimize.category.culling"));

        culling.addEntry(eb.startBooleanToggle(
                        Text.translatable("config.pvp_optimize.entityCullingEnabled"),
                        data.entityCullingEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> data.entityCullingEnabled = v)
                .build());

        culling.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.cullDistance"),
                        (int) data.cullDistance, 4, 64)
                .setDefaultValue(16)
                .setSaveConsumer(v -> data.cullDistance = v)
                .build());

        // ============================================================
        // 区域 4: 屏幕滤镜
        // ============================================================
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

        overlay.addEntry(eb.startIntSlider(
                        Text.translatable("config.pvp_optimize.overlayOpacity"),
                        (int) (data.overlayOpacity * 100), 0, 100)
                .setDefaultValue(15)
                .setSaveConsumer(v -> data.overlayOpacity = v / 100.0f)
                .build());

        builder.setSavingRunnable(PvPOptimizeConfig::save);

        return builder.build();
    }

    


    // ============ helpers ============

    private static AbstractConfigListEntry subHeader(ConfigEntryBuilder eb, String key) {
        return eb.startTextDescription(Text.translatable(key)).build();
    }

    private static HudPosition parsePos(String s) {
        try {
            return HudPosition.valueOf(s);
        } catch (IllegalArgumentException e) {
            return HudPosition.TOP_RIGHT;
        }
    }

    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}