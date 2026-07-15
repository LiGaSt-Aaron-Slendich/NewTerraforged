/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.combiner;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.combiner.Combiner;

public class Max
extends Combiner {
    public Max(Module ... modules) {
        super(modules);
    }

    @Override
    public String getSpecName() {
        return "Max";
    }

    @Override
    protected float minTotal(float total, Module next) {
        return this.maxTotal(total, next);
    }

    @Override
    protected float maxTotal(float total, Module next) {
        return Math.max(total, next.maxValue());
    }

    @Override
    protected float combine(float total, float value) {
        return Math.max(total, value);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static DataSpec<?> spec() {
        return Max.spec("Max", Max::new);
    }
}

