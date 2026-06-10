package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.concurrent.CompletableFuture;

public class ViabilityContext
implements Viability.Context {
    public int seed;
    public CompletableFuture<TerrainData> terrainData;
    public IBiomeSampler biomeSampler;

    @Override
    public int seed() {
        return this.seed;
    }

    @Override
    public boolean edge() {
        return false;
    }

    @Override
    public TerrainLevels getLevels() {
        return this.getTerrain().getLevels();
    }

    @Override
    public TerrainData getTerrain() {
        return this.terrainData.join();
    }

    @Override
    public IBiomeSampler getClimateSampler() {
        return this.biomeSampler;
    }
}
