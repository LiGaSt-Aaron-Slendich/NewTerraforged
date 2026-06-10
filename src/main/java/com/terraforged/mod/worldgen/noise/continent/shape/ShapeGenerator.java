package com.terraforged.mod.worldgen.noise.continent.shape;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.ContinentPoints;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.shape.FalloffPoint;
import com.terraforged.noise.util.NoiseUtil;

public class ShapeGenerator {
    private static final int RADIUS = 2;
    private final float baseFalloff;
    private final float continentFalloff;
    public final float threshold;
    public final float baseFalloffMin;
    public final float baseFalloffMax;
    private final ContinentGenerator continent;
    private final FalloffPoint[] falloffPoints;
    private final ThreadLocal<long[]> edgeBuffer = ThreadLocal.withInitial(() -> new long[9]);
    private final ThreadLocal<CellLocal[]> cellBuffer = ThreadLocal.withInitial(CellLocal::init);

    public ShapeGenerator(ContinentGenerator continent, ContinentConfig config, ControlPoints controlPoints) {
        this.continent = continent;
        this.baseFalloff = config.noise.baseNoiseFalloff;
        this.continentFalloff = config.noise.continentNoiseFalloff;
        this.falloffPoints = ContinentPoints.getFalloff(controlPoints);
        this.threshold = config.shape.threshold;
        this.baseFalloffMin = config.shape.threshold + config.shape.baseFalloffMin;
        this.baseFalloffMax = config.shape.threshold + config.shape.baseFalloffMax;
    }

    public float getThresholdValue(CellPoint cell) {
        return cell.noise < this.threshold ? 0.0f : 1.0f;
    }

    public float getFalloff(float continentNoise) {
        return ContinentPoints.getFalloff(continentNoise, this.falloffPoints);
    }

    public float getBaseNoise(float value) {
        float min = this.baseFalloffMin;
        float max = this.baseFalloffMax;
        return NoiseUtil.map(value, min, max, max - min);
    }

    public NoiseSample sample(int seed, float x, float y, NoiseSample sample) {
        long centre = this.continent.getNearestCell(seed, x, y);
        int centreX = PosUtil.unpackLeft(centre);
        int centreY = PosUtil.unpackRight(centre);
        x = this.continent.cellShape.adjustX(x);
        y = this.continent.cellShape.adjustY(y);
        int minX = centreX - 2;
        int minY = centreY - 2;
        int maxX = centreX + 2;
        int maxY = centreY + 2;
        int closest = -1;
        float min0 = Float.MAX_VALUE;
        float min1 = Float.MAX_VALUE;
        CellLocal[] buffer = this.cellBuffer.get();
        int i = 0;
        for (int cy = minY; cy <= maxY; ++cy) {
            int cx = minX;
            while (cx <= maxX) {
                CellPoint cell = this.continent.getCell(seed, cx, cy);
                CellLocal local = buffer[i];
                float distance = NoiseUtil.sqrt(NoiseUtil.dist2(x, y, cell.px, cell.py));
                local.cell = cell;
                local.context = distance;
                if (distance < min0) {
                    min1 = min0;
                    min0 = distance;
                    closest = i;
                } else if (distance < min1) {
                    min1 = distance;
                }
                ++cx;
                ++i;
            }
        }
        return this.sampleEdges(closest, min0, min1, buffer, sample);
    }

    private NoiseSample sampleEdges(int index, float min0, float min1, CellLocal[] buffer, NoiseSample sample) {
        float borderDistance = (min0 + min1) * 0.5f;
        float baseBlend = borderDistance * this.baseFalloff;
        float continentBlend = borderDistance * this.continentFalloff;
        float sumBase = 0.0f;
        float sumContinent = 0.0f;
        float sumBaseWeight = 0.0f;
        float sumContinentWeight = 0.0f;
        for (CellLocal local : buffer) {
            float dist = local.context;
            float baseValue = local.cell.noise();
            float continentValue = this.getThresholdValue(local.cell);
            float baseWeight = ShapeGenerator.getWeight(dist, min0, baseBlend);
            float continentWeight = ShapeGenerator.getWeight(dist, min0, continentBlend);
            sumBase += baseValue * baseWeight;
            sumContinent += continentValue * continentWeight;
            sumBaseWeight += baseWeight;
            sumContinentWeight += continentWeight;
        }
        sample.baseNoise = this.getBaseNoise(sumBase / sumBaseWeight);
        sample.continentNoise = this.getFalloff(sumContinent / sumContinentWeight);
        return sample;
    }

    private static float getWeight(float dist, float origin, float blendRange) {
        float delta = dist - origin;
        if (delta <= 0.0f) {
            return 1.0f;
        }
        if (delta >= blendRange) {
            return 0.0f;
        }
        return 1.0f - delta / blendRange;
    }

    protected static class CellLocal {
        public CellPoint cell;
        public float context;

        protected CellLocal() {
        }

        protected static CellLocal[] init() {
            int size = 5;
            CellLocal[] cells = new CellLocal[size * size];
            for (int i = 0; i < cells.length; ++i) {
                cells[i] = new CellLocal();
            }
            return cells;
        }
    }
}
