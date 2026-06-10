package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.asset.TerrainType;

public class NoiseSample {
    private static final NoiseSample DEFAULT = new NoiseSample();
    public long continentCentre = 0L;
    public float continentNoise = 1.0f;
    public float baseNoise = 0.0f;
    public float heightNoise = 0.0f;
    public float riverNoise = 1.0f;
    public Terrain terrainType = TerrainType.NONE.getTerrain();

    public NoiseSample() {
    }

    public NoiseSample(NoiseSample other) {
        this.copy(other);
    }

    public NoiseSample reset() {
        return this.copy(DEFAULT);
    }

    public NoiseSample copy(NoiseSample other) {
        this.continentCentre = other.continentCentre;
        this.continentNoise = other.continentNoise;
        this.heightNoise = other.heightNoise;
        this.baseNoise = other.baseNoise;
        this.riverNoise = other.riverNoise;
        this.terrainType = other.terrainType;
        return this;
    }
}
