package com.terraforged.mod.hooks;

import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.DataRegistry;
import com.terraforged.mod.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class BuiltinHook {
    private static final MethodHandle REGISTRY_GETTER = ReflectionUtil.field(RegistryAccess.WritableRegistryAccess.class, Map.class, new String[0]);
    private static final MethodHandle READ_CACHE_GETTER = ReflectionUtil.field(RegistryLoader.class, Map.class, new String[0]);

    public static <T> void load(RegistryAccess.Writable writable, RegistryOps<T> ops) {
        if (ops.registryLoader().isEmpty()) {
            return;
        }
        Map<ResourceKey<?>, Registry<?>> backing = BuiltinHook.getBacking(writable);
        for (DataRegistry<?> registry : CommonAPI.get().getRegistryManager().getInjectedRegistries()) {
            backing.putIfAbsent((ResourceKey<?>)(registry.key().get()), (Registry<?>)registry.copy());
        }
        for (DataRegistry<?> registry : CommonAPI.get().getRegistryManager().getInjectedRegistries()) {
            BuiltinHook.loadRegistry(registry, ops);
        }
        BuiltinHook.reloadPresetRegistry(writable, ops);
    }

    private static <T, E> void loadRegistry(DataRegistry<T> registry, RegistryOps<E> ops) {
        try {
            RegistryLoader.Bound loader = (RegistryLoader.Bound)ops.registryLoader().orElseThrow();
            ResourceKey key = registry.key().get();
            TerraForged.LOG.debug("[BuiltinHook] Registry loaded via backing: {}", key);
        }
        catch (Throwable t) {
            TerraForged.LOG.warn("[BuiltinHook] Could not load registry: {}", t.getMessage());
        }
    }

    private static <T> void reloadPresetRegistry(RegistryAccess.Writable writable, RegistryOps<T> ops) {
    }

    private static Map<ResourceKey<?>, Registry<?>> getBacking(RegistryAccess.Writable writable) {
        try {
            return (Map<ResourceKey<?>, Registry<?>>) REGISTRY_GETTER.invoke(writable);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void clearReadCache(RegistryLoader loader, ResourceKey<?> registry) {
        try {
            Map<?, ?> map = (Map<?, ?>) READ_CACHE_GETTER.invoke(loader);
            map.remove(registry);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
