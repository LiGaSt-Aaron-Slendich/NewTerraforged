/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import java.util.function.Function;

public abstract class Modifier
implements Module {
    protected final Module source;

    public Modifier(Module source) {
        this.source = source;
    }

    @Override
    public float getValue(float x, float y) {
        float value = this.source.getValue(x, y);
        return this.modify(x, y, value);
    }

    @Override
    public float minValue() {
        return this.source.minValue();
    }

    @Override
    public float maxValue() {
        return this.source.maxValue();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Modifier modifier = (Modifier)o;
        return this.source.equals(modifier.source);
    }

    public int hashCode() {
        return this.source.hashCode();
    }

    public String toString() {
        return "Modifier{source=" + this.source + '}';
    }

    public abstract float modify(float var1, float var2, float var3);

    protected static <M extends Modifier> DataSpec.Builder<M> specBuilder(Class<M> type, DataFactory<M> factory) {
        return DataSpec.builder(type.getSimpleName(), type, factory);
    }

    protected static <M extends Modifier> DataSpec.Builder<M> sourceBuilder(Class<M> type, DataFactory<M> factory) {
        return Modifier.sourceBuilder(type.getSimpleName(), type, factory);
    }

    protected static <M extends Modifier> DataSpec.Builder<M> sourceBuilder(String name, Class<M> type, DataFactory<M> factory) {
        return DataSpec.builder(name, type, factory).addObj("source", Module.class, m -> m.source);
    }

    public static <M extends Modifier> DataSpec<M> spec(Class<M> type, Function<Module, M> constructor) {
        DataFactory<Modifier> factory = (data, spec, context) -> (Modifier)constructor.apply(spec.get("source", data, Module.class, context));
        return Modifier.sourceBuilder(type, factory).build();
    }
}

