package com.terraforged.mod.platform.forge.util;

import com.terraforged.mod.registry.registrar.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public record ForgeRegistrar<T extends IForgeRegistryEntry<T>>(IForgeRegistry<T> registry) implements Registrar<T> {
   public void register(ResourceKey<T> key, T value) {
      if (value.getRegistryName() == null) {
         value.setRegistryName(key.location());
      }

      this.registry.register(value);
   }
}
