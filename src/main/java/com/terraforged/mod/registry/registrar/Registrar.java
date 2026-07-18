package com.terraforged.mod.registry.registrar;

import net.minecraft.resources.ResourceKey;

public interface Registrar<T> {
   void register(ResourceKey<T> var1, T var2);
}
