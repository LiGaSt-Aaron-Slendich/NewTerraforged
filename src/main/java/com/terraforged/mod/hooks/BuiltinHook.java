package com.terraforged.mod.hooks;

import com.mojang.serialization.DataResult;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.Writable;
import net.minecraft.core.RegistryAccess.WritableRegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryLoader.Bound;

public class BuiltinHook {
   private static final MethodHandle REGISTRY_GETTER = ReflectionUtil.field(WritableRegistryAccess.class, Map.class);

   public static <T> void load(Writable writable, RegistryOps<T> ops) {
      if (!ops.registryLoader().isEmpty()) {
         Map<ResourceKey<?>, Registry<?>> map = getBacking(writable);

         for (ModRegistries.HolderEntry<?> holderentry : ModRegistries.getHolders()) {
            map.put(holderentry.key(), RegistryAccessUtil.copy((Registry<T>)holderentry.registry()));
         }

         for (ModRegistries.HolderEntry<?> holderentry1 : ModRegistries.getHolders()) {
            loadHolder(holderentry1, ops);
         }
      }
   }

   private static <T, E> void loadHolder(ModRegistries.HolderEntry<T> holder, RegistryOps<E> ops) {
      Bound bound = (Bound)ops.registryLoader().orElseThrow();
      DataResult<? extends Registry<T>> dataresult = bound.overrideRegistryFromResources(holder.key(), holder.direct(), ops.getAsJson());
      RegistryAccessUtil.printRegistryContents((Registry<?>)dataresult.result().orElseThrow());
   }

   private static Map<ResourceKey<?>, Registry<?>> getBacking(Writable writable) {
      try {
         return (Map)REGISTRY_GETTER.invokeExact((WritableRegistryAccess)((WritableRegistryAccess)writable));
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return Map.of();
      }
   }
}
