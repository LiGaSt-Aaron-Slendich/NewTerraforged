package com.terraforged.mod.registry.lazy;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public abstract class LazyValue<T> implements Supplier<T> {
   protected final ResourceLocation name;
   protected volatile T value;

   protected LazyValue(ResourceLocation name) {
      this.name = name;
   }

   @Override
   public T get() {
      T t = this.value;
      if (t != null) {
         return t;
      } else {
         synchronized (this.name) {
            t = this.value;
            if (t == null) {
               t = this.compute();
               this.value = t;
            }

            return t;
         }
      }
   }

   protected void set(T value) {
      if (this.value == null) {
         this.value = value;
      }
   }

   protected abstract T compute();
}
