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

    public static boolean isCoastalCliffColumn(Generator generator, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        int surface = generator.getOceanFloorHeight(worldX, worldZ);
        if (surface <= sea) {
            return false;
        }
        if (CaveOceanFilter.sampleHeightGradient(generator, worldX, worldZ) < 0.5f) {
            return false;
        }
        return CaveOceanFilter.hasOceanNearby(generator, worldX, worldZ);
    }

    public static boolean isNearSea(Generator generator, int worldX, int worldZ) {
        if (CaveOceanFilter.hasOceanNearby(generator, worldX, worldZ)) {
            return true;
        }
        return generator.getTerrainSample((int)worldX, (int)worldZ).continentNoise < 0.6f;
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

    private static float sampleHeightGradient(Generator generator, int x, int z) {
        NoiseSample north = generator.getTerrainSample(x, z - 4);
        NoiseSample south = generator.getTerrainSample(x, z + 4);
        NoiseSample east = generator.getTerrainSample(x + 4, z);
        NoiseSample west = generator.getTerrainSample(x - 4, z);
        float dx = east.heightNoise - west.heightNoise;
        float dz = south.heightNoise - north.heightNoise;
        return NoiseUtil.clamp(NoiseUtil.sqrt(dx * dx + dz * dz), 0.0f, 1.0f);
    }

    private static boolean hasOceanNearby(Generator generator, int x, int z) {
        int sea = generator.getSeaLevel();
        for (int dist = 16; dist <= 64; dist += 16) {
            if (!CaveOceanFilter.isOpenOceanColumn(generator, x + dist, z, sea) && !CaveOceanFilter.isOpenOceanColumn(generator, x - dist, z, sea) && !CaveOceanFilter.isOpenOceanColumn(generator, x, z + dist, sea) && !CaveOceanFilter.isOpenOceanColumn(generator, x, z - dist, sea)) continue;
            return true;
        }
        return false;
    }

    private static boolean isOpenOceanColumn(Generator generator, int x, int z, int sea) {
        if (generator.getOceanFloorHeight(x, z) <= sea) {
            return true;
        }
        return generator.getTerrainSample((int)x, (int)z).continentNoise < 0.5f;
    }
}
