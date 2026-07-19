package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.cave.NoiseCaveGenerator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;

public class BiomeGenerator {
   private final SurfaceDecorator surfaceDecorator;
   private final FeatureDecorator featureDecorator;
   private final NoiseCaveGenerator noiseCaveGenerator;

   public BiomeGenerator(long seed, RegistryAccess access) {
      this.surfaceDecorator = new SurfaceDecorator();
      this.featureDecorator = new FeatureDecorator(access);
      this.noiseCaveGenerator = new NoiseCaveGenerator(seed, access);
   }

   public BiomeGenerator(long seed, BiomeGenerator other) {
      this.surfaceDecorator = other.surfaceDecorator;
      this.featureDecorator = other.featureDecorator;
      this.noiseCaveGenerator = new NoiseCaveGenerator(seed, other.noiseCaveGenerator);
   }

   public void surface(ChunkAccess chunk, WorldGenRegion region, Generator generator) {
      this.surfaceDecorator.decorate(chunk, region, generator);
      this.surfaceDecorator.decoratePost(chunk, generator);
   }

   public void carve(long seed, ChunkAccess chunk, WorldGenRegion region, BiomeManager biomes, Carving step, Generator generator) {
      this.noiseCaveGenerator.carve(chunk, generator);
   }

   public com.terraforged.mod.worldgen.cave.CarverChunk peekCaveCarver(net.minecraft.world.level.ChunkPos pos) {
      return this.noiseCaveGenerator.peekCarver(pos);
   }

   public void decorate(ChunkAccess chunk, WorldGenLevel region, StructureFeatureManager structures, Generator generator) {
      CompletableFuture<TerrainData> completablefuture = generator.getChunkDataAsync(chunk.getPos());
      this.featureDecorator.decorate(chunk, region, structures, completablefuture, generator);
      this.noiseCaveGenerator.decorate(chunk, region, generator);
      Surface.smoothWater(chunk, region, completablefuture.join());
      Surface.applyPost(chunk, completablefuture.join(), generator);
   }

   protected static void buildVanillaSurface(ChunkAccess chunk, WorldGenRegion region, Generator generator) {
      WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(generator, region);
      NoiseChunk noisechunk = NoiseChunkUtil.getNoiseChunk(chunk, generator);
      Registry<Biome> registry = generator.getBiomeSource().getRegistry();
      BiomeManager biomemanager = region.getBiomeManager();
      SurfaceSystem surfacesystem = generator.getVanillaGen().getSurfaceSystem();
      RuleSource rulesource = ((NoiseGeneratorSettings)generator.getVanillaGen().getSettings().value()).surfaceRule();
      surfacesystem.buildSurface(biomemanager, registry, false, worldgenerationcontext, chunk, noisechunk, rulesource);
   }
}
