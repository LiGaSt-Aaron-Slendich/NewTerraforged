package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.storage.ObjectPool;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;

public class TerrainGenerator {
    protected final TerrainLevels levels;
    protected final INoiseGenerator noiseGenerator;
    protected final ObjectPool<TerrainData> terrainDataPool;
    protected final int noiseSeed;

    public TerrainGenerator(TerrainLevels levels, INoiseGenerator noiseGenerator, int noiseSeed) {
        this.levels = levels;
        this.noiseGenerator = noiseGenerator;
        this.noiseSeed = noiseSeed;
        this.terrainDataPool = new ObjectPool<TerrainData>(() -> new TerrainData(this.levels));
    }

    public INoiseGenerator getNoiseGenerator() {
        return this.noiseGenerator;
    }

    public void restore(TerrainData terrainData) {
        this.terrainDataPool.restore(terrainData);
    }

    public TerrainData generate(int chunkX, int chunkZ) {
        TerrainData terrainData = this.terrainDataPool.take();
        this.noiseGenerator.generate(this.noiseSeed, chunkX, chunkZ, terrainData);
        return terrainData;
    }

    public int getHeight(int x, int z) {
        float heightNoise = this.noiseGenerator.getHeightNoise(this.noiseSeed, x, z);
        float scaledHeight = this.levels.getScaledHeight(heightNoise);
        return this.levels.getHeight(scaledHeight);
    }
}
