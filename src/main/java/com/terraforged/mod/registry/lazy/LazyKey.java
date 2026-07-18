package com.terraforged.mod.registry.lazy;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class LazyKey<T> extends LazyValue<ResourceKey<T>> {
   protected final Supplier<ResourceKey<Registry<T>>> registry;

   protected LazyKey(Supplier<ResourceKey<Registry<T>>> registry, ResourceLocation name) {
      super(name);
      this.registry = registry;
   }

   protected ResourceKey<T> compute() {
      return ResourceKey.create(this.registry.get(), this.name);
   }
}
