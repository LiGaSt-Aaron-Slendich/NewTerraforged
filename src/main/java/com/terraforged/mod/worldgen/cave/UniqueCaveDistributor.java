package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;

public class UniqueCaveDistributor
implements Module {
    private final int seed;
    private final float frequency;
    private final float jitter;
    private final float density;

    public UniqueCaveDistributor(int seed, float frequency, float jitter, float density) {
        this.seed = seed;
        this.frequency = frequency;
        this.jitter = jitter;
        this.density = density;
    }

    @Override
    public float getValue(float x, float y) {
        return this.sampleAt(this.seed, x, y);
    }

    public float sampleAt(int seed, float x, float y) {
        int maxX = NoiseUtil.floor(x *= this.frequency) + 1;
        int maxY = NoiseUtil.floor(y *= this.frequency) + 1;
        int cellX = maxX - 1;
        int cellY = maxY - 1;
        float distA = Float.POSITIVE_INFINITY;
        float distB = Float.POSITIVE_INFINITY;
        for (int cy = maxY - 2; cy <= maxY; ++cy) {
            float ox = (float)(cy & 1) * 0.5f;
            for (int cx = maxX - 2; cx <= maxX; ++cx) {
                Vec2f vec = NoiseUtil.cell(seed, cx, cy);
                float px = (float)cx + ox + vec.x * this.jitter;
                float py = (float)cy + vec.y * this.jitter;
                float d2 = NoiseUtil.dist2(x, y, px, py);
                if (d2 < distA) {
                    distB = distA;
                    distA = d2;
                    cellX = cx;
                    cellY = cy;
                    continue;
                }
                if (!(d2 < distB)) continue;
                distB = d2;
            }
        }
        if (this.cellValue(cellX, cellY) > this.density) {
            return 0.0f;
        }
        return UniqueCaveDistributor.edgeFloor(1.0f - NoiseUtil.sqrt(distA / distB));
    }

    private static float edgeFloor(float value) {
        return Math.max(0.1f, value);
    }

    private float cellValue(int cellX, int cellY) {
        float noise = NoiseUtil.valCoord2D(this.seed, cellX, cellY);
        return (1.0f + noise) * 0.5f;
    }
}
