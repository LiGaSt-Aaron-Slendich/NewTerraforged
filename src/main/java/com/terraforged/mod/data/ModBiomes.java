package com.terraforged.mod.data;

import com.terraforged.mod.platform.Platform;
import com.terraforged.mod.registry.registrar.Registrar;
import com.terraforged.mod.worldgen.biome.biomes.ModBiome;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome.BiomeBuilder;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.biome.BiomeGenerationSettings.Builder;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;

public interface ModBiomes {
   ModBiome CAVE = ModBiome.of("cave", Biomes.DRIPSTONE_CAVES, builder -> {
      Builder builderx = new Builder();
      builderx.addFeature(Decoration.LOCAL_MODIFICATIONS, CavePlacements.LARGE_DRIPSTONE);
      builderx.addFeature(Decoration.UNDERGROUND_DECORATION, CavePlacements.POINTED_DRIPSTONE);
      builderx.build();
      builder.generationSettings(builderx.build());
   });
   ModBiome OAK_FOREST = ModBiome.of("oak_forest", Biomes.PLAINS, builder -> builder.biomeCategory(BiomeCategory.FOREST));

   static void register() {
      register(Platform.ACTIVE_PLATFORM.get().getRegistrar(Registry.BIOME_REGISTRY));
   }

   static void register(Registrar<Biome> registrar) {
      registrar.register(CAVE.key(), CAVE.create());
      registrar.register(OAK_FOREST.key(), OAK_FOREST.create());
   }
}
