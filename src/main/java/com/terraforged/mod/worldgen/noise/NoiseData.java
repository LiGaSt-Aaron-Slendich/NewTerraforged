package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.storage.FloatMap;
import com.terraforged.mod.util.storage.Index;
import com.terraforged.mod.util.storage.ObjectMap;
import com.terraforged.mod.worldgen.noise.NoiseSample;

public class NoiseData {
    protected static final int BORDER = 1;
    protected static final int MIN = -1;
    protected static final int MAX = 17;
    protected final NoiseSample sample = new NoiseSample();
    protected final FloatMap height = new FloatMap(1);
    protected final FloatMap river = new FloatMap(1);
    protected final FloatMap base = new FloatMap(1);
    protected final ObjectMap<Terrain> terrain = new ObjectMap(1, Terrain[]::new);

    public int min() {
        return -1;
    }

    public int max() {
        return 17;
    }

    public Index index() {
        return this.height.index();
    }

    public NoiseSample getSample() {
        return this.sample;
    }

    public FloatMap getHeight() {
        return this.height;
    }

    public FloatMap getBase() {
        return this.base;
    }

    public FloatMap getRiver() {
        return this.river;
    }

    public ObjectMap<Terrain> getTerrain() {
        return this.terrain;
    }

    public void setNoise(int x, int z, NoiseSample sample) {
        int index = this.index().of(x, z);
        this.setNoise(index, sample);
    }

    public void setNoise(int index, NoiseSample sample) {
        this.terrain.set(index, sample.terrainType);
        this.height.set(index, sample.heightNoise);
        this.base.set(index, sample.baseNoise);
        this.river.set(index, sample.riverNoise);
    }

    public static boolean isInsideChunk(int x, int z) {
        return x >= -1 && x <= 16 && z >= -1 && z <= 16;
    }
}
