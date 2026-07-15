/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class LegacyTerrace
extends Modifier {
    private final int maxIndex;
    private final float blend;
    private final Step[] steps;
    private final Module lowerCurve;
    private final Module upperCurve;
    private static final DataFactory<LegacyTerrace> factory = (data, spec, context) -> new LegacyTerrace(spec.get("source", data, Module.class, context), spec.get("lower_curve", data, Module.class, context), spec.get("upper_curve", data, Module.class, context), spec.get("steps", data, DataValue::asInt), spec.get("blend_range", data, DataValue::asFloat).floatValue());

    public LegacyTerrace(Module source, Module lowerCurve, Module upperCurve, int steps, float blendRange) {
        super(source);
        this.blend = blendRange;
        this.maxIndex = steps - 1;
        this.steps = new Step[steps];
        this.lowerCurve = lowerCurve;
        this.upperCurve = upperCurve;
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
        return "LegacyTerrace";
    }

    @Override
    public float getValue(float x, float y) {
        float value = this.source.getValue(x, y);
        value = NoiseUtil.clamp(value, 0.0f, 1.0f);
        return this.modify(x, y, value);
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        int index = NoiseUtil.round(noiseValue * (float)this.maxIndex);
        Step step = this.steps[index];
        if (noiseValue < step.lowerBound) {
            if (index > 0) {
                Step lower = this.steps[index - 1];
                float alpha = (noiseValue - lower.upperBound) / (step.lowerBound - lower.upperBound);
                alpha = 1.0f - Interpolation.CURVE3.apply(alpha);
                float range = step.value - lower.value;
                return step.value - alpha * range * this.upperCurve.getValue(x, y);
            }
        } else if (noiseValue > step.upperBound && index < this.maxIndex) {
            Step upper = this.steps[index + 1];
            float alpha = (noiseValue - step.upperBound) / (upper.lowerBound - step.upperBound);
            alpha = Interpolation.CURVE3.apply(alpha);
            float range = upper.value - step.value;
            return step.value + alpha * range * this.lowerCurve.getValue(x, y);
        }
        return step.value;
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
        LegacyTerrace that = (LegacyTerrace)o;
        if (this.maxIndex != that.maxIndex) {
            return false;
        }
        if (Float.compare(that.blend, this.blend) != 0) {
            return false;
        }
        if (!this.lowerCurve.equals(that.lowerCurve)) {
            return false;
        }
        return this.upperCurve.equals(that.upperCurve);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.maxIndex;
        result = 31 * result + (this.blend != 0.0f ? Float.floatToIntBits(this.blend) : 0);
        result = 31 * result + this.lowerCurve.hashCode();
        result = 31 * result + this.upperCurve.hashCode();
        return result;
    }

    private int getIndex(float value) {
        int index = NoiseUtil.round(value * (float)this.maxIndex);
        if (index > this.maxIndex) {
            return this.maxIndex;
        }
        if (index < 0) {
            return 0;
        }
        return index;
    }

    public static DataSpec<LegacyTerrace> spec() {
        return Modifier.specBuilder(LegacyTerrace.class, factory).add("steps", (Object)1, s -> s.steps.length).add("blend_range", (Object)1, s -> Float.valueOf(s.blend)).addObj("source", Module.class, s -> s.source).addObj("lower_curve", Module.class, s -> s.lowerCurve).addObj("upper_curve", Module.class, s -> s.upperCurve).build();
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

