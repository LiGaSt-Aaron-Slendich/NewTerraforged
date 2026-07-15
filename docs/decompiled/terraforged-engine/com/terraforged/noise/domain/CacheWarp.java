/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.domain.Domain;

public class CacheWarp
implements Domain {
    private final Domain domain;
    private boolean cached = false;
    private float cachedX;
    private float cachedY;
    private float x;
    private float y;

    public CacheWarp(Domain domain) {
        this.domain = domain;
    }

    @Override
    public String getSpecName() {
        return "CacheWarp";
    }

    @Override
    public float getOffsetX(float x, float y) {
        if (this.cached && x == this.x && y == this.y) {
            return this.cachedX;
        }
        this.x = x;
        this.y = y;
        this.cachedX = this.domain.getOffsetX(x, y);
        return this.cachedX;
    }

    @Override
    public float getOffsetY(float x, float y) {
        if (this.cached && x == this.x && y == this.y) {
            return this.cachedY;
        }
        this.x = x;
        this.y = y;
        this.cachedY = this.domain.getOffsetY(x, y);
        return this.cachedY;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CacheWarp cacheWarp = (CacheWarp)o;
        if (this.cached != cacheWarp.cached) {
            return false;
        }
        if (Float.compare(cacheWarp.cachedX, this.cachedX) != 0) {
            return false;
        }
        if (Float.compare(cacheWarp.cachedY, this.cachedY) != 0) {
            return false;
        }
        if (Float.compare(cacheWarp.x, this.x) != 0) {
            return false;
        }
        if (Float.compare(cacheWarp.y, this.y) != 0) {
            return false;
        }
        return this.domain.equals(cacheWarp.domain);
    }

    public int hashCode() {
        int result = this.domain.hashCode();
        result = 31 * result + (this.cached ? 1 : 0);
        result = 31 * result + (this.cachedX != 0.0f ? Float.floatToIntBits(this.cachedX) : 0);
        result = 31 * result + (this.cachedY != 0.0f ? Float.floatToIntBits(this.cachedY) : 0);
        result = 31 * result + (this.x != 0.0f ? Float.floatToIntBits(this.x) : 0);
        result = 31 * result + (this.y != 0.0f ? Float.floatToIntBits(this.y) : 0);
        return result;
    }

    private static CacheWarp create(DataObject data, DataSpec<?> spec, Context context) {
        return new CacheWarp(spec.get("domain", data, Domain.class, context));
    }

    public static DataSpec<? extends Domain> spec() {
        return DataSpec.builder("CacheWarp", CacheWarp.class, CacheWarp::create).addObj("domain", Domain.class, w -> w.domain).build();
    }
}

