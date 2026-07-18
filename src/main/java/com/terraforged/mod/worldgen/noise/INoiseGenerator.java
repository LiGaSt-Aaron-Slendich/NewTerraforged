package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.function.Consumer;

public interface INoiseGenerator {
   INoiseGenerator with(long var1, TerrainLevels var3);

   NoiseLevels getLevels();

   TerrainLevels getTerrainLevels();

   IContinentNoise getContinent();

   NoiseSample getNoiseSample(int var1, int var2);

   void sample(int var1, int var2, NoiseSample var3);

   float getHeightNoise(int var1, int var2);

   long find(int var1, int var2, int var3, int var4, Terrain var5);

   void generate(int var1, int var2, Consumer<NoiseData> var3);

   default float getNoiseCoord(int coord) {
      return coord * this.getLevels().frequency;
   }
}
