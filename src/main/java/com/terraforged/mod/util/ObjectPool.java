package com.terraforged.mod.util;

import com.terraforged.mod.Environment;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class ObjectPool<T> implements Consumer<T> {
   protected final int capacity;
   protected final int maxIndex;
   protected final Supplier<T> factory;
   protected final AtomicInteger size = new AtomicInteger();
   protected final Queue<T> pool = new ConcurrentLinkedDeque<>();
   protected final IntUnaryOperator takeOp;
   protected final IntUnaryOperator restoreOp;

   public ObjectPool(Supplier<T> factory) {
      this(Environment.CORES, factory);
   }

   public ObjectPool(int capacity, Supplier<T> factory) {
      this.factory = factory;
      this.capacity = capacity;
      this.size.set(capacity);
      this.maxIndex = capacity + 1;
      this.takeOp = i -> i > 0 ? i - 1 : -1;
      this.restoreOp = i -> i < this.capacity ? i + 1 : this.maxIndex;

      while (capacity-- > 0) {
         this.pool.offer(factory.get());
      }
   }

   public T take() {
      T t = this.pool.poll();
      if (t == null) {
         return this.factory.get();
      } else {
         this.size.decrementAndGet();
         return this.factory.get();
      }
   }

   public void restore(T value) {
      if (this.size.updateAndGet(this.restoreOp) < this.maxIndex) {
         this.pool.offer(value);
      }
   }

   @Override
   public void accept(T t) {
      this.restore(t);
   }

   @Override
   public String toString() {
      return "ObjectPool{pool=" + this.pool + "}";
   }

   public static <T> ObjectPool<T> forCacheSize(int cacheSize, Supplier<T> supplier) {
      int i = NoiseUtil.floor(cacheSize * 1.75F);
      return new ObjectPool<>(i, supplier);
   }
}
