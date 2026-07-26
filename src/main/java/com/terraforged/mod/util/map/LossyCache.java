package com.terraforged.mod.util.map;

import com.terraforged.mod.Environment;
import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.util.Mth;

public class LossyCache<T> implements LongCache<T> {
   protected final long[] keys;
   protected final T[] values;
   protected final int mask;
   protected final Consumer<T> removalListener;

   private LossyCache(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
      capacity = Mth.smallestEncompassingPowerOfTwo(capacity);
      this.mask = capacity - 1;
      this.keys = new long[capacity];
      this.values = (T[])((Object[])constructor.apply(capacity));
      this.removalListener = removalListener;
      Arrays.fill(this.keys, Long.MIN_VALUE);
   }

   @Override
   public T computeIfAbsent(long key, Long2ObjectFunction<T> function) {
      int i = hash(key);
      int j = i & this.mask;
      T t = this.values[j];
      if (this.keys[j] == key && t != null) {
         return t;
      } else {
         T t1 = (T)function.apply(key);
         this.keys[j] = key;
         this.values[j] = t1;
         this.onRemove(t);
         return t1;
      }
   }

   protected void onRemove(T value) {
      if (value != null) {
         this.removalListener.accept(value);
      }
   }

   protected static int hash(long l) {
      return (int)HashCommon.mix(l);
   }

   public static <T> LongCache<T> of(int capacity, IntFunction<T[]> constructor) {
      return of(capacity, constructor, t -> {});
   }

   public static <T> LongCache<T> of(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
      return new LossyCache<>(capacity, constructor, removalListener);
   }

   public static <T> LongCache<T> concurrent(int capacity, IntFunction<T[]> constructor) {
      return concurrent(capacity, constructor, t -> {});
   }

   public static <T> LongCache<T> concurrent(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
      return concurrent(capacity, Environment.CORES, constructor, removalListener);
   }

   public static <T> LongCache<T> concurrent(int capacity, int concurrency, IntFunction<T[]> constructor, Consumer<T> removalListener) {
      return new LossyCache.Concurrent<>(capacity, concurrency, constructor, removalListener);
   }

   public static class Concurrent<T> implements LongCache<T> {
      protected static final int HASH_BITS = Integer.MAX_VALUE;
      protected final int mask;
      protected final LossyCache.Stamped<T>[] buckets;

      public Concurrent(int capacity, int concurrency, IntFunction<T[]> constructor, Consumer<T> removalListener) {
         concurrency = Mth.smallestEncompassingPowerOfTwo(concurrency);
         capacity = NoiseUtil.floor((float)capacity / concurrency);
         this.mask = concurrency - 1;
         this.buckets = new LossyCache.Stamped[concurrency];

         for (int i = 0; i < concurrency; i++) {
            this.buckets[i] = new LossyCache.Stamped<>(capacity, constructor, removalListener);
         }
      }

      @Override
      public T computeIfAbsent(long key, Long2ObjectFunction<T> function) {
         return this.buckets[this.index(key)].computeIfAbsent(key, function);
      }

      protected int index(long key) {
         return spread(key) & this.mask;
      }

      protected static int spread(long h) {
         return (int)(h ^ h >>> 16) & 2147483647;
      }
   }

   public static class Stamped<T> extends LossyCache<T> {
      protected final StampedLock lock = new StampedLock();

      public Stamped(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
         super(capacity, constructor, removalListener);
      }

      @Override
      public T computeIfAbsent(long key, Long2ObjectFunction<T> function) {
         int i = hash(key);
         int j = i & this.mask;
         long k = this.lock.tryOptimisticRead();
         long l = this.keys[j];
         T t = this.values[j];
         if (!this.lock.validate(k)) {
            long i1 = this.lock.readLock();

            try {
               l = this.keys[j];
               t = this.values[j];
            } finally {
               this.lock.unlockRead(i1);
            }
         }

         return l == key && t != null ? t : this.write(key, j, t, function);
      }

      protected T write(long key, int index, T currentValue, Long2ObjectFunction<T> function) {
         long i = this.lock.writeLock();

         Object object;
         try {
            T t = (T)function.apply(key);
            this.keys[index] = key;
            this.values[index] = t;
            object = t;
         } finally {
            this.lock.unlockWrite(i);
            this.onRemove(currentValue);
         }

         return (T)object;
      }
   }
}
