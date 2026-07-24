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
      Holder<Biome> fallback = this.biomeMapManager.getBiomes().getHolderOrThrow(Biomes.PLAINS);
      Holder<Biome> holder = com.terraforged.mod.worldgen.biome.util.BiomeTerrainIntegration.pick(
            climatesample.biomeNoise,
            climatesample,
            weightmap,
            fallback,
            this.noiseGenerator,
            x,
            z);
      return this.getBiomeOverride(holder, climatesample, x, z);
   }

   protected Holder<Biome> getBiomeOverride(Holder<Biome> input, ClimateSample sample, int x, int z) {
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
         // Tundra stays snowy beach; elsewhere LIA rocky segments → stony shore.
         if (biometype == BiomeType.TUNDRA) {
            return this.biomeMapManager.get(Biomes.SNOWY_BEACH);
         }
         var lia = this.noiseGenerator.getContinent().getCoastalLia();
         if (lia != null && lia.isRockyShore((float) x, (float) z, sample.continentNoise)) {
            return this.biomeMapManager.get(Biomes.STONY_SHORE);
         }
         if (biometype == BiomeType.COLD_STEPPE) {
            return this.biomeMapManager.get(Biomes.STONY_SHORE);
         }
         return this.biomeMapManager.get(Biomes.BEACH);
      } else if ((sample.terrainType.isRiver() || sample.terrainType.isLake()) && sample.riverNoise == 0.0F) {
         return biometype == BiomeType.TUNDRA ? this.biomeMapManager.get(Biomes.FROZEN_RIVER) : this.biomeMapManager.get(Biomes.RIVER);
      } else {
         return input;
      }
   }
}
