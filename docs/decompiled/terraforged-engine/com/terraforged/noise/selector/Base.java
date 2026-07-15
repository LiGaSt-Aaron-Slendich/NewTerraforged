/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.selector;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.selector.Selector;

public class Base
extends Selector {
    private final Module base;
    protected final float min;
    protected final float max;
    protected final float maxValue;
    protected final float falloff;
    private static final DataFactory<Base> factory = (data, spec, context) -> new Base(spec.get("base", data, Module.class, context), spec.get("control", data, Module.class, context), spec.get("falloff", data, DataValue::asFloat).floatValue(), spec.get("interpolation", data, v -> v.asEnum(Interpolation.class)));

    public Base(Module base, Module source, float falloff, Interpolation interpolation) {
        super(source, new Module[]{base, source}, interpolation);
        this.base = base;
        this.min = base.maxValue();
        this.max = base.maxValue() + falloff;
        this.falloff = falloff;
        this.maxValue = Math.max(base.maxValue(), source.maxValue());
    }

    @Override
    public String getSpecName() {
        return "Base";
    }

    @Override
    protected float selectValue(float x, float y, float upperValue) {
        if (upperValue < this.max) {
            float lowerValue = this.base.getValue(x, y);
            if (this.falloff > 0.0f) {
                float clamp = Math.max(this.min, upperValue);
                float alpha = (this.max - clamp) / this.falloff;
                return this.blendValues(upperValue, lowerValue, alpha);
            }
            return lowerValue;
        }
        return upperValue;
    }

    @Override
    public float minValue() {
        return this.base.minValue();
    }

    @Override
    public float maxValue() {
        return this.maxValue;
    }

    public static DataSpec<Base> spec() {
        return DataSpec.builder("Base", Base.class, factory).add("falloff", (Object)0, b -> Float.valueOf(b.falloff)).add("interpolation", (Object)Interpolation.LINEAR, b -> b.interpolation).addObj("control", Module.class, b -> b.selector).addObj("base", Module.class, b -> b.base).build();
    }
}

