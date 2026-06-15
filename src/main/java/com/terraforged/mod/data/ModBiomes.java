package com.terraforged.mod.data;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.key.EntryKey;
import com.terraforged.mod.worldgen.biome.biomes.ModBiome;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public interface ModBiomes {
    public static final EntryKey<Biome> CAVE = TerraForged.BIOMES.entryKey("cave");
    public static final EntryKey<Biome> OAK_FOREST = TerraForged.BIOMES.entryKey("oak_forest");
    public static final EntryKey<Biome> CORRUPTED_CHUNKS = TerraForged.BIOMES.entryKey("corrupted_chunks");

    public static void register() {
        TerraForged.register(TerraForged.BIOMES, "cave", ModBiome.create((ResourceKey<Biome>)Biomes.DRIPSTONE_CAVES, builder -> {
            BiomeGenerationSettings.Builder genSettings = new BiomeGenerationSettings.Builder();
            genSettings.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CavePlacements.LARGE_DRIPSTONE);
            genSettings.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, CavePlacements.POINTED_DRIPSTONE);
            genSettings.build();
            builder.generationSettings(genSettings.build());
        }));
        TerraForged.register(TerraForged.BIOMES, "oak_forest", ModBiome.create((ResourceKey<Biome>)Biomes.PLAINS, builder -> {}));
        TerraForged.register(TerraForged.BIOMES, "corrupted_chunks", ModBiome.create((ResourceKey<Biome>)Biomes.PLAINS, builder -> {
            builder.generationSettings(BiomeGenerationSettings.EMPTY);
            builder.specialEffects(new BiomeSpecialEffects.Builder()
                    .grassColorOverride(0xFF2020)
                    .foliageColorOverride(0xCC1010)
                    .skyColor(0x8B0000)
                    .fogColor(0xAA0000)
                    .waterColor(0xAA0000)
                    .waterFogColor(0x660000)
                    .build());
        }));
    }
}
