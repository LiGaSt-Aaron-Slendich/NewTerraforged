package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiered river proximity: region coarse gate, chunk refine, column uses {@link CarverColumnCache}.
 */
final class CaveRiverProximityCache {
    private static final int REGION_SHIFT = 7;
    private static final int REGION_SIZE = 1 << REGION_SHIFT;
    private static final float RIVER_COLUMN = 0.72f;
    private static final float RIVER_CHUNK = 0.78f;
    private static final float DRY_REGION = 0.92f;
    private static final byte UNKNOWN = 0;
    private static final byte DRY = 1;
    private static final byte WET = 2;
    private static final ThreadLocal<Long2ByteOpenHashMap> REGION = ThreadLocal.withInitial(Long2ByteOpenHashMap::new);
    private static final ThreadLocal<Map<Long, Boolean>> CHUNK = ThreadLocal.withInitial(HashMap::new);

    private CaveRiverProximityCache() {
    }

    static void clear() {
        REGION.get().clear();
        CHUNK.get().clear();
    }

    static boolean columnNearRiver(Generator generator, int worldX, int worldZ) {
        return generator.getTerrainSample(worldX, worldZ).riverNoise < RIVER_COLUMN;
    }

    static boolean chunkMayHaveRiver(Generator generator, int chunkCenterX, int chunkCenterZ) {
        long key = CaveRiverProximityCache.chunkKey(chunkCenterX, chunkCenterZ);
        Map<Long, Boolean> cache = CHUNK.get();
        Boolean cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!CaveRiverProximityCache.regionMayHaveRiver(generator, chunkCenterX, chunkCenterZ)) {
            cache.put(key, false);
            return false;
        }
        NoiseSample center = generator.getTerrainSample(chunkCenterX, chunkCenterZ);
        boolean wet = center.riverNoise < RIVER_CHUNK;
        if (!wet) {
            wet = CaveRiverProximityCache.columnNearRiver(generator, chunkCenterX + 16, chunkCenterZ)
                    || CaveRiverProximityCache.columnNearRiver(generator, chunkCenterX - 16, chunkCenterZ)
                    || CaveRiverProximityCache.columnNearRiver(generator, chunkCenterX, chunkCenterZ + 16)
                    || CaveRiverProximityCache.columnNearRiver(generator, chunkCenterX, chunkCenterZ - 16);
        }
        cache.put(key, wet);
        return wet;
    }

    private static boolean regionMayHaveRiver(Generator generator, int worldX, int worldZ) {
        int rx = worldX >> REGION_SHIFT;
        int rz = worldZ >> REGION_SHIFT;
        long key = CaveRiverProximityCache.pack(rx, rz);
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
        if (center.riverNoise >= DRY_REGION) {
            cache.put(key, DRY);
            return false;
        }
        boolean wet = center.riverNoise < RIVER_CHUNK;
        if (!wet) {
            for (int ox = -REGION_SIZE / 2; ox <= REGION_SIZE / 2; ox += REGION_SIZE / 2) {
                for (int oz = -REGION_SIZE / 2; oz <= REGION_SIZE / 2; oz += REGION_SIZE / 2) {
                    if (generator.getTerrainSample(centerX + ox, centerZ + oz).riverNoise >= RIVER_CHUNK) {
                        continue;
                    }
                    wet = true;
                    break;
                }
                if (wet) {
                    break;
                }
            }
        }
        cache.put(key, wet ? WET : DRY);
        return wet;
    }

    private static long chunkKey(int chunkCenterX, int chunkCenterZ) {
        return CaveRiverProximityCache.pack(chunkCenterX >> 4, chunkCenterZ >> 4);
    }

    private static long pack(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }
}
