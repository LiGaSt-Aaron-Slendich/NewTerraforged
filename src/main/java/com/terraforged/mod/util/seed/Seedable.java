package com.terraforged.mod.util.seed;

import com.terraforged.cereal.spec.Context;

public interface Seedable<T> {
   T withSeed(long var1);

   static Context context(long seed) {
      Context context = new Context();
      context.getData().add("seed", seed);
      return context;
   }
}
