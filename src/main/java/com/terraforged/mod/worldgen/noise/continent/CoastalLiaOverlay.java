package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.island.IslandScatter;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanCorridorGraph;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Post-shape coastal "Little Ice Age" overlay: regional strength mixes African-style
 * straight shores with bay/notch warp on {@code continentNoise} and mild cliff/carve
 * height modulation.
 *
 * <p>Experimental (EGF Untested → Coastal LIA) until explicitly released.
 * When Ocean Landscape provides a directed corridor graph, LIA is gated per continent:
 * 0 incoming corridors → no LIA; 3+ incoming → absolute-majority ragged coast.
 */
public final class CoastalLiaOverlay {
    /**
     * Master kill-switch in code. Runtime enable still requires
     * {@link com.terraforged.mod.platform.forge.TFNoiseVariantFlags#coastalLiaEnabled()}
     * and/or an active Ocean Landscape corridor graph.
     */
    public static final boolean CODE_ENABLED = true;

    /** Large regions of quiet vs eroded coast (blocks). */
    public static final float REGION_CELL = 2800.0F;
    /** Broad bay / inlet scale (blocks). */
    public static final float BAY_CELL = 420.0F;
    /** Fine coastal notches (blocks). */
    public static final float NOTCH_CELL = 95.0F;
    /** Cliff vs flat phase scale (blocks). */
    public static final float CLIFF_CELL = 160.0F;

    public static final float BAY_AMPLITUDE = 0.048F;
    public static final float NOTCH_AMPLITUDE = 0.016F;
    /** heightNoise lift on rocky phase. */
    public static final float CLIFF_AMPLITUDE = 0.075F;
    /** heightNoise carve on bay/flat phase. */
    public static final float NOTCH_CARVE = 0.040F;

    public static final float COAST_CN_MIN = 0.42F;
    public static final float COAST_CN_MAX = 0.62F;
    public static final float HEIGHT_CN_MIN = 0.48F;
    public static final float HEIGHT_CN_MAX = 0.58F;

    /** cliffPhase above this (with enough region strength) → stony/rocky shore. */
    public static final float ROCKY_CLIFF_THRESHOLD = 0.58F;
    public static final float ROCKY_STRENGTH_MIN = 0.35F;

    private final int seed;
    private ContinentGenerator continent;
    private OceanCorridorGraph corridorGraph;
    private float shapeFrequency = 1.0F / 3000.0F;

    public CoastalLiaOverlay(int seed) {
        this.seed = seed ^ 0xC0A571A;
    }

    /**
     * Optional Ocean Landscape graph: when active, incoming corridor count gates LIA
     * (0 → none, 3+ → majority ragged coast).
     */
    public void bindCorridorGraph(ContinentGenerator continent, OceanCorridorGraph graph, float continentScale) {
        this.continent = continent;
        this.corridorGraph = graph;
        this.shapeFrequency = 1.0F / Math.max(100.0F, continentScale);
    }

    /** @deprecated prefer {@link #bindCorridorGraph(ContinentGenerator, OceanCorridorGraph, float)} */
    @Deprecated
    public void bindCorridorGraph(ContinentGenerator continent, OceanCorridorGraph graph) {
        bindCorridorGraph(continent, graph, 3000.0F);
    }

    /** True when EGF Coastal LIA is on, or Ocean Landscape drives LIA via corridors. */
    public static boolean isActive() {
        if (!CODE_ENABLED) {
            return false;
        }
        return com.terraforged.mod.platform.forge.TFNoiseVariantFlags.coastalLiaEnabled()
                || com.terraforged.mod.platform.forge.TFNoiseVariantFlags.oceanLandscapeEnabled();
    }

    /**
     * Warp {@code continentNoise} near the shoreline after shape + islands.
     *
     * @param worldX worldZ block-equivalent coords (same frame as {@code IslandFeatureOverlay})
     * @param shapeX shapeY continent cell-space (same as Ocean Landscape) — used for LIA gate
     */
    public void applyContinent(float worldX, float worldZ, NoiseSample sample) {
        applyContinent(worldX, worldZ, Float.NaN, Float.NaN, sample);
    }

    public void applyContinent(float worldX, float worldZ, float shapeX, float shapeY, NoiseSample sample) {
        if (!isActive() || sample == null) {
            return;
        }
        float liaGate = corridorLiaFactor(shapeX, shapeY, worldX, worldZ);
        if (liaGate <= 0.0F) {
            return;
        }
        float cn = sample.continentNoise;
        float mask = bandMask(cn, COAST_CN_MIN, COAST_CN_MAX);
        if (mask <= 0.0F) {
            return;
        }
        float strength = regionStrength(worldX, worldZ) * liaGate;
        // Majority incoming (≥3): push nearly all coast into eroded/ragged regime.
        if (liaGate >= 0.99F) {
            strength = Math.max(strength, 0.82F);
        }
        if (strength <= 0.02F) {
            return;
        }

        float bay = (IslandScatter.valueNoise2(this.seed ^ 0x0BA11, worldX / BAY_CELL, worldZ / BAY_CELL) * 2.0F) - 1.0F;
        float notch = (IslandScatter.valueNoise2(this.seed ^ 0xA07C1, worldX / NOTCH_CELL, worldZ / NOTCH_CELL) * 2.0F) - 1.0F;
        float delta = (bay * BAY_AMPLITUDE + notch * NOTCH_AMPLITUDE) * strength * mask;

        sample.continentNoise = NoiseUtil.clamp(cn + delta, 0.0F, 1.0F);
        sample.baseNoise = NoiseUtil.clamp(sample.baseNoise + delta * 0.35F, 0.0F, 1.0F);
        sample.terrainType = ContinentPoints.getTerrainType(sample.continentNoise);
    }

