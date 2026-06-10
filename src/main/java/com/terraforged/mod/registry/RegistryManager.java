package com.terraforged.mod.registry;

import com.mojang.serialization.Codec;
import com.terraforged.mod.registry.DataRegistry;
import com.terraforged.mod.registry.key.RegistryKey;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;

public class RegistryManager {
    public static final RegistryManager DEFAULT = new RegistryManager();
    protected final List<DataRegistry<?>> injected = new ObjectArrayList();
    protected final Map<RegistryKey<?>, DataRegistry<?>> registries = new IdentityHashMap();

    public <T> void register(RegistryKey<T> registry, ResourceKey<T> key, T value) {
        this.getRegistry(registry).register(key, value);
    }

    public <T> void create(RegistryKey<T> registry) {
        this.create(registry, null, false);
    }

    public <T> void create(RegistryKey<T> registry, Codec<T> codec) {
        this.create(registry, codec, true);
    }

    public <T> void create(RegistryKey<T> key, Codec<T> codec, boolean injected) {
        DataRegistry<T> registry = new DataRegistry<T>(key, codec);
        this.registries.put(key, registry);
        if (injected) {
            this.injected.add(registry);
        }
    }

    public Iterable<DataRegistry<?>> getRegistries() {
        return this.registries.values();
    }

    public Iterable<DataRegistry<?>> getInjectedRegistries() {
        return this.injected;
    }

    public <T> DataRegistry<T> getRegistry(RegistryKey<T> key) {
        DataRegistry<?> registry = this.registries.get(key);
        return (DataRegistry<T>) Objects.requireNonNull(registry);
    }
}
