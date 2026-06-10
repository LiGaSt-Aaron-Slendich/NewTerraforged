package com.terraforged.mod.hooks;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.hooks.DatapackHook;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public class WorldGenHook {
    public static final String PRESET_TRANSLATION_KEY = "generator.newterraforged.newterraforged";

    public static boolean isNewTerraForgedSelected(CycleButton<WorldPreset> typeButton) {
        return typeButton != null && WorldGenHook.isNewTerraForgedPreset((WorldPreset)typeButton.getValue());
    }

    public static boolean isNewTerraForgedPreset(WorldPreset preset) {
        TranslatableComponent component;
        Component component2;
        return preset != null && (component2 = preset.description()) instanceof TranslatableComponent && PRESET_TRANSLATION_KEY.equals((component = (TranslatableComponent)component2).getKey());
    }

    public static WorldGenSettings applyIfSelected(CycleButton<WorldPreset> typeButton, RegistryAccess access, WorldGenSettings settings) {
        if (!WorldGenHook.isNewTerraForgedSelected(typeButton) && !DatapackHook.hasNewTerraForgedIntent()) {
            return settings;
        }
        return WorldGenHook.applyGenerator(access, settings);
    }

    public static WorldGenSettings applyGenerator(RegistryAccess access, WorldGenSettings settings) {
        if (access == null) {
            TerraForged.LOG.warn("Cannot apply NewTerraForged generator: registry access is null");
            return settings;
        }
        try {
            Generator generator = GeneratorPreset.build(settings.seed(), TerrainLevels.DEFAULT.get().copy(), access);
            Registry dimensionTypes = access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
            Registry dimensions = WorldGenSettings.withOverworld((Registry)dimensionTypes, (Registry)settings.dimensions(), (ChunkGenerator)generator);
            TerraForged.LOG.info("Applying NewTerraForged generator to world settings");
            DatapackHook.markServerApplyPending();
            return new WorldGenSettings(settings.seed(), settings.generateFeatures(), settings.generateBonusChest(), dimensions);
        }
        catch (Throwable t) {
            TerraForged.LOG.error("Failed to apply NewTerraForged generator", t);
            return settings;
        }
    }
}
