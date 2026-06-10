package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.NoiseUtil;

public final class CaveSystemGrid {
    private static final float GIGA_DOMINANCE = 0.12f;

    private CaveSystemGrid() {
    }

    public static CaveType dominantType(int seed, int x, int z) {
        if (CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > 0.12f) {
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
        int az;
        int dz;
        int spread;
        int cz;
        int h;
        int cx = CaveSystemGrid.snapCenter(x, type);
        int ax = cx + ((h = NoiseUtil.hash2D(seed ^ salt, cx, cz = CaveSystemGrid.snapCenter(z, type))) & 0xFF) * (spread = CaveSystemGrid.caveRadius(type) / 3) / 255 - spread / 2;
        int dx = x - ax;
        return dx * dx + (dz = z - (az = cz + (h >> 8 & 0xFF) * spread / 255 - spread / 2)) * dz <= 9;
    }

    private static long pack(int cx, int cz, CaveType type) {
        return (long)type.ordinal() << 62 ^ (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }
}
