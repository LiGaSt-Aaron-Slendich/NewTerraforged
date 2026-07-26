package com.terraforged.mod.worldgen.biome.util.matcher;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface BiomeMatcher {
   BiomeMatcher DEFAULT = key -> false;

   boolean test(Holder<Biome> var1);
}
