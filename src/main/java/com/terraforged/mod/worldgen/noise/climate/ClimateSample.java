package com.terraforged.mod.worldgen.noise.climate;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.worldgen.noise.NoiseSample;

public class ClimateSample
extends NoiseSample {
    public float biomeNoise;
    public float biomeEdgeNoise;
    public float moisture;
    public float temperature;
    public BiomeType climateType = BiomeType.GRASSLAND;

    @Override
    public ClimateSample reset() {
        super.reset();
        return this;
    }
}
