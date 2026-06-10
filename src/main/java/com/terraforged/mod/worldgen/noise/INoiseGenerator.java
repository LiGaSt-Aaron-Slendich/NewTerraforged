package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.function.Consumer;

public interface INoiseGenerator {
    public NoiseLevels getLevels();

    public TerrainLevels getTerrainLevels();

    public IContinentNoise getContinent();

    public NoiseSample getNoiseSample(int var1, int var2, int var3);

    public void sample(int var1, int var2, int var3, NoiseSample var4);

    public float getHeightNoise(int var1, int var2, int var3);

    public long find(int var1, int var2, int var3, int var4, int var5, Terrain var6);

    public void generate(int var1, int var2, int var3, Consumer<NoiseData> var4);

    public INoiseGenerator with(long var1, TerrainLevels var3);

    default public float getNoiseCoord(int coord) {
        return (float)coord * this.getLevels().frequency;
    }
}
