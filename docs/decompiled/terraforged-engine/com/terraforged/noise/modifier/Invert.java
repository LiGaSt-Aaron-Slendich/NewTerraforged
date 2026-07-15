/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Invert
extends Modifier {
    public Invert(Module source) {
        super(source);
    }

    @Override
    public String getSpecName() {
        return "Invert";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        if (noiseValue > this.source.maxValue()) {
            return this.source.minValue();
        }
        if (noiseValue < this.source.minValue()) {
            return this.source.maxValue();
        }
        return this.source.maxValue() - noiseValue;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static DataSpec<Invert> spec() {
        return Modifier.spec(Invert.class, Invert::new);
    }
}

