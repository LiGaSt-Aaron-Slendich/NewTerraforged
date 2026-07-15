/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Abs
extends Modifier {
    public Abs(Module source) {
        super(source);
    }

    @Override
    public String getSpecName() {
        return "Abs";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return Math.abs(noiseValue);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static DataSpec<Abs> spec() {
        return Modifier.spec(Abs.class, Abs::new);
    }
}

