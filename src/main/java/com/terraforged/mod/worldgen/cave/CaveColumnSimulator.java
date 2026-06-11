package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;

public final class CaveColumnSimulator {
    private static final int MEGA_GIGA_ROOF_BUFFER = 26;

    private CaveColumnSimulator() {
    }

    public record Sample(int floorY, int ceilingY, int midY) {
    }

    public static Sample sampleMegaGiga(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z) {
        return CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, x, z, 3, true);
    }

    public static Sample sampleMegaGigaForCartography(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z) {
        return CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, x, z, 1, false);
    }

    private static Sample sampleMegaGiga(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z, int minCavern, boolean applyOceanFilter) {
        if (config == null || !config.getType().isMegaOrGiga()) {
            return null;
        }
        if (applyOceanFilter && CaveOceanFilter.isBlockedForMegaGiga(generator, config.getType(), x, z)) {
            return null;
        }
        int surface = generator.getOceanFloorHeight(x, z);
        int minY = generator.getMinY();
        int y = config.getHeight(seed, x, z);
        float value = CaveNoise.sample(modifier, seed, x, z);
        int cavern = config.getCavernSize(seed, x, z, value);
        if (cavern < minCavern) {
            return null;
        }
        int ceilingCap = surface - MEGA_GIGA_ROOF_BUFFER;
        if (ceilingCap <= minY) {
            return null;
        }
        int maxUp = Math.max(0, ceilingCap - y);
        int maxDown = Math.max(0, y - minY);
        int vertRadius = Math.min(cavern, Math.min(maxUp, maxDown));
        if (vertRadius < 3) {
            return null;
        }
        int top = Math.min(y + vertRadius, ceilingCap);
        int bottom = Math.max(y - vertRadius, minY);
        if (top - bottom < 3) {
            return null;
        }
        return new Sample(bottom, top, bottom + top >> 1);
    }
}
