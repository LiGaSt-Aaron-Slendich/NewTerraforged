package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;

/**
 * Terrain relief gates for giga spawn (KEEP, TF118-adapted).
 * Ocean/massif helpers deferred — uses generator height samples only.
 */
public final class CaveReliefFilter {
    private static final int LOCAL_RELIEF_MIN_GIGA = 18;
    private static final int LOCAL_RELIEF_MIN_TUNNEL = 14;
    private static final int LOCAL_SAMPLE_RADIUS = 24;
    static final int MIN_TUNNEL_SPAN_SQ = 3600;

    private CaveReliefFilter() {
    }

    public static boolean qualifiesGigaTerrain(Generator generator, int x, int z) {
        return localRelief(generator, x, z) >= LOCAL_RELIEF_MIN_GIGA;
    }

    static boolean qualifiesGigaColumn(Generator generator, int x, int z, float gradient, boolean chunkRelief) {
        if (chunkRelief || gradient >= 0.28f) {
            return true;
        }
        return localRelief(generator, x, z) >= LOCAL_RELIEF_MIN_GIGA;
    }

    public static boolean qualifiesTunnelEndpoint(Generator generator, int seed, int x, int z) {
        return localRelief(generator, x, z) >= LOCAL_RELIEF_MIN_TUNNEL;
    }

    public static boolean validatesTunnelSpan(int mouthX, int mouthZ, int exitX, int exitZ) {
        int dx = mouthX - exitX;
        int dz = mouthZ - exitZ;
        return dx * dx + dz * dz >= MIN_TUNNEL_SPAN_SQ;
    }

    static int localRelief(Generator generator, int x, int z) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int ox = -LOCAL_SAMPLE_RADIUS; ox <= LOCAL_SAMPLE_RADIUS; ox += LOCAL_SAMPLE_RADIUS) {
            for (int oz = -LOCAL_SAMPLE_RADIUS; oz <= LOCAL_SAMPLE_RADIUS; oz += LOCAL_SAMPLE_RADIUS) {
                int h = sampleHeight(generator, x + ox, z + oz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        return min == Integer.MAX_VALUE ? 0 : max - min;
    }

    private static int sampleHeight(Generator generator, int x, int z) {
        try {
            return generator.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, null);
        } catch (Throwable ignored) {
            return generator.getSeaLevel();
        }
    }
}
