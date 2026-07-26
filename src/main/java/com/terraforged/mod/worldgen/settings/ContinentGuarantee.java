package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.WorldSettings;
import net.minecraft.nbt.CompoundTag;

/**
 * Resolves guaranteed-continent knobs from {@link WorldSettings.Continent},
 * with one-shot migration from legacy {@link WorldSettings.Islands} NBT keys.
 */
public final class ContinentGuarantee {
    private ContinentGuarantee() {
    }

    public static void migrateFromLegacyIslands(WorldSettings world, CompoundTag engineSettings) {
        if (world == null) {
            return;
        }
        CompoundTag islandsTag = engineSettings != null
                ? engineSettings.getCompound("world").getCompound("islands")
                : new CompoundTag();
        CompoundTag continentTag = engineSettings != null
                ? engineSettings.getCompound("world").getCompound("continent")
                : new CompoundTag();
        boolean continentHas = continentTag.contains("guaranteedContinents")
                || continentTag.contains("guaranteedContinentsEnabled")
                || continentTag.contains("continentsSpread");
        boolean islandsHas = islandsTag.contains("guaranteedContinents")
                || islandsTag.contains("guaranteedContinentsEnabled")
                || islandsTag.contains("continentsSpread");
        if (!continentHas && islandsHas) {
            if (islandsTag.contains("guaranteedContinentsEnabled")) {
                world.continent.guaranteedContinentsEnabled = islandsTag.getBoolean("guaranteedContinentsEnabled");
            }
            if (islandsTag.contains("guaranteedContinents")) {
                world.continent.guaranteedContinents = islandsTag.getInt("guaranteedContinents");
            }
            if (islandsTag.contains("continentsSpread")) {
                world.continent.continentsSpread = islandsTag.getFloat("continentsSpread");
            }
        }
    }

    /** No-op mirror kept for call sites; guarantee fields live only on Continent now. */
    public static void syncIslandsMirror(WorldSettings world) {
        // Intentionally empty — Islands no longer stores guarantee knobs (avoided #hide marker).
    }

    public static boolean enabled(WorldSettings world) {
        return world != null && world.continent.guaranteedContinentsEnabled;
    }

    public static int count(WorldSettings world) {
        return world == null ? 3 : world.continent.guaranteedContinents;
    }

    public static float spread(WorldSettings world) {
        return world == null ? 0.5F : world.continent.continentsSpread;
    }
}
