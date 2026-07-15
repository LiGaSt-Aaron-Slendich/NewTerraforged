/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.combiner;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.combiner.Combiner;

public class Min
extends Combiner {
    public Min(Module ... modules) {
        super(modules);
    }

    @Override
    public String getSpecName() {
        return "Min";
    }

    @Override
    protected float minTotal(float total, Module next) {
        return Math.min(total, next.minValue());
    }

    @Override
    protected float maxTotal(float total, Module next) {
        return this.minTotal(total, next);
    }

    @Override
    protected float combine(float total, float value) {
        return Math.min(total, value);
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
        return Min.spec("Min", Min::new);
    }
}

