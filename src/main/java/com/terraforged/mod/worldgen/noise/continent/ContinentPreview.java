package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.client.ui.Previewer;
import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.util.ColorUtil;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.util.NoiseUtil;
import java.util.concurrent.ThreadLocalRandom;

public class ContinentPreview {
    public static int SEED = ThreadLocalRandom.current().nextInt();

    public static void main(String[] args) {
        Previewer.launch(() -> {
            Noise noise = ContinentPreview.create();
            return (x, y) -> {
                ClimateSample sample = noise.getSample(273651, x, y);
                return ColorUtil.getBiomeColor(sample, 0.2f);
            };
        });
    }

    private static Noise create() {
        WorldSettings.ControlPoints controls = new WorldSettings.ControlPoints();
        controls.deepOcean = 0.05f;
        controls.shallowOcean = 0.3f;
        controls.beach = 0.45f;
        controls.coast = 0.75f;
        controls.inland = 0.8f;
        ContinentConfig config = new ContinentConfig();
        config.shape.seed0 = SEED;
        config.shape.seed1 = SEED + 39674;
        config.shape.threshold = 0.525f;
        TerrainLevels terrainLevels = new TerrainLevels();
        NoiseGenerator generator = new NoiseGenerator(0L, terrainLevels, ModTerrains.Factory.getDefault(null));
        return new Noise(generator, new ClimateNoise(generator.getContinent().getContext()));
    }

    public record Noise(NoiseGenerator generator, ClimateNoise climate) {
        public ClimateSample getSample(int seed, float x, float y) {
            ClimateSample sample = this.climate.getSample(seed, x, y);
            this.sampleContinent(seed, x, y, sample);
            this.sampleRivers(seed, x, y, sample);
            return sample;
        }

        public void sampleContinent(int seed, float x, float y, NoiseSample sample) {
            this.generator.sampleContinentNoise(seed, NoiseUtil.floor(x), NoiseUtil.floor(y), sample);
        }

        public void sampleRivers(int seed, float x, float y, NoiseSample sample) {
            this.generator.sampleRiverNoise(seed, NoiseUtil.floor(x), NoiseUtil.floor(y), sample);
        }
    }
}
