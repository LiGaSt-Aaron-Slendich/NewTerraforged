package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/** Maps engine {@link WorldSettings.Continent} onto NewTF {@link ContinentConfig} shape/noise. */
public final class ContinentShapeWiring {
    private ContinentShapeWiring() {
    }

    public static void apply(ContinentConfig config, Settings settings) {
        apply(config, settings.world.continent);
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent) {
        config.shape.scale = Math.max(100, continent.continentScale);
        config.shape.jitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);
        // Higher skipping → higher land threshold → fewer continents / more ocean.
        config.shape.threshold = NoiseUtil.lerp(0.35F, 0.72F, NoiseUtil.clamp(continent.continentSkipping, 0.0F, 1.0F));
        config.shape.noiseOctaves = Math.max(1, Math.min(8, continent.continentNoiseOctaves));
        config.shape.noiseGain = NoiseUtil.clamp(continent.continentNoiseGain, 0.0F, 1.0F);
        config.shape.noiseLacunarity = Math.max(1.0F, continent.continentNoiseLacunarity);
        config.shape.sizeVariance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        // Mild falloff tweak so variance also reads in the edge blend.
        config.noise.continentNoiseFalloff = 1.0F + config.shape.sizeVariance * 0.75F;
        config.noise.baseNoiseFalloff = 1.5F + config.shape.sizeVariance * 0.5F;
    }
}
