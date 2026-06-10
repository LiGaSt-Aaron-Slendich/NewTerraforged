package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;

public final class CaveBreaches {
    private static final float BREACH_THRESHOLD = 0.7f;
    private static final Module MASK = Source.simplexRidge(1567328, 300, 2).clamp(0.56f, 0.7f).map(0.0, 1.0);

    private CaveBreaches() {
    }

    public static Module mask() {
        return MASK;
    }

    public static float sample(int seed, int x, int z) {
        return 1.0f - CaveNoise.sample(MASK, seed, x, z);
    }
}
