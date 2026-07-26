package com.terraforged.mod.compat.legacy;

/**
 * 1.18 BiomeCategory stand-in for 1.19+ (vanilla removed BiomeCategory).
 * Used by NewTF biome/cave rule helpers until fully migrated to tags.
 */
public enum BiomeCategory {
    NONE,
    TAIGA,
    EXTREME_HILLS,
    JUNGLE,
    MESA,
    PLAINS,
    SAVANNA,
    ICY,
    THEEND,
    BEACH,
    FOREST,
    OCEAN,
    DESERT,
    RIVER,
    SWAMP,
    MUSHROOM,
    NETHER,
    UNDERGROUND,
    MOUNTAIN
}
