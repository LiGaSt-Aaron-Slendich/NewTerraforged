package com.terraforged.mod.registry.registrar;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;

public class BuiltinRegistrar<T> implements Registrar<T> {
   private final Supplier<Registry<T>> registry;

   public BuiltinRegistrar(ResourceKey<Registry<T>> registryKey) {
      this.registry = Suppliers.memoize(() -> (Registry)BuiltinRegistries.REGISTRY.get(registryKey.location()));
   }

   @Override
   public void register(ResourceKey<T> key, T value) {
      BuiltinRegistries.register(this.registry.get(), key, value);
   }
}
