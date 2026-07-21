package com.terraforged.mod.client.gui.screen;

import com.terraforged.mod.client.gui.screen.preset.PresetBrowserScreen;
import com.terraforged.mod.client.gui.screen.preset.SavePresetScreen;
import net.minecraft.client.Minecraft;

/** Preset save/import entry points for the Customize screen. */
public final class PresetWidgets {
    private PresetWidgets() {
    }

    public static void save(ConfigScreen screen, SettingsDraft draft) {
        Minecraft.getInstance().setScreen(new SavePresetScreen(screen, draft));
    }

    public static void openImport(ConfigScreen screen, SettingsDraft draft, Runnable onImported) {
        Minecraft.getInstance().setScreen(new PresetBrowserScreen(screen, draft, onImported));
    }
}
