package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

public class TerrainCache {
   private final TerrainGenerator generator;
   private final Map<ChunkPos, CompletableFuture<TerrainData>> cache = new ConcurrentHashMap<>();

   public TerrainCache(TerrainLevels levels, INoiseGenerator noiseGenerator) {
      this.generator = new TerrainGenerator(levels, noiseGenerator);
   }

   public void drop(ChunkPos pos) {
      CompletableFuture<TerrainData> completablefuture = this.cache.remove(pos);
      if (completablefuture != null && !completablefuture.isDone()) {
         this.generator.restore(completablefuture.join());
      }
   }

   public void hint(ChunkPos pos) {
      this.getAsync(pos);
   }

   public int getHeight(int x, int z) {
      TerrainData ready = this.getIfReady(new ChunkPos(x >> 4, z >> 4));
      if (ready != null) {
         return ready.getHeight(x & 15, z & 15);
      }
      return this.generator.getHeight(x, z);
   }

   public NoiseSample getSample(int x, int z) {
      return this.generator.noiseGenerator.getNoiseSample(x, z);
   }

   public void sample(int x, int z, NoiseSample sample) {
      this.generator.getNoiseGenerator().sample(x, z, sample);
   }

   public TerrainData getNow(ChunkPos pos) {
      return this.getAsync(pos).join();
   }

   @Nullable
   public TerrainData getIfReady(ChunkPos pos) {
      CompletableFuture<TerrainData> completablefuture = this.cache.get(pos);
      return completablefuture != null && completablefuture.isDone() ? completablefuture.join() : null;
   }

   public CompletableFuture<TerrainData> getAsync(ChunkPos pos) {
      return this.cache.computeIfAbsent(pos, this::generate);
   }

   public <T> CompletableFuture<ChunkAccess> combineAsync(Executor executor, ChunkAccess chunk, BiFunction<ChunkAccess, TerrainData, ChunkAccess> function) {
      return this.getAsync(chunk.getPos()).thenApplyAsync(terrainData -> function.apply(chunk, terrainData), executor);
   }

   protected CompletableFuture<TerrainData> generate(ChunkPos pos) {
      return CompletableFuture.supplyAsync(() -> this.generator.generate(pos.x, pos.z), ThreadPool.EXECUTOR);
   }
}
