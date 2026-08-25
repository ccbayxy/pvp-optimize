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
        public boolean keepCritParticles = true;
        public boolean keepDamageParticles = true;
        public boolean keepPotionParticles = true;
        public boolean keepXpParticles = true;

        public boolean entityCullingEnabled = true;
        public double cullDistance = 16.0;

        // 红色滤镜 (red filter / full-screen overlay)
        public boolean redOverlayEnabled = true;
        public int overlayColor = 0x10FF1010;     // ARGB
        public float overlayOpacity = 0.15f;
    }

    private static final Data DATA = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("pvp_optimize.json");

    public static Data get() { return DATA; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save(); // first run: write defaults
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            Data loaded = GSON.fromJson(r, Data.class);
            if (loaded != null) {
                DATA.particlesEnabled = loaded.particlesEnabled;
                DATA.keepCritParticles = loaded.keepCritParticles;
            DATA.keepDamageParticles = loaded.keepDamageParticles;
                DATA.keepPotionParticles = loaded.keepPotionParticles;
                DATA.keepXpParticles = loaded.keepXpParticles;
                DATA.entityCullingEnabled = loaded.entityCullingEnabled;
                DATA.cullDistance = loaded.cullDistance;
                DATA.redOverlayEnabled = loaded.redOverlayEnabled;
                DATA.overlayColor = loaded.overlayColor;
                DATA.overlayOpacity = loaded.overlayOpacity;
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