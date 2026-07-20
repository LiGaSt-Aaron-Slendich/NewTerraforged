package com.terraforged.mod.client.gui.screen;

import com.terraforged.mod.client.screen.ScreenUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;

/** Applies {@link SettingsDraft} to the Create World overworld {@link LevelStem}. */
public final class GeneratorSettingsApplier {
    private GeneratorSettingsApplier() {
    }

    public static void apply(CreateWorldScreen screen, SettingsDraft draft) {
        draft.applyToSettings();
        writeSeed(screen, draft.seed());

        RegistryAccess access = screen.worldGenSettingsComponent.registryHolder();
        WorldGenSettings current = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
        long seed = draft.seed();
        GeneratorSettings generatorSettings = draft.toGeneratorSettings();
        Generator generator = GeneratorPreset.build(seed, draft.levels(), generatorSettings, access);

        Registry<LevelStem> dimensions = WorldGenSettings.withOverworld(
                access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
                current.dimensions(),
                generator
        );
        WorldGenSettings updated = new WorldGenSettings(seed, current.generateFeatures(), current.generateBonusChest(), dimensions);
        screen.worldGenSettingsComponent.updateSettings(updated);
        ScreenUtil.enforceDefaultPreset(screen, "newterraforged");
    }

    private static void writeSeed(CreateWorldScreen screen, int seed) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
                String value = box.getValue();
                if (value.isEmpty()) {
                    box.setValue(String.valueOf(seed));
                    return;
                }
                try {
                    Long.parseLong(value);
                    box.setValue(String.valueOf(seed));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}
