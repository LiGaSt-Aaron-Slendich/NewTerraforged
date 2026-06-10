package com.terraforged.mod.registry.key;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.lazy.LazyValue;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class EntryKey<T>
extends LazyValue<ResourceKey<T>> {
    protected final Supplier<ResourceKey<Registry<T>>> registry;

    protected EntryKey(Supplier<ResourceKey<Registry<T>>> registry, ResourceLocation name) {
        super(name);
        this.registry = registry;
    }

    @Override
    protected ResourceKey<T> compute() {
        return ResourceKey.create(this.registry.get(), (ResourceLocation)this.name);
    }

    public static <T> EntryKey<T> of(ResourceKey<Registry<T>> key, String elementName) {
        ResourceLocation name = TerraForged.location(elementName);
        return new EntryKey<T>(() -> key, name);
    }
}
