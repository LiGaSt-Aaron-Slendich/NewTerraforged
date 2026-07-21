package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.continent.GuaranteedContinentMask;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Maps engine {@link WorldSettings.Continent} / {@link WorldSettings.Islands} onto NewTF
 * {@link ContinentConfig}. Exact continent count is enforced by {@link GuaranteedContinentMask}
 * (not by skip density alone).
 */
public final class ContinentShapeWiring {
    public static final int GUARANTEE_AREA = GuaranteedContinentMask.AREA;

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
        float baseJitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);
        config.shape.jitter = NoiseUtil.clamp(NoiseUtil.lerp(baseJitter * 0.85F, Math.max(baseJitter, 0.95F), spread), 0.0F, 1.0F);

        // Outside the guarantee window, keep a moderate ocean/land mix from user skipping.
        float skip = NoiseUtil.clamp(continent.continentSkipping, 0.0F, 1.0F);
        config.shape.threshold = NoiseUtil.lerp(0.35F, 0.72F, skip);

        config.shape.noiseOctaves = Math.max(1, Math.min(8, continent.continentNoiseOctaves));
        config.shape.noiseGain = NoiseUtil.clamp(continent.continentNoiseGain, 0.0F, 1.0F);
        config.shape.noiseLacunarity = Math.max(1.0F, continent.continentNoiseLacunarity);
        float variance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        config.shape.sizeVariance = NoiseUtil.clamp(NoiseUtil.lerp(variance, Math.max(variance, 0.65F), spread), 0.0F, 1.0F);
        config.noise.continentNoiseFalloff = 1.0F + config.shape.sizeVariance * 0.75F;
        config.noise.baseNoiseFalloff = 1.5F + config.shape.sizeVariance * 0.5F;

        config.shape.guaranteedContinents = Math.max(1, Math.min(16, islands.guaranteedContinents));
        config.shape.continentsSpread = spread;
        config.shape.coastalIslandsChance = effectiveChance(islands.coastalIslandsChance, islands.coastalIslands);
        config.shape.volcanicIslandsChance = effectiveChance(islands.volcanicIslandsChance, islands.volcanicIslands);
        config.shape.scatteredArchipelago = islands.scatteredArchipelago;
        config.shape.scatteredArchipelagoChance = NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F);
    }

    /**
     * Preview TileGenerator only reads continent.*; bake island chances into skip/jitter
     * and rely on overridden {@code AbstractContinent.shouldSkip} for exact-N.
     */
    public static void bakeIslandsIntoEngine(Settings settings) {
        if (settings == null || settings.world == null) {
            return;
        }
        WorldSettings.Islands islands = settings.world.islands != null ? settings.world.islands : new WorldSettings.Islands();
        WorldSettings.Continent c = settings.world.continent;
        float spread = NoiseUtil.clamp(islands.continentsSpread, 0.0F, 1.0F);
        c.continentJitter = NoiseUtil.clamp(NoiseUtil.lerp(c.continentJitter * 0.85F, Math.max(c.continentJitter, 0.95F), spread), 0.0F, 1.0F);
        // Outside-window density: leave user skipping mostly intact.
        // Exact-N inside the window is handled by AbstractContinent + GuaranteedContinentMask.
        float coastal = effectiveChance(islands.coastalIslandsChance, islands.coastalIslands);
        float volcanic = effectiveChance(islands.volcanicIslandsChance, islands.volcanicIslands);
        if (coastal <= 0.01F) {
            c.continentSkipping = NoiseUtil.clamp(c.continentSkipping + 0.04F, 0.0F, 1.0F);
        }
        if (volcanic <= 0.01F) {
            c.continentNoiseGain = NoiseUtil.clamp(c.continentNoiseGain * 0.9F, 0.0F, 1.0F);
        }
    }

    private static float effectiveChance(float chance, boolean legacyToggle) {
        float c = NoiseUtil.clamp(chance, 0.0F, 1.0F);
        // Legacy boolean OFF forces chance to 0 when still present in old presets.
        if (!legacyToggle && chance >= 0.99F) {
            return 0.0F;
        }
        if (!legacyToggle && chance <= 0.0F) {
            return 0.0F;
        }
        return c;
    }
}
