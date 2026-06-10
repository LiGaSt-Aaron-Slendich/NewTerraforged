package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.storage.ObjectPool;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainGenerator;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.lang.invoke.LambdaMetafactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

public class TerrainCache {
    private final TerrainGenerator generator;
    private final int noiseSeed;
    private final Map<CacheKey, CacheValue> cache = new ConcurrentHashMap<CacheKey, CacheValue>(512);
    private final ThreadLocal<CacheKey> localKey = ThreadLocal.withInitial(CacheKey::new);
    private final ObjectPool<CacheKey> keyPool = new ObjectPool<CacheKey>(512, CacheKey::new);
    private final ObjectPool<CacheValue> valuePool = new ObjectPool<CacheValue>(512, CacheValue::new);

    public TerrainCache(TerrainLevels levels, INoiseGenerator noiseGenerator, long worldSeed) {
        this.noiseSeed = Seeds.get(worldSeed);
        this.generator = new TerrainGenerator(levels, noiseGenerator, this.noiseSeed);
    }

    protected CacheKey allocPos(ChunkPos pos) {
        return this.keyPool.take().set(pos.x, pos.z);
    }

    protected CacheKey lookupPos(ChunkPos pos) {
        return this.localKey.get().set(pos.x, pos.z);
    }

    public void drop(ChunkPos pos) {
        CacheKey key = this.lookupPos(pos);
        CacheValue value = this.cache.remove(key);
        if (value == null || value.task == null) {
            return;
        }
        this.generator.restore(value.task.join());
        this.keyPool.restore(value.key);
        this.valuePool.restore(value.reset());
    }

    public void hint(ChunkPos pos) {
        this.getAsync(pos);
    }

    public int getHeight(int x, int z) {
        return this.generator.getHeight(x, z);
    }

    public NoiseSample getSample(int x, int z) {
        return this.generator.noiseGenerator.getNoiseSample(this.noiseSeed, x, z);
    }

    public void sample(int x, int z, NoiseSample sample) {
        this.generator.getNoiseGenerator().sample(this.noiseSeed, x, z, sample);
    }

    public TerrainData getNow(ChunkPos pos) {
        return this.getAsync(pos).join();
    }

    @Nullable
    public TerrainData getIfReady(ChunkPos pos) {
        CacheKey key = this.allocPos(pos);
        CacheValue value = this.cache.get(key);
        if (value == null || !value.task.isDone()) {
            return null;
        }
        return value.task.join();
    }

    public CompletableFuture<TerrainData> getAsync(ChunkPos pos) {
        CacheKey key = this.allocPos(pos);
        return this.cache.computeIfAbsent((CacheKey) key, this::generate).task;
    }

    public <T> CompletableFuture<ChunkAccess> combineAsync(Executor executor, ChunkAccess chunk, BiFunction<ChunkAccess, TerrainData, ChunkAccess> function) {
        return this.getAsync(chunk.getPos()).thenApplyAsync(terrainData -> (ChunkAccess)function.apply(chunk, (TerrainData)terrainData), executor);
    }

    protected CacheValue generate(CacheKey key) {
        CacheValue value = this.valuePool.take();
        value.key = key;
        value.generator = this.generator;
        value.task = CompletableFuture.supplyAsync(value, ThreadPool.EXECUTOR);
        return value;
    }

    protected static class CacheKey {
        protected int x;
        protected int z;

        protected CacheKey() {
        }

        public CacheKey set(int x, int y) {
            this.x = x;
            this.z = y;
            return this;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof CacheKey)) return false;
            CacheKey pos = (CacheKey)obj;
            if (this.x != pos.x) return false;
            if (this.z != pos.z) return false;
            return true;
        }

        public int hashCode() {
            int result = this.x;
            result = 31 * result + this.z;
            return result;
        }
    }

    protected static class CacheValue
    implements Supplier<TerrainData> {
        protected CacheKey key;
        protected TerrainGenerator generator;
        protected CompletableFuture<TerrainData> task;

        protected CacheValue() {
        }

        public CacheValue reset() {
            this.key = null;
            this.task = null;
            this.generator = null;
            return this;
        }

        @Override
        public TerrainData get() {
            return this.generator.generate(this.key.x, this.key.z);
        }
    }
}
