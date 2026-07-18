package com.terraforged.mod.worldgen.noise.erosion;

import com.terraforged.engine.settings.FilterSettings;
import com.terraforged.engine.util.FastRandom;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

public class ErosionFilter {
   private static final float HEIGHT_FALL_OFF = 0.4F;
   private static final int HEIGHT = 0;
   private static final int GRAD_X = 1;
   private static final int GRAD_Y = 2;
   private static final int erosionRadius = 7;
   private static final float inertia = 0.005F;
   private static final float sedimentCapacityFactor = 7.0F;
   private static final float minSedimentCapacity = 0.008F;
   private static final float evaporateSpeed = 0.35F;
   private static final float gravity = 2.5F;
   private final float erodeSpeed;
   private final float depositSpeed;
   private final float initialSpeed;
   private final float initialWaterVolume;
   private final int maxDropletLifetime;
   private final int[][] erosionBrushIndices;
   private final float[][] erosionBrushWeights;
   private final int seed;
   private final int iterations;

   public ErosionFilter(int seed, int mapSize, FilterSettings.Erosion settings) {
      this.seed = seed;
      this.iterations = settings.dropletsPerChunk;
      this.erodeSpeed = settings.erosionRate;
      this.depositSpeed = settings.depositeRate;
      this.initialSpeed = settings.dropletVelocity;
      this.initialWaterVolume = settings.dropletVolume;
      this.maxDropletLifetime = settings.dropletLifetime;
      this.erosionBrushIndices = new int[mapSize * mapSize][];
      this.erosionBrushWeights = new float[mapSize * mapSize][];
      this.initBrushes(mapSize, 7);
   }

   public void apply(float[] map, int chunkX, int chunkZ, NoiseTileSize size, ErosionFilter.Resource resource, FastRandom random) {
      int i = size.regionLength - 2;

      for (int j = 0; j < this.iterations; j++) {
         long k = NoiseUtil.seed(this.seed, j);

         for (int l = size.chunkMin; l < size.chunkMax; l++) {
            int i1 = l - size.chunkMin << 4;

            for (int j1 = size.chunkMin; j1 < size.chunkMax; j1++) {
               int k1 = j1 - size.chunkMin << 4;
               long l1 = NoiseUtil.seed(chunkX + j1, chunkZ + l);
               random.seed(l1, k);
               int i2 = k1 + random.nextInt(16);
               int j2 = i1 + random.nextInt(16);
               i2 = MathUtil.clamp(i2, 1, i);
               j2 = MathUtil.clamp(j2, 1, i);
               this.applyDrop(i2, j2, map, size.regionLength, resource);
            }
         }
      }
   }

