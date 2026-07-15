/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;

public class RidgeNoise
extends NoiseSource {
    private static final int RIDGED_MAX_OCTAVE = 30;
    private final Interpolation interpolation;
    private final float[] spectralWeights;
    private final float min;
    private final float max;
    private final float range;

    public RidgeNoise(Builder builder) {
        super(builder);
        this.interpolation = builder.getInterp();
        this.spectralWeights = new float[30];
        float h = 1.0f;
        float frequency = 1.0f;
        for (int i = 0; i < 30; ++i) {
            this.spectralWeights[i] = NoiseUtil.pow(frequency, -h);
            frequency *= this.lacunarity;
        }
        this.min = 0.0f;
        this.max = this.calculateBound(0.0f, builder.getOctaves(), builder.getGain());
        this.range = Math.abs(this.max - this.min);
    }

    @Override
    public String getSpecName() {
        return "Ridge";
    }

    @Override
    public float getValue(float x, float y, int seed) {
        x *= this.frequency;
        y *= this.frequency;
        float value = 0.0f;
        float weight = 1.0f;
        float offset = 1.0f;
        float amp = 2.0f;
        for (int octave = 0; octave < this.octaves; ++octave) {
            float signal = Noise.singlePerlin(x, y, seed + octave, this.interpolation);
            signal = Math.abs(signal);
            signal = offset - signal;
            signal *= signal;
            signal *= weight;
            weight = signal * amp;
            weight = NoiseUtil.clamp(weight, 0.0f, 1.0f);
            value += signal * this.spectralWeights[octave];
            x *= this.lacunarity;
            y *= this.lacunarity;
            amp *= this.gain;
        }
        return NoiseUtil.map(value, this.min, this.max, this.range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RidgeNoise)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        RidgeNoise that = (RidgeNoise)o;
        if (Float.compare(that.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(that.max, this.max) != 0) {
            return false;
        }
        if (Float.compare(that.range, this.range) != 0) {
            return false;
        }
        if (this.interpolation != that.interpolation) {
            return false;
        }
        return Arrays.equals(this.spectralWeights, that.spectralWeights);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.interpolation.hashCode();
        result = 31 * result + Arrays.hashCode(this.spectralWeights);
        result = 31 * result + (this.min != 0.0f ? Float.floatToIntBits(this.min) : 0);
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        result = 31 * result + (this.range != 0.0f ? Float.floatToIntBits(this.range) : 0);
        return result;
    }

    private float calculateBound(float signal, int octaves, float gain) {
        float value = 0.0f;
        float weight = 1.0f;
        float amp = 2.0f;
        float offset = 1.0f;
        for (int curOctave = 0; curOctave < octaves; ++curOctave) {
            float noise = signal;
            noise = Math.abs(noise);
            noise = offset - noise;
            noise *= noise;
            noise *= weight;
            weight = noise * amp;
            weight = Math.min(1.0f, Math.max(0.0f, weight));
            value += noise * this.spectralWeights[curOctave];
            amp *= gain;
        }
        return value;
    }

    public static DataSpec<RidgeNoise> ridgeSpec() {
        return RidgeNoise.specBuilder("Ridge", RidgeNoise.class, RidgeNoise::new).build();
    }
}

