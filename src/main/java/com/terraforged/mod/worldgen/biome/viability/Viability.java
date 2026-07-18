package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.Arrays;

public interface Viability {
   Viability NONE = (x, z, ctx) -> 1.0F;

   float getFitness(int var1, int var2, Viability.Context var3);

   default float getScaler(TerrainLevels levels) {
      return levels.maxY / 255.0F;
   }

   default Viability mult(Viability... others) {
      Viability[] aviability = Arrays.copyOf(others, others.length + 1);
      aviability[others.length] = this;
      return new MultViability(aviability);
   }

   static float getFallOff(float value, float max) {
      return value < max ? 1.0F - value / max : 0.0F;
   }

   static float getFallOff(float value, float min, float mid, float max) {
      if (value < min) {
         return 0.0F;
      } else if (value < mid) {
         return (value - min) / (mid - min);
      } else {
         return value < max ? 1.0F - (value - mid) / (max - mid) : 0.0F;
      }
   }

   public interface Context {
      boolean edge();

      TerrainLevels getLevels();

      TerrainData getTerrain();

      IBiomeSampler getClimateSampler();
   }
}
