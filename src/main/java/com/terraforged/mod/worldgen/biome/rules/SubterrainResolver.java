package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.ITerrain;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;

/** Derives active subterrain (+ steep slope flag) from the climate/terrain sample. */
public final class SubterrainResolver {
    public static final String NONE = "none";

    private SubterrainResolver() {
    }

    public static String resolve(ClimateSample sample) {
        if (sample == null || sample.terrainType == null) {
            return NONE;
        }
        Terrain t = sample.terrainType;
        if (t.isRiver() || t == TerrainType.RIVER) {
            return "river_bank";
        }
        if (t == TerrainType.BEACH || "beach".equalsIgnoreCase(t.getName())) {
            return "ocean_beach";
        }
        if (t.isLake() || t == TerrainType.LAKE) {
            if (matchesKind(t, TerrainType.MOUNTAINS) || sample.heightNoise > 0.62F) {
                return "mountain_lake";
            }
            return "lake_shore";
        }
        if (t == ModTerrainTypes.LAGUNA) {
            return "ocean_beach";
        }
        // Coastal fringe: mountain / high coasts → steep shore; only flat exits keep beach.
        if (sample.continentNoise > 0.5F && sample.continentNoise <= 0.505F) {
            if (isMountainCoast(sample, t)) {
                return "steep_shore";
            }
            return "ocean_beach";
        }
        // Near-coast mountain landforms (mega-ridge meets sea) also get steep shore.
        if (sample.continentNoise > 0.505F && sample.continentNoise < 0.58F && isMountainCoast(sample, t)) {
            return "steep_shore";
        }
        if (matchesKind(t, TerrainType.BADLANDS) || "badlands".equalsIgnoreCase(t.getName())) {
            if (sample.climateType == BiomeType.DESERT || sample.climateType == BiomeType.SAVANNA) {
                return "desert_canyon";
            }
            return "canyon";
        }
        if (matchesKind(t, TerrainType.MOUNTAINS)
                || matchesKind(t, TerrainType.MOUNTAIN_CHAIN)
                || t == ModTerrainTypes.DOLOMITES
                || t == ModTerrainTypes.ISLAND_MOUNTAINS) {
            if (t == ModTerrainTypes.DOLOMITES && sample.moisture < 0.35F) {
                return "bare_dolomites";
            }
            float h = sample.heightNoise;
            if (h >= 0.78F) {
                return sample.moisture < 0.3F ? "bare_mountain_peak" : "mountain_peak";
            }
            if (h <= 0.48F) {
                return "mountain_foothill";
            }
            return sample.moisture < 0.28F ? "bare_mountain" : "mountain_body";
        }
        if (t.isVolcano() || t == ModTerrainTypes.ISLAND_VOLCANO || t == TerrainType.VOLCANO || t == TerrainType.VOLCANO_PIPE) {
            return "volcanic_beach";
        }
        return NONE;
    }

    /** Steep / high-mountain slope context for {@code can_be_on_slope}. */
    public static boolean isSteepSlope(ClimateSample sample, String subterrain) {
        if (subterrain != null) {
            switch (subterrain) {
                case "steep_shore", "bare_mountain", "bare_mountain_peak", "bare_dolomites", "mountain_peak" -> {
                    return true;
                }
                default -> {
                }
            }
        }
        if (sample == null || sample.terrainType == null) {
            return false;
        }
        Terrain t = sample.terrainType;
        if (matchesKind(t, TerrainType.MOUNTAINS)
                || matchesKind(t, TerrainType.MOUNTAIN_CHAIN)
                || t == ModTerrainTypes.DOLOMITES
                || t == ModTerrainTypes.ISLAND_MOUNTAINS) {
            return sample.heightNoise >= 0.62F;
        }
        return false;
    }

    private static boolean isMountainCoast(ClimateSample sample, Terrain t) {
        if (sample.heightNoise > 0.52F) {
            return true;
        }
        return matchesKind(t, TerrainType.MOUNTAINS)
                || matchesKind(t, TerrainType.MOUNTAIN_CHAIN)
                || t == ModTerrainTypes.DOLOMITES
                || t == ModTerrainTypes.ISLAND_MOUNTAINS
                || (t.getName() != null && t.getName().toLowerCase().contains("mountain"));
    }

    private static boolean matchesKind(Terrain terrain, Terrain kind) {
        Terrain current = terrain;
        while (current != null) {
            if (current == kind) {
                return true;
            }
            if (current.getName() != null && kind.getName() != null && current.getName().equalsIgnoreCase(kind.getName())) {
                return true;
            }
            ITerrain delegate = current.getDelegate();
            if (!(delegate instanceof Terrain next) || next == current) {
                return false;
            }
            current = next;
        }
        return false;
    }
}
