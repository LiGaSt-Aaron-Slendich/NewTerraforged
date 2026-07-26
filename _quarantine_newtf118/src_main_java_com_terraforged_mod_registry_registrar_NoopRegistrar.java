package com.terraforged.mod.registry.registrar;

import net.minecraft.resources.ResourceKey;

public class NoopRegistrar<T> implements Registrar<T> {
   private static final NoopRegistrar<?> REGISTRAR = new NoopRegistrar();

   @Override
   public void register(ResourceKey<T> key, T value) {
   }

   public static <T> Registrar<T> none() {
      return (Registrar<T>)REGISTRAR;
   }
}
