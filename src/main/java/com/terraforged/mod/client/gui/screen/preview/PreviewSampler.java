package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.settings.Settings;
import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.util.ColorUtil;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.continent.ContinentPreview;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Create-world preview sampler driven by engine {@link Settings}.
 */
public final class PreviewSampler {
    private final ContinentPreview.Noise noise;

    private PreviewSampler(ContinentPreview.Noise noise) {
        this.noise = noise;
    }

    public static PreviewSampler create(int seed) {
        TerrainLevels levels = TerrainLevels.DEFAULT.get().copy();
        Settings settings = com.terraforged.mod.client.gui.screen.SettingsDraft.createFactorySettings(seed, levels);
        return createFromSettings(seed, levels, settings);
    }

    public static PreviewSampler createFromSettings(Settings settings) {
        long seed = settings.world.seed;
        TerrainLevels levels = TerrainLevels.DEFAULT.get().copy();
        return createFromSettings((int)seed, levels, settings);
    }

    public static PreviewSampler createFromSettings(int seed, TerrainLevels levels, Settings settings) {
        settings.world.seed = seed;
        settings.world.properties.seaLevel = levels.seaLevel;
        settings.world.properties.worldHeight = levels.maxY;
        NoiseGenerator generator = new NoiseGenerator(seed, levels, ModTerrains.Factory.getDefault(null), settings);
        ClimateNoise climate = new ClimateNoise(generator.getContinent().getContext());
        return new PreviewSampler(new ContinentPreview.Noise(generator, climate));
    }

    public int color(float worldX, float worldZ, RenderMode mode) {
        var sample = this.noise.getSample(worldX, worldZ);
        return switch (mode) {
            case TEMPERATURE -> heatMap(1.0F - sample.temperature);
            case MOISTURE -> heatMap(sample.moisture);
            case HEIGHT -> heightMap(sample);
            case BIOME_TYPE -> ColorUtil.getBiomeColor(sample, 0.2F);
        };
    }

    private static int heightMap(NoiseSample sample) {
        float h = NoiseUtil.clamp(sample.heightNoise, 0.0F, 1.0F);
        int v = NoiseUtil.floor(h * 220.0F) + 20;
        return ColorUtil.rgb(v, v, v);
    }

    private static int heatMap(float value) {
        float v = NoiseUtil.clamp(value, 0.0F, 1.0F);
        return java.awt.Color.HSBtoRGB(v * 0.65F, 0.7F, 0.85F) | 0xFF000000;
    }
}
