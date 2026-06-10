package com.terraforged.mod.worldgen.cave;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CaveSystemConfig(int megaRegionCountMin, int megaRegionCountMax, int megaTransitionPerRegion, float megaScale, int gigaRegionCountMin, int gigaRegionCountMax, int gigaTransitionPerRegion, float gigaScale, int normalMaxBiomesPerSystem, int transitionMaxWidthBlocks, float islandMaxRadiusChunks) {
    public static final CaveSystemConfig DEFAULT = new CaveSystemConfig(5, 8, 3, 1.0f, 5, 10, 6, 2.0f, 2, 24, 1.5f);
    public static final Codec<CaveSystemConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.intRange((int)1, (int)20).optionalFieldOf("mega_region_count_min", 5).forGetter(CaveSystemConfig::megaRegionCountMin), Codec.intRange((int)1, (int)20).optionalFieldOf("mega_region_count_max", 8).forGetter(CaveSystemConfig::megaRegionCountMax), Codec.intRange((int)1, (int)10).optionalFieldOf("mega_transition_per_region", 3).forGetter(CaveSystemConfig::megaTransitionPerRegion), Codec.floatRange((float)0.1f, (float)10.0f).optionalFieldOf("mega_scale", Float.valueOf(1.0f)).forGetter(CaveSystemConfig::megaScale), Codec.intRange((int)1, (int)30).optionalFieldOf("giga_region_count_min", 7).forGetter(CaveSystemConfig::gigaRegionCountMin), Codec.intRange((int)1, (int)30).optionalFieldOf("giga_region_count_max", 10).forGetter(CaveSystemConfig::gigaRegionCountMax), Codec.intRange((int)1, (int)15).optionalFieldOf("giga_transition_per_region", 5).forGetter(CaveSystemConfig::gigaTransitionPerRegion), Codec.floatRange((float)1.0f, (float)10.0f).optionalFieldOf("giga_scale", Float.valueOf(2.0f)).forGetter(CaveSystemConfig::gigaScale), Codec.intRange((int)1, (int)10).optionalFieldOf("normal_max_biomes_per_system", 2).forGetter(CaveSystemConfig::normalMaxBiomesPerSystem), Codec.intRange((int)1, (int)64).optionalFieldOf("transition_max_width_blocks", 16).forGetter(CaveSystemConfig::transitionMaxWidthBlocks), Codec.floatRange((float)0.5f, (float)10.0f).optionalFieldOf("island_max_radius_chunks", Float.valueOf(1.5f)).forGetter(CaveSystemConfig::islandMaxRadiusChunks)).apply(instance, CaveSystemConfig::new));

    public float effectiveGigaScale() {
        return this.megaScale * this.gigaScale;
    }
}
