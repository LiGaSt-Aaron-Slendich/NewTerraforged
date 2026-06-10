package com.terraforged.mod.worldgen.biome.vegetation;

import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;

public class BiomeVegetation {
    public final VegetationConfig config;
    public final VegetationFeatures features;

    public BiomeVegetation(VegetationConfig config, VegetationFeatures features) {
        this.config = config;
        this.features = features;
    }
}
