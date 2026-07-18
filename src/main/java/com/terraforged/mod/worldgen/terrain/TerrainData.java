package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.map.FloatMap;
import com.terraforged.mod.util.map.ObjectMap;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Consumer;

public class TerrainData implements Consumer<NoiseData> {
   protected final TerrainLevels levels;
   protected final FloatMap height = new FloatMap();
   protected final FloatMap gradient = new FloatMap();
   protected final FloatMap river = new FloatMap();
   protected final FloatMap baseHeight = new FloatMap();
   protected final ObjectMap<Terrain> terrain = new ObjectMap<>(Terrain[]::new);
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
      float f = this.height.get(x, z);
      return this.levels.getHeight(f);
   }

   public int getBaseHeight(int x, int z) {
      float f = this.baseHeight.get(x, z);
      return this.levels.getHeight(f);
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
      float f = this.getGradient().get(x, z);
      return NoiseUtil.clamp(f * norm, 0.0F, 1.0F);
   }

   public void accept(NoiseData noiseData) {
      FloatMap floatmap = noiseData.getBase();
      FloatMap floatmap1 = noiseData.getHeight();
      ObjectMap<Terrain> objectmap = noiseData.getTerrain();

      for (int i = 0; i < 16; i++) {
         for (int j = 0; j < 16; j++) {
            float f = floatmap1.get(j, i);
            float f1 = floatmap.get(j, i);
            float f2 = this.levels.getScaledHeight(f);
            float f3 = this.levels.getScaledBaseLevel(f1);
            Terrain terrain = objectmap.get(j, i);
            float f4 = floatmap1.get(j, i - 1);
            float f5 = floatmap1.get(j, i + 1);
            float f6 = floatmap1.get(j + 1, i);
            float f7 = floatmap1.get(j - 1, i);
            float f8 = f6 - f7;
            float f9 = f5 - f4;
            float f10 = NoiseUtil.sqrt(f8 * f8 + f9 * f9);
            float f11 = NoiseUtil.clamp(f10, 0.0F, 1.0F);
            this.gradient.set(j, i, f11);
            this.terrain.set(j, i, terrain);
            this.height.set(j, i, f2);
            this.baseHeight.set(j, i, f3);
            this.river.set(j, i, noiseData.getRiver().get(j, i));
            this.max = Math.max(this.max, f2);
            this.min = Math.min(this.min, f2);
            this.maxBase = Math.max(this.maxBase, f3);
         }
      }
   }
}
