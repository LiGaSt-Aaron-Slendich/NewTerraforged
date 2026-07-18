package com.terraforged.mod.worldgen.noise.continent.cell;

import com.terraforged.mod.util.MathUtil;

public enum CellShape {
   SQUARE,
   HEXAGON {
      @Override
      public float adjustY(float y) {
         return y * 1.2F;
      }

      @Override
      public float getCellX(int hash, int cx, int cy, float jitter) {
         float f = (cy & 1) * 0.5F;
         float f1 = f > 0.0F ? jitter * 0.5F : jitter;
         return MathUtil.getPosX(hash, cx, f1) + f;
      }
   };

   public float adjustX(float x) {
      return x;
   }

   public float adjustY(float y) {
      return y;
   }

   public float getCellX(int hash, int cx, int cy, float jitter) {
      return MathUtil.getPosX(hash, cx, jitter);
   }

   public float getCellY(int hash, int cx, int cy, float jitter) {
      return MathUtil.getPosY(hash, cy, jitter);
   }
}
