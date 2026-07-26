package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class CaveTemperatureCalculator {
    private static final float MC_TEMP_MIN = -0.5f;
    private static final float MC_TEMP_MAX = 2.0f;
    private static final float MC_TEMP_RANGE = 2.5f;
    private static final int DEPTH_NEUTRAL_Y = 64;
    private static final int DEPTH_HOT_Y = -64;
    private static final float DEPTH_HOT_TEMP = 1.0f;
    private static final float DEPTH_NEUTRAL_TEMP = 0.5f;

    public static float calculate(Holder<Biome> surfaceBiome, int blockY, int surfaceY) {
        float surfaceTemp = 0.5f;
        if (surfaceBiome != null) {
            float mcTemp = ((Biome)surfaceBiome.value()).getBaseTemperature();
            surfaceTemp = CaveTemperatureCalculator.normalizeMcTemp(mcTemp);
        }
        float depthTemp = CaveTemperatureCalculator.calcDepthTemp(blockY);
        float depthFactor = CaveTemperatureCalculator.calcDepthFactor(blockY, surfaceY);
        return NoiseUtil.clamp(NoiseUtil.lerp(surfaceTemp, depthTemp, depthFactor), 0.0f, 1.0f);
    }

    public static float normalizeMcTemp(float mcTemp) {
        return NoiseUtil.clamp((mcTemp - -0.5f) / 2.5f, 0.0f, 1.0f);
    }

    private static float calcDepthTemp(int y) {
        if (y >= 64) {
            return 0.5f;
        }
        float t = (float)(64 - y) / 128.0f;
        t = NoiseUtil.clamp(t, 0.0f, 1.0f);
        return NoiseUtil.lerp(0.5f, 1.0f, t);
    }

    private static float calcDepthFactor(int blockY, int surfaceY) {
        int shallowY = surfaceY - 16;
        int deepY = -32;
        if (blockY >= shallowY) {
            return 0.0f;
        }
        if (blockY <= deepY) {
            return 1.0f;
        }
        return NoiseUtil.clamp((float)(shallowY - blockY) / (float)(shallowY - deepY), 0.0f, 1.0f);
    }
}
