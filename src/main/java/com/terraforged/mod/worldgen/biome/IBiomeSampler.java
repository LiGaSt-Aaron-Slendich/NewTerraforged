package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.biome.SurfaceBiomeClimate;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;

public interface IBiomeSampler {
   ClimateSample getSample(int var1, int var2);

   float getShape(int var1, int var2);

   void sample(int var1, int var2, ClimateSample var3);

   static ClimateNoise createClimate(INoiseGenerator generator) {
      return generator == null ? null : new ClimateNoise(generator.getContinent().getContext());
   }

   public static class Sampler implements IBiomeSampler {
      protected final NoiseLevels levels;
      protected final ClimateNoise climateNoise;
      protected final INoiseGenerator noiseGenerator;
      protected final ThreadLocal<ClimateSample> localSample = ThreadLocal.withInitial(ClimateSample::new);

      public Sampler(INoiseGenerator noiseGenerator) {
         this.levels = noiseGenerator.getLevels();
         this.climateNoise = IBiomeSampler.createClimate(noiseGenerator);
         this.noiseGenerator = noiseGenerator;
      }

      public ClimateSample getSample() {
         return this.localSample.get().reset();
      }

      public INoiseGenerator getNoiseGenerator() {
         return this.noiseGenerator;
      }

      @Override
      public ClimateSample getSample(int x, int z) {
         float f = x * this.levels.frequency;
         float f1 = z * this.levels.frequency;
         ClimateSample climatesample = this.localSample.get().reset();
         this.noiseGenerator.getContinent().sampleContinent(f, f1, climatesample);
         this.noiseGenerator.getContinent().sampleRiver(f, f1, climatesample);
         this.climateNoise.sample(f, f1, climatesample);
         // Inland biome rules need the same landform as height (WeightMap + mountain belt).
         // Continent alone leaves terrainType=NONE, which emptied the rule filter → plains.
         if (climatesample.continentNoise > 0.5F) {
            var land = this.noiseGenerator.getNoiseSample(x, z);
            climatesample.terrainType = land.terrainType;
            climatesample.heightNoise = land.heightNoise;
            climatesample.baseNoise = land.baseNoise;
            climatesample.riverNoise = land.riverNoise;
            climatesample.oceanRelief = land.oceanRelief;
            climatesample.continentNoise = land.continentNoise;
            climatesample.continentCentre = land.continentCentre;
         }
         climatesample.climateType = SurfaceBiomeClimate.adjustForTerrain(
               climatesample.climateType, climatesample.terrainType, climatesample.temperature, climatesample.moisture);
         return climatesample;
      }

      @Override
      public float getShape(int x, int z) {
         float f = x * this.levels.frequency;
         float f1 = z * this.levels.frequency;
         ClimateSample climatesample = this.localSample.get().reset();
         this.climateNoise.sample(f, f1, climatesample);
         return climatesample.biomeEdgeNoise;
      }

      @Override
      public void sample(int x, int z, ClimateSample sample) {
         float f = x * this.levels.frequency;
         float f1 = z * this.levels.frequency;
         this.climateNoise.sample(f, f1, sample);
      }
   }
}
