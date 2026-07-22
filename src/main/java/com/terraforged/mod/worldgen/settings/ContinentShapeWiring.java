package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.continent.GuaranteedContinentMask;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Maps engine {@link WorldSettings.Continent} / {@link WorldSettings.Islands} onto NewTF
 * {@link ContinentConfig}. Continent count uses {@link GuaranteedContinentMask} (N±1 + soft cut).
 */
public final class ContinentShapeWiring {
    public static final int GUARANTEE_AREA = GuaranteedContinentMask.AREA;

    private ContinentShapeWiring() {
    }

    public static void apply(ContinentConfig config, Settings settings) {
        apply(config, settings.world.continent, settings.world.islands);
        if (settings.world.properties != null && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED) {
            applyShipwrecked(config);
        }
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent) {
        apply(config, continent, new WorldSettings.Islands());
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent, WorldSettings.Islands islands) {
        config.shape.scale = Math.max(100, continent.continentScale);
        float spread = NoiseUtil.clamp(continent.continentsSpread, 0.0F, 1.0F);
        float baseJitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);
        config.shape.jitter = NoiseUtil.clamp(NoiseUtil.lerp(baseJitter * 0.85F, Math.max(baseJitter, 0.95F), spread), 0.0F, 1.0F);

        // Outside the guarantee window, keep a moderate ocean/land mix from user skipping.
        float skip = NoiseUtil.clamp(continent.continentSkipping, 0.0F, 1.0F);
        config.shape.threshold = NoiseUtil.lerp(0.35F, 0.72F, skip);

        config.shape.noiseOctaves = Math.max(1, Math.min(8, continent.continentNoiseOctaves));
        // Mild bump so outlines stay irregular even on older presets with low gain.
        config.shape.noiseGain = NoiseUtil.clamp(continent.continentNoiseGain * 1.08F + 0.02F, 0.0F, 0.55F);
        config.shape.noiseLacunarity = Math.max(1.0F, continent.continentNoiseLacunarity);
        float variance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        config.shape.sizeVariance = NoiseUtil.clamp(NoiseUtil.lerp(variance, Math.max(variance, 0.72F), spread), 0.0F, 1.0F);
        config.noise.continentNoiseFalloff = 1.0F + config.shape.sizeVariance * 0.75F;
        config.noise.baseNoiseFalloff = 1.5F + config.shape.sizeVariance * 0.5F;

        config.shape.guaranteedContinents = Math.max(1, Math.min(16, continent.guaranteedContinents));
        config.shape.guaranteedContinentsEnabled = continent.guaranteedContinentsEnabled;
        config.shape.continentsSpread = spread;
        config.shape.coastalIslandsChance = effectiveChance(islands.coastalIslandsChance, islands.coastalIslands);
        config.shape.volcanicIslandsChance = effectiveChance(islands.volcanicIslandsChance, islands.volcanicIslands);
        config.shape.archipelago = islands.archipelago;
        config.shape.archipelagoChance = NoiseUtil.clamp(islands.archipelagoChance, 0.0F, 1.0F);
        config.shape.scatteredArchipelago = islands.scatteredArchipelago;
        config.shape.scatteredArchipelagoChance = NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F);
        config.shape.shipwrecked = false;
    }

    /** Islands-only mode: suppress continent land cells; overlay places all land. */
    public static void applyShipwrecked(ContinentConfig config) {
        config.shape.shipwrecked = true;
        config.shape.guaranteedContinentsEnabled = false;
        // Above any possible cell.noise so shape never forms mainland.
        config.shape.threshold = 1.01F;
        config.shape.scale = Math.min(Math.max(100, config.shape.scale), 1400);
        config.shape.archipelago = true;
        if (config.shape.archipelagoChance < 0.40F) {
            config.shape.archipelagoChance = 0.50F;
        }
        config.shape.scatteredArchipelago = true;
        if (config.shape.scatteredArchipelagoChance < 0.45F) {
            config.shape.scatteredArchipelagoChance = 0.55F;
        }
        if (config.shape.volcanicIslandsChance < 0.20F) {
            config.shape.volcanicIslandsChance = 0.28F;
        }
        if (config.shape.coastalIslandsChance < 0.35F) {
            config.shape.coastalIslandsChance = 0.40F;
        }
    }

    /**
     * Preview TileGenerator only reads continent.*; do not bake island chances into
     * continentSkipping — that made volcanic/archipelago knobs spawn extra continents.
     * Only apply spread-driven jitter so island UI still feels responsive without changing N.
     */
    public static void bakeIslandsIntoEngine(Settings settings) {
        if (settings == null || settings.world == null) {
            return;
        }
        ContinentGuarantee.syncIslandsMirror(settings.world);
        WorldSettings.Continent c = settings.world.continent;
        float spread = NoiseUtil.clamp(c.continentsSpread, 0.0F, 1.0F);
        c.continentJitter = NoiseUtil.clamp(NoiseUtil.lerp(c.continentJitter * 0.85F, Math.max(c.continentJitter, 0.95F), spread), 0.0F, 1.0F);
        if (settings.world.properties != null && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED) {
            c.guaranteedContinentsEnabled = false;
            // Engine preview TileGenerator still reads continentSkipping; push fully ocean.
            c.continentSkipping = 1.0F;
            c.continentScale = Math.min(c.continentScale, 1200);
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
