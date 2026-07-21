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
            } else {
                world.continent.guaranteedContinentsEnabled = world.islands.guaranteedContinentsEnabled;
            }
            if (islandsTag.contains("guaranteedContinents")) {
                world.continent.guaranteedContinents = islandsTag.getInt("guaranteedContinents");
            } else {
                world.continent.guaranteedContinents = world.islands.guaranteedContinents;
            }
            if (islandsTag.contains("continentsSpread")) {
                world.continent.continentsSpread = islandsTag.getFloat("continentsSpread");
            } else {
                world.continent.continentsSpread = world.islands.continentsSpread;
            }
        }
        // Keep deprecated Islands mirrors in sync for any leftover readers.
        world.islands.guaranteedContinentsEnabled = world.continent.guaranteedContinentsEnabled;
        world.islands.guaranteedContinents = world.continent.guaranteedContinents;
        world.islands.continentsSpread = world.continent.continentsSpread;
    }

    public static void syncIslandsMirror(WorldSettings world) {
        if (world == null) {
            return;
        }
        world.islands.guaranteedContinentsEnabled = world.continent.guaranteedContinentsEnabled;
        world.islands.guaranteedContinents = world.continent.guaranteedContinents;
        world.islands.continentsSpread = world.continent.continentsSpread;
    }

    public static WorldSettings.Islands asIslandsView(WorldSettings world) {
        ContinentGuarantee.syncIslandsMirror(world);
        WorldSettings.Islands view = new WorldSettings.Islands();
        view.guaranteedContinentsEnabled = world.continent.guaranteedContinentsEnabled;
        view.guaranteedContinents = world.continent.guaranteedContinents;
        view.continentsSpread = world.continent.continentsSpread;
        view.coastalIslandsChance = world.islands.coastalIslandsChance;
        view.volcanicIslandsChance = world.islands.volcanicIslandsChance;
        view.scatteredArchipelago = world.islands.scatteredArchipelago;
        view.scatteredArchipelagoChance = world.islands.scatteredArchipelagoChance;
        view.coastalIslands = world.islands.coastalIslands;
        view.volcanicIslands = world.islands.volcanicIslands;
        return view;
    }
}
