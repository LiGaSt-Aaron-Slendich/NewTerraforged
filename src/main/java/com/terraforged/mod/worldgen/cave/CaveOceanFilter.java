package com.terraforged.mod.worldgen.cave;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;

public final class CaveOceanFilter {
    private static final float SHALLOW_OCEAN_CONTINENT = 0.25f;
    private static final float BEACH_CONTINENT = 0.5f;
    private static final float NEAR_SEA_CONTINENT = 0.6f;
    private static final float STEEP_SLOPE = 0.5f;
    private static final int OCEAN_SEARCH_RADIUS = 64;
    private static final int OCEAN_SEARCH_STEP = 16;
    private static final int SLOPE_SAMPLE_STEP = 4;

    private CaveOceanFilter() {
    }

    public static boolean isBlockedForMegaGiga(Generator generator, CaveType type, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        int surface = generator.getOceanFloorHeight(worldX, worldZ);
        if (surface <= sea) {
            return true;
        }
        if (CaveOceanFilter.isDeepOcean(generator, worldX, worldZ)) {
            return true;
        }
        if (!CaveOceanFilter.isNearSea(generator, worldX, worldZ)) {
            return false;
        }
        return !CaveOceanFilter.isCoastalCliffColumn(generator, worldX, worldZ);
    }

    public static void clearProximityCache() {
        CaveOceanProximityCache.clear();
        CaveRiverProximityCache.clear();
    }

    public static boolean isCoastalCliffColumn(Generator generator, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        int surface = generator.getOceanFloorHeight(worldX, worldZ);
        if (surface <= sea) {
            return false;
        }
        if (generator.getTerrainSample(worldX, worldZ).continentNoise >= 0.78f) {
            return false;
        }
        if (CaveOceanFilter.sampleHeightGradient(generator, worldX, worldZ) < 0.5f) {
            return false;
        }
        return CaveOceanFilter.isNearSea(generator, worldX, worldZ);
    }

    public static boolean isNearSea(Generator generator, int worldX, int worldZ) {
        return CaveOceanProximityCache.isNearSea(generator, worldX, worldZ);
    }

    /** Column surface is at or below sea level (river/lake/ocean water). */
    public static boolean isSurfaceWaterColumn(Generator generator, int worldX, int worldZ) {
        return generator.getOceanFloorHeight(worldX, worldZ) <= generator.getSeaLevel();
    }

    /**
     * Hillside near water but not submerged — suitable for a ramp entrance with a stream, not a ceiling breach.
     */
    public static boolean qualifiesRiverEntranceVicinity(Generator generator, int worldX, int worldZ) {
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, worldX, worldZ)) {
            return false;
        }
        if (!CaveOceanFilter.isNearSea(generator, worldX, worldZ)) {
            return false;
        }
        return CaveOceanFilter.sampleHeightGradient(generator, worldX, worldZ) >= 0.35f;
    }

    /** Offset a mouth position away from submerged columns while staying near the watercourse. */
    public static int[] offsetMouthFromWater(Generator generator, int mouthX, int mouthZ, float ux, float uz, int minOffset, int maxOffset) {
        if (!CaveOceanFilter.isSurfaceWaterColumn(generator, mouthX, mouthZ)) {
            return new int[]{mouthX, mouthZ};
        }
        float perpX = -uz;
        float perpZ = ux;
        for (int dist = minOffset; dist <= maxOffset; dist += 2) {
            for (int side = -1; side <= 1; side += 2) {
                int cx = mouthX + Math.round(perpX * (float)dist * (float)side);
                int cz = mouthZ + Math.round(perpZ * (float)dist * (float)side);
                if (CaveOceanFilter.isSurfaceWaterColumn(generator, cx, cz)) {
                    continue;
                }
                if (generator.getOceanFloorHeight(cx, cz) <= generator.getSeaLevel() + 2) {
                    continue;
                }
                return new int[]{cx, cz};
            }
        }
        int upX = mouthX + Math.round(ux * (float)maxOffset);
        int upZ = mouthZ + Math.round(uz * (float)maxOffset);
        if (!CaveOceanFilter.isSurfaceWaterColumn(generator, upX, upZ)) {
            return new int[]{upX, upZ};
        }
        return new int[]{mouthX, mouthZ};
    }

    public static boolean isOcean(Generator generator, int worldX, int worldZ) {
        return CaveOceanFilter.isDeepOcean(generator, worldX, worldZ);
    }

    public static boolean isOcean(TerrainData terrainData, int localX, int localZ) {
        if (terrainData == null) {
            return false;
        }
        Terrain terrain = terrainData.getTerrain().get(localX, localZ);
        return terrain == TerrainType.DEEP_OCEAN || terrain == TerrainType.SHALLOW_OCEAN || terrain == TerrainType.COAST;
    }

    private static boolean isDeepOcean(Generator generator, int worldX, int worldZ) {
        return generator.getTerrainSample((int)worldX, (int)worldZ).continentNoise < 0.25f;
    }

    static float sampleHeightGradient(Generator generator, int x, int z) {
        NoiseSample north = generator.getTerrainSample(x, z - 4);
        NoiseSample south = generator.getTerrainSample(x, z + 4);
        NoiseSample east = generator.getTerrainSample(x + 4, z);
        NoiseSample west = generator.getTerrainSample(x - 4, z);
        float dx = east.heightNoise - west.heightNoise;
        float dz = south.heightNoise - north.heightNoise;
        return NoiseUtil.clamp(NoiseUtil.sqrt(dx * dx + dz * dz), 0.0f, 1.0f);
    }
}
