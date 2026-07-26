package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.worldgen.GenerationFeatureGates;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Stock TF118 carve path with minimal MEGA/GIGA distribution hooks.
 * River mask stays in {@link CarverChunk#getCarvingMask} (1 - noise * river).
 */
public class NoiseCaveGenerator {
   protected static final int POOL_SIZE = 32;
   protected static final float DENSITY = 0.05F;
   protected static final float BREACH_THRESHOLD = 0.7F;
   protected static final int GLOBAL_CAVE_REPS = 2;
   protected final NoiseCave[] caves;
   protected final Module uniqueCaveNoise;
   protected final Module megaCaveNoise;
   protected final Module gigaCaveNoise;
   protected final Module caveBreachNoise;
   protected final ObjectPool<CarverChunk> pool;
   protected final Map<ChunkPos, CarverChunk> cache = new ConcurrentHashMap<>();
   private final CaveEntranceClaims entranceClaims = new CaveEntranceClaims();

   public NoiseCaveGenerator(long seed, RegistryAccess access) {
      this.uniqueCaveNoise = createUniqueNoise((int)seed, 500, 0.05F);
      this.megaCaveNoise = CaveModifiers.mega();
      this.gigaCaveNoise = CaveModifiers.giga();
      this.caveBreachNoise = createBreachNoise((int)seed + 12, 300, 0.7F);
      this.caves = createArray(seed, java.util.Arrays.asList(TerraForged.CAVES.entries(access, NoiseCave[]::new)));
      this.pool = new ObjectPool<>(32, this::createCarverChunk);
   }

   public NoiseCaveGenerator(long seed, NoiseCaveGenerator other) {
      this.caves = copyOf(seed, other.caves);
      this.uniqueCaveNoise = createUniqueNoise((int)seed, 500, 0.05F);
      this.megaCaveNoise = CaveModifiers.mega();
      this.gigaCaveNoise = CaveModifiers.giga();
      this.caveBreachNoise = createBreachNoise((int)seed + 12, 300, 0.7F);
      this.pool = new ObjectPool<>(32, this::createCarverChunk);
   }

   public void carve(ChunkAccess chunk, Generator generator) {
      CarverChunk carverchunk = this.getPreCarveChunk(chunk);
      carverchunk.mask = this.caveBreachNoise;
      carverchunk.terrainData = generator.getChunkData(com.terraforged.mod.worldgen.Seeds.get(generator.getSeed()), chunk.getPos());
      carverchunk.megaModifier = this.megaCaveNoise;
      carverchunk.gigaModifier = this.gigaCaveNoise;
      carverchunk.prepareColumnCache(com.terraforged.mod.worldgen.Seeds.get(generator.getSeed()), chunk, generator);
      MegaGigaChunkCache.begin(generator, chunk, 8, carverchunk.columnCache());
      try {
         for (NoiseCave noisecave : this.caves) {
            if (!isCaveEnabled(noisecave)) {
               continue;
            }
            carverchunk.modifier = this.getModifier(noisecave);
            NoiseCaveCarver.carve(chunk, carverchunk, generator, noisecave, true);
         }
      } finally {
         MegaGigaChunkCache.end();
      }
   }

   public CarverChunk peekCarver(ChunkPos pos) {
      return this.cache.get(pos);
   }

   public CaveEntranceClaims getCaveEntranceClaims() {
      return this.entranceClaims;
   }

   public CaveEntranceClaims getEntranceClaims() {
      return this.entranceClaims;
   }

   public void carve(int seed, ChunkAccess chunk, Generator generator) {
      carve(chunk, generator);
   }

   public void decorate(int seed, ChunkAccess chunk, WorldGenLevel region, Generator generator) {
      decorate(chunk, region, generator);
   }

   public void decorate(ChunkAccess chunk, WorldGenLevel region, Generator generator) {
      CarverChunk carverchunk = this.getPostCarveChunk(chunk, generator);
      WorldGenLevel guarded = com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carverchunk);

      if (CaveDecorationSettings.usePerBiomeDecorators() || CaveDecorationSettings.useOfficialTfDecorator()) {
         TerraForgedOfficialCaveDecorator.decorateVolume(chunk, carverchunk, guarded, generator);
      } else if (CaveDecorationSettings.useLegacyDecorators()) {
         CaveBiomeVolumeDecorator.decorateChunk(chunk, carverchunk, guarded, generator);
      } else if (CaveDecorationSettings.useVanillaPass()) {
         CaveBiomeVanillaPass.decorateChunk(chunk, carverchunk, guarded, generator);
      } else {
         for (NoiseCave noisecave : this.caves) {
            if (!isCaveEnabled(noisecave)) {
               continue;
            }
            NoiseCaveDecorator.decorate(chunk, carverchunk, guarded, generator, noisecave);
         }
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
         carverchunk.terrainData = generator.getChunkData(com.terraforged.mod.worldgen.Seeds.get(generator.getSeed()), chunk.getPos());
         carverchunk.megaModifier = this.megaCaveNoise;
         carverchunk.gigaModifier = this.gigaCaveNoise;
         carverchunk.prepareColumnCache(com.terraforged.mod.worldgen.Seeds.get(generator.getSeed()), chunk, generator);

         for (NoiseCave noisecave : this.caves) {
            if (!isCaveEnabled(noisecave)) {
               continue;
            }
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
         case MEGA -> this.megaCaveNoise;
         case GIGA -> this.gigaCaveNoise;
      };
   }

   private static boolean isCaveEnabled(NoiseCave cave) {
      if (cave == null) {
         return false;
      }
      CaveType type = cave.getType();
      if (type == CaveType.GLOBAL && !GenerationFeatureGates.synapseCavesEnabled) {
         return false;
      }
      if (type != null && type.isMegaOrGiga() && !GenerationFeatureGates.megaGigaCavesEnabled) {
         return false;
      }
      return true;
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
