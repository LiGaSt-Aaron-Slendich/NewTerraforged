package com.terraforged.mod.client.gui.screen;

import com.terraforged.mod.TerraForged;
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
        long seed = Integer.toUnsignedLong(draft.seed());
        // Prefer seed already written into the edit box / current settings when present.
        if (current.seed() != 0L && draft.seed() == (int) current.seed()) {
            seed = current.seed();
        }
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
        TerraForged.LOG.info("Applied NewTF generator settings (seed={}, continentScale={})", seed, draft.settings().world.continent.continentScale);
    }

    private static void writeSeed(CreateWorldScreen screen, int seed) {
        // Prefer the seed box (selectWorld.enterSeed) over other numeric fields.
        EditBox seedBox = null;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
                var message = box.getMessage();
                if (message != null && message.getString().toLowerCase().contains("seed")) {
                    seedBox = box;
                    break;
                }
                String value = box.getValue();
                if (value.isEmpty()) {
                    seedBox = box;
                    continue;
                }
                try {
                    Long.parseLong(value);
                    if (seedBox == null) {
                        seedBox = box;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (seedBox != null) {
            seedBox.setValue(String.valueOf(Integer.toUnsignedLong(seed)));
        }
    }
}
