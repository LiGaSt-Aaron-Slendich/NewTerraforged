/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.CellFunc;
import com.terraforged.noise.func.DistanceFunc;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.util.Noise;

public class CellNoise
extends NoiseSource {
    private final Module lookup;
    private final CellFunc cellFunc;
    private final DistanceFunc distFunc;
    private final float min;
    private final float max;
    private final float range;
    private final float distance;

    public CellNoise(Builder builder) {
        super(builder);
        this.lookup = builder.getSource();
        this.cellFunc = builder.getCellFunc();
        this.distFunc = builder.getDistFunc();
        this.distance = builder.getDisplacement();
        this.min = CellNoise.min(this.cellFunc, this.lookup);
        this.max = CellNoise.max(this.cellFunc, this.lookup);
        this.range = this.max - this.min;
    }

    @Override
    public String getSpecName() {
        return "Cell";
    }

    @Override
    public float getValue(float x, float y, int seed) {
        float value = Noise.cell(x *= this.frequency, y *= this.frequency, seed, this.distance, this.cellFunc, this.distFunc, this.lookup);
        return this.cellFunc.mapValue(value, this.min, this.max, this.range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CellNoise cellNoise = (CellNoise)o;
        if (Float.compare(cellNoise.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(cellNoise.max, this.max) != 0) {
            return false;
        }
        if (Float.compare(cellNoise.range, this.range) != 0) {
            return false;
        }
        if (Float.compare(cellNoise.distance, this.distance) != 0) {
            return false;
        }
        if (!this.lookup.equals(cellNoise.lookup)) {
            return false;
        }
        if (this.cellFunc != cellNoise.cellFunc) {
            return false;
        }
        return this.distFunc == cellNoise.distFunc;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.lookup.hashCode();
        result = 31 * result + this.cellFunc.hashCode();
        result = 31 * result + this.distFunc.hashCode();
        result = 31 * result + (this.min != 0.0f ? Float.floatToIntBits(this.min) : 0);
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        result = 31 * result + (this.range != 0.0f ? Float.floatToIntBits(this.range) : 0);
        result = 31 * result + (this.distance != 0.0f ? Float.floatToIntBits(this.distance) : 0);
        return result;
    }

    static float min(CellFunc func, Module lookup) {
        if (func == CellFunc.NOISE_LOOKUP) {
            return lookup.minValue();
        }
        if (func == CellFunc.DISTANCE) {
            return -1.0f;
        }
        return -1.0f;
    }

    static float max(CellFunc func, Module lookup) {
        if (func == CellFunc.NOISE_LOOKUP) {
            return lookup.maxValue();
        }
        if (func == CellFunc.DISTANCE) {
            return 0.25f;
        }
        return 1.0f;
    }

    public static DataSpec<CellNoise> spec() {
        return CellNoise.specBuilder("Cell", CellNoise.class, CellNoise::new).add("distance", (Object)Float.valueOf(1.0f), f -> Float.valueOf(f.distance)).add("cell_func", (Object)Builder.DEFAULT_CELL_FUNC, f -> f.cellFunc).add("dist_func", (Object)Builder.DEFAULT_DIST_FUNC, f -> f.distFunc).addObj("source", Module.class, f -> f.lookup).build();
    }
}

