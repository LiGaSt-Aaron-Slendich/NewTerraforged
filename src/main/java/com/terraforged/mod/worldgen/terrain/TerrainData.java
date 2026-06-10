package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.storage.FloatMap;
import com.terraforged.mod.util.storage.ObjectMap;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Consumer;

public class TerrainData
implements Consumer<NoiseData> {
    protected final TerrainLevels levels;
    protected final FloatMap height = new FloatMap();
    protected final FloatMap gradient = new FloatMap();
    protected final FloatMap river = new FloatMap();
    protected final FloatMap baseHeight = new FloatMap();
    protected final ObjectMap<Terrain> terrain = new ObjectMap(Terrain[]::new);
    protected float min = Float.MAX_VALUE;
    protected float max = Float.MIN_VALUE;
    protected float maxBase = Float.MIN_VALUE;

    public TerrainData(TerrainLevels levels) {
        this.levels = levels;
    }

    public int getMin() {
        return this.levels.getHeight(this.min);
    }

    public int getMax() {
        return this.levels.getHeight(this.max);
    }

    public int getMaxBase() {
        return this.levels.getHeight(this.maxBase);
    }

    public int getHeight(int x, int z) {
        float scaledHeight = this.height.get(x, z);
        return this.levels.getHeight(scaledHeight);
    }

    public int getBaseHeight(int x, int z) {
        float scaledLevel = this.baseHeight.get(x, z);
        return this.levels.getHeight(scaledLevel);
    }

    public TerrainLevels getLevels() {
        return this.levels;
    }

    public FloatMap getHeight() {
        return this.height;
    }

    public FloatMap getBaseHeight() {
        return this.baseHeight;
    }

    public FloatMap getGradient() {
        return this.gradient;
    }

    public FloatMap getRiver() {
        return this.river;
    }

    public ObjectMap<Terrain> getTerrain() {
        return this.terrain;
    }

    public float getGradient(int x, int z, float norm) {
        float grad = this.getGradient().get(x, z);
        return NoiseUtil.clamp(grad * norm, 0.0f, 1.0f);
    }

    @Override
    public void accept(NoiseData noiseData) {
        FloatMap basemap = noiseData.getBase();
        FloatMap heightmap = noiseData.getHeight();
        ObjectMap<Terrain> terrainMap = noiseData.getTerrain();
        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                float heightNoise = heightmap.get(x, z);
                float baseLevelNoise = basemap.get(x, z);
                float scaledHeight = this.levels.getScaledHeight(heightNoise);
                float scaledBaseLevel = this.levels.getScaledBaseLevel(baseLevelNoise);
                Terrain terrainType = terrainMap.get(x, z);
                float n = heightmap.get(x, z - 1);
                float s = heightmap.get(x, z + 1);
                float e = heightmap.get(x + 1, z);
                float w = heightmap.get(x - 1, z);
                float dx = e - w;
                float dz = s - n;
                float grad = NoiseUtil.sqrt(dx * dx + dz * dz);
                float noiseGrad = NoiseUtil.clamp(grad, 0.0f, 1.0f);
                this.gradient.set(x, z, noiseGrad);
                this.terrain.set(x, z, terrainType);
                this.height.set(x, z, scaledHeight);
                this.baseHeight.set(x, z, scaledBaseLevel);
                this.river.set(x, z, noiseData.getRiver().get(x, z));
                this.max = Math.max(this.max, scaledHeight);
                this.min = Math.min(this.min, scaledHeight);
                this.maxBase = Math.max(this.maxBase, scaledBaseLevel);
            }
        }
    }
}
