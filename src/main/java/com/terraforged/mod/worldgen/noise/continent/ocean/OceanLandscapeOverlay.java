package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Replacement for {@code IslandFeatureOverlay} when EGF Untested → Ocean Landscape is on.
 * Corridor seafloor + rare deep volcanoes; near-shore freckles are intentionally omitted.
 */
public final class OceanLandscapeOverlay {
    private final int seed;
    private final ContinentGenerator continent;
    private final boolean shipwrecked;

    public OceanLandscapeOverlay(ContinentConfig config, ContinentGenerator continent) {
        this.seed = config.shape.seed0 ^ 0x0CEA11;
        this.continent = continent;
        this.shipwrecked = config.shape.shipwrecked;
    }

    public static boolean isActive() {
        return TFNoiseVariantFlags.oceanLandscapeEnabled();
    }

    /**
     * @param worldX worldZ block-equivalent frame (warp + worldOffset)
     * @param shapeX shapeY continent cell-space coords (same as ShapeGenerator)
     */
    public void apply(float worldX, float worldZ, float shapeX, float shapeY, NoiseSample sample) {
        if (!isActive() || sample == null) {
            return;
        }
        float cn = sample.continentNoise;
        OceanZoneMask.Zone zone = OceanZoneMask.evaluate(this.continent, shapeX, shapeY, cn, this.shipwrecked);

        if (zone.corridor() > 0.04F) {
            float relief = SeafloorLandscape.relief(worldX, worldZ, this.seed);
            float masked = relief * zone.corridor();
            float emergeAt = this.shipwrecked
                    ? SeafloorLandscape.SHIP_EMERGE_THRESHOLD
                    : SeafloorLandscape.EMERGE_THRESHOLD;
            if (masked >= emergeAt) {
                int cx = NoiseUtil.floor(worldX / 48.0F);
                int cz = NoiseUtil.floor(worldZ / 48.0F);
                IslandEmergence.emerge(sample, masked, SeafloorLandscape.formFor(masked, this.seed, cx, cz));
            } else {
                sample.oceanRelief = Math.max(sample.oceanRelief, masked);
            }
        }

        if (zone.deep() > 0.08F) {
            DeepVolcano.Result volcano = DeepVolcano.eval(worldX, worldZ, this.seed, zone.deep(), this.shipwrecked);
            IslandEmergence.emergeVolcano(sample, volcano);
        }
    }
}
