/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.func.DistanceFunc;
import com.terraforged.noise.func.EdgeFunc;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;

public class CellEdgeNoise
extends NoiseSource {
    private final EdgeFunc edgeFunc;
    private final DistanceFunc distFunc;
    private final float distance;

    public CellEdgeNoise(Builder builder) {
        super(builder);
        this.edgeFunc = builder.getEdgeFunc();
        this.distFunc = builder.getDistFunc();
        this.distance = builder.getDisplacement();
    }

    @Override
    public String getSpecName() {
        return "CellEdge";
    }

    @Override
    public float getValue(float x, float y, int seed) {
        float value = Noise.cellEdge(x *= this.frequency, y *= this.frequency, seed, this.distance, this.edgeFunc, this.distFunc);
        return NoiseUtil.map(value, this.edgeFunc.min(), this.edgeFunc.max(), this.edgeFunc.range());
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
        CellEdgeNoise that = (CellEdgeNoise)o;
        if (Float.compare(that.distance, this.distance) != 0) {
            return false;
        }
        if (this.edgeFunc != that.edgeFunc) {
            return false;
        }
        return this.distFunc == that.distFunc;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.edgeFunc.hashCode();
        result = 31 * result + this.distFunc.hashCode();
        result = 31 * result + (this.distance != 0.0f ? Float.floatToIntBits(this.distance) : 0);
        return result;
    }

    public static DataSpec<CellEdgeNoise> spec() {
        return CellEdgeNoise.specBuilder("CellEdge", CellEdgeNoise.class, CellEdgeNoise::new).add("distance", (Object)Float.valueOf(1.0f), f -> Float.valueOf(f.distance)).add("edge_func", (Object)Builder.DEFAULT_EDGE_FUNC, f -> f.edgeFunc).add("dist_func", (Object)Builder.DEFAULT_DIST_FUNC, f -> f.distFunc).build();
    }
}

