/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.source.Builder;

public class Constant
implements Module {
    private final float value;
    private static final DataFactory<Constant> factory = (data, spec, context) -> new Constant(new Builder().frequency(spec.get("value", data, DataValue::asDouble)));

    public Constant(Builder builder) {
        this.value = builder.getFrequency();
    }

    public Constant(float value) {
        this.value = value;
    }

    @Override
    public String getSpecName() {
        return "Const";
    }

    @Override
    public float getValue(float x, float y) {
        return this.value;
    }

    @Override
    public float minValue() {
        return this.value;
    }

    @Override
    public float maxValue() {
        return this.value;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Constant constant = (Constant)o;
        return Float.compare(constant.value, this.value) == 0;
    }

    public int hashCode() {
        return this.value != 0.0f ? Float.floatToIntBits(this.value) : 0;
    }

    public static DataSpec<Constant> spec() {
        return DataSpec.builder("Const", Constant.class, factory).add("value", (Object)Float.valueOf(1.0f), c -> Float.valueOf(c.value)).build();
    }
}

