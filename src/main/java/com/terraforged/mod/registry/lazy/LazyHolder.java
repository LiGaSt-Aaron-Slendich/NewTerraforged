package com.terraforged.mod.registry.lazy;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public record LazyHolder<T>(T value, Supplier<ResourceKey<T>> key) implements Holder<T>
{
    public boolean isBound() {
        return true;
    }

    public boolean is(ResourceLocation name) {
        return name.equals(this.key.get().location());
    }

    public boolean is(ResourceKey<T> key) {
        return key == this.key.get();
    }

    public boolean is(Predicate<ResourceKey<T>> test) {
        return test.test(this.key.get());
    }

    public boolean is(TagKey<T> tag) {
        return false;
    }

    public Stream<TagKey<T>> tags() {
        return Stream.empty();
    }

    public Either<ResourceKey<T>, T> unwrap() {
        return Either.left(this.key.get());
    }

    public Optional<ResourceKey<T>> unwrapKey() {
        return Optional.of(this.key.get());
    }

    public Holder.Kind kind() {
        return Holder.Kind.DIRECT;
    }

    public boolean isValidInRegistry(Registry<T> registry) {
        return registry.containsKey(this.key.get());
    }
}
