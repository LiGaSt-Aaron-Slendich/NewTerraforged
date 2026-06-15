package com.terraforged.mod.worldgen.noise.erosion;

import com.terraforged.engine.settings.FilterSettings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.storage.LongCache;
import com.terraforged.mod.util.storage.LossyCache;
import com.terraforged.mod.util.storage.ObjectPool;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.erosion.ErosionFilter;
import com.terraforged.mod.worldgen.noise.erosion.NoiseResource;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.mod.worldgen.terrain.TerrainBlender;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class ErodedNoiseGenerator
implements INoiseGenerator {
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

    public ErodedNoiseGenerator(NoiseTileSize tileSize, NoiseGenerator generator) {
        FilterSettings.Erosion settings = new FilterSettings.Erosion();
        settings.dropletsPerChunk = 96;
        this.tileSize = tileSize;
        this.generator = generator;
        this.erosion = new ErosionFilter(tileSize.regionLength, settings);
        this.localSample = ThreadLocal.withInitial(NoiseSample::new);
        this.localResource = ThreadLocal.withInitial(() -> new NoiseResource(tileSize));
        this.pool = ObjectPool.forCacheSize(256, CHUNK_ALLOCATOR);
        this.cache = LossyCache.concurrent(256, CHUNK_TASK_ALLOCATOR, this::restore);
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
    public NoiseSample getNoiseSample(int seed, int x, int z) {
        return this.generator.getNoiseSample(seed, x, z);
    }

    @Override
    public void sample(int seed, int x, int z, NoiseSample sample) {
        this.generator.sample(seed, x, z, sample);
    }

    @Override
    public float getHeightNoise(int seed, int x, int z) {
        return this.generator.getHeightNoise(seed, x, z);
    }

    @Override
    public long find(int seed, int x, int z, int minRadius, int maxRadius, Terrain terrain) {
        return this.generator.find(seed, x, z, minRadius, maxRadius, terrain);
    }

    @Override
    public INoiseGenerator with(long seed, TerrainLevels levels) {
        return this.generator.with(seed, levels);
    }

    @Override
    public void generate(int seed, int chunkX, int chunkZ, Consumer<NoiseData> consumer) {
        try {
            NoiseResource resource = this.localResource.get();
            this.collectNeighbours(seed, chunkX, chunkZ, resource);
            this.generateCenterChunk(seed, chunkX, chunkZ, resource);
            this.awaitNeighbours(resource);
            System.arraycopy(resource.heightmap, 0, resource.baselineHeightmap, 0, resource.heightmap.length);
            this.generateErosion(seed, chunkX, chunkZ, resource);
            this.generateRivers(seed, chunkX, chunkZ, resource);
            consumer.accept(resource.chunk);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void collectNeighbours(int seed, int chunkX, int chunkZ, NoiseResource resource) {
        for (int dz = this.tileSize.chunkMin; dz < this.tileSize.chunkMax; ++dz) {
            for (int dx = this.tileSize.chunkMin; dx < this.tileSize.chunkMax; ++dx) {
                if (dx == 0 && dz == 0) continue;
                int tileIndex = this.tileSize.chunkIndexOfRel(dx, dz);
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                resource.chunkCache[tileIndex] = this.getChunk(seed, cx, cz);
            }
        }
    }

    protected void generateCenterChunk(int seed, int chunkX, int chunkZ, NoiseResource resource) {
        TerrainBlender.Blender blender = this.generator.getBlenderResource();
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int min = resource.chunk.min();
        int max = resource.chunk.max();
        for (int dz = min; dz < max; ++dz) {
            float nz = this.getNoiseCoord(startZ + dz);
            for (int dx = min; dx < max; ++dx) {
                float nx = this.getNoiseCoord(startX + dx);
                NoiseSample sample = resource.chunkSample.get(dx, dz);
                this.generator.sampleTerrain(seed, nx, nz, sample, blender);
                int tileIndex = this.tileSize.indexOfRel(dx, dz);
                resource.heightmap[tileIndex] = sample.heightNoise;
            }
        }
    }

    protected void awaitNeighbours(NoiseResource resource) {
        for (int cz = this.tileSize.chunkMin; cz < this.tileSize.chunkMax; ++cz) {
            for (int cx = this.tileSize.chunkMin; cx < this.tileSize.chunkMax; ++cx) {
                if (cx == 0 && cz == 0) continue;
                int chunkIndex = this.tileSize.chunkIndexOfRel(cx, cz);
                float[] chunk = resource.chunkCache[chunkIndex].join();
                int relStartX = cx << 4;
                int relStartZ = cz << 4;
                for (int i = 0; i < chunk.length; ++i) {
                    int dx = i & 0xF;
                    int dz = i >> 4;
                    int index = this.tileSize.indexOfRel(relStartX + dx, relStartZ + dz);
                    resource.heightmap[index] = chunk[i];
                }
            }
        }
    }

    protected void generateErosion(int seed, int chunkX, int chunkZ, NoiseResource resource) {
        this.erosion.apply(seed, chunkX, chunkZ, this.tileSize, resource.erosionResource, resource.random, resource.heightmap);
    }

    protected void generateRivers(int seed, int chunkX, int chunkZ, NoiseResource resource) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int min = resource.chunk.min();
        int max = resource.chunk.max();
        for (int dz = min; dz < max; ++dz) {
            float nz = this.getNoiseCoord(startZ + dz);
            for (int dx = min; dx < max; ++dx) {
                float nx = this.getNoiseCoord(startX + dx);
                int tileIndex = this.tileSize.indexOfRel(dx, dz);
                float eroded = resource.heightmap[tileIndex];
                float baseline = resource.baselineHeightmap[tileIndex];
                int chunkIndex = resource.chunk.index().of(dx, dz);
                NoiseSample sample = resource.chunkSample.get(chunkIndex);
                if (sample.continentNoise < 0.25f) {
                    sample.heightNoise = baseline;
                } else if (sample.continentNoise < 0.55f) {
                    sample.heightNoise = Math.min(eroded, baseline);
                } else {
                    sample.heightNoise = eroded;
                }
                this.generator.sampleRiver(seed, nx, nz, sample);
                resource.chunk.setNoise(chunkIndex, sample);
            }
        }
    }

    protected void restore(CompletableFuture<float[]> task) {
        task.thenAccept(this.pool::restore);
    }

    protected CompletableFuture<float[]> getChunk(int seed, int x, int z) {
        return this.cache.computeIfAbsent(seed, PosUtil.pack(x, z), this::generateChunk);
    }

    protected CompletableFuture<float[]> generateChunk(int seed, long key) {
        return CompletableFuture.supplyAsync(() -> {
            int chunkX = PosUtil.unpackLeft(key);
            int chunkZ = PosUtil.unpackRight(key);
            int startX = chunkX << 4;
            int startZ = chunkZ << 4;
            float[] height = this.pool.take();
            NoiseSample sample = this.localSample.get();
            TerrainBlender.Blender blender = this.generator.getBlenderResource();
            for (int i = 0; i < height.length; ++i) {
                int dx = i & 0xF;
                int dz = i >> 4;
                float nx = this.getNoiseCoord(startX + dx);
                float nz = this.getNoiseCoord(startZ + dz);
                height[i] = this.generator.sampleTerrain((int)seed, (float)nx, (float)nz, (NoiseSample)sample, (TerrainBlender.Blender)blender).heightNoise;
            }
            return height;
        }, ThreadPool.EXECUTOR);
    }
}
