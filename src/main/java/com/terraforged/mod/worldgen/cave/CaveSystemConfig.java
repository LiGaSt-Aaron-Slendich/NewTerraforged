package com.terraforged.mod.worldgen.cave;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CaveSystemConfig(
        int megaRegionCountMin,
        int megaRegionCountMax,
        int megaTransitionPerRegion,
        float megaScale,
        int gigaRegionCountMin,
        int gigaRegionCountMax,
        int gigaTransitionPerRegion,
        float gigaScale,
        int normalMaxBiomesPerSystem,
        int transitionMaxWidthBlocks,
        float islandMaxRadiusChunks,
        int islandMaxPerRegion
) {
    public static final CaveSystemConfig DEFAULT = new CaveSystemConfig(
            5, 8, 2, 1.0f,
            5, 8, 3, 2.0f,
            2, 40, 3.0f, 4
    );

    public static final Codec<CaveSystemConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 20).optionalFieldOf("mega_region_count_min", 5).forGetter(CaveSystemConfig::megaRegionCountMin),
            Codec.intRange(1, 20).optionalFieldOf("mega_region_count_max", 8).forGetter(CaveSystemConfig::megaRegionCountMax),
            Codec.intRange(1, 10).optionalFieldOf("mega_transition_per_region", 2).forGetter(CaveSystemConfig::megaTransitionPerRegion),
            Codec.floatRange(0.1f, 10.0f).optionalFieldOf("mega_scale", 1.0f).forGetter(CaveSystemConfig::megaScale),
            Codec.intRange(1, 30).optionalFieldOf("giga_region_count_min", 5).forGetter(CaveSystemConfig::gigaRegionCountMin),
            Codec.intRange(1, 30).optionalFieldOf("giga_region_count_max", 8).forGetter(CaveSystemConfig::gigaRegionCountMax),
            Codec.intRange(1, 15).optionalFieldOf("giga_transition_per_region", 3).forGetter(CaveSystemConfig::gigaTransitionPerRegion),
            Codec.floatRange(1.0f, 10.0f).optionalFieldOf("giga_scale", 2.0f).forGetter(CaveSystemConfig::gigaScale),
            Codec.intRange(1, 10).optionalFieldOf("normal_max_biomes_per_system", 2).forGetter(CaveSystemConfig::normalMaxBiomesPerSystem),
            Codec.intRange(1, 64).optionalFieldOf("transition_max_width_blocks", 40).forGetter(CaveSystemConfig::transitionMaxWidthBlocks),
            Codec.floatRange(0.5f, 10.0f).optionalFieldOf("island_max_radius_chunks", 3.0f).forGetter(CaveSystemConfig::islandMaxRadiusChunks),
            Codec.intRange(0, 8).optionalFieldOf("island_max_per_region", 4).forGetter(CaveSystemConfig::islandMaxPerRegion)
    ).apply(instance, CaveSystemConfig::new));

    public float effectiveGigaScale() {
        return this.megaScale * this.gigaScale;
    }
}
