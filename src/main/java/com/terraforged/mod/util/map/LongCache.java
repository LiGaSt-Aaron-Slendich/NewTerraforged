package com.terraforged.mod.util.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;

public interface LongCache<T> {
   T computeIfAbsent(long var1, Long2ObjectFunction<T> var3);
}
