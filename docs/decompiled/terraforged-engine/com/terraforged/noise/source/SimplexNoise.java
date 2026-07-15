/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;

public class SimplexNoise
extends NoiseSource {
    private final float min;
    private final float max;
    private final float range;
    private static final float[] signals = new float[]{1.0f, 0.989f, 0.81f, 0.781f, 0.708f, 0.702f, 0.696f};

    public SimplexNoise(Builder builder) {
        super(builder);
        this.min = -SimplexNoise.max(builder.getOctaves(), builder.getGain());
        this.max = SimplexNoise.max(builder.getOctaves(), builder.getGain());
        this.range = this.max - this.min;
    }

    @Override
    public String getSpecName() {
        return "Simplex";
    }

    @Override
    public float getValue(float x, float y, int seed) {
        x *= this.frequency;
        y *= this.frequency;
        float sum = 0.0f;
        float amp = 1.0f;
        for (int i = 0; i < this.octaves; ++i) {
            sum += Noise.singleLegacySimplex(x, y, seed + i) * amp;
            x *= this.lacunarity;
            y *= this.lacunarity;
            amp *= this.gain;
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
        SimplexNoise that = (SimplexNoise)o;
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

    private static float max(int octaves, float gain) {
        float signal = SimplexNoise.signal(octaves);
        float sum = 0.0f;
        float amp = 1.0f;
        for (int i = 0; i < octaves; ++i) {
            sum += amp * signal;
            amp *= gain;
        }
        return sum;
    }

    private static float signal(int octaves) {
        int index = Math.min(octaves, signals.length - 1);
        return signals[index];
    }

    public static DataSpec<SimplexNoise> spec() {
        return SimplexNoise.specBuilder("Simplex", SimplexNoise.class, SimplexNoise::new).build();
    }
}

