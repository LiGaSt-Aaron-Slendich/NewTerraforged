/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.SpecName;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.AddWarp;
import com.terraforged.noise.domain.CacheWarp;
import com.terraforged.noise.domain.CompoundWarp;
import com.terraforged.noise.domain.CumulativeWarp;
import com.terraforged.noise.domain.DirectionWarp;
import com.terraforged.noise.domain.DomainWarp;

public interface Domain
extends SpecName {
    public static final Domain DIRECT = new Domain(){

        @Override
        public String getSpecName() {
            return "Direct";
        }

        @Override
        public float getOffsetX(float x, float y) {
            return 0.0f;
        }

        @Override
        public float getOffsetY(float x, float y) {
            return 0.0f;
        }
    };

    public float getOffsetX(float var1, float var2);

    public float getOffsetY(float var1, float var2);

    default public float getX(float x, float y) {
        return x + this.getOffsetX(x, y);
    }

    default public float getY(float x, float y) {
        return y + this.getOffsetY(x, y);
    }

    default public Domain cache() {
        return new CacheWarp(this);
    }

    default public Domain add(Domain next) {
        return new AddWarp(this, next);
    }

    default public Domain warp(Domain next) {
        return new CompoundWarp(this, next);
    }

    default public Domain then(Domain next) {
        return new CumulativeWarp(this, next);
    }

    public static Domain warp(Module x, Module y, Module distance) {
        return new DomainWarp(x, y, distance);
    }

    public static Domain warp(int seed, int scale, int octaves, double strength) {
        return Domain.warp(Source.PERLIN, seed, scale, octaves, strength);
    }

    public static Domain warp(Source type, int seed, int scale, int octaves, double strength) {
        return Domain.warp(Source.build(seed, scale, octaves).build(type), Source.build(seed + 1, scale, octaves).build(type), Source.constant(strength));
    }

    public static Domain direction(Module direction, Module distance) {
        return new DirectionWarp(direction, distance);
    }

    public static Domain direction(int seed, int scale, int octaves, double strength) {
        return Domain.direction(Source.PERLIN, seed, scale, octaves, strength);
    }

    public static Domain direction(Source type, int seed, int scale, int octaves, double strength) {
        return Domain.direction(Source.build(seed, scale, octaves).build(type), Source.constant(strength));
    }
}

