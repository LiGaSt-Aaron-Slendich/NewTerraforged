package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.NoiseUtil;

public final class CaveSystemGrid {
    private static final float GIGA_DOMINANCE = 0.12f;

    private CaveSystemGrid() {
    }

    public static CaveType dominantType(int seed, int x, int z) {
        if (CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > GIGA_DOMINANCE) {
            return CaveType.GIGA;
        }
        return CaveType.MEGA;
    }

    public static CaveType dominantType(Generator generator, int seed, int x, int z) {
        if (CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > GIGA_DOMINANCE
                && CaveReliefFilter.qualifiesGigaTerrain(generator, x, z)) {
            return CaveType.GIGA;
        }
        return CaveType.MEGA;
    }

    public static int caveRadius(CaveType type) {
        return switch (type) {
            case GIGA -> 400;
            case MEGA -> 250;
            default -> 250;
        };
    }

    public static int snapCenter(int coord, CaveType type) {
        int radius = CaveSystemGrid.caveRadius(type);
        int cell = radius * 2;
        return Math.floorDiv(coord, cell) * cell + radius;
    }

    public static long systemKey(int x, int z, CaveType type) {
        int cx = CaveSystemGrid.snapCenter(x, type);
        int cz = CaveSystemGrid.snapCenter(z, type);
        return CaveSystemGrid.pack(cx, cz, type);
    }

    public static boolean isEntranceAnchorColumn(int seed, int x, int z, CaveType type) {
        return CaveSystemGrid.isAnchorNear(seed, x, z, type, -462356479);
    }

    public static int[] resolveTunnelMouthAnchor(int seed, CaveType type, int refX, int refZ) {
        int cx = CaveSystemGrid.snapCenter(refX, type);
        int cz = CaveSystemGrid.snapCenter(refZ, type);
        return CaveSystemGrid.resolveAnchor(seed, cx, cz, type, -462356479);
    }

    public static int[] resolveTunnelExit(int mouthX, int mouthZ, CaveType type) {
        int cx = CaveSystemGrid.snapCenter(mouthX, type);
        int cz = CaveSystemGrid.snapCenter(mouthZ, type);
        return new int[]{cx * 2 - mouthX, cz * 2 - mouthZ};
    }

    public static boolean isTunnelExitAnchorColumn(int seed, int x, int z, CaveType type, int mouthX, int mouthZ) {
        int cz;
        int exitZ;
        int dz;
        int cx = CaveSystemGrid.snapCenter(mouthX, type);
        int exitX = cx * 2 - mouthX;
        int dx = x - exitX;
        return dx * dx + (dz = z - (exitZ = (cz = CaveSystemGrid.snapCenter(mouthZ, type)) * 2 - mouthZ)) * dz <= 16;
    }

    private static boolean isAnchorNear(int seed, int x, int z, CaveType type, int salt) {
        int cx = CaveSystemGrid.snapCenter(x, type);
        int cz = CaveSystemGrid.snapCenter(z, type);
        int[] anchor = CaveSystemGrid.resolveAnchor(seed, cx, cz, type, salt);
        int dx = x - anchor[0];
        int dz = z - anchor[1];
        return dx * dx + dz * dz <= 9;
    }

    private static int[] resolveAnchor(int seed, int cx, int cz, CaveType type, int salt) {
        int spread = CaveSystemGrid.caveRadius(type) / 3;
        int h = NoiseUtil.hash2D(seed ^ salt, cx, cz);
        int ax = cx + (h & 0xFF) * spread / 255 - spread / 2;
        int az = cz + (h >> 8 & 0xFF) * spread / 255 - spread / 2;
        return new int[]{ax, az};
    }

    private static long pack(int cx, int cz, CaveType type) {
        return (long)type.ordinal() << 62 ^ (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }
}
