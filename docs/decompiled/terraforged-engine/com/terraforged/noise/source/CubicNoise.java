/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;

public class CubicNoise
extends NoiseSource {
    private final float min;
    private final float max;
    private final float range;

    public CubicNoise(Builder builder) {
        super(builder);
        this.min = this.calculateBound(-0.75f, builder.getOctaves(), builder.getGain());
        this.max = this.calculateBound(0.75f, builder.getOctaves(), builder.getGain());
        this.range = this.max - this.min;
    }

    @Override
    public String getSpecName() {
        return "Cubic";
    }

    @Override
    public float minValue() {
        return this.min;
    }

    @Override
    public float maxValue() {
        return this.max;
    }

    @Override
    public float getValue(float x, float y, int seed) {
        float sum = Noise.singleCubic(x *= this.frequency, y *= this.frequency, seed);
        float amp = 1.0f;
        int i = 0;
        while (++i < this.octaves) {
            sum += Noise.singleCubic(x *= this.lacunarity, y *= this.lacunarity, ++seed) * (amp *= this.gain);
        }
        return NoiseUtil.map(sum, this.min, this.max, this.range);
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
        CubicNoise that = (CubicNoise)o;
        if (Float.compare(that.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(that.max, this.max) != 0) {
            return false;
        }
        return Float.compare(that.range, this.range) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.min != 0.0f ? Float.floatToIntBits(this.min) : 0);
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        result = 31 * result + (this.range != 0.0f ? Float.floatToIntBits(this.range) : 0);
        return result;
    }

    private float calculateBound(float signal, int octaves, float gain) {
        float amp = 1.0f;
        float value = signal;
        for (int i = 1; i < octaves; ++i) {
            value += signal * (amp *= gain);
        }
        return value;
    }

    public static DataSpec<CubicNoise> spec() {
        return CubicNoise.specBuilder("Cubic", CubicNoise.class, CubicNoise::new).build();
    }
}

