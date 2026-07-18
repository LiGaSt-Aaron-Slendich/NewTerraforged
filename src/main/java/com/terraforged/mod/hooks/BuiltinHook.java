package com.terraforged.mod.hooks;

import com.mojang.serialization.DataResult;
import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.registry.DataRegistry;
import com.terraforged.mod.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

/**
 * Restored to official TerraForged 1.18.2-0.3.1-alpha-2 flow:
 * inject holder copies into WritableRegistryAccess, then override from datapack resources.
 */
public class BuiltinHook {
    private static final MethodHandle REGISTRY_GETTER = ReflectionUtil.field(RegistryAccess.WritableRegistryAccess.class, Map.class);

    public static <T> void load(RegistryAccess.Writable writable, RegistryOps<T> ops) {
        if (ops.registryLoader().isEmpty()) {
            return;
        }
        Map<ResourceKey<?>, Registry<?>> backing = getBacking(writable);
        for (DataRegistry<?> registry : CommonAPI.get().getRegistryManager().getInjectedRegistries()) {
            backing.put(registry.key().get(), registry.copy());
        }
        for (DataRegistry<?> registry : CommonAPI.get().getRegistryManager().getInjectedRegistries()) {
            loadRegistry(registry, ops);
        }
    }

    private static <T, E> void loadRegistry(DataRegistry<T> registry, RegistryOps<E> ops) {
        RegistryLoader.Bound loader = ops.registryLoader().orElseThrow();
        DataResult<? extends Registry<T>> result = loader.overrideRegistryFromResources(
                registry.key().get(),
                registry.codec(),
                ops.getAsJson()
        );
        RegistryAccessUtil.printRegistryContents(result.result().orElseThrow());
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<?>, Registry<?>> getBacking(RegistryAccess.Writable writable) {
        try {
            return (Map<ResourceKey<?>, Registry<?>>) REGISTRY_GETTER.invokeExact((RegistryAccess.WritableRegistryAccess) writable);
        } catch (Throwable e) {
            e.printStackTrace();
            return Map.of();
        }
    }
}
