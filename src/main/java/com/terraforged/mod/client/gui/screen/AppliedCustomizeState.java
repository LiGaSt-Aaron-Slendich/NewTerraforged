package com.terraforged.mod.client.gui.screen;

import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import javax.annotation.Nullable;

/**
 * Remembers the last Customize → Done snapshot for the Create World session.
 * Survives World Type cycle rebuilds that would otherwise drop back to factory defaults.
 */
public final class AppliedCustomizeState {
    private static volatile boolean present;
    private static volatile GeneratorSettings settings = GeneratorSettings.DEFAULT;
    private static volatile TerrainLevels levels = TerrainLevels.DEFAULT.get();
    private static volatile long seed = -1L;

    private AppliedCustomizeState() {
    }

    public static void store(GeneratorSettings generatorSettings, TerrainLevels terrainLevels, long worldSeed) {
        settings = generatorSettings;
        levels = terrainLevels.copy();
        seed = worldSeed;
        present = true;
    }

    public static void clear() {
        present = false;
        settings = GeneratorSettings.DEFAULT;
        levels = TerrainLevels.DEFAULT.get();
        seed = -1L;
    }

    public static boolean present() {
        return present;
    }

    @Nullable
    public static GeneratorSettings settings() {
        return present ? settings : null;
    }

    @Nullable
    public static TerrainLevels levels() {
        return present ? levels.copy() : null;
    }

    public static long seed() {
        return seed;
    }
}
