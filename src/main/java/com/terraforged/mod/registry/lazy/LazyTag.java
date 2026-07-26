package com.terraforged.mod.registry.lazy;

import com.terraforged.mod.TerraForged;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class LazyTag<T> extends LazyValue<TagKey<T>> {
   protected final Supplier<ResourceKey<? extends Registry<T>>> registry;

   public LazyTag(Supplier<ResourceKey<? extends Registry<T>>> registry, ResourceLocation name) {
      super(name);
      this.registry = registry;
   }

   protected TagKey<T> compute() {
      return TagKey.create(this.registry.get(), this.name);
   }

   public static LazyTag<Biome> biome(String name) {
      return new LazyTag<>(() -> Registry.BIOME_REGISTRY, TerraForged.location(name));
   }

   public static <T> LazyTag<T> of(TagKey<T> tagKey) {
      LazyTag<T> lazytag = new LazyTag<>(tagKey::registry, tagKey.location());
      lazytag.set(tagKey);
      return lazytag;
   }
}
