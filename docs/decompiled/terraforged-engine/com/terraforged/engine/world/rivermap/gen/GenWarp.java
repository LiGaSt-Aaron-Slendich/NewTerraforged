/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.rivermap.gen;

import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class GenWarp {
    public static final GenWarp EMPTY = new GenWarp();
    private static final int MEGA_WARP = 1500;
    public final Domain lake;
    public final Domain river;

    private GenWarp() {
        this.lake = Domain.DIRECT;
        this.river = Domain.DIRECT;
    }

    public GenWarp(int seed, int continentScale) {
        this.lake = Domain.warp(++seed, 200, 1, 300.0).add(Domain.warp(++seed, 50, 2, 50.0));
        this.river = Domain.warp(++seed, 95, 1, 25.0).add(Domain.warp(++seed, 16, 1, 5.0));
    }

    private static int scaleMegaWarp(int scale) {
        float scaler = (float)scale / 3000.0f;
        return NoiseUtil.round(scaler * 1500.0f);
    }
}

