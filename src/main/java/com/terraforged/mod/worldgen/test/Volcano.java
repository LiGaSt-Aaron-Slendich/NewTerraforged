package com.terraforged.mod.worldgen.test;

import java.util.Arrays;
import net.minecraft.util.Mth;

public class Volcano {
   public static int toHeightValue(double height) {
      return 64 + Mth.floor(height * 0.5);
   }

   public static Volcano.Value getHighest(int x, int z, VolcanoConfig config, Volcano.Cache cache) {
      Volcano.Value volcano$value = cache.value0.reset();
      Volcano.Value volcano$value1 = cache.value1.reset();

      for (int i = 0; i < cache.size(); i++) {
         Volcano.Point volcano$point = cache.at(i);
         if (volcano$point.valid()) {
            evalPoint(x, z, volcano$point, volcano$value1.reset(), config);
            boolean flag = volcano$value1.height > volcano$value.height;
            if (volcano$value.mouth ? volcano$value1.mouth && flag : volcano$value1.mouth || flag) {
               volcano$value.height = volcano$value1.height;
               volcano$value.mouth = volcano$value1.mouth;
               volcano$value.hash = volcano$point.hash;
            }
         }
      }

      return volcano$value;
   }

   private static void evalPoint(int x, int y, Volcano.Point point, Volcano.Value value, VolcanoConfig config) {
      double d0 = getDistanceNoise(x, y, point, value, config);
      if (!Double.isNaN(d0)) {
         double d1 = 0.0;
         double d2 = config.height1().get(Volcano.Noise.rand(point.hash, 31643));
         if (value.mouth) {
            d1 = config.height0().get(Volcano.Noise.rand(point.hash, 30047));
         } else {
            d0 *= d0;
         }

         double d3 = Mth.lerp(d0, d1, d2);
         value.height = toHeightValue(d3);
      }
   }

   private static double getDistanceNoise(int x, int y, Volcano.Point point, Volcano.Value value, VolcanoConfig config) {
      double d0 = Mth.lengthSquared(point.x - x, point.y - y);
      double d1 = config.radius2().get(Volcano.Noise.rand(point.hash, 26921));
      if (d0 >= d1 * d1) {
         return Double.NaN;
      } else {
         double d2 = 1.0;
         double d3 = config.radius1().get(Volcano.Noise.rand(point.hash, 21701));
         if (d0 < d3 * d3) {
            value.mouth = true;
            double d4 = config.radius0().get(Volcano.Noise.rand(point.hash, 18899));
            return d0 <= d4 * d4 ? 0.0 : (Math.sqrt(d0) - d4) / (d3 - d4);
         } else {
            return d2 - (Math.sqrt(d0) - d3) / (d1 - d3);
         }
      }
   }

   public static <T> void collectPoints(
      long seed, int chunkX, int chunkZ, T context, VolcanoConfig config, Volcano.Cache cache, Volcano.VolcanoPredicate<T> filter
   ) {
      int i = chunkX << 4;
      int j = chunkZ << 4;
      int k = i + 15;
      int l = j + 15;
      double d0 = 1.0 / config.scale();
      double d1 = i * d0;
      double d2 = j * d0;
      double d3 = k * d0;
      double d4 = l * d0;
      int i1 = Mth.floor(d1) - 1;
      int j1 = Mth.floor(d2) - 1;
      int k1 = Mth.floor(d3) + 1;
      int l1 = Mth.floor(d4) + 1;
      collectPoints(seed, i1, j1, k1, l1, d0, context, config, cache, filter);
   }

