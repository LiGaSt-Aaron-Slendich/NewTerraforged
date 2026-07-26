package com.terraforged.mod.registry.lazy;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.ModRegistries;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class LazyRegistry<T> extends LazyValue<ResourceKey<Registry<T>>> {
   public LazyRegistry(ResourceLocation name) {
      super(name);
   }

   public LazyKey<T> element(String name) {
      return new LazyKey<>(this, TerraForged.location(name));
   }

   protected ResourceKey<Registry<T>> compute() {
      return ResourceKey.createRegistryKey(this.name);
   }

   protected Supplier<Registry<T>> registry(RegistryAccess access) {
      return access == null ? () -> ModRegistries.<T>getRegistry(this.get()) : () -> access.registryOrThrow(this.get());
   }
}
