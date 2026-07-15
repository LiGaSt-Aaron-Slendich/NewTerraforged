/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.Module;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class DirectionWarp
implements Domain {
    private final Module direction;
    private final Module strength;

    public DirectionWarp(Module direction, Module strength) {
        this.direction = direction;
        this.strength = strength;
    }

    @Override
    public String getSpecName() {
        return "DirectionWarp";
    }

    @Override
    public float getOffsetX(float x, float y) {
        float angle = this.direction.getValue(x, y) * ((float)Math.PI * 2);
        return NoiseUtil.sin(angle) * this.strength.getValue(x, y);
    }

    @Override
    public float getOffsetY(float x, float y) {
        float angle = this.direction.getValue(x, y) * ((float)Math.PI * 2);
        return NoiseUtil.cos(angle) * this.strength.getValue(x, y);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        DirectionWarp that = (DirectionWarp)o;
        if (!this.direction.equals(that.direction)) {
            return false;
        }
        return this.strength.equals(that.strength);
    }

    public int hashCode() {
        int result = this.direction.hashCode();
        result = 31 * result + this.strength.hashCode();
        return result;
    }

    private static DirectionWarp create(DataObject data, DataSpec<?> spec, Context context) {
        return new DirectionWarp(spec.get("direction", data, Module.class, context), spec.get("strength", data, Module.class, context));
    }

    public static DataSpec<? extends Domain> spec() {
        return DataSpec.builder("DirectionWarp", DirectionWarp.class, DirectionWarp::create).addObj("direction", Module.class, w -> w.direction).addObj("strength", Module.class, w -> w.strength).build();
    }
}

