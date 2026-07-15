/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Map
extends Modifier {
    private final Module min;
    private final Module max;
    private final float sourceRange;
    private static final DataFactory<Map> factory = (data, spec, context) -> new Map(spec.get("source", data, Module.class, context), spec.get("min", data, Module.class, context), spec.get("max", data, Module.class, context));

    public Map(Module source, Module min, Module max) {
        super(source);
        this.min = min;
        this.max = max;
        this.sourceRange = source.maxValue() - source.minValue();
    }

    @Override
    public String getSpecName() {
        return "Map";
    }

    @Override
    public float minValue() {
        return this.min.minValue();
    }

    @Override
    public float maxValue() {
        return this.max.maxValue();
    }

    @Override
    public float modify(float x, float y, float value) {
        float alpha = (value - this.source.minValue()) / this.sourceRange;
        float min = this.min.getValue(x, y);
        float max = this.max.getValue(x, y);
        return min + alpha * (max - min);
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
        Map map = (Map)o;
        if (Float.compare(map.sourceRange, this.sourceRange) != 0) {
            return false;
        }
        if (!this.min.equals(map.min)) {
            return false;
        }
        return this.max.equals(map.max);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.min.hashCode();
        result = 31 * result + this.max.hashCode();
        result = 31 * result + (this.sourceRange != 0.0f ? Float.floatToIntBits(this.sourceRange) : 0);
        return result;
    }

    public static DataSpec<Map> spec() {
        return Modifier.sourceBuilder(Map.class, factory).addObj("min", Module.class, m -> m.min).addObj("max", Module.class, m -> m.max).build();
    }
}

