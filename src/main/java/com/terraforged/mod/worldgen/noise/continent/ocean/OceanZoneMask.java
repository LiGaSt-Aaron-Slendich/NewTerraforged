package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Sample-time ocean zoning for the ocean-landscape island system.
 * Corridor = soft Voronoi edge between two land cells (inter-continent ridge).
 * Deep = far from shore by continentNoise. Shipwrecked uses dense noise banks.
 */
public final class OceanZoneMask {
    /** Above this cn the new system does not paint (coast left to LIA / blend). */
    public static final float SHORE_CN = 0.28F;
    /** Mid-ocean deep gate (same spirit as IslandScatter.midOceanAllow). */
    public static final float DEEP_CN = 0.18F;
    /** Shipwrecked bank density cap — avoids wall-to-wall islands. */
    public static final float SHIP_BANK_CAP = 0.58F;

    public record Zone(float corridor, float deep) {
        public static final Zone NONE = new Zone(0.0F, 0.0F);
    }

    private OceanZoneMask() {
    }

    public static Zone evaluate(
            ContinentGenerator continent,
            float shapeX,
            float shapeY,
            float continentNoise,
            boolean shipwrecked
    ) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        float deep = deepAllow(cn);
        float corridor;
        if (shipwrecked) {
            corridor = shipwreckedBanks(shapeX, shapeY, continent.seed) * SHIP_BANK_CAP;
            deep = Math.max(deep, 0.55F + (1.0F - cn) * 0.35F);
        } else if (cn >= SHORE_CN) {
            corridor = 0.0F;
        } else {
            float edge = landLandEdge(continent, shapeX, shapeY);
            float oceanFade = 1.0F - cn / SHORE_CN;
            corridor = edge * oceanFade;
        }
        return new Zone(corridor, deep);
    }

    public static float deepAllow(float continentNoise) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        if (cn >= DEEP_CN) {
            return 0.0F;
        }
        return (DEEP_CN - cn) / DEEP_CN;
    }

    /**
     * Strength of equidistance ridge between the two nearest land continent cells.
     */
    public static float landLandEdge(ContinentGenerator continent, float shapeX, float shapeY) {
        float x = continent.cellShape.adjustX(shapeX);
        float y = continent.cellShape.adjustY(shapeY);
        long nearest = continent.getNearestCell(x, y);
        int cx = PosUtil.unpackLeft(nearest);
        int cy = PosUtil.unpackRight(nearest);

        float d0 = Float.MAX_VALUE;
        float d1 = Float.MAX_VALUE;
        int landCount = 0;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                CellPoint cell = continent.getCell(cx + dx, cy + dz);
                if (continent.shapeGenerator.getThresholdValue(cell) <= 0.0F) {
                    continue;
                }
                landCount++;
                float dist = NoiseUtil.sqrt(NoiseUtil.dist2(x, y, cell.px, cell.py));
                if (dist < d0) {
                    d1 = d0;
                    d0 = dist;
                } else if (dist < d1) {
                    d1 = dist;
                }
            }
        }

        if (landCount < 2 || d1 >= Float.MAX_VALUE * 0.5F) {
            return 0.0F;
        }

        float avg = 0.5F * (d0 + d1);
        float width = Math.max(0.04F, avg * 0.32F);
        float edge = 1.0F - NoiseUtil.clamp(Math.abs(d0 - d1) / width, 0.0F, 1.0F);
        // Prefer mid-ocean ridges: weaken if very close to a land centre.
        float away = NoiseUtil.clamp(d0 / Math.max(0.08F, avg * 0.9F), 0.0F, 1.0F);
        return NoiseUtil.clamp(edge * away, 0.0F, 1.0F);
    }

    /** Dense soft banks when continents are suppressed (Shipwrecked). */
    public static float shipwreckedBanks(float shapeX, float shapeY, int seed) {
        float n = valueNoise2(seed ^ 0x0CEA11, shapeX * 2.4F, shapeY * 2.4F);
        float n2 = valueNoise2(seed ^ 0x51ED, shapeX * 0.55F, shapeY * 0.55F);
        float ridge = NoiseUtil.clamp((n - 0.38F) / 0.42F, 0.0F, 1.0F);
        float macro = NoiseUtil.clamp((n2 - 0.30F) / 0.50F, 0.0F, 1.0F);
        return ridge * (0.45F + 0.55F * macro);
    }

    private static float valueNoise2(int seed, float x, float z) {
        int x0 = NoiseUtil.floor(x);
        int z0 = NoiseUtil.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fz * fz * (3.0F - 2.0F * fz);
        float a = hash01(seed, x0, z0);
        float b = hash01(seed, x0 + 1, z0);
        float c = hash01(seed, x0, z0 + 1);
        float d = hash01(seed, x0 + 1, z0 + 1);
        return NoiseUtil.lerp(NoiseUtil.lerp(a, b, u), NoiseUtil.lerp(c, d, u), v);
    }

    private static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
