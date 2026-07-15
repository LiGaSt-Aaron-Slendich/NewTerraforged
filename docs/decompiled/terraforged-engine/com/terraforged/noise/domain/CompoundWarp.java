/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.domain.Domain;

public class CompoundWarp
implements Domain {
    private final Domain a;
    private final Domain b;

    public CompoundWarp(Domain a, Domain b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String getSpecName() {
        return "CompoundWarp";
    }

    @Override
    public float getOffsetX(float x, float y) {
        float ax = this.a.getX(x, y);
        float ay = this.a.getY(x, y);
        return this.b.getOffsetX(ax, ay);
    }

    @Override
    public float getOffsetY(float x, float y) {
        float ax = this.a.getX(x, y);
        float ay = this.a.getY(x, y);
        return this.b.getOffsetY(ax, ay);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CompoundWarp that = (CompoundWarp)o;
        if (!this.a.equals(that.a)) {
            return false;
        }
        return this.b.equals(that.b);
    }

    public int hashCode() {
        int result = this.a.hashCode();
        result = 31 * result + this.b.hashCode();
        return result;
    }

    private static CompoundWarp create(DataObject data, DataSpec<?> spec, Context context) {
        return new CompoundWarp(spec.get("warp_1", data, Domain.class, context), spec.get("warp_2", data, Domain.class, context));
    }

    public static DataSpec<? extends Domain> spec() {
        return DataSpec.builder("CumulativeWarp", CompoundWarp.class, CompoundWarp::create).addObj("warp_1", Domain.class, w -> w.a).addObj("warp_2", Domain.class, w -> w.b).build();
    }
}

