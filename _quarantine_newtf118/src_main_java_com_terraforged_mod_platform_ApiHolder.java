package com.terraforged.mod.platform;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.Init;

public class ApiHolder<T> extends Init {
   private volatile T value;

   public ApiHolder(T defaultValue) {
      this.value = defaultValue;
   }

   @Override
   protected void doInit() {
      TerraForged.LOG.info("Set TerraForged API: {}", this.value.getClass().getSimpleName());
   }

   public void set(T value) {
      if (!this.isDone()) {
         this.value = value;
         this.init();
      }
   }

   T get() {
      return this.value;
   }
}
