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
 * The user-tweakable values are stored in {@link Data} and persisted to
 * {@code config/pvp_optimize.json} so they survive game restarts. Keybinds
 * are registered in {@link #register()} which the mod entry point calls
 * from {@code onInitializeClient} (must run before the first tick).
 */
public final class PvPOptimizeConfig {

    private PvPOptimizeConfig() {}

    // ============ Persisted data ============
    public static final class Data {
        public boolean particlesEnabled = true;

        /**
         * 保留所有 PARTICLE_SHEET_LIT 表粒子 (fix-2026-09-19 v2):
         *   - SweepAttack (剑横扫弧光 / 平砍粒子)
         *   - Crit       (暴击星)
         *   - Damage     (伤害红心)
         *   - FireworkSpark (烟花)
         *   - Note       (音符盒音符)
         *   - AngryVillager
         * 这些都是 PvP 材质包 (pvp16x / 动态材质) 主要修改的 generic_* 贴图所在表。
         */
        public boolean keepLitParticles = true;

        public boolean keepPotionParticles = true;
        public boolean keepXpParticles = true;

        // 向后兼容: 老字段保留但默认 false (新逻辑下用户用 keepLitParticles 即可)
        public boolean keepCritParticles = false;
        public boolean keepDamageParticles = false;
        public boolean keepSweepParticles = false;

        public boolean entityCullingEnabled = true;
        public double cullDistance = 16.0;

        // 红色滤镜 (red filter / full-screen overlay)
        public boolean redOverlayEnabled = true;
        public int overlayColor = 0x10FF1010;     // ARGB
        public float overlayOpacity = 0.15f;

        // ===== 药水时间 HUD (2026-09-27 新增) =====
        public boolean potionHudEnabled = true;
        public boolean potionHudColorByCategory = true;
        public int potionHudMaxLines = 6;   // 0 = 不限
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
            if (loaded != null) {
                DATA.particlesEnabled = loaded.particlesEnabled;
                DATA.keepLitParticles = loaded.keepLitParticles;
                DATA.keepPotionParticles = loaded.keepPotionParticles;
                DATA.keepXpParticles = loaded.keepXpParticles;
                // 老字段如果存在则读, 否则使用默认值 (false)
                DATA.keepCritParticles = loaded.keepCritParticles;
                DATA.keepDamageParticles = loaded.keepDamageParticles;
                DATA.keepSweepParticles = loaded.keepSweepParticles;
                DATA.entityCullingEnabled = loaded.entityCullingEnabled;
                DATA.cullDistance = loaded.cullDistance;
                DATA.redOverlayEnabled = loaded.redOverlayEnabled;
                DATA.overlayColor = loaded.overlayColor;
                DATA.overlayOpacity = loaded.overlayOpacity;
                DATA.potionHudEnabled = loaded.potionHudEnabled;
                DATA.potionHudColorByCategory = loaded.potionHudColorByCategory;
                DATA.potionHudMaxLines = loaded.potionHudMaxLines;
            }
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
    /** 红色滤镜开关 (J) */
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