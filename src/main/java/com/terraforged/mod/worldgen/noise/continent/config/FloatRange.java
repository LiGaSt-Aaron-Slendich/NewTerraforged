package com.terraforged.mod.worldgen.noise.continent.config;

import com.terraforged.noise.util.NoiseUtil;

public class FloatRange {
    public float min;
    public float max;

    public FloatRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public float at(float alpha) {
        return NoiseUtil.lerp(this.min, this.max, alpha);
    }

    public FloatRange copy(FloatRange other) {
        this.min = other.min;
        this.max = other.max;
        return this;
    }

    public FloatRange scale(float factor) {
        this.min *= factor;
        this.max *= factor;
        return this;
    }
}
