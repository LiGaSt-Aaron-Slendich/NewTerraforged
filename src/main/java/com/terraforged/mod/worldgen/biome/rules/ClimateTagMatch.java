package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.engine.world.biome.type.BiomeType;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hard climate_tags ∩ cell {@link BiomeType} gate for surface biome pick.
 * Tags alone used to be advisory — hot biomes in a tundra WeightMap leftover still painted.
 */
public final class ClimateTagMatch {
    private ClimateTagMatch() {
    }

    /**
     * @return true if rule has no climate tags (legacy) or at least one tag fits {@code climate}
     */
    public static boolean matches(BiomeType climate, List<String> climateTags) {
        if (climateTags == null || climateTags.isEmpty()) {
            return true;
        }
        if (climate == null) {
            return true;
        }
        Set<String> allowed = allowedTags(climate);
        for (String raw : climateTags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String t = raw.trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> allowedTags(BiomeType climate) {
        return switch (climate) {
            case TUNDRA -> Set.of("cold", "snowy", "tundra", "alpine", "frozen");
            case TAIGA -> Set.of("cold", "taiga", "snowy", "temperate", "boreal");
            case ALPINE -> Set.of("alpine", "cold", "snowy", "tundra", "mountain");
            case COLD_STEPPE -> Set.of("cold", "steppe", "dry", "temperate");
            case TEMPERATE_FOREST -> Set.of("temperate", "warm", "wet", "forest");
            case TEMPERATE_RAINFOREST -> Set.of("temperate", "warm", "wet", "rainforest", "jungle");
            case GRASSLAND -> Set.of("temperate", "warm", "grassland", "plains", "steppe");
            case STEPPE -> Set.of("steppe", "warm", "dry", "temperate", "grassland");
            case SAVANNA -> Set.of("savanna", "hot", "warm", "dry");
            case DESERT -> Set.of("desert", "hot", "dry", "arid", "mesa", "badlands");
            case TROPICAL_RAINFOREST -> Set.of("hot", "wet", "jungle", "tropical", "rainforest", "warm");
            default -> Set.of("temperate", "warm", "cold", "hot", "wet", "dry");
        };
    }
}
