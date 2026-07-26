package com.terraforged.mod.compat.legacy;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** 1.18 BiomeCategory stand-in for 1.19+. */
public enum BiomeCategory {
    NONE, TAIGA, EXTREME_HILLS, JUNGLE, MESA, PLAINS, SAVANNA, ICY, THEEND,
    BEACH, FOREST, OCEAN, DESERT, RIVER, SWAMP, MUSHROOM, NETHER, UNDERGROUND, MOUNTAIN;

    public static BiomeCategory getBiomeCategory(Holder<Biome> biome) {
        LegacyBiomeCategory legacy = LegacyBiomeCategory.of(biome);
        try {
            return BiomeCategory.valueOf(legacy.name());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
