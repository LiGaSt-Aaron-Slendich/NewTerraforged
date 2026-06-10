package com.terraforged.mod.data;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.key.EntryKey;
import com.terraforged.mod.worldgen.biome.biomes.ModBiome;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public interface ModBiomes {
    public static final EntryKey<Biome> CAVE = TerraForged.BIOMES.entryKey("cave");
    public static final EntryKey<Biome> OAK_FOREST = TerraForged.BIOMES.entryKey("oak_forest");

    public static void register() {
        TerraForged.register(TerraForged.BIOMES, "cave", ModBiome.create((ResourceKey<Biome>)Biomes.DRIPSTONE_CAVES, builder -> {
            BiomeGenerationSettings.Builder genSettings = new BiomeGenerationSettings.Builder();
            genSettings.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CavePlacements.LARGE_DRIPSTONE);
            genSettings.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, CavePlacements.POINTED_DRIPSTONE);
            genSettings.build();
            builder.generationSettings(genSettings.build());
        }));
        TerraForged.register(TerraForged.BIOMES, "oak_forest", ModBiome.create((ResourceKey<Biome>)Biomes.PLAINS, builder -> {}));
    }
}
