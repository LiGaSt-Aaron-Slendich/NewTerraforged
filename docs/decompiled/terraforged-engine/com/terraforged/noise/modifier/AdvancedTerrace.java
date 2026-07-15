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

public class AdvancedTerrace
extends Modifier {
    private final int steps;
    private final int octaves;
    private final float modRange;
    private final float blendMin;
    private final float blendMax;
    private final float blendRange;
    private final Module slope;
    private final Module mask;
    private final Module modulation;
    private static final DataFactory<AdvancedTerrace> factory = (data, spec, context) -> new AdvancedTerrace(spec.get("source", data, Module.class, context), spec.get("modulation", data, Module.class, context), spec.get("mask", data, Module.class, context), spec.get("slope", data, Module.class, context), spec.get("blend_min", data, DataValue::asFloat).floatValue(), spec.get("blend_max", data, DataValue::asFloat).floatValue(), spec.get("steps", data, DataValue::asInt), spec.get("octaves", data, DataValue::asInt));

    public AdvancedTerrace(Module source, Module modulation, Module mask, Module slope, float blendMin, float blendMax, int steps, int octaves) {
        super(source);
        this.mask = mask;
        this.steps = steps;
        this.octaves = octaves;
        this.slope = slope;
        this.modulation = modulation;
        this.blendMin = blendMin;
        this.blendMax = blendMax;
        this.blendRange = this.blendMax - this.blendMin;
        this.modRange = source.maxValue() + modulation.maxValue();
    }

    @Override
    public String getSpecName() {
        return "AdvTerrace";
    }

    @Override
    public float modify(float x, float y, float value) {
        if (value <= this.blendMin) {
            return value;
        }
        float mask = this.mask.getValue(x, y);
        if (mask == 0.0f) {
            return value;
        }
        float result = value;
        float slope = this.slope.getValue(x, y);
        float modulation = this.modulation.getValue(x, y);
        for (int i = 1; i <= this.octaves; ++i) {
            result = this.getStepped(result, this.steps * i);
            result = this.getSloped(value, result, slope);
        }
        result = this.getModulated(result, modulation);
        float alpha = this.getAlpha(value);
        if (mask != 1.0f) {
            alpha *= mask;
        }
        return NoiseUtil.lerp(value, result, alpha);
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
        AdvancedTerrace that = (AdvancedTerrace)o;
        if (this.steps != that.steps) {
            return false;
        }
        if (this.octaves != that.octaves) {
            return false;
        }
        if (Float.compare(that.modRange, this.modRange) != 0) {
            return false;
        }
        if (Float.compare(that.blendMin, this.blendMin) != 0) {
            return false;
        }
        if (Float.compare(that.blendMax, this.blendMax) != 0) {
            return false;
        }
        if (Float.compare(that.blendRange, this.blendRange) != 0) {
            return false;
        }
        if (!this.slope.equals(that.slope)) {
            return false;
        }
        if (!this.mask.equals(that.mask)) {
            return false;
        }
        return this.modulation.equals(that.modulation);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.steps;
        result = 31 * result + this.octaves;
        result = 31 * result + (this.modRange != 0.0f ? Float.floatToIntBits(this.modRange) : 0);
        result = 31 * result + (this.blendMin != 0.0f ? Float.floatToIntBits(this.blendMin) : 0);
        result = 31 * result + (this.blendMax != 0.0f ? Float.floatToIntBits(this.blendMax) : 0);
        result = 31 * result + (this.blendRange != 0.0f ? Float.floatToIntBits(this.blendRange) : 0);
        result = 31 * result + this.slope.hashCode();
        result = 31 * result + this.mask.hashCode();
        result = 31 * result + this.modulation.hashCode();
        return result;
    }

    private float getModulated(float value, float modulation) {
        return (value + modulation) / this.modRange;
    }

    private float getStepped(float value, int steps) {
        value = NoiseUtil.round(value * (float)steps);
        return value / (float)steps;
    }

    private float getSloped(float value, float stepped, float slope) {
        float delta = value - stepped;
        float amount = delta * slope;
        return stepped + amount;
    }

    private float getAlpha(float value) {
        if (value > this.blendMax) {
            return 1.0f;
        }
        return (value - this.blendMin) / this.blendRange;
    }

    public static DataSpec<AdvancedTerrace> spec() {
        return DataSpec.builder("AdvTerrace", AdvancedTerrace.class, factory).add("steps", (Object)Float.valueOf(1.0f), a -> a.steps).add("octaves", (Object)Float.valueOf(1.0f), a -> a.octaves).add("blend_min", (Object)Float.valueOf(0.0f), a -> Float.valueOf(a.blendMin)).add("blend_max", (Object)Float.valueOf(1.0f), a -> Float.valueOf(a.blendMax)).addObj("source", Module.class, a -> a.source).addObj("modulation", Module.class, a -> a.modulation).addObj("slope", Module.class, a -> a.slope).addObj("mask", Module.class, a -> a.mask).build();
    }
}

