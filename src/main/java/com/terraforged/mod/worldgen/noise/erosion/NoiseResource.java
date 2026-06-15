package com.terraforged.mod.worldgen.noise.erosion;

import com.terraforged.engine.util.FastRandom;
import com.terraforged.mod.util.storage.ObjectMap;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.erosion.ErosionFilter;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import java.util.concurrent.CompletableFuture;

public class NoiseResource {
    public final FastRandom random = new FastRandom();
    public final NoiseData chunk = new NoiseData();
    public final ErosionFilter.Resource erosionResource = new ErosionFilter.Resource();
    public final float[] heightmap;
    public final float[] baselineHeightmap;
    public final NoiseSample sharedSample;
    public final ObjectMap<NoiseSample> chunkSample;
    public final CompletableFuture<float[]>[] chunkCache;

    public NoiseResource() {
        this(NoiseTileSize.DEFAULT);
    }

    public NoiseResource(NoiseTileSize tileSize) {
        this.heightmap = new float[tileSize.regionSize];
        this.baselineHeightmap = new float[tileSize.regionSize];
        this.sharedSample = new NoiseSample();
        this.chunkSample = new ObjectMap(1, NoiseSample[]::new);
        this.chunkSample.fill(NoiseSample::new);
        this.chunkCache = new CompletableFuture[tileSize.chunkSize];
    }

    public NoiseSample getSample(int dx, int dz) {
        return NoiseData.isInsideChunk(dx, dz) ? this.chunkSample.get(dx, dz) : this.sharedSample;
    }
}
