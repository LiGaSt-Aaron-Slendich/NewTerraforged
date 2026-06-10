package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.UniqueCaveDistributor;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;

public final class CaveModifiers {
    public static final int MEGA_CELL_SCALE = 500;
    public static final float MEGA_DENSITY = 0.04f;
    public static final int GIGA_CELL_SCALE = 800;
    public static final float GIGA_DENSITY = 0.025f;
    public static final int UNIQUE_CELL_SCALE = 800;
    public static final float UNIQUE_DENSITY = 0.05f;

    private CaveModifiers() {
    }

    public static Module giga() {
        return CaveModifiers.create(800, 0.025f, true, 0.12f);
    }

    public static Module mega() {
        return CaveModifiers.create(500, 0.04f, true, 0.15f);
    }

    public static Module unique() {
        return CaveModifiers.create(800, 0.05f, true, 0.15f);
    }

    public static Module get(CaveType type) {
        return switch (type) {
            case GIGA -> CaveModifiers.giga();
            case MEGA -> CaveModifiers.mega();
            case UNIQUE -> CaveModifiers.unique();
            default -> Source.ONE;
        };
    }

    private static Module create(int scale, float density, boolean warp, float edgeClamp) {
        Module noise = new UniqueCaveDistributor(1286745, 1.0f / (float)scale, 0.75f, density).clamp(edgeClamp, 1.0).map(0.0, 1.0);
        if (warp) {
            noise = noise.warp(781624, 30, 1, 20.0);
        }
        return noise;
    }
}
