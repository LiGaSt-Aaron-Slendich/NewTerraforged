package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-chunk scratch cache for expensive cave-system queries (mountain massif relief).
 */
public final class CaveMassifCache {
    private static final ThreadLocal<Map<Long, Boolean>> MASSIF = ThreadLocal.withInitial(HashMap::new);

    private CaveMassifCache() {
    }

    public static boolean qualifiesMountainMassif(Generator generator, int seed, int wx, int wz) {
        CaveType type = CaveSystemGrid.dominantType(generator, seed, wx, wz);
        long key = CaveSystemGrid.systemKey(wx, wz, type);
        Map<Long, Boolean> cache = MASSIF.get();
        Boolean cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        boolean result = CaveMassifCache.computeMassif(generator, seed, wx, wz, type);
        cache.put(key, result);
        return result;
    }

    public static void clear() {
        MASSIF.get().clear();
    }

    private static boolean computeMassif(Generator generator, int seed, int wx, int wz, CaveType type) {
        int cx = CaveSystemGrid.snapCenter(wx, type);
        int cz = CaveSystemGrid.snapCenter(wz, type);
        int radius = CaveSystemGrid.caveRadius(type) / 2;
        int minS = Integer.MAX_VALUE;
        int maxS = Integer.MIN_VALUE;
        int sea = generator.getSeaLevel();
        for (int ox = -radius; ox <= radius; ox += 16) {
            for (int oz = -radius; oz <= radius; oz += 16) {
                int s = generator.getOceanFloorHeight(cx + ox, cz + oz);
                if (s <= sea + 4) {
                    continue;
                }
                minS = Math.min(minS, s);
                maxS = Math.max(maxS, s);
            }
        }
        return minS != Integer.MAX_VALUE && maxS - minS >= 38;
    }
}
