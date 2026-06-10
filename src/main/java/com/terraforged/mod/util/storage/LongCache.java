package com.terraforged.mod.util.storage;

public interface LongCache<T> {
    public T computeIfAbsent(int var1, long var2, SeededKeyFunction<T> var4);

    public static interface SeededKeyFunction<T> {
        public T apply(int var1, long var2);
    }
}
