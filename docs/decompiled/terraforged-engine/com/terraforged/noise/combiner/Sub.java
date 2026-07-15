/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.combiner;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.combiner.Combiner;

public class Sub
extends Combiner {
    public Sub(Module ... modules) {
        super(modules);
    }

    @Override
    public String getSpecName() {
        return "Sub";
    }

    @Override
    protected float minTotal(float total, Module next) {
        return total - next.maxValue();
    }

    @Override
    protected float maxTotal(float total, Module next) {
        return total - next.minValue();
    }

    @Override
    protected float combine(float total, float value) {
        return total - value;
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
        return Sub.spec("Sub", Sub::new);
    }
}

