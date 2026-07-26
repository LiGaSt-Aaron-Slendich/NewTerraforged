package com.terraforged.mod.registry.registrar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;

public class DelayedRegistrar<T> implements Registrar<T> {
   private final List<DelayedRegistrar.Entry<T>> entries = new ArrayList<>();

   @Override
   public void register(ResourceKey<T> key, T value) {
      this.entries.add(new DelayedRegistrar.Entry<>(key, value));
   }

   public void register(Registrar<T> delegate) {
      for (DelayedRegistrar.Entry<T> entry : this.entries) {
         delegate.register(entry.key, entry.value);
      }
   }

   protected record Entry<T>(ResourceKey<T> key, T value) {
   }
}
