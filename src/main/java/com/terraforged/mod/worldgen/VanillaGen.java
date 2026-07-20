package com.terraforged.mod.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom.Algorithm;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public class VanillaGen {
   protected final Registry<StructureSet> structureSets;
   protected final NoiseBasedChunkGenerator vanillaGenerator;
   protected final Holder<NoiseGeneratorSettings> settings;
   protected final Registry<NoiseParameters> parameters;
   /** NewTF sea level (may differ from {@link NoiseGeneratorSettings#seaLevel()}). */
   protected final int seaLevel;
   protected final int lavaLevel;
   protected final FluidStatus fluidStatus1;
   protected final FluidStatus fluidStatus2;
   protected final FluidPicker globalFluidPicker;
   protected final SurfaceSystem surfaceSystem;

   public VanillaGen(long seed, BiomeSource biomeSource, VanillaGen other) {
      this(seed, biomeSource, other.settings, other.parameters, other.structureSets, other.seaLevel);
   }

   public VanillaGen(
      long seed, BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, Registry<NoiseParameters> parameters, Registry<StructureSet> structures
   ) {
      this(seed, biomeSource, settings, parameters, structures, settings.value().seaLevel());
   }

   public VanillaGen(
      long seed,
      BiomeSource biomeSource,
      Holder<NoiseGeneratorSettings> settings,
      Registry<NoiseParameters> parameters,
      Registry<StructureSet> structures,
      int seaLevel
   ) {
      NoiseGeneratorSettings ngs = settings.value();
      this.settings = settings;
      this.parameters = parameters;
      this.structureSets = structures;
      this.seaLevel = seaLevel > 0 ? seaLevel : ngs.seaLevel();
      this.lavaLevel = Math.min(-54, this.seaLevel);
      this.fluidStatus1 = new FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      this.fluidStatus2 = new FluidStatus(this.seaLevel, ngs.defaultFluid());
      this.globalFluidPicker = (x, y, z) -> y < this.lavaLevel ? this.fluidStatus1 : this.fluidStatus2;
      BlockState blockstate = ngs.defaultBlock();
      Algorithm algorithm = ngs.getRandomSource();
      this.surfaceSystem = new SurfaceSystem(parameters, blockstate, this.seaLevel, seed, algorithm);
      this.vanillaGenerator = new NoiseBasedChunkGenerator(structures, parameters, biomeSource, seed, settings);
   }

   public Holder<NoiseGeneratorSettings> getSettings() {
      return this.settings;
   }

   public Registry<StructureSet> getStructureSets() {
      return this.structureSets;
   }

   public int getSeaLevel() {
      return this.seaLevel;
   }

   public FluidPicker getGlobalFluidPicker() {
      return this.globalFluidPicker;
   }

   public SurfaceSystem getSurfaceSystem() {
      return this.surfaceSystem;
   }

   public CarvingContext createCarvingContext(WorldGenRegion region, ChunkAccess chunk, NoiseChunk noiseChunk) {
      return new CarvingContext(this.vanillaGenerator, region.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk);
   }
}
