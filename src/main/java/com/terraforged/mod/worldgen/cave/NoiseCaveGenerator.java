package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public class NoiseCaveGenerator {
   protected static final int POOL_SIZE = 32;
   protected static final float DENSITY = 0.05F;
   protected static final float BREACH_THRESHOLD = 0.7F;
   protected static final int GLOBAL_CAVE_REPS = 2;
   protected final NoiseCave[] caves;
   protected final Module uniqueCaveNoise;
   protected final Module caveBreachNoise;
   protected final ObjectPool<CarverChunk> pool;
   protected final Map<ChunkPos, CarverChunk> cache = new ConcurrentHashMap<>();

   public NoiseCaveGenerator(long seed, RegistryAccess access) {
      this.uniqueCaveNoise = createUniqueNoise((int)seed, 500, 0.05F);
      this.caveBreachNoise = createBreachNoise((int)seed + 12, 300, 0.7F);
      this.caves = createArray(seed, access.registryOrThrow(ModRegistry.CAVE.get()));
      this.pool = new ObjectPool<>(32, this::createCarverChunk);
   }

   public NoiseCaveGenerator(long seed, NoiseCaveGenerator other) {
      this.caves = copyOf(seed, other.caves);
      this.uniqueCaveNoise = createUniqueNoise((int)seed, 500, 0.05F);
      this.caveBreachNoise = createBreachNoise((int)seed + 12, 300, 0.7F);
      this.pool = new ObjectPool<>(32, this::createCarverChunk);
   }

   public void carve(ChunkAccess chunk, Generator generator) {
      CarverChunk carverchunk = this.getPreCarveChunk(chunk);
      carverchunk.terrainData = generator.getChunkData(chunk.getPos());
      carverchunk.mask = this.caveBreachNoise;

      for (NoiseCave noisecave : this.caves) {
         carverchunk.modifier = this.getModifier(noisecave);
         NoiseCaveCarver.carve(chunk, carverchunk, generator, noisecave, true);
      }
   }

   public void decorate(ChunkAccess chunk, WorldGenLevel region, Generator generator) {
      CarverChunk carverchunk = this.getPostCarveChunk(chunk, generator);

      for (NoiseCave noisecave : this.caves) {
         NoiseCaveDecorator.decorate(chunk, carverchunk, region, generator, noisecave);
      }

      this.pool.restore(carverchunk);
   }

   private CarverChunk getPreCarveChunk(ChunkAccess chunk) {
      return this.cache.computeIfAbsent(chunk.getPos(), p -> this.pool.take().reset());
   }

   private CarverChunk getPostCarveChunk(ChunkAccess chunk, Generator generator) {
      CarverChunk carverchunk = this.cache.remove(chunk.getPos());
      if (carverchunk != null) {
         return carverchunk;
      } else {
         carverchunk = this.pool.take().reset();
         carverchunk.mask = this.caveBreachNoise;
         carverchunk.terrainData = generator.getChunkData(chunk.getPos());

         for (NoiseCave noisecave : this.caves) {
            carverchunk.modifier = this.getModifier(noisecave);
            NoiseCaveCarver.carve(chunk, carverchunk, generator, noisecave, false);
         }

         return carverchunk;
      }
   }

   private Module getModifier(NoiseCave cave) {
      return switch (cave.getType()) {
         case GLOBAL -> Source.ONE;
         case UNIQUE -> this.uniqueCaveNoise;
      };
   }

   private CarverChunk createCarverChunk() {
      return new CarverChunk(this.caves.length);
   }

   private static Module createUniqueNoise(int seed, int scale, float density) {
      return new UniqueCaveDistributor(seed + 1286745, 1.0F / scale, 0.75F, density).clamp(0.2, 1.0).map(0.0, 1.0).warp(seed + 781624, 30, 1, 20.0);
   }

   private static Module createBreachNoise(int seed, int scale, float threshold) {
      return Source.simplexRidge(seed, scale, 2).clamp(threshold * 0.8F, threshold).map(0.0, 1.0);
   }

   private static NoiseCave[] copyOf(long seed, NoiseCave[] other) {
      NoiseCave[] anoisecave = Arrays.copyOf(other, other.length);

      for (int i = 0; i < anoisecave.length; i++) {
         anoisecave[i] = anoisecave[i].withSeed(seed);
      }

      return anoisecave;
   }

   private static NoiseCave[] createArray(long seed, Iterable<NoiseCave> source) {
      int i = 0;

      for (NoiseCave noisecave : source) {
         i += getCount(noisecave);
      }

      NoiseCave[] anoisecave = new NoiseCave[i];
      int l = 0;

      for (NoiseCave noisecave1 : source) {
         int j = getCount(noisecave1);

         for (int k = 0; k < j; k++) {
            anoisecave[l++] = noisecave1.withSeed(seed + k * 16421058L);
         }
      }

      return anoisecave;
   }

   private static int getCount(NoiseCave cave) {
      return cave.getType() == CaveType.GLOBAL ? 2 : 1;
   }
}
