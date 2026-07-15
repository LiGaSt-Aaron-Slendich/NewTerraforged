/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.Module;
import com.terraforged.noise.domain.Domain;

public class DomainWarp
implements Domain {
    private final Module x;
    private final Module y;
    private final Module distance;

    public DomainWarp(Module x, Module y, Module distance) {
        this.x = DomainWarp.map(x);
        this.y = DomainWarp.map(y);
        this.distance = distance;
    }

    @Override
    public String getSpecName() {
        return "DomainWarp";
    }

    @Override
    public float getOffsetX(float x, float y) {
        return this.x.getValue(x, y) * this.distance.getValue(x, y);
    }

    @Override
    public float getOffsetY(float x, float y) {
        return this.y.getValue(x, y) * this.distance.getValue(x, y);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        DomainWarp that = (DomainWarp)o;
        if (!this.x.equals(that.x)) {
            return false;
        }
        if (!this.y.equals(that.y)) {
            return false;
        }
        return this.distance.equals(that.distance);
    }

    public int hashCode() {
        int result = this.x.hashCode();
        result = 31 * result + this.y.hashCode();
        result = 31 * result + this.distance.hashCode();
        return result;
    }

    private static Module map(Module in) {
        if (in.minValue() == -0.5f && in.maxValue() == 0.5f) {
            return in;
        }
        return in.map(-0.5, 0.5);
    }

    private static DomainWarp create(DataObject data, DataSpec<?> spec, Context context) {
        return new DomainWarp(spec.get("x", data, Module.class, context), spec.get("y", data, Module.class, context), spec.get("distance", data, Module.class, context));
    }

    public static DataSpec<? extends Domain> spec() {
        return DataSpec.builder("DomainWarp", DomainWarp.class, DomainWarp::create).addObj("x", Module.class, w -> w.x).addObj("y", Module.class, w -> w.y).addObj("distance", Module.class, w -> w.distance).build();
    }
}

