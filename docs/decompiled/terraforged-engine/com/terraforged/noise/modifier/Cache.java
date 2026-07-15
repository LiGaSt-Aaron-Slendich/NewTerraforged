/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Cache
extends Modifier {
    private final Value value = new Value();

    public Cache(Module source) {
        super(source);
    }

    @Override
    public String getSpecName() {
        return "Cache";
    }

    @Override
    public float minValue() {
        return this.source.minValue();
    }

    @Override
    public float maxValue() {
        return this.source.maxValue();
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return 0.0f;
    }

    @Override
    public float getValue(float x, float y) {
        Value value = this.value;
        if (value.matches(x, y)) {
            return value.value;
        }
        return value.set(x, y, this.source.getValue(x, y));
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
        Cache cache = (Cache)o;
        return this.value.equals(cache.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.value.hashCode();
        return result;
    }

    public static DataSpec<Cache> spec() {
        return Cache.spec(Cache.class, Cache::new);
    }

    private static class Value {
        private float x = 0.0f;
        private float y = 0.0f;
        private float value = 0.0f;
        private boolean empty = true;

        private Value() {
        }

        private boolean matches(float x, float y) {
            return !this.empty && x == this.x && y == this.y;
        }

        private float set(float x, float y, float value) {
            this.x = x;
            this.y = y;
            this.value = value;
            this.empty = false;
            return value;
        }
    }
}

