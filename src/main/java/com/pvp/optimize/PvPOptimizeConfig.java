package com.pvp.optimize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central mod configuration / state holder.
 *
 * <p>2026-09-27 v5: 拆分为 4 区 (粒子 / HUD / 实体 / 屏幕滤镜), 粒子下细分
 * 12 类对标 Sodium 的 "粒子渲染模式" 多档分类. 默认 master = false 关闭
 * 全部过滤 (让 Sodium 接管渲染, 自己只负责 HUD 和配置 UI).</p>
 */
public final class PvPOptimizeConfig {

    private PvPOptimizeConfig() {}

    // ============ Persisted data ============
    public static final class Data {

        // ====== 粒子 master (默认关闭) ======
        /** 总开关. 关 = 不过滤任何粒子 (默认). 开 = 按细类开关筛选. */
        public boolean particlesEnabled = false;

        // ====== 粒子细类 (master 开时生效; 默认全 true 即全放行) ======
        public boolean particleCombat     = true;
        public boolean particleDamage     = true;
        public boolean particleBlock      = true;
        public boolean particleSmoke      = true;
        public boolean particleExplosion  = true;
        public boolean particlePotion     = true;
        public boolean particlePortal     = true;
        public boolean particleBubble     = true;
        public boolean particleFirework   = true;
        public boolean particleFire       = true;
        public boolean particleAmbient    = true;
        public boolean particleExperience = true;

        // ====== 实体剔除 ======
        public boolean entityCullingEnabled = true;
        public double cullDistance = 16.0;

        // ====== 红色滤镜 ======
        public boolean redOverlayEnabled = true;
        public int overlayColor = 0x10FF1010;
        public float overlayOpacity = 0.15f;

        // ====== 药水时间 HUD ======
        public boolean potionHudEnabled = true;
        public boolean potionHudColorByCategory = true;
        public int potionHudMaxLines = 6;
        public String potionHudPosition = "TOP_RIGHT";
        public int potionHudScale = 100;
        public int potionHudBgOpacity = 90;
        public int potionHudBgColor = 0x90000000;
        public int potionHudBeneficialColor = 0xFF55FF55;
        public int potionHudHarmfulColor = 0xFFFF5555;
        public int potionHudNeutralColor = 0xFFFFFFFF;
        public int potionHudTitleColor = 0xFFFFAA00;
        public boolean potionHudShowTitle = true;
        public boolean potionHudShowAmplifier = true;
        public boolean potionHudShowDuration = true;
        public boolean potionHudHideWhenEmpty = true;
    }

    private static final Data DATA = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize.json");

    public static Data get() { return DATA; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            Data loaded = GSON.fromJson(r, Data.class);
            if (loaded == null) return;
            DATA.particlesEnabled    = loaded.particlesEnabled;
            DATA.particleCombat      = loaded.particleCombat;
            DATA.particleDamage      = loaded.particleDamage;
            DATA.particleBlock       = loaded.particleBlock;
            DATA.particleSmoke       = loaded.particleSmoke;
            DATA.particleExplosion   = loaded.particleExplosion;
            DATA.particlePotion      = loaded.particlePotion;
            DATA.particlePortal      = loaded.particlePortal;
            DATA.particleBubble      = loaded.particleBubble;
            DATA.particleFirework    = loaded.particleFirework;
            DATA.particleFire        = loaded.particleFire;
            DATA.particleAmbient     = loaded.particleAmbient;
            DATA.particleExperience  = loaded.particleExperience;
            DATA.entityCullingEnabled = loaded.entityCullingEnabled;
            DATA.cullDistance         = loaded.cullDistance;
            DATA.redOverlayEnabled    = loaded.redOverlayEnabled;
            DATA.overlayColor         = loaded.overlayColor;
            DATA.overlayOpacity       = loaded.overlayOpacity;
            DATA.potionHudEnabled         = loaded.potionHudEnabled;
            DATA.potionHudColorByCategory = loaded.potionHudColorByCategory;
            DATA.potionHudMaxLines        = loaded.potionHudMaxLines;
            DATA.potionHudPosition        = loaded.potionHudPosition != null ? loaded.potionHudPosition : "TOP_RIGHT";
            DATA.potionHudScale           = loaded.potionHudScale > 0 ? loaded.potionHudScale : 100;
            DATA.potionHudBgOpacity       = loaded.potionHudBgOpacity;
            DATA.potionHudBgColor         = loaded.potionHudBgColor;
            DATA.potionHudBeneficialColor = loaded.potionHudBeneficialColor;
            DATA.potionHudHarmfulColor    = loaded.potionHudHarmfulColor;
            DATA.potionHudNeutralColor    = loaded.potionHudNeutralColor;
            DATA.potionHudTitleColor      = loaded.potionHudTitleColor;
            DATA.potionHudShowTitle       = loaded.potionHudShowTitle;
            DATA.potionHudShowAmplifier   = loaded.potionHudShowAmplifier;
            DATA.potionHudShowDuration    = loaded.potionHudShowDuration;
            DATA.potionHudHideWhenEmpty   = loaded.potionHudHideWhenEmpty;
        } catch (IOException e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to read config, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(DATA, w);
            }
        } catch (IOException e) {
            PvPOptimize.LOGGER.warn("[PvP-Optimize] Failed to write config", e);
        }
    }

    // ============ Keybinds ============
    public static final KeyBinding OPEN_HUD = new KeyBinding(
            "key.pvp_optimize.open_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.pvp_optimize");
    public static final KeyBinding TOGGLE_PARTICLES = new KeyBinding(
            "key.pvp_optimize.toggle_particles",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.pvp_optimize");
    public static final KeyBinding TOGGLE_CULL = new KeyBinding(
            "key.pvp_optimize.toggle_cull",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.pvp_optimize");
    public static final KeyBinding TOGGLE_RED_OVERLAY = new KeyBinding(
            "key.pvp_optimize.toggle_red_overlay",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.pvp_optimize");

    public static void register() {
        load();
        KeyBindingHelper.registerKeyBinding(OPEN_HUD);
        KeyBindingHelper.registerKeyBinding(TOGGLE_PARTICLES);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CULL);
        KeyBindingHelper.registerKeyBinding(TOGGLE_RED_OVERLAY);
    }
}