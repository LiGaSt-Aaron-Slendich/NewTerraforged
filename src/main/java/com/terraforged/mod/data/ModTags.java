package com.terraforged.mod.data;

import com.terraforged.mod.registry.lazy.LazyTag;
import net.minecraft.world.level.biome.Biome;

public interface ModTags {
   LazyTag<Biome> OVERWORLD = LazyTag.biome("overworld");
   LazyTag<Biome> COPSES = LazyTag.biome("trees/copses");
   LazyTag<Biome> HARDY = LazyTag.biome("trees/hardy");
   LazyTag<Biome> HARDY_SLOPES = LazyTag.biome("trees/hardy_slopes");
   LazyTag<Biome> PATCHY = LazyTag.biome("trees/patchy");
   LazyTag<Biome> RAINFOREST = LazyTag.biome("trees/rainforest");
   LazyTag<Biome> SPARSE = LazyTag.biome("trees/sparse");
   LazyTag<Biome> SPARSE_RAINFOREST = LazyTag.biome("trees/sparse_rainforest");
   LazyTag<Biome> TEMPERATE = LazyTag.biome("trees/temperate");
}
