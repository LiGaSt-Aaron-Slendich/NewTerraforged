package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiered wet/dry proximity cache: one coarse sample per region, optional chunk refine, column checks last.
 */
final class CaveOceanProximityCache {
    private static final int REGION_SHIFT = 7;
    private static final int REGION_SIZE = 1 << REGION_SHIFT;
    private static final int COARSE_OCEAN_RADIUS = 128;
    private static final int COARSE_OCEAN_STEP = 32;
    private static final int FINE_OCEAN_RADIUS = 64;
    private static final int FINE_OCEAN_STEP = 16;
    private static final float INLAND_CONTINENT = 0.78f;
    private static final float NEAR_SEA_CONTINENT = 0.6f;
    private static final byte UNKNOWN = 0;
    private static final byte DRY = 1;
    private static final byte WET = 2;
    private static final ThreadLocal<Long2ByteOpenHashMap> REGION = ThreadLocal.withInitial(Long2ByteOpenHashMap::new);
    private static final ThreadLocal<Map<Long, Boolean>> CHUNK = ThreadLocal.withInitial(HashMap::new);

    private CaveOceanProximityCache() {
    }

    static void clear() {
        REGION.get().clear();
        CHUNK.get().clear();
    }

    static boolean isNearSea(Generator generator, int worldX, int worldZ) {
        if (!CaveOceanProximityCache.regionMayBeNearSea(generator, worldX, worldZ)) {
            return false;
        }
        return CaveOceanProximityCache.isNearSeaFine(generator, worldX, worldZ);
    }

    static boolean chunkNearSea(Generator generator, int chunkCenterX, int chunkCenterZ) {
        long key = CaveOceanProximityCache.chunkKey(chunkCenterX, chunkCenterZ);
        Map<Long, Boolean> cache = CHUNK.get();
        Boolean cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!CaveOceanProximityCache.regionMayBeNearSea(generator, chunkCenterX, chunkCenterZ)) {
            cache.put(key, false);
            return false;
        }
        boolean result = CaveOceanProximityCache.hasOceanNearby(generator, chunkCenterX, chunkCenterZ, FINE_OCEAN_RADIUS, FINE_OCEAN_STEP) || generator.getTerrainSample(chunkCenterX, chunkCenterZ).continentNoise < NEAR_SEA_CONTINENT;
        cache.put(key, result);
        return result;
    }

    private static boolean regionMayBeNearSea(Generator generator, int worldX, int worldZ) {
        int rx = worldX >> REGION_SHIFT;
        int rz = worldZ >> REGION_SHIFT;
        long key = CaveOceanProximityCache.pack(rx, rz);
        Long2ByteOpenHashMap cache = REGION.get();
        byte flag = cache.getOrDefault(key, UNKNOWN);
        if (flag == DRY) {
            return false;
        }
        if (flag == WET) {
            return true;
        }
        int centerX = (rx << REGION_SHIFT) + (REGION_SIZE >> 1);
        int centerZ = (rz << REGION_SHIFT) + (REGION_SIZE >> 1);
        NoiseSample center = generator.getTerrainSample(centerX, centerZ);
        if (center.continentNoise >= INLAND_CONTINENT) {
            cache.put(key, DRY);
            return false;
        }
        boolean wet = center.continentNoise < NEAR_SEA_CONTINENT || CaveOceanProximityCache.hasOceanNearby(generator, centerX, centerZ, COARSE_OCEAN_RADIUS, COARSE_OCEAN_STEP);
        cache.put(key, wet ? WET : DRY);
        return wet;
    }

    private static boolean isNearSeaFine(Generator generator, int worldX, int worldZ) {
        if (CaveOceanProximityCache.hasOceanNearby(generator, worldX, worldZ, FINE_OCEAN_RADIUS, FINE_OCEAN_STEP)) {
            return true;
        }
        return generator.getTerrainSample(worldX, worldZ).continentNoise < NEAR_SEA_CONTINENT;
    }

    private static boolean hasOceanNearby(Generator generator, int x, int z, int maxRadius, int step) {
        int sea = generator.getSeaLevel();
        for (int dist = step; dist <= maxRadius; dist += step) {
            if (CaveOceanProximityCache.isOpenOceanColumn(generator, x + dist, z, sea) || CaveOceanProximityCache.isOpenOceanColumn(generator, x - dist, z, sea) || CaveOceanProximityCache.isOpenOceanColumn(generator, x, z + dist, sea) || CaveOceanProximityCache.isOpenOceanColumn(generator, x, z - dist, sea)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpenOceanColumn(Generator generator, int x, int z, int sea) {
        if (generator.getOceanFloorHeight(x, z) <= sea) {
            return true;
        }
        return generator.getTerrainSample(x, z).continentNoise < 0.5f;
    }

    private static long chunkKey(int chunkCenterX, int chunkCenterZ) {
        return CaveOceanProximityCache.pack(chunkCenterX >> 4, chunkCenterZ >> 4);
    }

    private static long pack(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }
}
