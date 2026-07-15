/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class VariableCurve
extends Modifier {
    private final Module midpoint;
    private final Module gradient;
    private static final DataFactory<VariableCurve> factory = (data, spec, context) -> new VariableCurve(spec.get("source", data, Module.class, context), spec.get("midpoint", data, Module.class, context), spec.get("gradient", data, Module.class, context));

    public VariableCurve(Module source, Module mid, Module gradient) {
        super(source);
        this.midpoint = mid;
        this.gradient = gradient;
    }

    @Override
    public String getSpecName() {
        return "VariCurve";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float mid = this.midpoint.getValue(x, y);
        float curve = this.gradient.getValue(x, y);
        return NoiseUtil.curve(noiseValue, mid, curve);
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
        VariableCurve that = (VariableCurve)o;
        if (!this.midpoint.equals(that.midpoint)) {
            return false;
        }
        return this.gradient.equals(that.gradient);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.midpoint.hashCode();
        result = 31 * result + this.gradient.hashCode();
        return result;
    }

    public static DataSpec<VariableCurve> spec() {
        return Modifier.sourceBuilder("VariCurve", VariableCurve.class, factory).addObj("midpoint", Module.class, v -> v.midpoint).addObj("gradient", Module.class, v -> v.gradient).build();
    }
}

