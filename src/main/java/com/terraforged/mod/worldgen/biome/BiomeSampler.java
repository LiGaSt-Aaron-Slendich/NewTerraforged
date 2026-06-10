package com.terraforged.mod.worldgen.biome;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.util.storage.WeightMap;
import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class BiomeSampler
extends IBiomeSampler.Sampler
implements IBiomeSampler {
    protected final BiomeMapManager biomeMapManager;
    protected final float beachSize = 0.005f;

    public BiomeSampler(INoiseGenerator noiseGenerator, BiomeMapManager biomeMapManager) {
        super(noiseGenerator);
        this.biomeMapManager = biomeMapManager;
    }

    public Holder<Biome> sampleBiome(int seed, int x, int z) {
        ClimateSample sample = this.getSample(seed, x, z);
        Holder<Biome> biome = this.getInitialBiome(sample.biomeNoise, sample.climateType);
        return this.getBiomeOverride(biome, sample);
    }

    private Holder<Biome> getInitialBiome(float noise, BiomeType climateType) {
        WeightMap<Holder<Biome>> map = this.biomeMapManager.getBiomeMap().get(climateType);
        if (map == null || map.isEmpty()) {
            return this.biomeMapManager.getBiomes().getHolderOrThrow(Biomes.PLAINS);
        }
        return map.getValue(noise);
    }

    protected Holder<Biome> getBiomeOverride(Holder<Biome> input, ClimateSample sample) {
        BiomeType biomeType = sample.climateType;
        if (sample.continentNoise <= 0.25f) {
            return switch (biomeType) {
                case TAIGA, COLD_STEPPE -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.DEEP_COLD_OCEAN);
                case TUNDRA -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.DEEP_FROZEN_OCEAN);
                case DESERT, SAVANNA, TROPICAL_RAINFOREST -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.DEEP_LUKEWARM_OCEAN);
                default -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.DEEP_OCEAN);
            };
        }
        if (sample.continentNoise <= 0.5f) {
            return switch (biomeType) {
                case TAIGA, COLD_STEPPE -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.COLD_OCEAN);
                case TUNDRA -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.FROZEN_OCEAN);
                case DESERT, SAVANNA, TROPICAL_RAINFOREST -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.WARM_OCEAN);
                default -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.OCEAN);
            };
        }
        if (sample.continentNoise <= 0.505f) {
            return switch (biomeType) {
                case TUNDRA -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.SNOWY_BEACH);
                case COLD_STEPPE -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.STONY_SHORE);
                default -> this.biomeMapManager.get((ResourceKey<Biome>)Biomes.BEACH);
            };
        }
        if ((sample.terrainType.isRiver() || sample.terrainType.isLake()) && sample.riverNoise == 0.0f) {
            return biomeType == BiomeType.TUNDRA ? this.biomeMapManager.get((ResourceKey<Biome>)Biomes.FROZEN_RIVER) : this.biomeMapManager.get((ResourceKey<Biome>)Biomes.RIVER);
        }
        return input;
    }
}
