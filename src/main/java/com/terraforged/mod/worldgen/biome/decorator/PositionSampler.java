package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.HeightClimateZones;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetation;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class PositionSampler {
   protected static final float BORDER = 6.0F;
   public static final float SQUASH_FACTOR = 2.0F / NoiseUtil.sqrt(3.0F);

   public static void placeVegetation(
      long seed,
      BlockPos origin,
      Holder<Biome> biome,
      ChunkAccess chunk,
      WorldGenLevel level,
      Generator generator,
      WorldgenRandom random,
      CompletableFuture<TerrainData> terrain,
      FeatureDecorator decorator
   ) {
      int i = placeTreesAndGrass(seed, chunk, level, terrain, generator, random, decorator);
      placeOther(seed, i, chunk, level, generator, random, decorator);
   }

   public static int placeTreesAndGrass(
      long seed,
      ChunkAccess chunk,
      WorldGenLevel level,
      CompletableFuture<TerrainData> terrain,
      Generator generator,
      WorldgenRandom random,
      FeatureDecorator decorator
   ) {
      SamplerContext samplercontext = SamplerContext.get();
      samplercontext.chunk = chunk;
      samplercontext.region = level;
      samplercontext.random = random;
      samplercontext.generator = generator;
      samplercontext.viabilityContext.terrainData = terrain;
      samplercontext.viabilityContext.biomeSampler = generator.getBiomeSource().getBiomeSampler();
      populate(samplercontext, decorator);
      int i = 0;
      int j = chunk.getPos().getMinBlockX();
      int k = chunk.getPos().getMinBlockZ();

      for (int l = 0; l < samplercontext.biomeList.size(); l++) {
         Holder<Biome> holder = samplercontext.biomeList.get(l);
         if (!holder.is(BiomeTags.IS_RIVER)) {
            BiomeVegetation biomevegetation = decorator.getVegetationManager().getVegetation(holder);
            VegetationConfig vegetationconfig = biomevegetation.config;
            samplercontext.push((Biome)holder.value(), biomevegetation);
            if (vegetationconfig == VegetationConfig.NONE) {
               i = placeAt(seed, i + l, j, k, samplercontext);
            } else {
               i = sample(seed, i + l, j, k, vegetationconfig.frequency(), vegetationconfig.jitter(), samplercontext, PositionSampler::placeAt);
               i = placeGrassAt(seed, i + l, j, k, samplercontext);
            }
         }
      }

      return i;
   }

   /**
    * Place non-tree/grass vegetal features for every unique biome in the chunk
    * (not only the chunk-center origin — fungal jungle toadstools were missing at edges).
    */
   public static void placeOther(
      long seed, int offset, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, FeatureDecorator decorator
   ) {
      SamplerContext samplercontext = SamplerContext.current();
      BlockPos origin = chunk.getPos().getWorldPosition().offset(8, 0, 8);
      int y = chunk.getHeight(Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
      BlockPos at = new BlockPos(origin.getX(), y, origin.getZ());
      if (isAboveTreeline(samplercontext, at.getX(), at.getZ(), y)) {
         return;
      }
      for (int i = 0; i < samplercontext.biomeList.size(); i++) {
         Holder<Biome> holder = samplercontext.biomeList.get(i);
         BiomeVegetation biomevegetation = decorator.getVegetationManager().getVegetation(holder);
         if (biomevegetation.features == VegetationFeatures.NONE) {
            continue;
         }
         for (PlacedFeature placedfeature : biomevegetation.features.other()) {
            random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
            if (placedfeature.placeWithBiomeCheck(level, generator, random, at)) {
               offset++;
            }
         }
      }
   }

   public static void populate(SamplerContext context, FeatureDecorator decorator) {
      ChunkAccess chunkaccess = context.chunk;
      int i = chunkaccess.getPos().getMinBlockX();
      int j = chunkaccess.getPos().getMinBlockZ();

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            int i1 = i + l;
            int j1 = j + k;
            int k1 = context.getHeight(l, k);
            Holder<Biome> holder = context.getBiome(i1, k1, j1);
            BiomeVegetation biomevegetation = decorator.getVegetationManager().getVegetation(holder);
            Viability viability = biomevegetation.config.viability();
            float f = viability.getFitness(i1, j1, context.viabilityContext);
            context.viability.set(l, k, f);
            context.biomeList.add(holder);
         }
      }
   }

   public static <T> int sample(long seed, int offset, int x, int z, float freq, float jitter, T context, PositionSampler.Sampler<T> sampler) {
      float f = freq * SQUASH_FACTOR;
      int i = NoiseUtil.floor((x - 6.0F) * freq);
      int j = NoiseUtil.floor((z - 6.0F) * f);
      int k = NoiseUtil.floor((x + 15 + 6.0F) * freq);
      int l = NoiseUtil.floor((z + 15 + 6.0F) * f);
      return sample(seed, offset, i, j, k, l, freq, f, jitter, context, sampler);
   }

   public static <T> int sample(
      long seed, int offset, int minX, int minZ, int maxX, int maxZ, float freqX, float freqZ, float jitter, T context, PositionSampler.Sampler<T> sampler
   ) {
      int i = (int)seed;

      for (int j = minZ; j <= maxZ; j++) {
         float f = (j & 1) * 0.5F;

         for (int k = minX; k <= maxX; k++) {
            int l = MathUtil.hash(i, k, j);
            float f1 = MathUtil.randX(l);
            float f2 = MathUtil.randZ(l);
            float f3 = k + f + f1 * jitter * 0.65F;
            float f4 = j + f2 * jitter;
            int i1 = NoiseUtil.floor(f3 / freqX);
            int j1 = NoiseUtil.floor(f4 / freqZ);
            offset = sampler.sample(seed, offset, l, i1, j1, context);
         }
      }

      return offset;
   }

   private static int placeAt(long seed, int offset, int x, int z, SamplerContext context) {
      int i = x >> 4;
      int j = z >> 4;
      if (i == context.chunk.getPos().x && j == context.chunk.getPos().z) {
         int k = context.chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z);
         if (k <= context.generator.getSeaLevel()) {
            return offset;
         } else if (isAboveTreeline(context, x, z, k)) {
            return offset;
         } else {
            context.pos.set(x, k, z);

            for (PlacedFeature placedfeature : context.features.trees()) {
               context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
               if (placedfeature.placeWithBiomeCheck(context.region, context.generator, context.random, context.pos)) {
                  offset++;
               }
            }

            for (PlacedFeature placedfeature1 : context.features.grass()) {
               context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
               if (placedfeature1.placeWithBiomeCheck(context.region, context.generator, context.random, context.pos)) {
                  offset++;
               }
            }

            return offset;
         }
      } else {
         return offset;
      }
   }

   private static int placeAt(long seed, int offset, int hash, int x, int z, SamplerContext context) {
      if (!isFeatureChunk(x, z, context)) {
         return offset;
      } else {
         int i = context.chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z);
         if (i <= context.generator.getSeaLevel()) {
            return offset;
         } else if (isAboveTreeline(context, x, z, i)) {
            return offset;
         } else {
            context.pos.set(x, i, z);
            Holder<Biome> holder = context.region.getBiome(context.pos);
            if (holder.value() != context.biome) {
               return offset;
            } else {
               float f = context.viability.get(x & 15, z & 15);
               float f1 = (1.0F - context.vegetation.density()) * MathUtil.rand(hash);
               if (f < f1) {
                  return offset;
               } else {
                  for (PlacedFeature placedfeature : context.features.trees()) {
                     context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
                     if (placedfeature.placeWithBiomeCheck(context.region, context.generator, context.random, context.pos)) {
                        offset++;
                     }
                  }

                  return offset;
               }
            }
         }
      }
   }

   private static boolean isAboveTreeline(SamplerContext context, int worldX, int worldZ, int surfaceY) {
      TerrainData data = context.terrainData();
      if (data != null) {
         int lx = worldX & 15;
         int lz = worldZ & 15;
         float n = HeightClimateZones.noiseFromScaled(data.getHeight().get(lx, lz), data.getLevels().maxY);
         return HeightClimateZones.isAboveTreeline(n);
      }
      int minY = context.chunk.getMinBuildHeight();
      int span = Math.max(1, context.generator.getGenDepth());
      float n = (surfaceY - minY) / (float) span;
      return HeightClimateZones.isAboveTreeline(n);
   }

   private static int placeGrassAt(long seed, int offset, int x, int z, SamplerContext context) {
      WorldGenLevel worldgenlevel = context.region;
      Generator generator = context.generator;
      WorldgenRandom worldgenrandom = context.random;
      MutableBlockPos mutableblockpos = context.pos.set(x, 0, z);
      int i = 2;
      i += NoiseUtil.floor(2.0F * (1.0F - context.maxViability));
      i += NoiseUtil.floor(4.0F * context.terrainData().getRiver().get(8, 8));
      i -= NoiseUtil.floor(5 * context.terrainData().getHeight(8, 8));
      i = Math.max(2, i);

      for (int j = 0; j < i; j++) {
         for (PlacedFeature placedfeature : context.features.grass()) {
            worldgenrandom.setFeatureSeed(seed, offset + j, VegetationFeatures.STAGE);
            if (placedfeature.placeWithBiomeCheck(worldgenlevel, generator, worldgenrandom, mutableblockpos)) {
               offset++;
            }
         }
      }

      return offset;
   }

   private static boolean isFeatureChunk(int x, int z, SamplerContext context) {
      int i = x >> 4;
      int j = z >> 4;
      return i == context.chunk.getPos().x && j == context.chunk.getPos().z;
   }

   public interface Sampler<T> {
      int sample(long var1, int var3, int var4, int var5, int var6, T var7);
   }
}
