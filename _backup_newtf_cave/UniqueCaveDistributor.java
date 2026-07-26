package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.Module;
import com.terraforged.noise.util.N2DUtil;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class UniqueCaveDistributor implements Module {
   private final int seed;
   private final float frequency;
   private final float jitter;
   private final float density;

   public UniqueCaveDistributor(int seed, float frequency, float jitter, float density) {
      this.seed = seed;
      this.frequency = frequency;
      this.jitter = jitter;
      this.density = density;
   }

   @Override
   public float getValue(float x, float y) {
      x *= this.frequency;
      y *= this.frequency;
      int i = NoiseUtil.floor(x) + 1;
      int j = NoiseUtil.floor(y) + 1;
      int k = i - 1;
      int l = j - 1;
      float f = Float.POSITIVE_INFINITY;
      float f1 = Float.POSITIVE_INFINITY;

      for (int i1 = j - 2; i1 <= j; i1++) {
         float f2 = (i1 & 1) * 0.5F;

         for (int j1 = i - 2; j1 <= i; j1++) {
            Vec2f vec2f = NoiseUtil.cell(this.seed, j1, i1);
            float f3 = j1 + f2 + vec2f.x * this.jitter;
            float f4 = i1 + vec2f.y * this.jitter;
            float f5 = NoiseUtil.dist2(x, y, f3, f4);
            if (f5 < f) {
               f1 = f;
               f = f5;
               k = j1;
               l = i1;
            } else if (f5 < f1) {
               f1 = f5;
            }
         }
      }

      return this.cellValue(k, l) > this.density ? 0.0F : 1.0F - NoiseUtil.sqrt(f / f1);
   }

   private float cellValue(int cellX, int cellY) {
      float f = NoiseUtil.valCoord2D(this.seed, cellX, cellY);
      return (1.0F + f) * 0.5F;
   }

   public static void main(String[] args) {
      Module module = new UniqueCaveDistributor(123, 0.01F, 0.75F, 0.1F).clamp(0.2, 1.0).map(0.0, 1.0).warp(12312, 30, 1, 20.0);
      N2DUtil.display(1000, 1000, (N2DUtil.PixelShader<BufferedImage>) (x, y, img) -> Color.HSBtoRGB(0.0F, 0.0F, module.getValue(x, y))).setVisible(true);
   }
}
