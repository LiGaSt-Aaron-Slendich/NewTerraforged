package com.terraforged.mod;

import com.google.common.base.Suppliers;
import com.terraforged.mod.platform.Platform;
import com.terraforged.mod.registry.lazy.LazyRegistry;
import com.terraforged.mod.registry.registrar.BuiltinRegistrar;
import com.terraforged.mod.registry.registrar.Registrar;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TerraForged implements Platform {
   public static final String MODID = "newterraforged";
   public static final String TITLE = "NewTerraforged";
   public static final String DATAPACK_VERSION = "v0.1";
   public static final Logger LOG = LogManager.getLogger("NewTerraforged");
   private final Supplier<Path> container;
   private final Map<ResourceKey<? extends Registry<?>>, Registrar<?>> registrars = new HashMap<>();

   protected TerraForged(Supplier<Path> containerGetter) {
      this.container = Suppliers.memoize(containerGetter::get);
      Platform.ACTIVE_PLATFORM.set(this);
      Environment.log();
      this.registrars.put(Registry.BIOME_REGISTRY, new BuiltinRegistrar(Registry.BIOME_REGISTRY));
   }

   @Override
   public final Path getContainer() {
      return this.container.get();
   }

   @Override
   public <T> Registrar<T> getRegistrar(ResourceKey<Registry<T>> key) {
      Registrar<?> registrar = this.registrars.get(key);
      Objects.requireNonNull(registrar);
      return (Registrar<T>)registrar;
   }

   protected <T> void setRegistrar(ResourceKey<? extends Registry<T>> registry, Registrar<T> registrar) {
      this.registrars.put(registry, registrar);
   }

   public static Platform getPlatform() {
      return Platform.ACTIVE_PLATFORM.get();
   }

   public static ResourceLocation location(String name) {
      // Worldgen datapack on this TF118 base stays under terraforged/; Forge modId is newterraforged.
      return name.contains(":") ? new ResourceLocation(name) : new ResourceLocation("terraforged", name);
   }

   public static <T> LazyRegistry<T> registry(String name) {
      return new LazyRegistry<>(location(name));
   }
}
