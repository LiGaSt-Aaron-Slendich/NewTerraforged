package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;

public class TerrainGenerator {
   protected final TerrainLevels levels;
   protected final INoiseGenerator noiseGenerator;
   protected final ObjectPool<TerrainData> terrainDataPool;

   public TerrainGenerator(TerrainLevels levels, INoiseGenerator noiseGenerator) {
      this.levels = levels;
      this.noiseGenerator = noiseGenerator;
      this.terrainDataPool = new ObjectPool<>(() -> new TerrainData(this.levels));
   }

   public INoiseGenerator getNoiseGenerator() {
      return this.noiseGenerator;
   }

   public void restore(TerrainData terrainData) {
      this.terrainDataPool.restore(terrainData);
   }

   public TerrainData generate(int chunkX, int chunkZ) {
      TerrainData terraindata = this.terrainDataPool.take();
      this.noiseGenerator.generate(chunkX, chunkZ, terraindata);
      return terraindata;
   }

   public int getHeight(int x, int z) {
      float f = this.noiseGenerator.getHeightNoise(x, z);
      float f1 = this.levels.getScaledHeight(f);
      return this.levels.getHeight(f1);
   }
}
