package com.terraforged.mod.hooks;

import com.mojang.serialization.DynamicOps;
import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.RegistryAccess.Writable;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class RegistryAccessUtil {
   private static final MethodHandle REGISTRY_ACCESS_GETTER = ReflectionUtil.field(RegistryOps.class, RegistryAccess.class);

   public static Optional<RegistryAccess> getRegistryAccess(DynamicOps<?> ops) {
      if (!(ops instanceof RegistryOps)) {
         return Optional.empty();
      } else {
         try {
            return Optional.ofNullable(getRegistryAccess((RegistryOps<?>)ops));
         } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Optional.empty();
         }
      }
   }

   public static RegistryAccess getRegistryAccess(RegistryOps<?> ops) {
      try {
         return (RegistryAccess)REGISTRY_ACCESS_GETTER.invokeExact((RegistryOps)ops);
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return null;
      }
   }

   public static <T> MappedRegistry<T> copy(Registry<T> input) {
      MappedRegistry<T> mappedregistry = new MappedRegistry(input.key(), input.lifecycle(), null);

      for (Entry<ResourceKey<T>, T> entry : input.entrySet()) {
         mappedregistry.register(entry.getKey(), entry.getValue(), input.lifecycle(entry.getValue()));
      }

      return mappedregistry;
   }

   public static <T> void copy(Registry<T> registry, Writable holder) {
      WritableRegistry<T> writableregistry = (WritableRegistry<T>)holder.ownedRegistryOrThrow(registry.key());

      for (Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
         writableregistry.register(entry.getKey(), entry.getValue(), registry.lifecycle(entry.getValue()));
      }
   }

   public static void printRegistryContents(Registry<?> registry) {
      if (Environment.DEBUGGING) {
         TerraForged.LOG.info(" - Registry: {}, Size: {}", registry.key().location(), registry.size());

         for (Entry<? extends ResourceKey<?>, ?> entry : registry.entrySet()) {
            TerraForged.LOG.info("  - {}", entry.getKey().location());
         }
      }
   }
}
