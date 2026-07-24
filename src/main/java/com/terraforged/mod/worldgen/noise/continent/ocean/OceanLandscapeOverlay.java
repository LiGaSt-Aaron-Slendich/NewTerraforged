package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Replacement for {@code IslandFeatureOverlay} when EGF Untested → Ocean Landscape is on.
 * Corridor seafloor + rare deep volcanoes; near-shore freckles are intentionally omitted.
 *
 * <p>Corridor graph is built lazily on first sample so world-create never stalls at 0%.
 */
public final class OceanLandscapeOverlay {
    private final int seed;
    private final ContinentGenerator continent;
    private final ContinentConfig config;
    private final boolean shipwrecked;
    private final float noiseScale;
    private final float landscapeScale;
    private final float corridorStrength;
    private final float shelfStrength;
    private final float volcanoDensity;
    private volatile OceanCorridorGraph corridorGraph;

    public OceanLandscapeOverlay(ContinentConfig config, ContinentGenerator continent) {
        this.seed = config.shape.seed0 ^ 0x0CEA11;
        this.continent = continent;
        this.config = config;
        this.shipwrecked = config.shape.shipwrecked;
        this.noiseScale = config.shape.oceanNoiseScale;
        this.landscapeScale = config.shape.oceanLandscapeScale;
        this.corridorStrength = config.shape.oceanCorridorStrength;
        this.shelfStrength = config.shape.oceanShelfStrength;
        this.volcanoDensity = config.shape.oceanVolcanoDensity;
        // Graph deferred — see corridorGraph().
    }

    public OceanCorridorGraph corridorGraph() {
        OceanCorridorGraph g = this.corridorGraph;
        if (g != null) {
            return g;
        }
        synchronized (this) {
            if (this.corridorGraph == null) {
                long t0 = System.nanoTime();
                try {
                    this.corridorGraph = OceanCorridorGraph.build(
                            this.continent,
                            this.config.shape.oceanCorridorPartners,
                            this.config.shape.oceanCorridorMaxDistance);
                } catch (Throwable t) {
                    TerraForged.LOG.error("[OceanLandscape] corridor graph failed; using empty", t);
                    this.corridorGraph = OceanCorridorGraph.emptyGraph();
                }
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                if (ms > 50L) {
                    TerraForged.LOG.warn("[OceanLandscape] corridor graph took {} ms (nodes/edges built lazily)", ms);
                }
            }
            return this.corridorGraph;
        }
    }

    public ContinentGenerator continent() {
        return this.continent;
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
        OceanCorridorGraph graph = this.corridorGraph();
        OceanZoneMask.Zone zone = OceanZoneMask.evaluate(
                this.continent,
                graph,
                shapeX,
                shapeY,
                cn,
                this.shipwrecked,
                this.corridorStrength,
                this.shelfStrength);

        // Volcano first so pipe/crater is never overwritten by corridor island emerge.
        DeepVolcano.Result volcano = DeepVolcano.Result.NONE;
        if (zone.deep() > 0.08F && this.volcanoDensity > 0.01F) {
            volcano = DeepVolcano.eval(
                    worldX, worldZ, this.seed, zone.deep(), this.shipwrecked, this.volcanoDensity);
            IslandEmergence.emergeVolcano(sample, volcano);
        }

        if (!volcano.hit() && zone.corridor() > 0.04F) {
            float relief = SeafloorLandscape.relief(
                    worldX, worldZ, this.seed, this.noiseScale, this.landscapeScale);
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
    }
}
