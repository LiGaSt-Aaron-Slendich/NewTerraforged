package com.terraforged.mod.platform;

import com.terraforged.mod.registry.registrar.Registrar;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface Platform {
   AtomicReference<Platform> ACTIVE_PLATFORM = new AtomicReference<>();

   Path getContainer();

   <T> Registrar<T> getRegistrar(ResourceKey<Registry<T>> var1);
}
