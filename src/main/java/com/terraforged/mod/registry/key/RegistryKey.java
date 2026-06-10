package com.terraforged.mod.registry.key;

import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.key.EntryKey;
import com.terraforged.mod.registry.lazy.LazyHolder;
import com.terraforged.mod.registry.lazy.LazyValue;
import java.util.Comparator;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class RegistryKey<T>
extends LazyValue<ResourceKey<Registry<T>>> {
    public RegistryKey(ResourceLocation name) {
        super(name);
    }

    public EntryKey<T> entryKey(String name) {
        return new EntryKey(this, TerraForged.location(name));
    }

    public Holder<T> holder(String name, RegistryAccess access, Supplier<T> defaultSupplier) {
        EntryKey<T> key = this.entryKey(name);
        if (access == null) {
            return new LazyHolder<T>(defaultSupplier.get(), key);
        }
        return access.ownedRegistryOrThrow(this.get()).getHolderOrThrow(key.get());
    }

    public T[] entries(RegistryAccess access, IntFunction<T[]> arrayFunc) {
        if (access == null) {
            return RegistryKey.toSortedArray(CommonAPI.get().getRegistryManager().getRegistry(this).stream(), arrayFunc);
        }
        return RegistryKey.toSortedArray(access.ownedRegistryOrThrow(this.get()).entrySet().stream(), arrayFunc);
    }

    @Override
    protected ResourceKey<Registry<T>> compute() {
        return ResourceKey.createRegistryKey((ResourceLocation)this.name);
    }

    public String toString() {
        return "RegistryKey{" + this.name + "}";
    }

    private static <T> T[] toSortedArray(Stream<Map.Entry<ResourceKey<T>, T>> stream, IntFunction<T[]> arrayFunc) {
        return stream.sorted(Comparator.comparing(e -> (e.getKey()).location())).map(Map.Entry::getValue).toArray(arrayFunc);
    }
}
