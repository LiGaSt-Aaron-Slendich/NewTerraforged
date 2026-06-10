package com.terraforged.mod.worldgen;

import net.minecraft.world.level.WorldGenLevel;

public interface Seeds {
    public static int get(WorldGenLevel level) {
        return Seeds.get(level.getSeed());
    }

    public static int get(long seed) {
        return (int)seed;
    }
}
