package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveClimateType;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.mod.worldgen.cave.CaveTemperatureCalculator;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

public final class CaveStatInitializer {
    private CaveStatInitializer() {
    }

    public static CaveStatSnapshot initialize(Holder<Biome> surfaceBiome, int centerY, int surfaceY, float oceanWeight, float riverWeight, int seed, int worldX, int worldZ) {
        float surfaceTemp = 0.0f;
        float surfaceMoisture = 0.0f;
        float surfaceFertility = 0.0f;
        if (surfaceBiome != null) {
            Biome biome = (Biome)surfaceBiome.value();
            float mcTemp = biome.getBaseTemperature();
            surfaceTemp = (CaveTemperatureCalculator.normalizeMcTemp(mcTemp) - 0.5f) * 20.0f;
            surfaceMoisture = (biome.getDownfall() - 0.5f) * 12.0f;
            surfaceFertility = CaveStatInitializer.fertilityFromSurface(surfaceBiome);
        }
        float depthFactor = CaveStatInitializer.depthFactor(centerY, surfaceY);
        float depthHeat = NoiseUtil.lerp(0.0f, 4.0f, depthFactor);
        float moisture = NoiseUtil.clamp(surfaceMoisture + oceanWeight * 6.0f + riverWeight * 4.0f, -10.0f, 10.0f);
        float temperature = NoiseUtil.clamp(surfaceTemp + depthHeat, -10.0f, 10.0f);
        float fertility = NoiseUtil.clamp(surfaceFertility + NoiseUtil.lerp(-2.0f, 2.0f, depthFactor), -10.0f, 10.0f);
        float moistureJitter = NoiseUtil.valCoord2D((int)((long)seed ^ 0x4D0154L), worldX, worldZ) * 3.0f;
        float temperatureJitter = NoiseUtil.valCoord2D((int)((long)seed ^ 0x7E3F01L), worldX, worldZ) * 3.0f;
        moisture = NoiseUtil.clamp(moisture + moistureJitter, -10.0f, 10.0f);
        temperature = NoiseUtil.clamp(temperature + temperatureJitter, -10.0f, 10.0f);
        CaveStatVector initial = new CaveStatVector(moisture, temperature, fertility).clamped();
        return new CaveStatSnapshot(initial, CaveClimateType.classify(initial));
    }

    private static float fertilityFromSurface(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return 8.0f;
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return 5.0f;
        }
        if (biome.is(BiomeTags.IS_BADLANDS) || ((Biome)biome.value()).getBaseTemperature() > 1.2f) {
            return -2.0f;
        }
        return 1.0f;
    }

    private static float depthFactor(int y, int surfaceY) {
        int shallow = surfaceY - 24;
        int deep = Math.min(-32, surfaceY - 96);
        if (y >= shallow) {
            return 0.0f;
        }
        if (y <= deep) {
            return 1.0f;
        }
        return NoiseUtil.clamp((float)(shallow - y) / (float)(shallow - deep), 0.0f, 1.0f);
    }

    public record CaveStatSnapshot(CaveStatVector initial, CaveClimateType climateType) {
    }
}
