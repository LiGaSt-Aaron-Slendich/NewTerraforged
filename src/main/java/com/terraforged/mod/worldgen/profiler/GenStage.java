package com.terraforged.mod.worldgen.profiler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class GenStage {
   private static final double NANO_TO_MS = 1.0 / TimeUnit.MILLISECONDS.toNanos(1L);
   private final AtomicLong time = new AtomicLong();
   private final AtomicInteger count = new AtomicInteger();
   private final String name;

   public GenStage(String name) {
      this.name = name;
   }

   public String name() {
      return this.name;
   }

   public void reset() {
      this.time.set(0L);
      this.count.set(0);
   }

   public GenTimer start() {
      return new GenTimer(this, System.nanoTime());
   }

   public void push(long duration) {
      this.time.addAndGet(duration);
      this.count.incrementAndGet();
   }

   public double getAverageNanos() {
      double d0 = (double)this.time.get() / this.count.get();
      return Double.isNaN(d0) ? 0.0 : d0;
   }

   public double getAverageMS() {
      return this.getAverageNanos() * NANO_TO_MS;
   }
}