   private void applyDrop(float posX, float posY, float[] map, int mapSize, ErosionFilter.Resource resource) {
      float f = 0.0F;
      float f1 = 0.0F;
      float f2 = 0.0F;
      float f3 = this.initialSpeed;
      float f4 = this.initialWaterVolume;

      for (int i = 0; i < this.maxDropletLifetime; i++) {
         int j = (int)posX;
         int k = (int)posY;
         int l = k * mapSize + j;
         float f5 = posX - j;
         float f6 = posY - k;
         float[] afloat = this.grad(map, mapSize, posX, posY, resource.grad1);
         f = f * 0.005F - afloat[1] * 0.995F;
         f1 = f1 * 0.005F - afloat[2] * 0.995F;
         float f7 = f * f + f1 * f1;
         if (f7 == 0.0F) {
            return;
         }

         float f8 = NoiseUtil.sqrt(f7);
         f /= f8;
         f1 /= f8;
         posX += f;
         posY += f1;
         if (f == 0.0F && f1 == 0.0F || posX < 0.0F || posX >= mapSize - 1 || posY < 0.0F || posY >= mapSize - 1) {
            return;
         }

         float f9 = getFalloff(map[l]);
         float f10 = this.grad(map, mapSize, posX, posY, resource.grad2)[0];
         float f11 = (f10 - afloat[0]) * f9;
         float f12 = Math.max(-f11 * f3 * f4 * 7.0F, 0.008F);
         if (!(f2 > f12) && !(f11 > 0.0F)) {
            float f16 = Math.min((f12 - f2) * this.erodeSpeed, -f11);

            for (int i1 = 0; i1 < this.erosionBrushIndices[l].length; i1++) {
               int j1 = this.erosionBrushIndices[l][i1];
               float f14 = f16 * this.erosionBrushWeights[l][i1];
               float f15 = Math.min(map[j1], f14);
               map[j1] -= f15;
               f2 += f15;
            }
         } else {
            float f13 = f11 > 0.0F ? Math.min(f11, f2) : (f2 - f12) * this.depositSpeed;
            f2 -= f13;
            map[l] += f13 * (1.0F - f5) * (1.0F - f6);
            map[l + 1] = map[l + 1] + f13 * f5 * (1.0F - f6);
            map[l + mapSize] = map[l + mapSize] + f13 * (1.0F - f5) * f6;
            map[l + mapSize + 1] = map[l + mapSize + 1] + f13 * f5 * f6;
         }

         float f17 = f3 * f3 + f11 * 2.5F;
         if (f17 <= 0.0F) {
            return;
         }

         f3 = NoiseUtil.sqrt(f17);
         f4 *= 0.65F;
      }
   }

   private void initBrushes(int size, int radius) {
      int[] aint = new int[radius * radius * 4];
      int[] aint1 = new int[radius * radius * 4];
      float[] afloat = new float[radius * radius * 4];
      float f = 0.0F;
      int i = 0;

      for (int j = 0; j < this.erosionBrushIndices.length; j++) {
         int k = j % size;
         int l = j / size;
         if (l <= radius || l >= size - radius || k <= radius + 1 || k >= size - radius) {
            f = 0.0F;
            i = 0;

            for (int i1 = -radius; i1 <= radius; i1++) {
               for (int j1 = -radius; j1 <= radius; j1++) {
                  float f1 = j1 * j1 + i1 * i1;
                  if (f1 < radius * radius) {
                     int k1 = k + j1;
                     int l1 = l + i1;
                     if (k1 >= 0 && k1 < size && l1 >= 0 && l1 < size) {
                        float f2 = 1.0F - (float)Math.sqrt(f1) / radius;
                        f += f2;
                        afloat[i] = f2;
                        aint[i] = j1;
                        aint1[i] = i1;
                        i++;
                     }
                  }
               }
            }
         }

         int i2 = i;
         this.erosionBrushIndices[j] = new int[i];
         this.erosionBrushWeights[j] = new float[i];

         for (int j2 = 0; j2 < i2; j2++) {
            this.erosionBrushIndices[j][j2] = (aint1[j2] + l) * size + aint[j2] + k;
            this.erosionBrushWeights[j][j2] = afloat[j2] / f;
         }
      }
   }

   private float[] grad(float[] nodes, int mapSize, float posX, float posY, float[] resource) {
      int i = (int)posX;
      int j = (int)posY;
      float f = posX - i;
      float f1 = posY - j;
      int k = j * mapSize + i;
      float f2 = nodes[k];
      float f3 = nodes[k + 1];
      float f4 = nodes[k + mapSize];
      float f5 = nodes[k + mapSize + 1];
      resource[0] = f2 * (1.0F - f) * (1.0F - f1) + f3 * f * (1.0F - f1) + f4 * (1.0F - f) * f1 + f5 * f * f1;
      resource[1] = (f3 - f2) * (1.0F - f1) + (f5 - f4) * f1;
      resource[2] = (f4 - f2) * (1.0F - f) + (f5 - f3) * f;
      return resource;
   }

   private static float getFalloff(float height) {
      return height >= 0.4F ? 1.0F : height / 0.4F;
   }

   public static class Resource {
      public final float[] grad1 = new float[3];
      public final float[] grad2 = new float[3];
   }
}
