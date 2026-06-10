package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.UniqueCaveDistributor;
import com.terraforged.noise.Module;

public final class CaveNoise {
    private CaveNoise() {
    }

    public static float sample(Module noise, int seed, int x, int z) {
        if (noise instanceof UniqueCaveDistributor) {
            UniqueCaveDistributor distributor = (UniqueCaveDistributor)noise;
            return distributor.sampleAt(seed, x, z);
        }
        return noise.getValue(x, z);
    }

    public static float sampleMerged(Module noise, int seed, int x, int z) {
        float center = CaveNoise.sample(noise, seed, x, z);
        if (center >= 0.14f) {
            return center;
        }
        float merged = center;
        int step = 10;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (dx == 0 && dz == 0) continue;
                float neighbour = CaveNoise.sample(noise, seed, x + dx * step, z + dz * step);
                merged = Math.max(merged, neighbour * 0.9f);
            }
        }
        return merged;
    }
}
