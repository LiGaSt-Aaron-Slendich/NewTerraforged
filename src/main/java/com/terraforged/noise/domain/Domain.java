package com.terraforged.noise.domain;

import com.terraforged.cereal.spec.SpecName;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;

/**
 * Compatibility Domain with seed-parameterized getX/getY for TerraForged 0.3.x.
 * Implementations still live in the Engine jar (AddWarp, DomainWarp, …).
 */
public interface Domain extends SpecName {
    Domain DIRECT = new Domain() {
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

    float getOffsetX(float x, float y);

    float getOffsetY(float x, float y);

    default float getX(float x, float y) {
        return x + getOffsetX(x, y);
    }

    default float getY(float x, float y) {
        return y + getOffsetY(x, y);
    }

    default float getX(int seed, float x, float y) {
        return getX(x, y);
    }

    default float getY(int seed, float x, float y) {
        return getY(x, y);
    }

    default float getOffsetX(int seed, float x, float y) {
        return getOffsetX(x, y);
    }

    default float getOffsetY(int seed, float x, float y) {
        return getOffsetY(x, y);
    }

    default Domain cache() {
        return new CacheWarp(this);
    }

    default Domain add(Domain next) {
        return new AddWarp(this, next);
    }

    default Domain warp(Domain next) {
        return new CompoundWarp(this, next);
    }

    default Domain then(Domain next) {
        return new CumulativeWarp(this, next);
    }

    static Domain warp(Module x, Module y, Module distance) {
        return new DomainWarp(x, y, distance);
    }

    static Domain warp(int seed, int scale, int octaves, double strength) {
        return warp(Source.PERLIN, seed, scale, octaves, strength);
    }

    static Domain warp(Source type, int seed, int scale, int octaves, double strength) {
        return warp(Source.build(seed, scale, octaves).build(type), Source.build(seed + 1, scale, octaves).build(type), Source.constant(strength));
    }

    static Domain direction(Module direction, Module distance) {
        return new DirectionWarp(direction, distance);
    }

    static Domain direction(int seed, int scale, int octaves, double strength) {
        return direction(Source.PERLIN, seed, scale, octaves, strength);
    }

    static Domain direction(Source type, int seed, int scale, int octaves, double strength) {
        return direction(Source.build(seed, scale, octaves).build(type), Source.constant(strength));
    }
}
