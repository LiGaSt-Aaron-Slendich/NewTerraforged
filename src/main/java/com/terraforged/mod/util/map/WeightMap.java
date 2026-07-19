package com.terraforged.mod.util.map;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import java.util.function.Predicate;

public class WeightMap<T> {
   protected final T[] values;
   protected final float[] weights;
   protected final float sumWeight;
   protected final float zeroWeight;

   public WeightMap(T[] values, float[] weights) {
      this.values = values;
      this.weights = getCumulativeWeights(values.length, weights);
      this.zeroWeight = weights.length > 0 ? weights[0] : 0.0F;
      this.sumWeight = MathUtil.sum(weights) * 0.99999F;
   }

   public boolean isEmpty() {
      return this.values.length == 0;
   }

   public T[] getValues() {
      return this.values;
   }

   public T getValue(float noise) {
      if (this.values.length == 0) {
         return null;
      }
      noise *= this.sumWeight;
      if (noise < this.zeroWeight) {
         return this.values[0];
      } else {
         for (int i = 1; i < this.weights.length; i++) {
            if (noise < this.weights[i]) {
               T value = this.values[i];
               return value != null ? value : this.values[0];
            }
         }

         // Never fall through to null — empty/edge noise must still yield a palette entry.
         T last = this.values[this.values.length - 1];
         return last != null ? last : this.values[0];
      }
   }

   public T find(Predicate<T> predicate) {
      for (T t : this.values) {
         if (predicate.test(t)) {
            return t;
         }
      }

      return null;
   }

   public long getBand(T value) {
      float f = 0.0F;

      for (int i = 0; i < this.values.length; i++) {
         float f1 = this.weights[i];
         if (this.values[i] == value) {
            return PosUtil.packf(f / this.sumWeight, f1 / this.sumWeight);
         }

         f = f1;
      }

      return 0L;
   }

   public static <T extends WeightMap.Weighted> WeightMap<T> of(T[] values) {
      float[] afloat = new float[values.length];

      for (int i = 0; i < afloat.length; i++) {
         afloat[i] = values[i].weight();
      }

      return new WeightMap<>(values, afloat);
   }

   private static float[] getCumulativeWeights(int len, float[] weights) {
      float[] afloat = new float[len];
      float f = 0.0F;

      for (int i = 0; i < len; i++) {
         f += i < weights.length ? weights[i] : 1.0F;
         afloat[i] = f;
      }

      return afloat;
   }

   public interface Weighted {
      float weight();
   }
}
