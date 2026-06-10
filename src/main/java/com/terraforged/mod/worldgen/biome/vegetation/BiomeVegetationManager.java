package com.terraforged.mod.worldgen.biome.vegetation;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetation;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;

public class BiomeVegetationManager {
    private final RegistryAccess access;
    private final VegetationConfig[] configs;
    private final Map<Holder<Biome>, BiomeVegetation> vegetation = new ConcurrentHashMap<Holder<Biome>, BiomeVegetation>();

    public BiomeVegetationManager(RegistryAccess access) {
        this.access = access;
        this.configs = TerraForged.VEGETATIONS.entries(access, VegetationConfig[]::new);
    }

    public BiomeVegetation getVegetation(Holder<Biome> biome) {
        return this.vegetation.computeIfAbsent(biome, this::compute);
    }

    private BiomeVegetation compute(Holder<Biome> biome) {
        VegetationConfig config = BiomeVegetationManager.getConfig(biome, this.configs);
        VegetationFeatures features = VegetationFeatures.create((Biome)biome.value(), this.access, config);
        return new BiomeVegetation(config, features);
    }

    private static VegetationConfig getConfig(Holder<Biome> biome, VegetationConfig[] configs) {
        for (VegetationConfig config : configs) {
            if (!biome.is(config.biomes())) continue;
            return config;
        }
        return VegetationConfig.NONE;
    }
}
