package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.util.map.Object2FloatCache;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.util.seed.Seedable;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class TerrainBlender implements Module, Seedable<TerrainBlender> {
   private static final int REGION_SEED_OFFSET = 21491124;
   private static final int WARP_SEED_OFFSET = 12678;
   private final int regionSeed;
   private final int scale;
   private final float frequency;
   private final float jitter;
   private final float blending;
   private final Domain warp;
   private final WeightMap<TerrainNoise> terrains;
   private final ThreadLocal<TerrainBlender.Blender> localBlender = ThreadLocal.withInitial(TerrainBlender.Blender::new);

   public TerrainBlender(long seed, int scale, float jitter, float blending, TerrainNoise[] terrains) {
      this.regionSeed = (int)seed + 21491124;
      this.scale = scale;
      this.frequency = 1.0F / scale;
      this.jitter = jitter;
      this.blending = blending;
      this.terrains = WeightMap.of(terrains);
      this.warp = Domain.warp(Source.SIMPLEX, (int)seed + 12678, scale, 3, scale / 2.5F);
   }

   public TerrainBlender withSeed(long seed) {
      TerrainNoise[] aterrainnoise = this.terrains.getValues();
      TerrainNoise[] aterrainnoise1 = new TerrainNoise[aterrainnoise.length];

      for (int i = 0; i < aterrainnoise1.length; i++) {
         aterrainnoise1[i] = aterrainnoise[i].withSeed(seed);
      }

      return new TerrainBlender(seed, this.scale, this.jitter, this.blending, aterrainnoise1);
   }

   @Override
   public float getValue(float x, float z) {
      TerrainBlender.Blender terrainblender$blender = this.localBlender.get();
      return this.getValue(x, z, terrainblender$blender);
   }

   public float getValue(float x, float z, TerrainBlender.Blender blender) {
      float f = this.warp.getX(x, z) * this.frequency;
      float f1 = this.warp.getY(x, z) * this.frequency;
      getCell(this.regionSeed, f, f1, this.jitter, blender);
      return blender.getValue(x, z, this.blending, this.terrains);
   }

   public TerrainBlender.Blender getBlenderResource() {
      return this.localBlender.get();
   }

   public Terrain getTerrain(TerrainBlender.Blender blender) {
      float f = blender.getCentreNoiseIndex();
      return this.terrains.getValue(f).terrain();
   }

   public SpiralIterator.PositionFinder findNearest(float x, float z, int minRadius, int maxRadius, Terrain type) {
      TerrainNoise terrainnoise = this.terrains.find(t -> t.terrain().getName().equals(type.getName()));
      if (terrainnoise == null) {
         return null;
      } else {
         long i = this.terrains.getBand(terrainnoise);
         float f = PosUtil.unpackLeftf(i);
         float f1 = PosUtil.unpackRightf(i);
         return this.iterator(x, z, minRadius, maxRadius).finder(it -> {
            long j = find(this.regionSeed, this.jitter, f, f1, it);
            float f2 = PosUtil.unpackLeftf(j) / this.frequency;
            float f3 = PosUtil.unpackRightf(j) / this.frequency;
            return PosUtil.packf(f2, f3);
         });
      }
   }

   public SpiralIterator iterator(float x, float z, int min, int max) {
      float f = this.warp.getX(x, z) * this.frequency;
      float f1 = this.warp.getY(x, z) * this.frequency;
      int i = NoiseUtil.floor(f);
      int j = NoiseUtil.floor(f1);
      return new SpiralIterator(i, j, min, max);
   }

   private static long find(int seed, float jitter, float lower, float upper, SpiralIterator iterator) {
      while (iterator.hasNext()) {
         long i = iterator.next();
         int j = PosUtil.unpackLeft(i);
         int k = PosUtil.unpackRight(i);
         int l = NoiseUtil.hash2D(seed, j, k);
         float f = MathUtil.rand(l);
         if (f > lower && f <= upper) {
            float f1 = MathUtil.rand(l, 1619);
            float f2 = MathUtil.rand(l, 31337);
            float f3 = j + f1 * jitter;
            float f4 = k + f2 * jitter;
            return PosUtil.packf(f3, f4);
         }
      }

      return 0L;
   }

   private static void getCell(int seed, float x, float z, float jitter, TerrainBlender.Blender blender) {
      int i = NoiseUtil.floor(x) + 1;
      int j = NoiseUtil.floor(z) + 1;
      blender.closestIndex = 0;
      blender.closestIndex2 = 0;
      int k = -1;
      int l = -1;
      float f = Float.MAX_VALUE;
      float f1 = Float.MAX_VALUE;
      int i1 = j - 2;

      for (int j1 = 0; i1 <= j; i1++) {
         for (int k1 = i - 2; k1 <= i; j1++) {
            int l1 = NoiseUtil.hash2D(seed, k1, i1);
            float f2 = MathUtil.rand(l1, 1619);
            float f3 = MathUtil.rand(l1, 31337);
            float f4 = k1 + f2 * jitter;
            float f5 = i1 + f3 * jitter;
            float f6 = NoiseUtil.dist2(x, z, f4, f5);
            blender.hashes[j1] = l1;
            blender.distances[j1] = f6;
            if (f6 < f) {
               f1 = f;
               f = f6;
               l = k;
               k = j1;
            } else if (f6 < f1) {
               f1 = f6;
               l = j1;
            }

            k1++;
         }
      }

      blender.closestIndex = k;
      blender.closestIndex2 = l;
   }

   public static class Blender {
      protected int closestIndex;
      protected int closestIndex2;
      protected final int[] hashes = new int[9];
      protected final float[] distances = new float[9];
      protected final Object2FloatCache<TerrainNoise> cache = new Object2FloatCache<>(9);

      public float getCentreNoiseIndex() {
         return this.getNoiseIndex(this.closestIndex);
      }

      public float getDistance(int index) {
         return NoiseUtil.sqrt(this.distances[index]);
      }

      public float getCentreValue(float x, float z, WeightMap<TerrainNoise> terrains) {
         float f = this.getCentreNoiseIndex();
         return terrains.getValue(f).noise().getValue(x, z);
      }

      public float getValue(float x, float z, float blending, WeightMap<TerrainNoise> terrains) {
         float f = this.getDistance(this.closestIndex);
         float f1 = this.getDistance(this.closestIndex2);
         float f2 = (f + f1) * 0.5F;
         float f3 = f2 * blending;
         float f4 = f2 - f3;
         return f <= f4 ? this.getCentreValue(x, z, terrains) : this.getBlendedValue(x, z, f, f1, f3, terrains);
      }

      public float getBlendedValue(float x, float z, float nearest, float nearest2, float blendRange, WeightMap<TerrainNoise> terrains) {
         this.cache.clear();
         float f = this.getCacheValue(this.closestIndex, x, z, terrains);
         float f1 = getWeight(nearest, nearest, blendRange);
         float f2 = getWeight(nearest2, nearest, blendRange);
         if (f2 > 0.0F) {
            f += this.getCacheValue(this.closestIndex2, x, z, terrains) * f2;
            f1 += f2;
         }

         for (int i = 0; i < 9; i++) {
            if (i != this.closestIndex && i != this.closestIndex2) {
               float f3 = getWeight(this.getDistance(i), nearest, blendRange);
               if (f3 > 0.0F) {
                  f += this.getCacheValue(i, x, z, terrains) * f3;
                  f1 += f3;
               }
            }
         }

         return NoiseUtil.clamp(f / f1, 0.0F, 1.0F);
      }

      private float getCacheValue(int index, float x, float z, WeightMap<TerrainNoise> terrains) {
         float f = this.getNoiseIndex(index);
         TerrainNoise terrainnoise = terrains.getValue(f);
         float f1 = this.cache.get(terrainnoise);
         if (Float.isNaN(f1)) {
            f1 = terrainnoise.noise().getValue(x, z);
            this.cache.put(terrainnoise, f1);
         }

         return f1;
      }

      private float getNoiseIndex(int index) {
         return MathUtil.rand(this.hashes[index]);
      }

      private static float getWeight(float dist, float origin, float blendRange) {
         float f = dist - origin;
         if (f <= 0.0F) {
            return 1.0F;
         } else if (f >= blendRange) {
            return 0.0F;
         } else {
            float f1 = 1.0F - f / blendRange;
            return f1 * f1;
         }
      }
   }
}
