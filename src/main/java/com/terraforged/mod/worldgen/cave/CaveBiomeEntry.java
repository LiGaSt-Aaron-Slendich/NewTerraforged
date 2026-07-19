package com.terraforged.mod.worldgen.cave;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public record CaveBiomeEntry(ResourceLocation biome, CaveBiomeCategory category, CavePlacementType placementType, float caveTemperature, float vegetationDensity, float weight, float ceilingPatchMin, float ceilingPatchMax, float islandMaxRadius, CaveBiomeStats stats, boolean statGenerator) {
        public static final Codec<CaveBiomeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("biome").forGetter(CaveBiomeEntry::biome),
            CaveBiomeCategory.CODEC.fieldOf("category").forGetter(CaveBiomeEntry::category),
            CavePlacementType.CODEC.optionalFieldOf("placement_type", CavePlacementType.FULL_REGION).forGetter(CaveBiomeEntry::placementType),
            Codec.floatRange(0f, 1f).fieldOf("cave_temperature").forGetter(CaveBiomeEntry::caveTemperature),
            Codec.floatRange(0f, 1f).optionalFieldOf("vegetation_density", 0.5f).forGetter(CaveBiomeEntry::vegetationDensity),
            Codec.floatRange(0f, 10f).optionalFieldOf("weight", 1.0f).forGetter(CaveBiomeEntry::weight),
            Codec.floatRange(0f, 1f).optionalFieldOf("ceiling_patch_min", 0.2f).forGetter(CaveBiomeEntry::ceilingPatchMin),
            Codec.floatRange(0f, 1f).optionalFieldOf("ceiling_patch_max", 0.5f).forGetter(CaveBiomeEntry::ceilingPatchMax),
            Codec.floatRange(0f, 10f).optionalFieldOf("island_max_radius", 1.5f).forGetter(CaveBiomeEntry::islandMaxRadius)
    ).apply(instance, (biome, category, placementType, caveTemperature, vegetationDensity, weight,
            ceilingPatchMin, ceilingPatchMax, islandMaxRadius) -> new CaveBiomeEntry(
            biome, category, placementType, caveTemperature, vegetationDensity, weight,
            ceilingPatchMin, ceilingPatchMax, islandMaxRadius, CaveBiomeStats.EMPTY, false)));

    public CaveBiomeEntry {
        if (stats == null) {
            stats = CaveBiomeStats.EMPTY;
        }
    }

    public static CaveBiomeEntry of(ResourceLocation biome, CaveBiomeCategory category, CavePlacementType placementType, float caveTemperature, float vegetationDensity, float weight, float ceilingPatchMin, float ceilingPatchMax, float islandMaxRadius) {
        return new CaveBiomeEntry(biome, category, placementType, caveTemperature, vegetationDensity, weight, ceilingPatchMin, ceilingPatchMax, islandMaxRadius, CaveBiomeStats.EMPTY, false);
    }

    public boolean isAvailable(Registry<Biome> biomeRegistry) {
        return biomeRegistry.containsKey(this.biome);
    }
}
