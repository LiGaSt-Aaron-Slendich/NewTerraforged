package com.terraforged.mod.worldgen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.terraforged.mod.codec.WorldGenCodec;
import com.terraforged.mod.worldgen.biome.BiomeGenerator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.terrain.TerrainCache;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.jetbrains.annotations.Nullable;

public class Generator extends ChunkGenerator implements IGenerator {
   public static final Codec<Generator> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(g -> g.seed),
            TerrainLevels.CODEC.optionalFieldOf("levels", TerrainLevels.DEFAULT.get()).forGetter(g -> g.levels),
            WorldGenCodec.CODEC.forGetter(Generator::getRegistries)
         )
         .apply(instance, instance.stable(GeneratorPreset::build))
   );
   protected final long seed;
   protected final Source biomeSource;
   protected final TerrainLevels levels;
   protected final VanillaGen vanillaGen;
   protected final BiomeGenerator biomeGenerator;
   protected final INoiseGenerator noiseGenerator;
   protected final TerrainCache terrainCache;
   protected final ThreadLocal<GeneratorResource> localResource = ThreadLocal.withInitial(GeneratorResource::new);

   public Generator(long seed, TerrainLevels levels, VanillaGen vanillaGen, Source biomeSource, BiomeGenerator biomeGenerator, INoiseGenerator noiseGenerator) {
      super(vanillaGen.getStructureSets(), Optional.empty(), biomeSource, biomeSource, seed);
      this.seed = seed;
      this.levels = levels;
      this.vanillaGen = vanillaGen;
      this.biomeSource = biomeSource;
      this.biomeGenerator = biomeGenerator;
      this.noiseGenerator = noiseGenerator;
      this.terrainCache = new TerrainCache(levels, noiseGenerator);
   }

   public long getSeed() {
      return this.seed;
   }

   protected RegistryAccess getRegistries() {
      return this.biomeSource.getRegistries();
   }

   public VanillaGen getVanillaGen() {
      return this.vanillaGen;
   }

   public INoiseGenerator getNoiseGenerator() {
      return this.noiseGenerator;
   }

   public TerrainData getChunkData(ChunkPos pos) {
      return this.terrainCache.getNow(pos);
   }

   public CompletableFuture<TerrainData> getChunkDataAsync(ChunkPos pos) {
      return this.terrainCache.getAsync(pos);
   }

   public Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Generator withSeed(long seed) {
      INoiseGenerator inoisegenerator = this.noiseGenerator.with(seed, this.levels);
      Source source = new Source(seed, inoisegenerator, this.biomeSource);
      VanillaGen vanillagen = new VanillaGen(seed, source, this.vanillaGen);
      BiomeGenerator biomegenerator = new BiomeGenerator(seed, this.biomeGenerator);
      return new Generator(seed, this.levels, vanillagen, source, biomegenerator, inoisegenerator);
   }

   public int getMinY() {
      return this.levels.minY;
   }

   public int getSeaLevel() {
      return this.levels.seaLevel;
   }

   public int getGenDepth() {
      return this.levels.maxY;
   }

   public Source getBiomeSource() {
      return this.biomeSource;
   }

   public Sampler climateSampler() {
      return Source.NOOP_CLIMATE_SAMPLER;
   }

   @Nullable
   public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(
      ServerLevel server, HolderSet<ConfiguredStructureFeature<?, ?>> feature, BlockPos pos, int i, boolean first
   ) {
      return super.findNearestMapFeature(server, feature, pos, i, first);
   }

   public void createStructures(RegistryAccess access, StructureFeatureManager structureFeatures, ChunkAccess chunk, StructureManager structures, long seed) {
      this.terrainCache.hint(chunk.getPos());
      super.createStructures(access, structureFeatures, chunk, structures, seed);
   }

   public void createReferences(WorldGenLevel level, StructureFeatureManager structureFeatures, ChunkAccess chunk) {
      this.terrainCache.hint(chunk.getPos());
      super.createReferences(level, structureFeatures, chunk);
   }

   public CompletableFuture<ChunkAccess> createBiomes(
      Registry<Biome> registry, Executor executor, Blender blender, StructureFeatureManager structures, ChunkAccess chunk
   ) {
      this.terrainCache.hint(chunk.getPos());
      return CompletableFuture.supplyAsync(() -> {
         ChunkUtil.fillNoiseBiomes(chunk, this.biomeSource, this.climateSampler(), this.localResource.get());
         return chunk;
      }, ThreadPool.EXECUTOR);
   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureManager, ChunkAccess chunkAccess) {
      return this.terrainCache.combineAsync(executor, chunkAccess, (chunk, terrainData) -> {
         ChunkUtil.fillChunk(this.getSeaLevel(), chunk, terrainData, ChunkUtil.FILLER, this.localResource.get());
         ChunkUtil.primeHeightmaps(this.getSeaLevel(), chunk, terrainData, ChunkUtil.FILLER);
         ChunkUtil.buildStructureTerrain(chunk, terrainData, structureManager);
         return chunk;
      });
   }

   public void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk) {
      this.biomeGenerator.surface(chunk, region, this);
   }

   public void applyCarvers(WorldGenRegion region, long seed, BiomeManager biomes, StructureFeatureManager structures, ChunkAccess chunk, Carving stage) {
      this.biomeGenerator.carve(seed, chunk, region, biomes, stage, this);
   }

   public void applyBiomeDecoration(WorldGenLevel region, ChunkAccess chunk, StructureFeatureManager structures) {
      this.biomeGenerator.decorate(chunk, region, structures, this);
      this.terrainCache.drop(chunk.getPos());
   }

   public void spawnOriginalMobs(WorldGenRegion region) {
      NoiseGeneratorSettings noisegeneratorsettings = (NoiseGeneratorSettings)this.vanillaGen.getSettings().value();
      if (!noisegeneratorsettings.disableMobGeneration()) {
         ChunkPos chunkpos = region.getCenter();
         BlockPos blockpos = chunkpos.getWorldPosition().atY(region.getMaxBuildHeight() - 1);
         Holder<Biome> holder = region.getBiome(blockpos);
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
         worldgenrandom.setDecorationSeed(region.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(region, holder, chunkpos, worldgenrandom);
      }
   }

   public int getBaseHeight(int x, int z, Types types, LevelHeightAccessor levelHeightAccessor) {
      NoiseSample noisesample = this.terrainCache.getSample(x, z);
      float f = this.levels.getScaledBaseLevel(noisesample.baseNoise);
      float f1 = this.levels.getScaledHeight(noisesample.heightNoise);
      int i = this.levels.getHeight(f);
      int j = this.levels.getHeight(f1);

      return switch (types) {
         case WORLD_SURFACE, WORLD_SURFACE_WG, MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES -> Math.max(i, j) + 1;
         case OCEAN_FLOOR, OCEAN_FLOOR_WG -> j + 1;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor) {
      NoiseSample noisesample = this.terrainCache.getSample(x, z);
      float f = this.levels.getScaledBaseLevel(noisesample.baseNoise);
      float f1 = this.levels.getScaledHeight(noisesample.heightNoise);
      int i = this.levels.getHeight(f);
      int j = this.levels.getHeight(f1);
      int k = Math.max(i, j);
      BlockState[] ablockstate = new BlockState[k];
      Arrays.fill(ablockstate, 0, j, Blocks.STONE.defaultBlockState());
      if (k > j) {
         Arrays.fill(ablockstate, j, k, Blocks.WATER.defaultBlockState());
      }

      return new NoiseColumn(j, ablockstate);
   }

   public void addDebugScreenInfo(List<String> lines, BlockPos pos) {
      ClimateSample climatesample = this.biomeSource.getBiomeSampler().getSample();
      this.terrainCache.sample(pos.getX(), pos.getZ(), climatesample);
      this.biomeSource.getBiomeSampler().sample(pos.getX(), pos.getZ(), climatesample);
      lines.add("");
      lines.add("[TerraForged]");
      lines.add("Terrain Type: " + climatesample.terrainType.getName());
      lines.add("Climate Type: " + climatesample.climateType.name());
      lines.add("Base Noise: " + climatesample.baseNoise);
      lines.add("Height Noise: " + climatesample.heightNoise);
      lines.add("Ocean Proximity: " + (1.0F - climatesample.continentNoise));
      lines.add("River Proximity: " + (1.0F - climatesample.riverNoise));
   }
}
