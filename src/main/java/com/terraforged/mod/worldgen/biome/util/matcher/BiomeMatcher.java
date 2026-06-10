package com.terraforged.mod.worldgen.biome.util.matcher;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface BiomeMatcher {
    public static final BiomeMatcher DEFAULT = key -> false;

    public boolean test(Holder<Biome> var1);
}
