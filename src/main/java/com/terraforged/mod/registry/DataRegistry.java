package com.terraforged.mod.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.hooks.RegistryAccessUtil;
import com.terraforged.mod.registry.key.RegistryKey;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

public class DataRegistry<T>
implements Iterable<Map.Entry<ResourceKey<T>, T>> {
    protected final MappedRegistry<T> registry;
    protected final RegistryKey<T> key;
    protected final Codec<T> codec;

    public DataRegistry(RegistryKey<T> key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
        this.registry = new MappedRegistry(key.get(), Lifecycle.stable(), null);
    }

    public Codec<T> codec() {
        return this.codec;
    }

    public RegistryKey<T> key() {
        return this.key;
    }

    public void register(ResourceKey<T> key, T value) {
        this.registry.register(key, value, Lifecycle.stable());
    }

    @Override
    @NotNull
    public Iterator<Map.Entry<ResourceKey<T>, T>> iterator() {
        return this.registry.entrySet().iterator();
    }

    public Stream<Map.Entry<ResourceKey<T>, T>> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public MappedRegistry<T> copy() {
        return RegistryAccessUtil.copy(this.registry);
    }
}
