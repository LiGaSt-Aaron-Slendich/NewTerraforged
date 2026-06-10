package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.biome.SurfaceBiomeClimate;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;

public interface IBiomeSampler {
    public ClimateSample getSample(int var1, int var2, int var3);

    public float getShape(int var1, int var2, int var3);

    public void sample(int var1, int var2, int var3, ClimateSample var4);

    public static ClimateNoise createClimate(INoiseGenerator generator) {
        if (generator == null) {
            return null;
        }
        return new ClimateNoise(generator.getContinent().getContext());
    }

    public static class Sampler
    implements IBiomeSampler {
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
        public ClimateSample getSample(int seed, int x, int z) {
            float px = (float)x * this.levels.frequency;
            float pz = (float)z * this.levels.frequency;
            ClimateSample sample = this.localSample.get().reset();
            this.noiseGenerator.getContinent().sampleContinent(seed, px, pz, sample);
            this.noiseGenerator.getContinent().sampleRiver(seed, px, pz, sample);
            this.sampleLandTerrain(seed, px, pz, sample);
            this.climateNoise.sample(seed, px, pz, sample);
            sample.climateType = SurfaceBiomeClimate.adjustForTerrain(sample.climateType, sample.terrainType, sample.temperature, sample.moisture);
            return sample;
        }

        private void sampleLandTerrain(int seed, float px, float pz, ClimateSample sample) {
            if (sample.continentNoise < 0.55f) {
                return;
            }
            INoiseGenerator iNoiseGenerator = this.noiseGenerator;
            if (iNoiseGenerator instanceof NoiseGenerator) {
                NoiseGenerator generator = (NoiseGenerator)iNoiseGenerator;
                generator.sampleTerrain(seed, px, pz, sample, generator.getBlenderResource());
            }
        }

        @Override
        public float getShape(int seed, int x, int z) {
            float px = (float)x * this.levels.frequency;
            float pz = (float)z * this.levels.frequency;
            ClimateSample sample = this.localSample.get().reset();
            this.climateNoise.sample(seed, px, pz, sample);
            return sample.biomeEdgeNoise;
        }

        @Override
        public void sample(int seed, int x, int z, ClimateSample sample) {
            float px = (float)x * this.levels.frequency;
            float pz = (float)z * this.levels.frequency;
            this.climateNoise.sample(seed, px, pz, sample);
        }
    }
}
