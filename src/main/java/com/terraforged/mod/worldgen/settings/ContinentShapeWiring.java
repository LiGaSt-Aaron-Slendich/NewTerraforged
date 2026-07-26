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
        apply(config, settings.world.continent, settings.world.islands, settings.world.oceanLandscape);
        if (settings.world.properties != null && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED) {
            applyShipwrecked(config);
        }
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent) {
        apply(config, continent, new WorldSettings.Islands(), new WorldSettings.OceanLandscape());
    }

    public static void apply(ContinentConfig config, WorldSettings.Continent continent, WorldSettings.Islands islands) {
        apply(config, continent, islands, new WorldSettings.OceanLandscape());
    }

    public static void apply(
            ContinentConfig config,
            WorldSettings.Continent continent,
            WorldSettings.Islands islands,
            WorldSettings.OceanLandscape oceanLandscape
    ) {
        int baseScale = Math.max(100, continent.continentScale);
        // River widths are converted with riverScale — must stay equal to ContinentNoise's
        // 1/continentScale frame (stock TF used the same value for shape.scale).
        config.shape.riverScale = baseScale;
        config.shape.scale = baseScale;
        float spread = NoiseUtil.clamp(continent.continentsSpread, 0.0F, 1.0F);
        // Jitter is user-controlled only — do not bake spread into it (that looked like a one-way shift).
        config.shape.jitter = NoiseUtil.clamp(continent.continentJitter, 0.0F, 1.0F);

        // Stock TF kept land-cell threshold at 0.525 for drainage. Continents Spread already
        // pitches cell gaps via shape.scale; folding skip/spread into threshold (old NewTF
        // path → ~0.60–0.72) starved RiverGenerator of land cells. Keep stock constant.
        config.shape.threshold = 0.525F;
        boolean egfGuarantee = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.guaranteedContinentsEnabled();
        boolean guaranteeActive = egfGuarantee && continent.guaranteedContinentsEnabled;
        // Cell-pitch stretch: higher spread → larger gaps between Voronoi land blobs.
        // Stronger when guarantee is off (guarantee uses minSep instead).
        // Pitch only shape.scale (mask / gulf wavelength) — never riverScale.
        float pitch = guaranteeActive
                ? NoiseUtil.lerp(1.0F, 1.25F, spread)
                : NoiseUtil.lerp(1.0F, 2.35F, spread);
        config.shape.scale = Math.max(100, Math.round(baseScale * pitch));

        config.shape.noiseOctaves = Math.max(1, Math.min(8, continent.continentNoiseOctaves));
        // Stronger outline noise → gulfs / embayments instead of smooth blob coasts.
        config.shape.noiseGain = NoiseUtil.clamp(continent.continentNoiseGain * 1.18F + 0.04F, 0.0F, 0.58F);
        config.shape.noiseLacunarity = Math.max(1.0F, continent.continentNoiseLacunarity * 1.06F);
        float variance = NoiseUtil.clamp(continent.continentSizeVariance, 0.0F, 1.0F);
        // Floor a little size variance so landmasses aren't uniformly dense blobs.
        config.shape.sizeVariance = Math.max(0.18F, variance);
        // Narrower falloff = sharper neighbor mixing → more inland seas / Mediterranean gulfs.
        config.noise.continentNoiseFalloff = 0.78F + config.shape.sizeVariance * 0.85F;
        config.noise.baseNoiseFalloff = 1.35F + config.shape.sizeVariance * 0.55F;

        config.shape.guaranteedContinents = Math.max(1, Math.min(16, continent.guaranteedContinents));
        // EGF master switch + Customize toggle must both be on.
        config.shape.guaranteedContinentsEnabled = guaranteeActive;
        config.shape.continentsSpread = spread;
        wireOceanLandscape(config, oceanLandscape);
        // Ocean Landscape replaces legacy island paint when EGF is on.
        if (com.terraforged.mod.platform.forge.TFNoiseVariantFlags.oceanLandscapeEnabled()) {
            config.shape.coastalIslandsChance = 0.0F;
            config.shape.volcanicIslandsChance = 0.0F;
            config.shape.archipelago = false;
            config.shape.archipelagoChance = 0.0F;
            config.shape.scatteredArchipelago = false;
            config.shape.scatteredArchipelagoChance = 0.0F;
            config.shape.shipwrecked = false;
            return;
        }
        // Coastal / volcanic islands + island_flats paint are EGF (Islands) — off unless enabled.
        boolean egfIslands = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.islandsEnabled();
        float coastal = effectiveChance(islands.coastalIslandsChance, islands.coastalIslands);
        float volcanic = effectiveChance(islands.volcanicIslandsChance, islands.volcanicIslands);
        config.shape.coastalIslandsChance = egfIslands ? coastal : 0.0F;
        config.shape.volcanicIslandsChance = egfIslands ? volcanic : 0.0F;
        // Archipelago / Scattered are Experimental Generation Features (EGF) — off unless enabled.
        // Both EGF master switch AND Customize toggle must be on (defaults are OFF).
        boolean egfArch = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.archipelagoEnabled();
        boolean egfScattered = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.scatteredArchipelagoEnabled();
        config.shape.archipelago = egfArch && islands.archipelago;
        config.shape.archipelagoChance = config.shape.archipelago
                ? NoiseUtil.clamp(islands.archipelagoChance, 0.0F, 1.0F)
                : 0.0F;
        config.shape.scatteredArchipelago = egfScattered && islands.scatteredArchipelago;
        config.shape.scatteredArchipelagoChance = config.shape.scatteredArchipelago
                ? NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F)
                : 0.0F;
        config.shape.shipwrecked = false;
    }

    private static void wireOceanLandscape(ContinentConfig config, WorldSettings.OceanLandscape ol) {
        WorldSettings.OceanLandscape src = ol != null ? ol : new WorldSettings.OceanLandscape();
        config.shape.oceanLandscapeScale = NoiseUtil.clamp(src.landscapeScale, 0.5F, 4.0F);
        config.shape.oceanNoiseScale = NoiseUtil.clamp(src.noiseScale, 0.25F, 3.0F);
        config.shape.oceanCorridorPartners = Math.max(1, Math.min(4, src.corridorPartners));
        config.shape.oceanCorridorStrength = NoiseUtil.clamp(src.corridorStrength, 0.0F, 1.0F);
        config.shape.oceanCorridorMaxDistance = NoiseUtil.clamp(src.corridorMaxDistance, 2.0F, 24.0F);
        config.shape.oceanShelfStrength = NoiseUtil.clamp(src.shelfStrength, 0.0F, 1.0F);
        config.shape.oceanVolcanoDensity = NoiseUtil.clamp(src.volcanoDensity, 0.0F, 1.0F);
    }

    /** Islands-only mode: suppress continent land cells; overlay places all land. */
    public static void applyShipwrecked(ContinentConfig config) {
        config.shape.shipwrecked = true;
        config.shape.guaranteedContinentsEnabled = false;
        // Above any possible cell.noise so shape never forms mainland.
        config.shape.threshold = 1.01F;
        config.shape.scale = Math.min(Math.max(100, config.shape.scale), 1400);
        config.shape.riverScale = Math.min(Math.max(100, config.shape.riverScale), 1400);
        // Ocean Landscape owns Shipwrecked islands when EGF is on.
        if (com.terraforged.mod.platform.forge.TFNoiseVariantFlags.oceanLandscapeEnabled()) {
            config.shape.archipelago = false;
            config.shape.archipelagoChance = 0.0F;
            config.shape.scatteredArchipelago = false;
            config.shape.scatteredArchipelagoChance = 0.0F;
            config.shape.volcanicIslandsChance = 0.0F;
            config.shape.coastalIslandsChance = 0.0F;
            return;
        }
        boolean egfArch = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.archipelagoEnabled();
        boolean egfScattered = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.scatteredArchipelagoEnabled();
        boolean egfIslands = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.islandsEnabled();
        if (egfArch) {
            config.shape.archipelago = true;
            if (config.shape.archipelagoChance < 0.40F) {
                config.shape.archipelagoChance = 0.50F;
            }
        } else {
            config.shape.archipelago = false;
            config.shape.archipelagoChance = 0.0F;
        }
        if (egfScattered) {
            config.shape.scatteredArchipelago = true;
            if (config.shape.scatteredArchipelagoChance < 0.45F) {
                config.shape.scatteredArchipelagoChance = 0.55F;
            }
        } else {
            config.shape.scatteredArchipelago = false;
            config.shape.scatteredArchipelagoChance = 0.0F;
        }
        if (egfIslands) {
            if (config.shape.volcanicIslandsChance < 0.20F) {
                config.shape.volcanicIslandsChance = 0.28F;
            }
            if (config.shape.coastalIslandsChance < 0.35F) {
                config.shape.coastalIslandsChance = 0.40F;
            }
        } else {
            config.shape.volcanicIslandsChance = 0.0F;
            config.shape.coastalIslandsChance = 0.0F;
        }
    }

    /**
     * Preview TileGenerator only reads continent.*; bake island/spread knobs the engine understands.
     * Spread must mutate skipping + scale here or Customize preview shows zero effect.
     */
    public static void bakeIslandsIntoEngine(Settings settings) {
        if (settings == null || settings.world == null) {
            return;
        }
        ContinentGuarantee.syncIslandsMirror(settings.world);
        WorldSettings.Continent c = settings.world.continent;
        float spread = NoiseUtil.clamp(c.continentsSpread, 0.0F, 1.0F);
        float skip = NoiseUtil.clamp(c.continentSkipping, 0.0F, 1.0F);
        c.continentSkipping = NoiseUtil.clamp(NoiseUtil.lerp(skip, Math.min(1.0F, skip + 0.55F), spread), 0.0F, 1.0F);
        float pitch = NoiseUtil.lerp(1.0F, 2.35F, spread);
        c.continentScale = Math.max(100, Math.round(c.continentScale * pitch));
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
