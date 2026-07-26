package com.terraforged.noise;

/**
 * Compatibility shim for TerraForged 0.3.x (1.19) against older Engine jars.
 * Official Engine 0.3.0 used seed-parameterized Noise; bundled libs use (x,y) only.
 */
public interface Noise {
    float getValue(float x, float y);

    default float getValue(int seed, float x, float y) {
        return getValue(x, y);
    }

    default float maxValue() {
        return 1.0f;
    }

    default float minValue() {
        return 0.0f;
    }
}