   private static <T> void collectPoints(
      long seed,
      int minX,
      int minY,
      int maxX,
      int maxY,
      double frequency,
      T context,
      VolcanoConfig config,
      Volcano.Cache cache,
      Volcano.VolcanoPredicate<T> filter
   ) {
      for (int i = minY; i <= maxY; i++) {
         for (int j = minX; j <= maxX; j++) {
            long k = Volcano.Noise.mix(seed, j, i);
            if (!(Volcano.Noise.rand(k, 6869) > config.density())) {
               double d0 = point(k, 12343, j, config.jitter());
               double d1 = point(k, 16477, i, config.jitter());
               int l = Mth.floor(d0 / frequency);
               int i1 = Mth.floor(d1 / frequency);
               if (filter.test(l, i1, context)) {
                  Volcano.Point volcano$point = cache.next();
                  volcano$point.x = l;
                  volcano$point.y = i1;
                  volcano$point.hash = k;
               }
            }
         }
      }
   }

   private static double point(long hash, int hashOffset, int cell, double jitter) {
      return cell + Volcano.Noise.rand(hash, hashOffset) * jitter;
   }

   public static class Cache {
      protected int size;
      protected Volcano.Point[] points = new Volcano.Point[9];
      protected final Volcano.Value value0 = new Volcano.Value();
      protected final Volcano.Value value1 = new Volcano.Value();

      public Cache() {
         for (int i = 0; i < this.points.length; i++) {
            this.points[i] = new Volcano.Point();
         }
      }

      public int size() {
         return this.size;
      }

      public Volcano.Cache reset() {
         this.size = 0;
         return this;
      }

      public Volcano.Point at(int index) {
         return this.points[index];
      }

      public Volcano.Point next() {
         int i = this.size;
         this.ensure(i);
         this.size++;
         return this.points[i].reset();
      }

      protected void ensure(int index) {
         if (index >= this.points.length) {
            int i = this.points.length;
            int j = i << 1;
            this.points = Arrays.copyOf(this.points, j);

            for (int k = i; k < j; k++) {
               this.points[k] = new Volcano.Point();
            }
         }
      }
   }

   public interface Noise {
      int DENSITY = 6869;
      int POINT_X = 12343;
      int POINT_Y = 16477;
      int RADIUS_0 = 18899;
      int RADIUS_1 = 21701;
      int RADIUS_2 = 26921;
      int HEIGHT_0 = 30047;
      int HEIGHT_1 = 31643;
      int HEIGHT_2 = 33199;
      int FLUID_FILLER = 39761;

      static long mixGamma(long z) {
         z = (z ^ z >>> 33) * -49064778989728563L;
         z = (z ^ z >>> 33) * -4265267296055464877L;
         z = z ^ z >>> 33 | 1L;
         int i = Long.bitCount(z ^ z >>> 1);
         return i < 24 ? z ^ -6148914691236517206L : z;
      }

      static long mix(long z) {
         z = (z ^ z >>> 30) * -4658895280553007687L;
         z = (z ^ z >>> 27) * -7723592293110705685L;
         return z ^ z >>> 31;
      }

      static long mix(long seed, long offset) {
         return mix(seed + mixGamma(offset));
      }

      static long mix(long seed, int x, int y) {
         long i = mix(seed, x);
         return mix(i, y);
      }

      static double rand(long hash, int offset) {
         return rand(mix(hash, offset));
      }

      static double rand(long hash) {
         return (hash >>> 11) * 1.110223E-16F;
      }
   }

   public static class Point {
      public long hash = Long.MAX_VALUE;
      public int x = Integer.MAX_VALUE;
      public int y = Integer.MAX_VALUE;

      public boolean valid() {
         return this.hash != Long.MAX_VALUE && this.x != Integer.MAX_VALUE && this.y != Integer.MAX_VALUE;
      }

      public Volcano.Point reset() {
         this.hash = Long.MAX_VALUE;
         this.x = Integer.MAX_VALUE;
         this.y = Integer.MAX_VALUE;
         return this;
      }
   }

   public static class Value {
      public long hash = 0L;
      public int height = 0;
      public boolean mouth = false;

      public Volcano.Value reset() {
         this.hash = 0L;
         this.height = 0;
         this.mouth = false;
         return this;
      }
   }

   public interface VolcanoPredicate<T> {
      boolean test(int var1, int var2, T var3);
   }
}
