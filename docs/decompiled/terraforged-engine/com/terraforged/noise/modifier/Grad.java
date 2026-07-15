/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class Grad
extends Modifier {
    private final Module lower;
    private final Module upper;
    private final Module strength;
    private static DataFactory<Grad> factory = (data, spec, context) -> new Grad(spec.get("source", data, Module.class, context), spec.get("lower", data, Module.class, context), spec.get("upper", data, Module.class, context), spec.get("strength", data, Module.class, context));

    public Grad(Module source, Module lower, Module upper, Module strength) {
        super(source);
        this.lower = lower;
        this.upper = upper;
        this.strength = strength;
    }

    @Override
    public String getSpecName() {
        return "Grad";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float upperBound = this.upper.getValue(x, y);
        if (noiseValue > upperBound) {
            return noiseValue;
        }
        float amount = this.strength.getValue(x, y);
        float lowerBound = this.lower.getValue(x, y);
        if (noiseValue < lowerBound) {
            return NoiseUtil.pow(noiseValue, 1.0f - amount);
        }
        float alpha = 1.0f - (noiseValue - lowerBound) / (upperBound - lowerBound);
        float power = 1.0f - amount * alpha;
        return NoiseUtil.pow(noiseValue, power);
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
        Grad grad = (Grad)o;
        if (!this.lower.equals(grad.lower)) {
            return false;
        }
        if (!this.upper.equals(grad.upper)) {
            return false;
        }
        return this.strength.equals(grad.strength);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.lower.hashCode();
        result = 31 * result + this.upper.hashCode();
        result = 31 * result + this.strength.hashCode();
        return result;
    }

    public static DataSpec<Grad> spec() {
        return Modifier.sourceBuilder(Grad.class, factory).addObj("lower", Module.class, g -> g.lower).addObj("upper", Module.class, g -> g.upper).addObj("strength", Module.class, g -> g.strength).build();
    }
}

