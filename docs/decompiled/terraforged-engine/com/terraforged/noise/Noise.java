/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise;

public interface Noise {
    public float getValue(float var1, float var2);

    default public float maxValue() {
        return 1.0f;
    }

    default public float minValue() {
        return 0.0f;
    }
}

