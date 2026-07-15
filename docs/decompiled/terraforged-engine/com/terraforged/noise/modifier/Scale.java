/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Scale
extends Modifier {
    private final Module scale;
    private final float min;
    private final float max;
    private static final DataFactory<Scale> factory = (data, spec, context) -> new Scale(spec.get("source", data, Module.class, context), spec.get("scale", data, Module.class, context));

    public Scale(Module source, Module scale) {
        super(source);
        this.scale = scale;
        this.min = source.minValue() * scale.minValue();
        this.max = source.maxValue() * scale.maxValue();
    }

    @Override
    public String getSpecName() {
        return "Scale";
    }

    @Override
    public float minValue() {
        return this.min;
    }

    @Override
    public float maxValue() {
        return this.max;
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return noiseValue * this.scale.getValue(x, y);
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
        Scale scale1 = (Scale)o;
        if (Float.compare(scale1.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(scale1.max, this.max) != 0) {
            return false;
        }
        return this.scale.equals(scale1.scale);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.scale.hashCode();
        result = 31 * result + (this.min != 0.0f ? Float.floatToIntBits(this.min) : 0);
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        return result;
    }

    public static DataSpec<Scale> spec() {
        return Modifier.sourceBuilder(Scale.class, factory).addObj("scale", Module.class, s -> s.scale).build();
    }
}

