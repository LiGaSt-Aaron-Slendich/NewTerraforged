/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.serialization.io.pre;

import com.terraforged.engine.settings.Settings;

public class PreConfig {
    private static final int GEN_FACTOR = 3;
    private static final int TILE_SIZE_BLOCKS = 128;

    public static void create(int blockRadius, Settings settings) {
        int tiles = blockRadius / 128 + 1;
        System.out.println(tiles);
    }

    public static void main(String[] args) {
        PreConfig.create(4000, null);
    }
}

