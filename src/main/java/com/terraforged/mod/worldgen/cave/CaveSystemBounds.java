package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Canonical mega/giga system geometry. Layout, cartography, and grid placement all use the same footprint.
 */
public final class CaveSystemBounds {
    /** Matches {@link CaveLayoutRegionGrid#build} inclusion test. */
    public static final float FOOTPRINT_FRACTION = 0.98f;
    private static final float MEGA_INFLUENCE = 0.22f;
    private static final float GIGA_INFLUENCE = 0.1f;

    private CaveSystemBounds() {
    }

    public static int systemRadius(CaveType type) {
        return CaveSystemGrid.caveRadius(type);
    }

    public static int cellDiameter(CaveType type) {
        return CaveSystemBounds.systemRadius(type) * 2;
    }

    public static float footprintRadius(CaveType type) {
        return (float)CaveSystemBounds.systemRadius(type) * FOOTPRINT_FRACTION;
    }

    public static float footprintRadiusSq(CaveType type) {
        float r = CaveSystemBounds.footprintRadius(type);
        return r * r;
    }

    public static int snapCenter(int coord, CaveType type) {
        return CaveSystemGrid.snapCenter(coord, type);
    }

    public static boolean isWithinFootprint(int x, int z, float centerX, float centerZ, CaveType type) {
        float dx = (float)x - centerX;
        float dz = (float)z - centerZ;
        return dx * dx + dz * dz <= CaveSystemBounds.footprintRadiusSq(type);
    }

    public static boolean isWithinFootprint(int x, int z, CaveType type) {
        int cx = CaveSystemBounds.snapCenter(x, type);
        int cz = CaveSystemBounds.snapCenter(z, type);
        return CaveSystemBounds.isWithinFootprint(x, z, cx, cz, type);
    }

    /** Runtime carve influence — noise threshold inside the system cell (may be smaller than footprint). */
    public static boolean hasCarveInfluence(int seed, int x, int z, CaveType type) {
        if (type == CaveType.GIGA) {
            return CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > GIGA_INFLUENCE;
        }
        if (type == CaveType.MEGA) {
            return CaveNoise.sample(CaveModifiers.mega(), seed, x, z) > MEGA_INFLUENCE;
        }
        return CaveNoise.sample(CaveModifiers.mega(), seed, x, z) > MEGA_INFLUENCE || CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > GIGA_INFLUENCE;
    }

    public static float distFromCenter(int x, int z, float centerX, float centerZ) {
        float dx = (float)x - centerX;
        float dz = (float)z - centerZ;
        return NoiseUtil.sqrt(dx * dx + dz * dz);
    }
}
