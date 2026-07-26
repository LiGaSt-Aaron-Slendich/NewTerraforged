package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.settings.RiverSettings;
import com.terraforged.engine.settings.Settings;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.config.RiverConfig;

/** Maps engine {@link RiverSettings} onto NewTF {@link ContinentConfig} river carve params. */
public final class ContinentRiverWiring {
    private ContinentRiverWiring() {
    }

    public static void apply(ContinentConfig config, Settings settings) {
        apply(config, settings.rivers);
    }

    public static void apply(ContinentConfig config, RiverSettings rivers) {
        config.rivers.seed = rivers.seedOffset;
        // Stock continent RiverGenerator ignored riverCount. Keep 0 = off; treat count>=8 as
        // full density (1.0). Counts 1..7 thin links — never accidentally zero from float noise.
        if (rivers.riverCount <= 0) {
            config.rivers.riverDensity = 0.0F;
        } else if (rivers.riverCount >= 8) {
            config.rivers.riverDensity = 1.0F;
        } else {
            config.rivers.riverDensity = rivers.riverCount / 8.0F;
        }
        config.rivers.lakeDensity = clamp01(rivers.lakes.chance);

        applyRiver(config.rivers.rivers, rivers.mainRivers);
        applyLake(config.rivers.lakes, rivers.lakes);
    }

    private static void applyRiver(RiverConfig target, RiverSettings.River source) {
        setRange(target.bedDepth, source.bedDepth * 0.25F, source.bedDepth);
        setRange(target.bedWidth, source.bedWidth * 0.125F, source.bedWidth);
        setRange(target.bankWidth, source.bankWidth * 0.15F, source.bankWidth);
        setRange(target.bankDepth, source.minBankHeight, source.maxBankHeight);
        // fade 0..1 → higher fade = softer banks / less erosion punch
        target.erosion = 0.02F + (1.0F - clamp01(source.fade)) * 0.1F;
    }

    private static void applyLake(RiverConfig target, RiverSettings.Lake source) {
        setRange(target.bedDepth, Math.max(1.0F, source.depth * 0.2F), source.depth);
        setRange(target.bankDepth, source.minBankHeight, source.maxBankHeight);
        // Old lake size (blocks) → carve width ranges used by continent river system
        float minW = Math.max(4.0F, source.sizeMin * 0.08F);
        float maxW = Math.max(minW, source.sizeMax * 0.1F);
        setRange(target.bedWidth, minW * 0.5F, maxW * 0.5F);
        setRange(target.bankWidth, minW, maxW);
        // Keep the dry valley apron tight — wide valleys were ~perfect circles on height maps.
        setRange(target.valleyWidth, source.sizeMin * 0.22F, source.sizeMax * 0.42F);
    }

    private static void setRange(com.terraforged.mod.worldgen.noise.continent.config.FloatRange range, float min, float max) {
        float lo = Math.max(0.01F, Math.min(min, max));
        float hi = Math.max(lo, Math.max(min, max));
        range.min = lo;
        range.max = hi;
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }
}
