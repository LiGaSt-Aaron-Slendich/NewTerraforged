package com.terraforged.mod.worldgen.util;

import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.blending.Blender;

public class NoiseChunkUtil {
   private static final MethodHandle SURFACE_CACHE = ReflectionUtil.field(NoiseChunk.class, Long2IntMap.class);

   public static void initChunk(ChunkAccess chunk, Generator generator) {
      getNoiseChunk(chunk, generator);
   }

   public static NoiseChunk getNoiseChunk(ChunkAccess chunk, Generator generator) {
      VanillaGen vanillagen = generator.getVanillaGen();
      FluidPicker fluidpicker = vanillagen.getGlobalFluidPicker();
      NoiseGeneratorSettings noisegeneratorsettings = (NoiseGeneratorSettings)vanillagen.getSettings().value();
      CompletableFuture<TerrainData> completablefuture = generator.getChunkDataAsync(chunk.getPos());
      NoiseChunk noisechunk = chunk.getOrCreateNoiseChunk(NoopNoise.ROUTER, NoopNoise.BEARDIFIER, noisegeneratorsettings, fluidpicker, Blender.empty());
      initChunk(chunk, noisechunk, completablefuture);
      return noisechunk;
   }

   private static void initChunk(ChunkAccess chunk, NoiseChunk noiseChunk, CompletableFuture<TerrainData> terrainData) {
      Long2IntMap long2intmap = getCache(noiseChunk);
      if (long2intmap.isEmpty()) {
         initSurfaceCache(chunk, long2intmap, terrainData);
      }
   }

   private static void initSurfaceCache(ChunkAccess chunk, Long2IntMap cache, CompletableFuture<TerrainData> terrainData) {
      ChunkPos chunkpos = chunk.getPos();
      TerrainData terraindata = terrainData.join();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      int k = Integer.MAX_VALUE;
      int l = Integer.MIN_VALUE;
      cache.clear();

      for (int i1 = 0; i1 < 16; i1++) {
         for (int j1 = 0; j1 < 16; j1++) {
            int k1 = terraindata.getHeight(j1, i1);
            int l1 = QuartPos.fromBlock(i + j1);
            int i2 = QuartPos.fromBlock(j + i1);
            long j2 = ChunkPos.asLong(l1, i2);
            cache.put(j2, k1);
            k = Math.min(k, k1);
            l = Math.max(l, k1);
         }
      }

      for (int k2 = -16; k2 < 32; k2++) {
         for (int l2 = -16; l2 < 32; l2++) {
            if ((l2 & 15) != l2 || (k2 & 15) != k2) {
               int i3 = QuartPos.fromBlock(i + l2);
               int j3 = QuartPos.fromBlock(j + k2);
               long k3 = ChunkPos.asLong(i3, j3);
               cache.put(k3, k);
            }
         }
      }
   }

   private static Long2IntMap getCache(NoiseChunk noiseChunk) {
      try {
         return (Long2IntMap)SURFACE_CACHE.invokeExact((NoiseChunk)noiseChunk);
      } catch (Throwable throwable) {
         throw new Error(throwable);
      }
   }
}
