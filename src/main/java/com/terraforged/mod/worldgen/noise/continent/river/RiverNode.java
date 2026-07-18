package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.source.Line;
import com.terraforged.noise.util.NoiseUtil;

public record RiverNode(float ax, float ay, float bx, float by, float ah, float bh, float ar, float br, float displacement) {
   public float getProjection(float x, float y) {
      float f = this.bx - this.ax;
      float f1 = this.by - this.ay;
      float f2 = (x - this.ax) * f + (y - this.ay) * f1;
      f2 /= f * f + f1 * f1;
      return f2 < 0.0F ? 0.0F : (f2 > 1.0F ? 1.0F : f2);
   }

   public float getDistance2(float x, float y, float t) {
      float f = 0.05F;
      float f1 = NoiseUtil.map(t, f, 1.0F - f, 1.0F - f * 2.0F);
      f1 = f1 < 0.5F ? f1 / 0.5F : (1.0F - f1) / 0.5F;
      f1 = Interpolation.CURVE3.apply(f1);
      f1 *= this.displacement;
      float f2 = this.getX(t);
      float f3 = this.getY(t);
      float f4 = f2 - (this.by - this.ay) * f1;
      float f5 = f3 + (this.bx - this.ax) * f1;
      return Line.dist2(x, y, f4, f5);
   }

   public float getDistance(float x, float y, float t) {
      float f = this.getDistance2(x, y, t);
      return NoiseUtil.sqrt(f);
   }

   public float getDistance(float t, float d2) {
      float f = this.getRadius(t);
      return d2 >= f * f ? 0.0F : 1.0F - NoiseUtil.sqrt(d2) / f;
   }

   public float getX(float t) {
      return this.ax + t * (this.bx - this.ax);
   }

   public float getY(float t) {
      return this.ay + t * (this.by - this.ay);
   }

   public float getHeight(float t) {
      return this.ah + t * (this.bh - this.ah);
   }

   public float getRadius(float t) {
      return this.ar + t * (this.br - this.ar);
   }
}
