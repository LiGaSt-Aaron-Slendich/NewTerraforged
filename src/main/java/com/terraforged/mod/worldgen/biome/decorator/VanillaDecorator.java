package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class VanillaDecorator {
   public static void decorate(
      long seed,
      int from,
      int to,
      BlockPos origin,
      Holder<Biome> biome,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      StructureFeatureManager structureManager,
      FeatureDecorator decorator
   ) {
      for (int i = from; i <= to; i++) {
         List<Holder<ConfiguredStructureFeature<?, ?>>> list = decorator.getStageStructures(i);
         HolderSet<PlacedFeature> holderset = decorator.getStageFeatures(i, (Biome)biome.value());
         if (holderset != null) {
            placeStructures(seed, i, chunk, level, generator, random, structureManager, list);
            placeFeatures(seed, list.size(), i, origin, level, generator, random, holderset);
         }
      }
   }

   /**
    * TOP_LAYER_MODIFICATION (freeze_top_layer) for every unique biome present in this chunk's
    * surface-ish sections — matches vanilla multi-biome decoration for snow.
    */
   public static void decorateTopLayerAllBiomes(
      long seed,
      BlockPos origin,
      Holder<Biome> primary,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      StructureFeatureManager structureManager,
      FeatureDecorator decorator
   ) {
      int stage = Decoration.TOP_LAYER_MODIFICATION.ordinal();
      List<Holder<ConfiguredStructureFeature<?, ?>>> structures = decorator.getStageStructures(stage);
      // Structures once from primary.
      placeStructures(seed, stage, chunk, level, generator, random, structureManager, structures);

      java.util.LinkedHashSet<Biome> biomes = new java.util.LinkedHashSet<>();
      if (primary != null && primary.isBound()) {
         biomes.add(primary.value());
      }
      // Sample a coarse surface grid so mixed snowy/non-snowy chunks all freeze.
      ChunkPos pos = chunk.getPos();
      int minX = pos.getMinBlockX();
      int minZ = pos.getMinBlockZ();
      for (int lz = 0; lz < 16; lz += 4) {
         for (int lx = 0; lx < 16; lx += 4) {
            int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
            if (y < level.getMinBuildHeight() + 1) {
               y = generator.getSeaLevel();
            }
            Holder<Biome> h = level.getBiome(new BlockPos(minX + lx, y, minZ + lz));
            if (h != null && h.isBound()) {
               biomes.add(h.value());
            }
         }
      }

      int offset = structures != null ? structures.size() : 0;
      int biomeIndex = 0;
      for (Biome biome : biomes) {
         HolderSet<PlacedFeature> features = decorator.getStageFeatures(stage, biome);
         if (features == null) {
            continue;
         }
         // Distinct feature seed per biome so freeze isn't identical / skipped.
         random.setFeatureSeed(seed ^ (biomeIndex * 341873128712L), offset, stage);
         placeFeatures(seed ^ (biomeIndex * 132897987541L), offset, stage, origin, level, generator, random, features);
         biomeIndex++;
      }
   }

   private static void placeStructures(
      long seed,
      int stage,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      StructureFeatureManager structureManager,
      List<Holder<ConfiguredStructureFeature<?, ?>>> structures
   ) {
      ChunkPos chunkpos = chunk.getPos();
      SectionPos sectionpos = SectionPos.of(chunkpos, level.getMinSection());

      for (int i = 0; i < structures.size(); i++) {
         random.setFeatureSeed(seed, i, stage);
         Holder<ConfiguredStructureFeature<?, ?>> holder = structures.get(i);
         List<StructureStart> list = structureManager.startsForFeature(sectionpos, (ConfiguredStructureFeature)holder.value());

         for (int j = 0; j < list.size(); j++) {
            StructureStart structurestart = list.get(j);
            structurestart.placeInChunk(level, structureManager, generator, random, getWritableArea(chunk), chunkpos);
         }
      }
   }

   private static void placeFeatures(
      long seed, int offset, int stage, BlockPos origin, WorldGenLevel level, Generator generator, WorldgenRandom random, HolderSet<PlacedFeature> features
   ) {
      for (int i = 0; i < features.size(); i++) {
         random.setFeatureSeed(seed, offset + i, stage);
         Holder<PlacedFeature> holder = features.get(i);
         if (com.terraforged.mod.compat.WwooCompat.shouldSkipSurfaceFeature(holder)) {
            continue;
         }
         PlacedFeature placedfeature = (PlacedFeature)holder.value();
         placedfeature.placeWithBiomeCheck(level, generator, random, origin);
      }
   }

   public static Map<Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>> buildStructureMap(RegistryAccess access) {
      EnumMap<Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>> enummap = new EnumMap<>(Decoration.class);
      Registry<ConfiguredStructureFeature<?, ?>> registry = access.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

      for (Entry<ResourceKey<ConfiguredStructureFeature<?, ?>>, ConfiguredStructureFeature<?, ?>> entry : registry.entrySet()) {
         ResourceKey<ConfiguredStructureFeature<?, ?>> resourcekey = entry.getKey();
         ConfiguredStructureFeature<?, ? extends StructureFeature<?>> configuredstructurefeature = (ConfiguredStructureFeature<?, ? extends StructureFeature<?>>)entry.getValue();
         enummap.computeIfAbsent(configuredstructurefeature.feature.step(), s -> new ArrayList()).add(registry.getHolderOrThrow(resourcekey));
      }

      for (Decoration decoration : FeatureDecorator.STAGES) {
         if (!enummap.containsKey(decoration)) {
            enummap.put(decoration, Collections.emptyList());
         }
      }

      return enummap;
   }

   private static BoundingBox getWritableArea(ChunkAccess chunkAccess) {
      ChunkPos chunkpos = chunkAccess.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      LevelHeightAccessor levelheightaccessor = chunkAccess.getHeightAccessorForGeneration();
      int k = levelheightaccessor.getMinBuildHeight() + 1;
      int l = levelheightaccessor.getMaxBuildHeight() - 1;
      return new BoundingBox(i, k, j, i + 15, l, j + 15);
   }
}
