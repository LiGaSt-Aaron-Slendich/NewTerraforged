/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.source.Builder;
import com.terraforged.noise.source.RidgeNoise;

public class BillowNoise
extends RidgeNoise {
    public BillowNoise(Builder builder) {
        super(builder);
    }

    @Override
    public String getSpecName() {
        return "Billow";
    }

    @Override
    public float getValue(float x, float y, int seed) {
        return 1.0f - super.getValue(x, y, seed);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static DataSpec<BillowNoise> billowSpec() {
        return BillowNoise.specBuilder("Billow", BillowNoise.class, BillowNoise::new).build();
    }
}

