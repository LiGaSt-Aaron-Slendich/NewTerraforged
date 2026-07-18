package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetationManager;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class FeatureDecorator {
   public static final Decoration[] STAGES = Decoration.values();
   private static final int MAX_DECORATION_STAGE = Decoration.TOP_LAYER_MODIFICATION.ordinal();
   private final BiomeVegetationManager vegetation;
   private final Map<Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>> structures;

   public FeatureDecorator(RegistryAccess access) {
      this.vegetation = new BiomeVegetationManager(access);
      this.structures = VanillaDecorator.buildStructureMap(access);
   }

   public BiomeVegetationManager getVegetationManager() {
      return this.vegetation;
   }

   public List<Holder<ConfiguredStructureFeature<?, ?>>> getStageStructures(int stage) {
      return this.structures.get(STAGES[stage]);
   }

   public HolderSet<PlacedFeature> getStageFeatures(int stage, Biome biome) {
      List<HolderSet<PlacedFeature>> list = biome.getGenerationSettings().features();
      return stage >= list.size() ? null : list.get(stage);
   }

   public void decorate(ChunkAccess chunk, WorldGenLevel level, StructureFeatureManager structures, CompletableFuture<TerrainData> terrain, Generator generator) {
      BlockPos blockpos = getOrigin(level, chunk);
      Holder<Biome> holder = level.getBiome(blockpos);
      WorldgenRandom worldgenrandom = getRandom();
      long i = worldgenrandom.setDecorationSeed(level.getSeed(), blockpos.getX(), blockpos.getZ());
      this.decoratePre(i, blockpos, holder, chunk, level, generator, worldgenrandom, structures);
      this.decorateVegetation(i, blockpos, holder, chunk, level, generator, worldgenrandom, terrain);
      this.decoratePost(i, blockpos, holder, chunk, level, generator, worldgenrandom, structures);
   }

   private void decoratePre(
      long seed,
      BlockPos origin,
      Holder<Biome> biome,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      StructureFeatureManager structureManager
   ) {
      VanillaDecorator.decorate(seed, 0, VegetationFeatures.STAGE - 1, origin, biome, chunk, level, generator, random, structureManager, this);
   }

   private void decoratePost(
      long seed,
      BlockPos origin,
      Holder<Biome> biome,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      StructureFeatureManager structureManager
   ) {
      VanillaDecorator.decorate(
         seed, VegetationFeatures.STAGE + 1, MAX_DECORATION_STAGE, origin, biome, chunk, level, generator, random, structureManager, this
      );
   }

   private void decorateVegetation(
      long seed,
      BlockPos origin,
      Holder<Biome> biome,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      CompletableFuture<TerrainData> terrain
   ) {
      PositionSampler.placeVegetation(seed, origin, biome, chunk, level, generator, random, terrain, this);
   }

   private static BlockPos getOrigin(WorldGenLevel level, ChunkAccess chunk) {
      ChunkPos chunkpos = chunk.getPos();
      SectionPos sectionpos = SectionPos.of(chunkpos, level.getMinSection());
      return sectionpos.origin();
   }

   private static WorldgenRandom getRandom() {
      return new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
   }
}
