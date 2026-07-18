package com.terraforged.mod.worldgen.profiler;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.IGenerator;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class GeneratorProfiler extends ChunkGenerator implements IGenerator {
   public static final Codec<GeneratorProfiler> CODEC = ChunkGenerator.CODEC.xmap(GeneratorProfiler::wrap, GeneratorProfiler::getGenerator);
   public static final AtomicBoolean PROFILING = new AtomicBoolean(true);
   protected static final MethodHandle STRUCTURE_REGISTRY = ReflectionUtil.field(ChunkGenerator.class, Registry.class);
   protected static final MethodHandle STRUCTURE_OVERRIDES = ReflectionUtil.field(ChunkGenerator.class, Optional.class);
   private static final long LOG_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10L);
   private static final long DEBUG_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1L);
   protected final ChunkGenerator generator;
   protected final ProfilerStages stages = new ProfilerStages();

   private GeneratorProfiler(Registry<StructureSet> structures, Optional<HolderSet<StructureSet>> overrides, ChunkGenerator generator) {
      super(structures, overrides, generator.getBiomeSource());
      this.generator = generator;
   }

   public ChunkGenerator getGenerator() {
      return this.generator;
   }

   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long seed) {
      return new GeneratorProfiler(this.structureSets, this.structureOverrides, this.generator.withSeed(seed));
   }

   public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return this.generator.getTypeNameForDataFixer();
   }

   public Sampler climateSampler() {
      return this.generator.climateSampler();
   }

   public Holder<Biome> getNoiseBiome(int p_187755_, int p_187756_, int p_187757_) {
      return this.generator.getNoiseBiome(p_187755_, p_187756_, p_187757_);
   }

   public void createStructures(RegistryAccess p_62200_, StructureFeatureManager p_62201_, ChunkAccess p_62202_, StructureManager p_62203_, long p_62204_) {
      GenTimer gentimer = this.stages.starts.start();
      this.generator.createStructures(p_62200_, p_62201_, p_62202_, p_62203_, p_62204_);
      gentimer.punchOut();
   }

   public void createReferences(WorldGenLevel p_62178_, StructureFeatureManager p_62179_, ChunkAccess p_62180_) {
      GenTimer gentimer = this.stages.refs.start();
      this.generator.createReferences(p_62178_, p_62179_, p_62180_);
      gentimer.punchOut();
   }

   public CompletableFuture<ChunkAccess> createBiomes(
      Registry<Biome> p_196743_, Executor p_196744_, Blender p_196745_, StructureFeatureManager p_196746_, ChunkAccess p_196747_
   ) {
      return CompletableFuture.completedFuture(this.stages.biomes.start())
         .thenCombine(this.generator.createBiomes(p_196743_, p_196744_, p_196745_, p_196746_, p_196747_), GenTimer::punchOut);
   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor p_187748_, Blender p_187749_, StructureFeatureManager p_187750_, ChunkAccess p_187751_) {
      return CompletableFuture.completedFuture(this.stages.noise.start())
         .thenCombine(this.generator.fillFromNoise(p_187748_, p_187749_, p_187750_, p_187751_), GenTimer::punchOut);
   }

   public void buildSurface(WorldGenRegion p_187697_, StructureFeatureManager p_187698_, ChunkAccess p_187699_) {
      GenTimer gentimer = this.stages.surface.start();
      this.generator.buildSurface(p_187697_, p_187698_, p_187699_);
      gentimer.punchOut();
   }

   public void applyCarvers(
      WorldGenRegion p_187691_, long p_187692_, BiomeManager p_187693_, StructureFeatureManager p_187694_, ChunkAccess p_187695_, Carving p_187696_
   ) {
      GenTimer gentimer = this.stages.carve.start();
      this.generator.applyCarvers(p_187691_, p_187692_, p_187693_, p_187694_, p_187695_, p_187696_);
      gentimer.punchOut();
   }

   public void applyBiomeDecoration(WorldGenLevel p_187712_, ChunkAccess p_187713_, StructureFeatureManager p_187714_) {
      GenTimer gentimer = this.stages.decoration.start();
      this.generator.applyBiomeDecoration(p_187712_, p_187713_, p_187714_);
      gentimer.punchOut();
      this.stages.incrementChunks();
   }

   public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(
      ServerLevel p_62162_, HolderSet<ConfiguredStructureFeature<?, ?>> p_62163_, BlockPos p_62164_, int p_62165_, boolean p_62166_
   ) {
      return this.generator.findNearestMapFeature(p_62162_, p_62163_, p_62164_, p_62165_, p_62166_);
   }

   public void spawnOriginalMobs(WorldGenRegion p_62167_) {
      this.generator.spawnOriginalMobs(p_62167_);
   }

   public int getSpawnHeight(LevelHeightAccessor p_156157_) {
      return this.generator.getSpawnHeight(p_156157_);
   }

   public BiomeSource getBiomeSource() {
      return this.generator.getBiomeSource();
   }

   public int getGenDepth() {
      return this.generator.getGenDepth();
   }

   public int getSeaLevel() {
      return this.generator.getSeaLevel();
   }

   public int getMinY() {
      return this.generator.getMinY();
   }

   public int getBaseHeight(int p_156153_, int p_156154_, Types p_156155_, LevelHeightAccessor p_156156_) {
      return this.generator.getBaseHeight(p_156153_, p_156154_, p_156155_, p_156156_);
   }

   public NoiseColumn getBaseColumn(int p_156150_, int p_156151_, LevelHeightAccessor p_156152_) {
      return this.generator.getBaseColumn(p_156150_, p_156151_, p_156152_);
   }

   public int getFirstFreeHeight(int p_156175_, int p_156176_, Types p_156177_, LevelHeightAccessor p_156178_) {
      return this.generator.getFirstFreeHeight(p_156175_, p_156176_, p_156177_, p_156178_);
   }

   public int getFirstOccupiedHeight(int p_156180_, int p_156181_, Types p_156182_, LevelHeightAccessor p_156183_) {
      return this.generator.getFirstOccupiedHeight(p_156180_, p_156181_, p_156182_, p_156183_);
   }

   public void addDebugScreenInfo(List<String> lines, BlockPos pos) {
      this.generator.addDebugScreenInfo(lines, pos);
      this.stages.addDebugInfo(DEBUG_INTERVAL_MS, lines);
   }

   private static Registry<StructureSet> getStructures(ChunkGenerator generator) {
      try {
         return (Registry)STRUCTURE_REGISTRY.invokeExact((ChunkGenerator)generator);
      } catch (Throwable throwable) {
         throw new Error(throwable);
      }
   }

   private static Optional<HolderSet<StructureSet>> getOverrides(ChunkGenerator generator) {
      try {
         return (Optional)STRUCTURE_OVERRIDES.invokeExact((ChunkGenerator)generator);
      } catch (Throwable throwable) {
         throw new Error(throwable);
      }
   }

   public static GeneratorProfiler wrap(ChunkGenerator generator) {
      if (generator instanceof GeneratorProfiler generatorprofiler) {
         generator = generatorprofiler.getGenerator();
      }

      Registry<StructureSet> registry = getStructures(generator);
      Optional<HolderSet<StructureSet>> optional = getOverrides(generator);
      return new GeneratorProfiler(registry, optional, generator);
   }
}
