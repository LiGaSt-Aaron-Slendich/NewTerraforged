package com.terraforged.mod.util;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Init {
   private final AtomicBoolean lock = new AtomicBoolean(false);

   public final void init() {
      if (this.lock.compareAndSet(false, true)) {
         this.doInit();
      }
   }

   public final boolean isDone() {
      return this.lock.get();
   }

   protected abstract void doInit();
}
