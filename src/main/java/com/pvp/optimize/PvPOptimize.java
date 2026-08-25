package com.pvp.optimize;

import com.pvp.optimize.hud.OverlayHud;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvPOptimize implements ClientModInitializer {
    public static final String MOD_ID = "pvp_optimize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Register keybinds first (must happen before any tick callback fires)
        PvPOptimizeConfig.register();
        // Then attach callbacks
        OverlayHud.register();
        LOGGER.info("[PvP-Optimize] Initialized. Press H to toggle the status panel.");
    }
}