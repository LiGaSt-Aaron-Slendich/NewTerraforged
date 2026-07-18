package com.terraforged.mod.util;

import com.terraforged.engine.util.pos.PosUtil;

public class SpiralIterator {
   private final int x;
   private final int z;
   private final int minRadius;
   private final int maxRadius;
   private int i = -1;
   private int radius;
   private int length;
   private int maxIndex;

   public SpiralIterator(int x, int z, int radius, int maxRadius) {
      this.x = x;
      this.z = z;
      this.minRadius = radius;
      this.maxRadius = maxRadius;
      this.setRadius(radius);
   }

   public void reset() {
      this.i = -1;
      this.setRadius(this.minRadius);
   }

   public boolean hasNext() {
      return this.i + 1 < this.maxIndex || this.radius < this.maxRadius;
   }

   public long next() {
      this.nextIndex();
      int i = this.i / this.length;
      int j = this.i % this.length;
      int k = -this.radius;
      int l = -this.radius;
      switch (i) {
         case 0:
            k = -this.radius + j;
            break;
         case 1:
            k = this.radius;
            l = -this.radius + j;
            break;
         case 2:
            k = this.radius - j;
            l = this.radius;
            break;
         case 3:
            l = this.radius - j;
      }

      return PosUtil.pack(this.x + k, this.z + l);
   }

   public SpiralIterator.PositionFinder finder(SpiralIterator.Object2Long<SpiralIterator> function) {
      return new SpiralIterator.PositionFinder(function);
   }

   private void nextIndex() {
      if (this.radius <= this.maxRadius) {
         if (++this.i >= this.maxIndex) {
            this.setRadius(this.radius + 1);
            this.i = 0;
         }
      }
   }

   private void setRadius(int radius) {
      int i = 1 + radius * 2;
      this.radius = radius;
      this.length = i - 1;
      this.maxIndex = (i - 1) * 4;
   }

   public interface Object2Long<T> {
      long apply(T var1);
   }

   public class PositionFinder {
      private final SpiralIterator.Object2Long<SpiralIterator> function;

      public PositionFinder(SpiralIterator.Object2Long<SpiralIterator> function) {
         this.function = function;
      }

      public boolean hasNext() {
         return SpiralIterator.this.hasNext();
      }

      public long next() {
         return !this.hasNext() ? 0L : this.function.apply(SpiralIterator.this);
      }
   }
}
