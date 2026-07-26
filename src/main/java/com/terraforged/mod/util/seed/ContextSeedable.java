package com.terraforged.mod.util.seed;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.value.DataValue;

public interface ContextSeedable<T> extends Seedable<T> {
   default <V> V withSeed(long seed, V value, Class<V> type) {
      try {
         DataValue datavalue = Cereal.serialize(value);
         Context context = new Context();
         context.getData().add("seed", seed);
         return Cereal.deserialize(datavalue.asObj(), type, context);
      } catch (Throwable throwable) {
         throw new Error("Failed to reseed value: " + value, throwable);
      }
   }
}
