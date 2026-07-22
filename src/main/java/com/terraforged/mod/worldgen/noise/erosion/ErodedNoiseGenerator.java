package com.terraforged.mod.worldgen.noise.erosion;

import com.terraforged.engine.settings.FilterSettings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.util.map.LongCache;
import com.terraforged.mod.util.map.LossyCache;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.terrain.TerrainBlender;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class ErodedNoiseGenerator implements INoiseGenerator {
   private static final int CACHE_SIZE = 256;
   private static final Supplier<float[]> CHUNK_ALLOCATOR = () -> new float[256];
   private static final IntFunction<CompletableFuture<float[]>[]> CHUNK_TASK_ALLOCATOR = CompletableFuture[]::new;
   protected final NoiseTileSize tileSize;
   protected final ErosionFilter erosion;
   protected final NoiseGenerator generator;
   protected final ThreadLocal<NoiseSample> localSample;
   protected final ThreadLocal<NoiseResource> localResource;
   protected final ObjectPool<float[]> pool;
   protected final LongCache<CompletableFuture<float[]>> cache;

   public ErodedNoiseGenerator(long seed, NoiseTileSize tileSize, NoiseGenerator generator) {
      this(seed, tileSize, generator, defaultErosion());
   }

   public ErodedNoiseGenerator(long seed, NoiseTileSize tileSize, NoiseGenerator generator, FilterSettings.Erosion erosionSettings) {
      FilterSettings.Erosion filtersettings$erosion = erosionSettings != null ? erosionSettings : defaultErosion();
      this.tileSize = tileSize;
      this.generator = generator;
      this.erosion = new ErosionFilter((int)seed, tileSize.regionLength, filtersettings$erosion);
      this.localSample = ThreadLocal.withInitial(NoiseSample::new);
      this.localResource = ThreadLocal.withInitial(() -> new NoiseResource(tileSize));
      this.pool = ObjectPool.forCacheSize(256, CHUNK_ALLOCATOR);
      this.cache = LossyCache.concurrent(256, CHUNK_TASK_ALLOCATOR, this::restore);
   }

   public NoiseGenerator getDelegate() {
      return this.generator;
   }

   private static FilterSettings.Erosion defaultErosion() {
      FilterSettings.Erosion erosion = new FilterSettings.Erosion();
      erosion.dropletsPerChunk = 350;
      return erosion;
   }

   @Override
   public INoiseGenerator with(long seed, TerrainLevels levels) {
      return this.generator.with(seed, levels).withErosion();
   }

   @Override
   public NoiseLevels getLevels() {
      return this.generator.getLevels();
   }

   @Override
   public TerrainLevels getTerrainLevels() {
      return this.generator.getTerrainLevels();
   }

   @Override
   public IContinentNoise getContinent() {
      return this.generator.getContinent();
   }

   @Override
   public NoiseSample getNoiseSample(int x, int z) {
      return this.generator.getNoiseSample(x, z);
   }

   @Override
   public void sample(int x, int z, NoiseSample sample) {
      this.generator.sample(x, z, sample);
   }

   @Override
   public float getHeightNoise(int x, int z) {
      return this.generator.getHeightNoise(x, z);
   }

   @Override
   public long find(int x, int z, int minRadius, int maxRadius, Terrain terrain) {
      return this.generator.find(x, z, minRadius, maxRadius, terrain);
   }

   @Override
   public void generate(int chunkX, int chunkZ, Consumer<NoiseData> consumer) {
      try {
         NoiseResource noiseresource = this.localResource.get();
         this.collectNeighbours(chunkX, chunkZ, noiseresource);
         this.generateCenterChunk(chunkX, chunkZ, noiseresource);
         this.awaitNeighbours(noiseresource);
         this.generateErosion(chunkX, chunkZ, noiseresource);
         this.generateRivers(chunkX, chunkZ, noiseresource);
         consumer.accept(noiseresource.chunk);
      } catch (Throwable throwable) {
         throwable.printStackTrace();
      }
   }

   protected void collectNeighbours(int chunkX, int chunkZ, NoiseResource resource) {
      for (int i = this.tileSize.chunkMin; i < this.tileSize.chunkMax; i++) {
         for (int j = this.tileSize.chunkMin; j < this.tileSize.chunkMax; j++) {
            if (j != 0 || i != 0) {
               int k = this.tileSize.chunkIndexOfRel(j, i);
               int l = chunkX + j;
               int i1 = chunkZ + i;
               resource.chunkCache[k] = this.getChunk(l, i1);
            }
         }
      }
   }

   protected void generateCenterChunk(int chunkX, int chunkZ, NoiseResource resource) {
      TerrainBlender.Blender terrainblender$blender = this.generator.getBlenderResource();
      int i = chunkX << 4;
      int j = chunkZ << 4;
      int k = resource.chunk.min();
      int l = resource.chunk.max();

      for (int i1 = k; i1 < l; i1++) {
         float f = this.getNoiseCoord(j + i1);

         for (int j1 = k; j1 < l; j1++) {
            float f1 = this.getNoiseCoord(i + j1);
            NoiseSample noisesample = resource.chunkSample.get(j1, i1);
            this.generator.sampleTerrain(f1, f, noisesample, terrainblender$blender);
            int k1 = this.tileSize.indexOfRel(j1, i1);
            resource.heightmap[k1] = noisesample.heightNoise;
         }
      }
   }

   protected void awaitNeighbours(NoiseResource resource) {
      for (int i = this.tileSize.chunkMin; i < this.tileSize.chunkMax; i++) {
         for (int j = this.tileSize.chunkMin; j < this.tileSize.chunkMax; j++) {
            if (j != 0 || i != 0) {
               int k = this.tileSize.chunkIndexOfRel(j, i);
               float[] afloat = resource.chunkCache[k].join();
               int l = j << 4;
               int i1 = i << 4;

               for (int j1 = 0; j1 < afloat.length; j1++) {
                  int k1 = j1 & 15;
                  int l1 = j1 >> 4;
                  int i2 = this.tileSize.indexOfRel(l + k1, i1 + l1);
                  resource.heightmap[i2] = afloat[j1];
               }
            }
         }
      }
   }

   protected void generateErosion(int chunkX, int chunkZ, NoiseResource resource) {
      this.erosion.apply(resource.heightmap, chunkX, chunkZ, this.tileSize, resource.erosionResource, resource.random);
   }

   protected void generateRivers(int chunkX, int chunkZ, NoiseResource resource) {
      int i = chunkX << 4;
      int j = chunkZ << 4;
      int k = resource.chunk.min();
      int l = resource.chunk.max();

      for (int i1 = k; i1 < l; i1++) {
         float f = this.getNoiseCoord(j + i1);

         for (int j1 = k; j1 < l; j1++) {
            float f1 = this.getNoiseCoord(i + j1);
            int k1 = this.tileSize.indexOfRel(j1, i1);
            float f2 = resource.heightmap[k1];
            int l1 = resource.chunk.index().of(j1, i1);
            NoiseSample noisesample = resource.chunkSample.get(l1);
            noisesample.heightNoise = f2;
            this.generator.sampleRiver(f1, f, noisesample);
            resource.chunk.setNoise(l1, noisesample);
         }
      }
   }

   protected void restore(CompletableFuture<float[]> task) {
      task.thenAccept(this.pool::restore);
   }

   protected CompletableFuture<float[]> getChunk(int x, int z) {
      return this.cache.computeIfAbsent(PosUtil.pack(x, z), this::generateChunk);
   }

   protected CompletableFuture<float[]> generateChunk(long key) {
      return CompletableFuture.supplyAsync(() -> {
         int i = PosUtil.unpackLeft(key);
         int j = PosUtil.unpackRight(key);
         int k = i << 4;
         int l = j << 4;
         float[] afloat = this.pool.take();
         NoiseSample noisesample = this.localSample.get();
         TerrainBlender.Blender terrainblender$blender = this.generator.getBlenderResource();

         for (int i1 = 0; i1 < afloat.length; i1++) {
            int j1 = i1 & 15;
            int k1 = i1 >> 4;
            float f = this.getNoiseCoord(k + j1);
            float f1 = this.getNoiseCoord(l + k1);
            afloat[i1] = this.generator.sampleTerrain(f, f1, noisesample, terrainblender$blender).heightNoise;
         }

         return afloat;
      }, ThreadPool.EXECUTOR);
   }
}
