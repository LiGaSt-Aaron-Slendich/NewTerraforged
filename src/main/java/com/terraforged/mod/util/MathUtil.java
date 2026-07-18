package com.terraforged.mod.util;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.noise.source.Line;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.geom.Line2D;

public class MathUtil {
   public static final float EPSILON = 0.99999F;

   public static int clamp(int value, int min, int max) {
      return value < min ? min : (value > max ? max : value);
   }

   public static float clamp(float value, float min, float max) {
      return value < min ? min : (value > max ? max : value);
   }

   public static float map(float value, float min0, float max0, float min1, float max1) {
      float f = NoiseUtil.map(value, min0, max0, max0 - min0);
      return NoiseUtil.lerp(min1, max1, f);
   }

   public static int hash(int seed, int x) {
      return NoiseUtil.hash(seed, x);
   }

   public static int hash(int seed, int x, int z) {
      return NoiseUtil.hash2D(seed, x, z);
   }

   public static float getPosX(int hash, int cx, float jitter) {
      float f = rand(hash, 1619);
      return cx + f * jitter;
   }

   public static float getPosY(int hash, int cy, float jitter) {
      float f = rand(hash, 31337);
      return cy + f * jitter;
   }

   public static float getPosX(int hash, int cx, float ox, float jitter) {
      float f = rand(hash, 1619);
      return cx + ox + f * jitter;
   }

   public static float getPosY(int hash, int cy, float oy, float jitter) {
      float f = rand(hash, 31337);
      return cy + oy + f * jitter;
   }

   public static float randX(int hash) {
      return rand(hash, 1619);
   }

   public static float randZ(int hash) {
      return rand(hash, 31337);
   }

   public static float rand(int seed, int offset) {
      return rand(hash(seed, offset));
   }

   public static float rand(int seed, int x, int y) {
      return rand(hash(seed, x, y));
   }

   public static float rand(int n) {
      n ^= 1619;
      n ^= 31337;
      float f = n * n * n * 60493 / 2.1474836E9F;
      return NoiseUtil.map(f, -1.0F, 1.0F, 2.0F);
   }

   public static float sum(float[] values) {
      float f = 0.0F;

      for (float f1 : values) {
         f += f1;
      }

      return f;
   }

   protected static float getLineDistance(float x, float y, float ax, float ay, float bx, float by, float ar, float br) {
      float f = bx - ax;
      float f1 = by - ay;
      float f2 = (x - ax) * f + (y - ay) * f1;
      f2 /= f * f + f1 * f1;
      f2 = f2 < 0.0F ? 0.0F : (f2 > 1.0F ? 1.0F : f2);
      float f3 = ax + f * f2;
      float f4 = ay + f1 * f2;
      float f5 = ar + (br - ar) * f2;
      float f6 = f5 * f5;
      float f7 = Line.dist2(x, y, f3, f4);
      return f7 < f6 ? 1.0F - NoiseUtil.sqrt(f7) / f5 : 0.0F;
   }

   public static long getIntersection(float x, float y, float ax, float ay, float bx, float by) {
      float f = bx - ax;
      float f1 = by - ay;
      float f2 = (x - ax) * f + (y - ay) * f1;
      f2 /= f * f + f1 * f1;
      float f3 = ax + f * f2;
      float f4 = ay + f1 * f2;
      return PosUtil.packf(f3, f4);
   }

   public static long getIntersection(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
      float f = by - ay;
      float f1 = ax - bx;
      float f2 = dy - cy;
      float f3 = cx - dx;
      float f4 = f * f3 - f2 * f1;
      if (f4 == 0.0F) {
         return Long.MAX_VALUE;
      } else {
         float f5 = f * ax + f1 * ay;
         float f6 = f2 * cx + f3 * cy;
         float f7 = (f3 * f5 - f1 * f6) / f4;
         float f8 = (f * f6 - f2 * f5) / f4;
         return PosUtil.packf(f7, f8);
      }
   }

   public static boolean intersects(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
      return Line2D.linesIntersect(ax, ay, bx, by, cx, cy, dx, dy);
   }
}
