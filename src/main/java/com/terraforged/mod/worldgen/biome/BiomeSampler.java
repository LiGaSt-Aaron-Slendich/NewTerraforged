package com.terraforged.mod.worldgen.biome;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class BiomeSampler extends IBiomeSampler.Sampler implements IBiomeSampler {
   protected final BiomeMapManager biomeMapManager;
   protected final float beachSize = 0.005F;

   public BiomeSampler(INoiseGenerator noiseGenerator, BiomeMapManager biomeMapManager) {
      super(noiseGenerator);
      this.biomeMapManager = biomeMapManager;
   }

   public Holder<Biome> sampleBiome(int x, int z) {
      ClimateSample climatesample = this.getSample(x, z);
      WeightMap<Holder<Biome>> weightmap = this.biomeMapManager.getBiomeMap().get(climatesample.climateType);
      Holder<Biome> holder = this.getInitialBiome(climatesample.biomeNoise, climatesample.climateType);
      holder = com.terraforged.mod.worldgen.biome.util.BiomeTerrainIntegration.filter(
            holder, climatesample.terrainType != null ? climatesample.terrainType.getName() : null, weightmap);
      return this.getBiomeOverride(holder, climatesample);
   }

   private Holder<Biome> getInitialBiome(float noise, BiomeType climateType) {
      WeightMap<Holder<Biome>> weightmap = this.biomeMapManager.getBiomeMap().get(climateType);
      if (weightmap == null || weightmap.isEmpty()) {
         return this.biomeMapManager.getBiomes().getHolderOrThrow(Biomes.PLAINS);
      }
      Holder<Biome> holder = weightmap.getValue(noise);
      return holder != null ? holder : this.biomeMapManager.getBiomes().getHolderOrThrow(Biomes.PLAINS);
   }

   protected Holder<Biome> getBiomeOverride(Holder<Biome> input, ClimateSample sample) {
      BiomeType biometype = sample.climateType;
      if (sample.continentNoise <= 0.25F) {
         return switch (biometype) {
            case TAIGA, COLD_STEPPE -> this.biomeMapManager.get(Biomes.DEEP_COLD_OCEAN);
            case TUNDRA -> this.biomeMapManager.get(Biomes.DEEP_FROZEN_OCEAN);
            case DESERT, SAVANNA, TROPICAL_RAINFOREST -> this.biomeMapManager.get(Biomes.DEEP_LUKEWARM_OCEAN);
            default -> this.biomeMapManager.get(Biomes.DEEP_OCEAN);
         };
      } else if (sample.continentNoise <= 0.5F) {
         return switch (biometype) {
            case TAIGA, COLD_STEPPE -> this.biomeMapManager.get(Biomes.COLD_OCEAN);
            case TUNDRA -> this.biomeMapManager.get(Biomes.FROZEN_OCEAN);
            case DESERT, SAVANNA, TROPICAL_RAINFOREST -> this.biomeMapManager.get(Biomes.WARM_OCEAN);
            default -> this.biomeMapManager.get(Biomes.OCEAN);
         };
      } else if (sample.continentNoise <= 0.505F) {
         return switch (biometype) {
            case COLD_STEPPE -> this.biomeMapManager.get(Biomes.STONY_SHORE);
            case TUNDRA -> this.biomeMapManager.get(Biomes.SNOWY_BEACH);
            default -> this.biomeMapManager.get(Biomes.BEACH);
         };
      } else if ((sample.terrainType.isRiver() || sample.terrainType.isLake()) && sample.riverNoise == 0.0F) {
         return biometype == BiomeType.TUNDRA ? this.biomeMapManager.get(Biomes.FROZEN_RIVER) : this.biomeMapManager.get(Biomes.RIVER);
      } else {
         return input;
      }
   }
}
