/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.modifier.Modifier;

public class Bias
extends Modifier {
    private final Module bias;
    private static final DataFactory<Bias> factory = (data, spec, context) -> new Bias(spec.get("source", data, Module.class, context), spec.get("bias", data, DataValue::asFloat).floatValue());

    public Bias(Module source, float bias) {
        this(source, Source.constant(bias));
    }

    public Bias(Module source, Module bias) {
        super(source);
        this.bias = bias;
    }

    @Override
    public String getSpecName() {
        return "Bias";
    }

    @Override
    public float minValue() {
        return super.minValue() + this.bias.minValue();
    }

    @Override
    public float maxValue() {
        return super.maxValue() + this.bias.maxValue();
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return noiseValue + this.bias.getValue(x, y);
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
        Bias bias1 = (Bias)o;
        return this.bias.equals(bias1.bias);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.bias.hashCode();
        return result;
    }

    public static DataSpec<Bias> spec() {
        return Modifier.sourceBuilder(Bias.class, factory).addObj("bias", Module.class, b -> b.bias).build();
    }
}

