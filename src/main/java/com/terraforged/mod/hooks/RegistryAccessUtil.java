package com.terraforged.mod.hooks;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class RegistryAccessUtil {
    private static final MethodHandle REGISTRY_ACCESS_GETTER = ReflectionUtil.field(RegistryOps.class, RegistryAccess.class, new String[0]);

    public static Optional<RegistryAccess> getRegistryAccess(DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(RegistryAccessUtil.getRegistryAccess((RegistryOps)ops));
        }
        catch (Throwable t) {
            t.printStackTrace();
            return Optional.empty();
        }
    }

    public static RegistryAccess getRegistryAccess(RegistryOps<?> ops) {
        try {
            return (RegistryAccess) REGISTRY_ACCESS_GETTER.invoke(ops);
        }
        catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> MappedRegistry<T> copy(Registry<T> input) {
        MappedRegistry copy = new MappedRegistry(input.key(), input.lifecycle(), null);
        for (Object value : input) {
            ResourceKey<T> key = input.getResourceKey((T) value).orElseThrow();
            Lifecycle lifecycle = input.lifecycle((T) value);
            copy.register(key, (T) value, lifecycle);
        }
        return copy;
    }

    public static <T> void copy(Registry<T> registry, RegistryAccess.Writable holder) {
        WritableRegistry dest = (WritableRegistry)holder.ownedRegistryOrThrow(registry.key());
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            dest.register(entry.getKey(), entry.getValue(), registry.lifecycle(entry.getValue()));
        }
    }

    public static void printRegistryContents(Registry<?> registry) {
        if (!Environment.DEBUGGING) {
            return;
        }
        TerraForged.LOG.info(" - Registry: {}, Size: {}", registry.key().location(), registry.size());
        for (var entry : registry.entrySet()) {
            TerraForged.LOG.info("  - {}", entry.getKey().location());
        }
    }
}
