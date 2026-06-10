package com.terraforged.mod.data;

import com.terraforged.mod.registry.lazy.LazyTag;
import net.minecraft.world.level.biome.Biome;

public interface ModTags {
    public static final LazyTag<Biome> OVERWORLD = LazyTag.biome("overworld");
    public static final LazyTag<Biome> COPSES = LazyTag.biome("trees/copses");
    public static final LazyTag<Biome> HARDY = LazyTag.biome("trees/hardy");
    public static final LazyTag<Biome> HARDY_SLOPES = LazyTag.biome("trees/hardy_slopes");
    public static final LazyTag<Biome> PATCHY = LazyTag.biome("trees/patchy");
    public static final LazyTag<Biome> RAINFOREST = LazyTag.biome("trees/rainforest");
    public static final LazyTag<Biome> SPARSE = LazyTag.biome("trees/sparse");
    public static final LazyTag<Biome> SPARSE_RAINFOREST = LazyTag.biome("trees/sparse_rainforest");
    public static final LazyTag<Biome> TEMPERATE = LazyTag.biome("trees/temperate");
}
