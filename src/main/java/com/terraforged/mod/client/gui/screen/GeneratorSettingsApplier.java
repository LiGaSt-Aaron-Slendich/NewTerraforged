package com.terraforged.mod.client.gui.screen;

import com.terraforged.mod.TerraForged;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/**
 * Stub for 1.19 first green build — Customize → apply needs WorldPreset / GeneratorSettings port.
 */
public final class GeneratorSettingsApplier {
    private GeneratorSettingsApplier() {}

    public static void apply(CreateWorldScreen screen, SettingsDraft draft) {
        TerraForged.LOG.warn("GeneratorSettingsApplier.apply deferred on 1.19 port (seed={})", draft.seed());
    }
}
