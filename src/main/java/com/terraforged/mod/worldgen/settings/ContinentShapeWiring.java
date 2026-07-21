package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Maps engine {@link WorldSettings.Continent} / {@link WorldSettings.Islands} onto NewTF
 * {@link ContinentConfig}, and bakes the same knobs into engine continent fields for Preview
 * ({@code TileGenerator} never reads {@code world.islands} directly).
 */
public final class ContinentShapeWiring {
    /** Preview / guarantee contract: farthest zoom coverage side length in blocks. */
    public static final int GUARANTEE_AREA = 640_000;

    private ContinentShapeWiring() {
    }

    public static void apply(ContinentConfig config, Settings settings) {
        apply(config, settings.world.continent, settings.world.islands);
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent) {
        apply(config, continent, new WorldSettings.Islands());
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent, WorldSettings.Islands islands) {
        WorldSettings.Continent baked = bakeContinentCopy(continent, islands, GUARANTEE_AREA);
        config.shape.scale = Math.max(100, baked.continentScale);
        config.shape.jitter = NoiseUtil.clamp(baked.continentJitter, 0.0F, 1.0F);
        config.shape.threshold = NoiseUtil.lerp(0.35F, 0.72F, NoiseUtil.clamp(baked.continentSkipping, 0.0F, 1.0F));
        config.shape.noiseOctaves = Math.max(1, Math.min(8, baked.continentNoiseOctaves));
        config.shape.noiseGain = NoiseUtil.clamp(baked.continentNoiseGain, 0.0F, 1.0F);
        config.shape.noiseLacunarity = Math.max(1.0F, baked.continentNoiseLacunarity);
        config.shape.sizeVariance = NoiseUtil.clamp(baked.continentSizeVariance, 0.0F, 1.0F);
        config.noise.continentNoiseFalloff = 1.0F + config.shape.sizeVariance * 0.75F;
        config.noise.baseNoiseFalloff = 1.5F + config.shape.sizeVariance * 0.5F;

        if (!islands.coastalIslands) {
            config.shape.baseFalloffMin = Math.max(config.shape.baseFalloffMin, 0.08F);
            config.shape.baseFalloffMax = Math.max(config.shape.baseFalloffMax, 0.32F);
        }
        if (!islands.volcanicIslands) {
            config.shape.threshold = NoiseUtil.clamp(config.shape.threshold + 0.04F, 0.0F, 1.0F);
            config.shape.noiseGain = NoiseUtil.clamp(config.shape.noiseGain * 0.85F, 0.0F, 1.0F);
        }
    }

    /**
     * Mutate {@code settings.world.continent} so engine {@code TileGenerator} reflects Islands knobs.
     * Call on a copy used only for preview generation.
     */
    public static void bakeIslandsIntoEngine(Settings settings) {
        if (settings == null || settings.world == null) {
            return;
        }
        WorldSettings.Continent baked = bakeContinentCopy(
                settings.world.continent,
                settings.world.islands != null ? settings.world.islands : new WorldSettings.Islands(),
                GUARANTEE_AREA
        );
        settings.world.continent.continentSkipping = baked.continentSkipping;
        settings.world.continent.continentJitter = baked.continentJitter;
        settings.world.continent.continentSizeVariance = baked.continentSizeVariance;
        settings.world.continent.continentNoiseGain = baked.continentNoiseGain;
    }

    /**
     * Target ~{@code guaranteedContinents} landmasses inside a {@code areaSide}×{@code areaSide} window
     * (default 640000). Aims for neither fewer nor many more by solving skip density from cell grid size.
     */
    public static WorldSettings.Continent bakeContinentCopy(
            WorldSettings.Continent continent,
            WorldSettings.Islands islands,
            int areaSide
    ) {
        WorldSettings.Continent out = new WorldSettings.Continent();
        out.continentType = continent.continentType;
        out.continentShape = continent.continentShape;
        out.continentScale = continent.continentScale;
        out.continentNoiseOctaves = continent.continentNoiseOctaves;
        out.continentNoiseLacunarity = continent.continentNoiseLacunarity;

        float spread = NoiseUtil.clamp(islands.continentsSpread, 0.0F, 1.0F);
        float baseJitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);
        out.continentJitter = NoiseUtil.clamp(NoiseUtil.lerp(baseJitter * 0.85F, Math.max(baseJitter, 0.95F), spread), 0.0F, 1.0F);

        float variance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        out.continentSizeVariance = NoiseUtil.clamp(NoiseUtil.lerp(variance, Math.max(variance, 0.65F), spread), 0.0F, 1.0F);

        int scale = Math.max(100, continent.continentScale);
        int area = Math.max(scale, areaSide);
        float cellsAcross = area / (float) scale;
        float cellCount = Math.max(1.0F, cellsAcross * cellsAcross);
        // Empirically, ~1 land cell per "guaranteed" continent after neighbour merges (k≈1.35).
        float want = Math.max(1, islands.guaranteedContinents) * 1.35F;
        float landFraction = NoiseUtil.clamp(want / cellCount, 0.04F, 0.55F);
        // Engine skipThreshold: higher → fewer continents. Invert landFraction into skip.
        float targetSkip = NoiseUtil.clamp(1.0F - landFraction, 0.05F, 0.92F);
        // Blend with user continentSkipping so both knobs matter (60% guarantee, 40% user).
        float userSkip = NoiseUtil.clamp(continent.continentSkipping, 0.0F, 1.0F);
        out.continentSkipping = NoiseUtil.clamp(targetSkip * 0.6F + userSkip * 0.4F, 0.0F, 1.0F);

        float gain = NoiseUtil.clamp(continent.continentNoiseGain, 0.0F, 1.0F);
        if (!islands.coastalIslands) {
            // Less edge freckle / near-shore noise.
            out.continentSkipping = NoiseUtil.clamp(out.continentSkipping + 0.06F, 0.0F, 1.0F);
            gain *= 0.9F;
        }
        if (!islands.volcanicIslands) {
            out.continentSkipping = NoiseUtil.clamp(out.continentSkipping + 0.05F, 0.0F, 1.0F);
            gain *= 0.85F;
        }
        out.continentNoiseGain = NoiseUtil.clamp(gain, 0.0F, 1.0F);
        return out;
    }
}
