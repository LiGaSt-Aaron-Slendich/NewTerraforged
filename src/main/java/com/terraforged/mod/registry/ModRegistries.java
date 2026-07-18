package com.terraforged.mod.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.lazy.LazyRegistry;
import com.terraforged.mod.util.Init;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ModRegistries extends Init {
   public static final ModRegistries INSTANCE = new ModRegistries();
   private final List<ModRegistries.HolderEntry<?>> holders = new ArrayList<>();
   private final Map<ResourceKey<? extends Registry<?>>, MappedRegistry<?>> registries = new IdentityHashMap<>();

   @Override
   protected void doInit() {
   }

   public static void commit() {
      INSTANCE.init();
   }

   public static List<ModRegistries.HolderEntry<?>> getHolders() {
      return INSTANCE.holders;
   }

   public static <T> void createRegistry(LazyRegistry<T> key, Codec<T> codec) {
      createRegistry(key.get(), codec);
   }

   public static <T> void createRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
      if (INSTANCE.isDone()) {
         TerraForged.LOG.warn("Attempted to register extension after init: {}", key);
      } else {
         MappedRegistry<T> mappedregistry = new MappedRegistry(key, Lifecycle.stable(), null);
         ModRegistries.HolderEntry<T> holderentry = new ModRegistries.HolderEntry<>(mappedregistry, codec);
         INSTANCE.holders.add(holderentry);
         INSTANCE.registries.put(key, mappedregistry);
      }
   }

   public static <T> ResourceKey<T> register(LazyRegistry<T> registryKey, String name, T value) {
      return register(registryKey.get(), name, value);
   }

   public static <T> ResourceKey<T> register(ResourceKey<Registry<T>> registryKey, String name, T value) {
      MappedRegistry<T> mappedregistry = getRegistry(registryKey);
      ResourceKey<T> resourcekey = ResourceKey.create(registryKey, TerraForged.location(name));
      mappedregistry.register(resourcekey, value, Lifecycle.stable());
      return resourcekey;
   }

   public static <T> Holder<T> holder(ResourceKey<Registry<T>> registry, String name) {
      ResourceLocation resourcelocation = TerraForged.location(name);
      ResourceKey<T> resourcekey = ResourceKey.create(registry, resourcelocation);
      return Reference.createStandAlone(getRegistry(registry), resourcekey);
   }

   private static <T> T getEntry(ResourceKey<Registry<T>> registry, ResourceLocation name) {
      T t = (T)getRegistry(registry).get(name);
      if (t == null) {
         throw new Error("Missing entry: " + name + ", Registry: " + registry.location());
      } else {
         return t;
      }
   }

   public static <T> MappedRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
      MappedRegistry<?> mappedregistry = INSTANCE.registries.get(key);
      if (mappedregistry == null) {
         throw new Error("Missing registry: " + key);
      } else {
         return (MappedRegistry<T>)mappedregistry;
      }
   }

   public record HolderEntry<T>(Registry<T> registry, Codec<T> direct) {
      public ResourceKey<? extends Registry<T>> key() {
         return this.registry.key();
      }
   }
}
