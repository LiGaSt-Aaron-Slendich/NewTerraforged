/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.CellFunc;
import com.terraforged.noise.func.DistanceFunc;
import com.terraforged.noise.func.EdgeFunc;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.util.NoiseSpec;
import java.util.function.Function;

public abstract class NoiseSource
implements Module {
    protected final int seed;
    protected final int octaves;
    protected final float gain;
    protected final float frequency;
    protected final float lacunarity;
    protected final Interpolation interpolation;

    public NoiseSource(Builder builder) {
        this.seed = builder.getSeed();
        this.octaves = builder.getOctaves();
        this.lacunarity = builder.getLacunarity();
        this.gain = builder.getGain();
        this.frequency = builder.getFrequency();
        this.interpolation = builder.getInterp();
    }

    @Override
    public float getValue(float x, float y) {
        return this.getValue(x, y, this.seed);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        NoiseSource that = (NoiseSource)o;
        if (this.seed != that.seed) {
            return false;
        }
        if (this.octaves != that.octaves) {
            return false;
        }
        if (Float.compare(that.gain, this.gain) != 0) {
            return false;
        }
        if (Float.compare(that.frequency, this.frequency) != 0) {
            return false;
        }
        if (Float.compare(that.lacunarity, this.lacunarity) != 0) {
            return false;
        }
        return this.interpolation == that.interpolation;
    }

    public int hashCode() {
        int result = this.seed;
        result = 31 * result + this.octaves;
        result = 31 * result + (this.gain != 0.0f ? Float.floatToIntBits(this.gain) : 0);
        result = 31 * result + (this.frequency != 0.0f ? Float.floatToIntBits(this.frequency) : 0);
        result = 31 * result + (this.lacunarity != 0.0f ? Float.floatToIntBits(this.lacunarity) : 0);
        result = 31 * result + this.interpolation.hashCode();
        return result;
    }

    public abstract float getValue(float var1, float var2, int var3);

    public static Builder readData(DataObject data, DataSpec<?> spec, Context context) {
        Builder builder = new Builder();
        builder.seed(NoiseSpec.seed(data, spec, context));
        builder.gain(spec.get("gain", data, DataValue::asDouble));
        builder.octaves(spec.get("octaves", data, DataValue::asInt));
        builder.frequency(spec.get("frequency", data, DataValue::asDouble));
        builder.lacunarity(spec.get("lacunarity", data, DataValue::asDouble));
        builder.interp(spec.get("interpolation", data, v -> Interpolation.valueOf(v.asString())));
        if (data.has("cell_func")) {
            builder.cellFunc(spec.getEnum("cell_func", data, CellFunc.class));
        }
        if (data.has("edge_func")) {
            builder.edgeFunc(spec.getEnum("edge_func", data, EdgeFunc.class));
        }
        if (data.has("dist_func")) {
            builder.distFunc(spec.getEnum("dist_func", data, DistanceFunc.class));
        }
        if (data.has("source")) {
            builder.source(spec.get("source", data, Module.class, context));
        }
        return builder;
    }

    private static <S extends NoiseSource> DataFactory<S> constructor(Function<Builder, S> constructor) {
        return (data, spec, context) -> (NoiseSource)constructor.apply(NoiseSource.readData(data, spec, context));
    }

    public static <S extends NoiseSource> DataSpec.Builder<S> specBuilder(String name, Class<S> type, Function<Builder, S> constructor) {
        return NoiseSource.specBuilder(name, type, NoiseSource.constructor(constructor));
    }

    public static <S extends NoiseSource> DataSpec.Builder<S> specBuilder(String name, Class<S> type, DataFactory<S> constructor) {
        return DataSpec.builder(name, type, constructor).add("seed", (Object)1337, NoiseSpec.seed(f -> f.seed)).add("gain", (Object)Float.valueOf(0.5f), f -> Float.valueOf(f.gain)).add("octaves", (Object)1, f -> f.octaves).add("frequency", (Object)Float.valueOf(1.0f), f -> Float.valueOf(f.frequency)).add("lacunarity", (Object)Float.valueOf(2.0f), f -> Float.valueOf(f.lacunarity)).add("interpolation", (Object)Builder.DEFAULT_INTERPOLATION, f -> f.interpolation);
    }
}

