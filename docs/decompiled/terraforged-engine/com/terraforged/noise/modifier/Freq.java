/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Freq
extends Modifier {
    private final Module x;
    private final Module y;
    private static final DataFactory<Freq> factory = (data, spec, context) -> new Freq(spec.get("source", data, Module.class, context), spec.get("x", data, Module.class, context), spec.get("y", data, Module.class, context));

    public Freq(Module source, Module x, Module y) {
        super(source);
        this.x = x;
        this.y = y;
    }

    @Override
    public String getSpecName() {
        return "Freq";
    }

    @Override
    public float getValue(float x, float y) {
        float fx = this.x.getValue(x, y);
        float fy = this.y.getValue(x, y);
        return this.source.getValue(x * fx, y * fy);
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return 0.0f;
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
        Freq freq = (Freq)o;
        if (!this.x.equals(freq.x)) {
            return false;
        }
        return this.y.equals(freq.y);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.x.hashCode();
        result = 31 * result + this.y.hashCode();
        return result;
    }

    public static DataSpec<Freq> spec() {
        return Freq.sourceBuilder(Freq.class, factory).addObj("x", Module.class, f -> f.x).addObj("y", Module.class, f -> f.y).build();
    }
}