    /**
     * Mild coastal height lift/carve in the land/sea blend band.
     */
    public void applyHeight(float worldX, float worldZ, NoiseSample sample) {
        applyHeight(worldX, worldZ, Float.NaN, Float.NaN, sample);
    }

    public void applyHeight(float worldX, float worldZ, float shapeX, float shapeY, NoiseSample sample) {
        if (!isActive() || sample == null) {
            return;
        }
        float liaGate = corridorLiaFactor(shapeX, shapeY, worldX, worldZ);
        if (liaGate <= 0.0F) {
            return;
        }
        float cn = sample.continentNoise;
        float mask = bandMask(cn, HEIGHT_CN_MIN, HEIGHT_CN_MAX);
        if (mask <= 0.0F) {
            return;
        }
        float strength = regionStrength(worldX, worldZ) * liaGate;
        if (liaGate >= 0.99F) {
            strength = Math.max(strength, 0.82F);
        }
        if (strength <= 0.02F) {
            return;
        }

        float cliff = cliffPhase(worldX, worldZ);
        float rocky = smoothstep(0.42F, 0.82F, cliff);
        float carve = 1.0F - rocky;

        float h = sample.heightNoise;
        h += CLIFF_AMPLITUDE * rocky * strength * mask;
        h -= NOTCH_CARVE * carve * strength * mask;
        sample.heightNoise = NoiseUtil.clamp(h, 0.0F, 1.0F);
    }

    /**
     * Beach-band biome pick: rocky LIA segment → stony shore instead of sand.
     */
    public boolean isRockyShore(float worldX, float worldZ, float continentNoise) {
        if (!isActive()) {
            return false;
        }
        if (corridorLiaFactor(Float.NaN, Float.NaN, worldX, worldZ) <= 0.0F) {
            return false;
        }
        if (continentNoise <= 0.5F || continentNoise > 0.505F) {
            return false;
        }
        if (regionStrength(worldX, worldZ) < ROCKY_STRENGTH_MIN) {
            return false;
        }
        return cliffPhase(worldX, worldZ) >= ROCKY_CLIFF_THRESHOLD;
    }

    public float regionStrength(float worldX, float worldZ) {
        float n = IslandScatter.valueNoise2(this.seed ^ 0x5E610, worldX / REGION_CELL, worldZ / REGION_CELL);
        // Bias so some stretches stay near-zero (African straight), others fully eroded.
        return smoothstep(0.28F, 0.78F, n);
    }

    public float cliffPhase(float worldX, float worldZ) {
        return IslandScatter.valueNoise2(this.seed ^ 0xC11F0, worldX / CLIFF_CELL, worldZ / CLIFF_CELL);
    }

    /**
     * When Ocean Landscape graph is bound: LIA factor from nearest landmass incoming count.
     * When no graph (classic Coastal LIA only): 1.0 if EGF Coastal LIA on, else 0.
     */
    private float corridorLiaFactor(float shapeX, float shapeY, float worldX, float worldZ) {
        boolean coastalEgf = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.coastalLiaEnabled();
        boolean olEgf = com.terraforged.mod.platform.forge.TFNoiseVariantFlags.oceanLandscapeEnabled();
        if (this.corridorGraph != null && this.corridorGraph.active() && this.continent != null && olEgf) {
            long nearest = nearestLandKey(shapeX, shapeY, worldX, worldZ);
            if (nearest == Long.MIN_VALUE) {
                return 0.0F;
            }
            return this.corridorGraph.liaFactor(nearest);
        }
        // No OL graph — classic Coastal LIA EGF only.
        return coastalEgf ? 1.0F : 0.0F;
    }

    private long nearestLandKey(float shapeX, float shapeY, float worldX, float worldZ) {
        float x;
        float y;
        if (!Float.isNaN(shapeX) && !Float.isNaN(shapeY)) {
            x = this.continent.cellShape.adjustX(shapeX);
            y = this.continent.cellShape.adjustY(shapeY);
        } else {
            x = this.continent.cellShape.adjustX(worldX * this.shapeFrequency);
            y = this.continent.cellShape.adjustY(worldZ * this.shapeFrequency);
        }
        float best = Float.MAX_VALUE;
        long bestKey = Long.MIN_VALUE;
        for (long key : this.corridorGraph.landKeys()) {
            int cx = PosUtil.unpackLeft(key);
            int cy = PosUtil.unpackRight(key);
            CellPoint cell = this.continent.getCell(cx, cy);
            float dist = NoiseUtil.dist2(x, y, cell.px, cell.py);
            if (dist < best) {
                best = dist;
                bestKey = key;
            }
        }
        return bestKey;
    }

    private static float bandMask(float cn, float min, float max) {
        if (cn <= min || cn >= max) {
            return 0.0F;
        }
        float mid = (min + max) * 0.5F;
        float half = (max - min) * 0.5F;
        if (half <= 1.0E-5F) {
            return 0.0F;
        }
        float t = 1.0F - Math.abs(cn - mid) / half;
        return NoiseUtil.clamp(t * t * (3.0F - 2.0F * t), 0.0F, 1.0F);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = NoiseUtil.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
