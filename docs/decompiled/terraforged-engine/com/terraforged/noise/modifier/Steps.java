/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class Steps
extends Modifier {
    private final Module steps;
    private final Module slopeMin;
    private final Module slopeMax;
    private final CurveFunc curve;
    private static final DataFactory<Steps> factory = (data, spec, context) -> new Steps(spec.get("source", data, Module.class, context), spec.get("steps", data, Module.class, context), spec.get("slope_min", data, Module.class, context), spec.get("slope_max", data, Module.class, context), spec.get("curve", data, CurveFunc.class, context));

    public Steps(Module source, Module steps, Module slopeMin, Module slopeMax, CurveFunc slopeCurve) {
        super(source);
        this.steps = steps;
        this.curve = slopeCurve;
        this.slopeMin = slopeMin;
        this.slopeMax = slopeMax;
    }

    @Override
    public String getSpecName() {
        return "Steps";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float min = this.slopeMin.getValue(x, y);
        float max = this.slopeMax.getValue(x, y);
        float stepCount = this.steps.getValue(x, y);
        float range = max - min;
        if (range <= 0.0f) {
            return (float)((int)(noiseValue * stepCount)) / stepCount;
        }
        noiseValue = 1.0f - noiseValue;
        float value = (float)((int)(noiseValue * stepCount)) / stepCount;
        float delta = noiseValue - value;
        float alpha = NoiseUtil.map(delta * stepCount, min, max, range);
        return 1.0f - NoiseUtil.lerp(value, noiseValue, this.curve.apply(alpha));
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
        Steps steps1 = (Steps)o;
        if (!this.steps.equals(steps1.steps)) {
            return false;
        }
        if (!this.slopeMin.equals(steps1.slopeMin)) {
            return false;
        }
        if (!this.slopeMax.equals(steps1.slopeMax)) {
            return false;
        }
        return this.curve.equals(steps1.curve);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.steps.hashCode();
        result = 31 * result + this.slopeMin.hashCode();
        result = 31 * result + this.slopeMax.hashCode();
        result = 31 * result + this.curve.hashCode();
        return result;
    }

    public static DataSpec<Steps> spec() {
        return Steps.specBuilder(Steps.class, factory).addObj("curve", CurveFunc.class, s -> s.curve).addObj("source", Module.class, s -> s.source).addObj("steps", Module.class, s -> s.steps).addObj("slope_min", Module.class, s -> s.slopeMin).addObj("slope_max", Module.class, s -> s.slopeMax).build();
    }
}

