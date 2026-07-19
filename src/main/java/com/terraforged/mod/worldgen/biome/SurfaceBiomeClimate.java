package com.terraforged.mod.worldgen.biome;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.ITerrain;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainCategory;
import com.terraforged.engine.world.terrain.TerrainType;

public final class SurfaceBiomeClimate {
    private SurfaceBiomeClimate() {
    }

    public static BiomeType adjustForTerrain(BiomeType climate, Terrain terrain, float temperature, float moisture) {
        if (climate == null || terrain == null) {
            return climate;
        }
        if (terrain.isSubmerged() || terrain.isDeepOcean() || terrain.isShallowOcean()) {
            return climate;
        }
        if (terrain.isCoast() || terrain.isRiver() || terrain.isLake()) {
            return climate;
        }
        if (terrain.isMountain() || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.MOUNTAINS) || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.MOUNTAIN_CHAIN)) {
            return SurfaceBiomeClimate.alpineForMountains(climate, temperature, moisture);
        }
        if (terrain.isVolcano() || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.VOLCANO)) {
            return SurfaceBiomeClimate.volcanicClimate(climate, temperature);
        }
        if (SurfaceBiomeClimate.matchesKind(terrain, TerrainType.BADLANDS)) {
            return SurfaceBiomeClimate.badlandsClimate(climate, moisture);
        }
        if (terrain.isWetland() || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.WETLAND)) {
            return SurfaceBiomeClimate.wetterClimate(climate, temperature);
        }
        if (SurfaceBiomeClimate.matchesKind(terrain, TerrainType.PLATEAU) || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.HILLS)) {
            return SurfaceBiomeClimate.highlandClimate(climate, temperature, moisture);
        }
        if (terrain.isFlat() || SurfaceBiomeClimate.matchesKind(terrain, TerrainType.FLATS) || terrain.getCategory() == TerrainCategory.FLATLAND) {
            return SurfaceBiomeClimate.lowlandClimate(climate, moisture);
        }
        return climate;
    }

    private static BiomeType alpineForMountains(BiomeType climate, float temperature, float moisture) {
        if (climate == BiomeType.DESERT && temperature > 0.62f && moisture < 0.38f) {
            return climate;
        }
        if (climate == BiomeType.TROPICAL_RAINFOREST) {
            return BiomeType.TEMPERATE_RAINFOREST;
        }
        if (climate == BiomeType.SAVANNA && temperature > 0.72f && moisture < 0.45f) {
            return BiomeType.STEPPE;
        }
        return BiomeType.ALPINE;
    }

    private static BiomeType volcanicClimate(BiomeType climate, float temperature) {
        if (climate == BiomeType.TUNDRA || climate == BiomeType.TAIGA) {
            return climate;
        }
        return temperature > 0.58f ? BiomeType.SAVANNA : BiomeType.GRASSLAND;
    }

    private static BiomeType badlandsClimate(BiomeType climate, float moisture) {
        if (climate == BiomeType.TUNDRA || climate == BiomeType.TAIGA) {
            return climate;
        }
        return moisture < 0.55f ? BiomeType.DESERT : BiomeType.SAVANNA;
    }

    private static BiomeType wetterClimate(BiomeType climate, float temperature) {
        return switch (climate) {
            case DESERT, STEPPE, COLD_STEPPE, SAVANNA -> {
                if (temperature > 0.65f) {
                    yield BiomeType.TROPICAL_RAINFOREST;
                }
                yield BiomeType.TEMPERATE_RAINFOREST;
            }
            case GRASSLAND -> {
                if (temperature > 0.65f) {
                    yield BiomeType.TROPICAL_RAINFOREST;
                }
                yield BiomeType.TEMPERATE_FOREST;
            }
            case TEMPERATE_FOREST -> BiomeType.TEMPERATE_RAINFOREST;
            default -> climate;
        };
    }

    private static BiomeType highlandClimate(BiomeType climate, float temperature, float moisture) {
        if (temperature < 0.38f) {
            return BiomeType.ALPINE;
        }
        if (moisture < 0.38f) {
            return temperature < 0.55f ? BiomeType.COLD_STEPPE : BiomeType.STEPPE;
        }
        if (climate == BiomeType.GRASSLAND || climate == BiomeType.STEPPE || climate == BiomeType.COLD_STEPPE) {
            return BiomeType.TEMPERATE_FOREST;
        }
        return climate;
    }

    private static BiomeType lowlandClimate(BiomeType climate, float moisture) {
        return switch (climate) {
            case STEPPE, COLD_STEPPE -> {
                if (moisture > 0.45f) {
                    yield BiomeType.GRASSLAND;
                }
                yield climate;
            }
            case TEMPERATE_RAINFOREST -> {
                if (moisture < 0.42f) {
                    yield BiomeType.TEMPERATE_FOREST;
                }
                yield climate;
            }
            default -> climate;
        };
    }

    private static boolean matchesKind(Terrain terrain, Terrain kind) {
        Terrain current = terrain;
        while (current != null) {
            Terrain next;
            if (current == kind) {
                return true;
            }
            ITerrain delegate = current.getDelegate();
            if (!(delegate instanceof Terrain) || (next = (Terrain)delegate) == current) {
                return false;
            }
            current = next;
        }
        return false;
    }
}
