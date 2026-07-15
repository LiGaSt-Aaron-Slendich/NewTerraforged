/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;

public class Terrace
extends Modifier {
    private static final float MIN_NOISE_VALUE = 0.0f;
    private static final float MAX_NOISE_VALUE = 0.999999f;
    private final float blend;
    private final float length;
    private final int maxIndex;
    private final Step[] steps;
    private final Module ramp;
    private final Module cliff;
    private final Module rampHeight;
    private static final DataFactory<Terrace> factory = (data, spec, context) -> new Terrace(spec.get("source", data, Module.class, context), spec.get("ramp", data, Module.class, context), spec.get("cliff", data, Module.class, context), spec.get("ramp_height", data, Module.class, context), spec.get("steps", data, DataValue::asInt), spec.get("blend_range", data, DataValue::asFloat).floatValue());

    public Terrace(Module source, Module ramp, Module cliff, Module rampHeight, int steps, float blendRange) {
        super(source);
        this.blend = blendRange;
        this.maxIndex = steps - 1;
        this.steps = new Step[steps];
        this.length = (float)steps * 0.999999f;
        this.ramp = ramp;
        this.cliff = cliff;
        this.rampHeight = rampHeight;
        float min = source.minValue();
        float max = source.maxValue();
        float range = max - min;
        float spacing = range / (float)(steps - 1);
        for (int i = 0; i < steps; ++i) {
            float value = (float)i * spacing;
            this.steps[i] = new Step(value, spacing, blendRange);
        }
    }

    @Override
    public String getSpecName() {
        return "Terrace";
    }

    @Override
    public float getValue(float x, float y) {
        float value = this.source.getValue(x, y);
        value = NoiseUtil.clamp(value, 0.0f, 0.999999f);
        return this.modify(x, y, value);
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        Step next;
        int index = NoiseUtil.floor(noiseValue * (float)this.steps.length);
        Step step = this.steps[index];
        if (index == this.maxIndex) {
            return step.value;
        }
        if (noiseValue < step.lowerBound) {
            return step.value;
        }
        if (noiseValue > step.upperBound) {
            Step next2 = this.steps[index + 1];
            return next2.value;
        }
        float ramp = 1.0f - this.ramp.getValue(x, y) * 0.5f;
        float cliff = 1.0f - this.cliff.getValue(x, y) * 0.5f;
        float alpha = (noiseValue - step.lowerBound) / (step.upperBound - step.lowerBound);
        float value = step.value;
        if (alpha > ramp) {
            next = this.steps[index + 1];
            float rampSize = 1.0f - ramp;
            float rampAlpha = (alpha - ramp) / rampSize;
            float rampHeight = this.rampHeight.getValue(x, y);
            value += (next.value - value) * rampAlpha * rampHeight;
        }
        if (alpha > cliff) {
            next = this.steps[index + 1];
            float cliffAlpha = (alpha - cliff) / (1.0f - cliff);
            value = NoiseUtil.lerp(value, next.value, cliffAlpha);
        }
        return value;
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
        Terrace terrace = (Terrace)o;
        if (Float.compare(terrace.blend, this.blend) != 0) {
            return false;
        }
        if (Float.compare(terrace.length, this.length) != 0) {
            return false;
        }
        if (this.maxIndex != terrace.maxIndex) {
            return false;
        }
        if (!Arrays.equals(this.steps, terrace.steps)) {
            return false;
        }
        if (!this.ramp.equals(terrace.ramp)) {
            return false;
        }
        if (!this.cliff.equals(terrace.cliff)) {
            return false;
        }
        return this.rampHeight.equals(terrace.rampHeight);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.blend != 0.0f ? Float.floatToIntBits(this.blend) : 0);
        result = 31 * result + (this.length != 0.0f ? Float.floatToIntBits(this.length) : 0);
        result = 31 * result + this.maxIndex;
        result = 31 * result + Arrays.hashCode(this.steps);
        result = 31 * result + this.ramp.hashCode();
        result = 31 * result + this.cliff.hashCode();
        result = 31 * result + this.rampHeight.hashCode();
        return result;
    }

    public static DataSpec<Terrace> spec() {
        return Modifier.specBuilder(Terrace.class, factory).add("steps", (Object)1, s -> s.steps.length).add("blend_range", (Object)1, s -> Float.valueOf(s.blend)).addObj("source", Module.class, s -> s.source).addObj("ramp", Module.class, s -> s.ramp).addObj("cliff", Module.class, s -> s.cliff).addObj("ramp_height", Module.class, s -> s.rampHeight).build();
    }

    private static class Step {
        private final float value;
        private final float lowerBound;
        private final float upperBound;

        private Step(float value, float distance, float blendRange) {
            this.value = value;
            float blend = distance * blendRange;
            float bound = (distance - blend) / 2.0f;
            this.lowerBound = value - bound;
            this.upperBound = value + bound;
        }
    }
}

