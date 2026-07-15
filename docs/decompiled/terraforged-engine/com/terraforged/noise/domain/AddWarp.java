/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.domain.Domain;

public class AddWarp
implements Domain {
    private final Domain a;
    private final Domain b;

    public AddWarp(Domain a, Domain b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String getSpecName() {
        return "AddWarp";
    }

    @Override
    public float getOffsetX(float x, float y) {
        return this.a.getOffsetX(x, y) + this.b.getOffsetX(x, y);
    }

    @Override
    public float getOffsetY(float x, float y) {
        return this.a.getOffsetY(x, y) + this.b.getOffsetY(x, y);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AddWarp addWarp = (AddWarp)o;
        if (!this.a.equals(addWarp.a)) {
            return false;
        }
        return this.b.equals(addWarp.b);
    }

    public int hashCode() {
        int result = this.a.hashCode();
        result = 31 * result + this.b.hashCode();
        return result;
    }

    private static AddWarp create(DataObject data, DataSpec<?> spec, Context context) {
        return new AddWarp(spec.get("warp_1", data, Domain.class, context), spec.get("warp_2", data, Domain.class, context));
    }

    public static DataSpec<? extends Domain> spec() {
        return DataSpec.builder("CumulativeWarp", AddWarp.class, AddWarp::create).addObj("warp_1", Domain.class, w -> w.a).addObj("warp_2", Domain.class, w -> w.b).build();
    }
}

