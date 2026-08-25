package com.pvp.optimize.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * Hook for Mod Menu: registers our Cloth Config screen as the mod's
 * configuration UI. The screen itself is built in {@link PvPOptimizeConfigScreen}.
 *
 * Mod Menu 10+ uses ConfigScreenFactory<Screen>; older versions had a
 * non-generic factory. Mod Menu is a soft dependency so we list the
 * import against the resolved version (10.0.0) declared in gradle.properties.
 */
public class PvPOptimizeModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> PvPOptimizeConfigScreen.create(parent);
    }
}