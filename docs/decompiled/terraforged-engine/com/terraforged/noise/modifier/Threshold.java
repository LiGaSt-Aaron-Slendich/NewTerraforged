/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Threshold
extends Modifier {
    private final Module threshold;
    private static final DataFactory<Threshold> factory = (data, spec, context) -> new Threshold(spec.get("source", data, Module.class, context), spec.get("threshold", data, Module.class, context));

    public Threshold(Module source, Module threshold) {
        super(source);
        this.threshold = threshold;
    }

    @Override
    public String getSpecName() {
        return "Threshold";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float limit = this.threshold.getValue(x, y);
        if (noiseValue < limit) {
            return 0.0f;
        }
        return 1.0f;
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
        Threshold threshold1 = (Threshold)o;
        return this.threshold.equals(threshold1.threshold);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.threshold.hashCode();
        return result;
    }

    public static DataSpec<Threshold> spec() {
        return Modifier.sourceBuilder(Threshold.class, factory).addObj("threshold", Module.class, t -> t.threshold).build();
    }
}

