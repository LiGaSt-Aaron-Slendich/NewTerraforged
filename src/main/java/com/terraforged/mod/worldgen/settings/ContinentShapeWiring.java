package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/** Maps engine {@link WorldSettings.Continent} / {@link WorldSettings.Islands} onto NewTF {@link ContinentConfig}. */
public final class ContinentShapeWiring {
    private ContinentShapeWiring() {
    }

    public static void apply(ContinentConfig config, Settings settings) {
        apply(config, settings.world.continent, settings.world.islands);
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent) {
        apply(config, continent, new WorldSettings.Islands());
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent, WorldSettings.Islands islands) {
        config.shape.scale = Math.max(100, continent.continentScale);
        float spread = NoiseUtil.clamp(islands.continentsSpread, 0.0F, 1.0F);
        // Base jitter from continent setting, then push apart with Continents Spread.
        float jitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);
        config.shape.jitter = NoiseUtil.clamp(NoiseUtil.lerp(jitter * 0.85F, Math.max(jitter, 0.95F), spread), 0.0F, 1.0F);

        // Higher skipping → higher land threshold → fewer continents / more ocean.
        float skip = NoiseUtil.clamp(continent.continentSkipping, 0.0F, 1.0F);
        // Guaranteed continents in preview: lower threshold so more landmasses appear.
        float guaranteed = NoiseUtil.clamp((islands.guaranteedContinents - 1) / 15.0F, 0.0F, 1.0F);
        float skipAdj = NoiseUtil.clamp(skip - guaranteed * 0.35F, 0.0F, 1.0F);
        config.shape.threshold = NoiseUtil.lerp(0.35F, 0.72F, skipAdj);

        config.shape.noiseOctaves = Math.max(1, Math.min(8, continent.continentNoiseOctaves));
        config.shape.noiseGain = NoiseUtil.clamp(continent.continentNoiseGain, 0.0F, 1.0F);
        config.shape.noiseLacunarity = Math.max(1.0F, continent.continentNoiseLacunarity);
        float variance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        config.shape.sizeVariance = NoiseUtil.clamp(NoiseUtil.lerp(variance, Math.max(variance, 0.65F), spread), 0.0F, 1.0F);

        // Mild falloff tweak so variance also reads in the edge blend.
        config.noise.continentNoiseFalloff = 1.0F + config.shape.sizeVariance * 0.75F;
        config.noise.baseNoiseFalloff = 1.5F + config.shape.sizeVariance * 0.5F;

        // Coastal islands off → push freckles farther from the mainland edge.
        if (!islands.coastalIslands) {
            config.shape.baseFalloffMin = Math.max(config.shape.baseFalloffMin, 0.08F);
            config.shape.baseFalloffMax = Math.max(config.shape.baseFalloffMax, 0.32F);
        }
        // Volcanic islands off → slightly higher land threshold / less freckle noise.
        if (!islands.volcanicIslands) {
            config.shape.threshold = NoiseUtil.clamp(config.shape.threshold + 0.04F, 0.0F, 1.0F);
            config.shape.noiseGain = NoiseUtil.clamp(config.shape.noiseGain * 0.85F, 0.0F, 1.0F);
        }
    }
}
