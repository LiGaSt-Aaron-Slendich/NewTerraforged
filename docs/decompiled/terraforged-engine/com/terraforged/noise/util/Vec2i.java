/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.util;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.spec.SpecName;
import com.terraforged.cereal.value.DataValue;
import java.util.Objects;

public class Vec2i
implements SpecName {
    public final int x;
    public final int y;
    private static final DataFactory<Vec2i> factory = (data, spec, context) -> new Vec2i(spec.get("x", data, DataValue::asInt), spec.get("y", data, DataValue::asInt));

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public float dist2(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;
        return dx * dx + dy * dy;
    }

    @Override
    public String getSpecName() {
        return "Vec2i";
    }

    public String toString() {
        return "Vec2i{x=" + this.x + ", y=" + this.y + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Vec2i vec2i = (Vec2i)o;
        return this.x == vec2i.x && this.y == vec2i.y;
    }

    public int hashCode() {
        return Objects.hash(this.x, this.y);
    }

    public static DataSpec<Vec2i> spec() {
        return DataSpec.builder(Vec2i.class, factory).add("x", (Object)0, v -> v.x).add("y", (Object)0, v -> v.y).build();
    }
}

