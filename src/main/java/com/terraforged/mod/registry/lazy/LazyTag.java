package com.terraforged.mod.registry.lazy;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.lazy.LazyValue;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class LazyTag<T>
extends LazyValue<TagKey<T>> {
    protected final Supplier<ResourceKey<? extends Registry<T>>> registry;

    public LazyTag(Supplier<ResourceKey<? extends Registry<T>>> registry, ResourceLocation name) {
        super(name);
        this.registry = registry;
    }

    @Override
    protected TagKey<T> compute() {
        return TagKey.create(this.registry.get(), (ResourceLocation)this.name);
    }

    public static LazyTag<Biome> biome(String name) {
        return new LazyTag<Biome>(() -> Registry.BIOME_REGISTRY, TerraForged.location(name));
    }

    public static <T> LazyTag<T> of(TagKey<T> tagKey) {
        LazyTag<T> lazy = new LazyTag<>(tagKey::registry, tagKey.location());
        lazy.set(tagKey);
        return lazy;
    }
}
