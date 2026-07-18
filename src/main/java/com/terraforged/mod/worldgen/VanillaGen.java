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
   protected final int lavaLevel;
   protected final FluidStatus fluidStatus1;
   protected final FluidStatus fluidStatus2;
   protected final FluidPicker globalFluidPicker;
   protected final SurfaceSystem surfaceSystem;

   public VanillaGen(long seed, BiomeSource biomeSource, VanillaGen other) {
      this(seed, biomeSource, other.settings, other.parameters, other.structureSets);
   }

   public VanillaGen(
      long seed, BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, Registry<NoiseParameters> parameters, Registry<StructureSet> structures
   ) {
      this.settings = settings;
      this.parameters = parameters;
      this.structureSets = structures;
      this.lavaLevel = Math.min(-54, ((NoiseGeneratorSettings)settings.value()).seaLevel());
      this.fluidStatus1 = new FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      this.fluidStatus2 = new FluidStatus(((NoiseGeneratorSettings)settings.value()).seaLevel(), ((NoiseGeneratorSettings)settings.value()).defaultFluid());
      this.globalFluidPicker = (x, y, z) -> y < this.lavaLevel ? this.fluidStatus1 : this.fluidStatus2;
      int i = ((NoiseGeneratorSettings)settings.value()).seaLevel();
      BlockState blockstate = ((NoiseGeneratorSettings)settings.value()).defaultBlock();
      Algorithm algorithm = ((NoiseGeneratorSettings)settings.value()).getRandomSource();
      this.surfaceSystem = new SurfaceSystem(parameters, blockstate, i, seed, algorithm);
      this.vanillaGenerator = new NoiseBasedChunkGenerator(structures, parameters, biomeSource, seed, settings);
   }

   public Holder<NoiseGeneratorSettings> getSettings() {
      return this.settings;
   }

   public Registry<StructureSet> getStructureSets() {
      return this.structureSets;
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
