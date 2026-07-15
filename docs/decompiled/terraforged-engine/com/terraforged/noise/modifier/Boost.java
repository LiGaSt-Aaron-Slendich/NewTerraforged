/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class Boost
extends Modifier {
    private final int iterations;
    private static final DataFactory<Boost> factory = (data, spec, context) -> new Boost(spec.get("source", data, Module.class, context), spec.get("iterations", data, DataValue::asInt));

    public Boost(Module source, int iterations) {
        super(source.map(0.0, 1.0));
        this.iterations = Math.max(1, iterations);
    }

    @Override
    public String getSpecName() {
        return "Boost";
    }

    @Override
    public float modify(float x, float y, float value) {
        for (int i = 0; i < this.iterations; ++i) {
            value = NoiseUtil.pow(value, 1.0f - value);
        }
        return value;
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
        Boost boost = (Boost)o;
        return this.iterations == boost.iterations;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.iterations;
        return result;
    }

    public static DataSpec<Boost> spec() {
        return Modifier.specBuilder(Boost.class, factory).add("iterations", (Object)1, b -> b.iterations).addObj("source", Module.class, b -> b.source).build();
    }
}

