package com.terraforged.mod.data.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;

public interface LazyCodec<V>
extends Codec<V>,
Supplier<Codec<V>> {
    @Override
    public Codec<V> get();

    public <T> DataResult<T> encode(V var1, DynamicOps<T> var2, T var3);

    public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> var1, T var2);

    public static <V> LazyCodec<V> of(Supplier<Codec<V>> supplier) {
        return new Instance(Suppliers.memoize(supplier::get));
    }

    public static <V> LazyCodec<V> record(Function<RecordCodecBuilder.Instance<V>, ? extends App<RecordCodecBuilder.Mu<V>, V>> builder) {
        return new Instance(Suppliers.memoize(() -> RecordCodecBuilder.create((Function)builder)));
    }

    public static <V> LazyCodec<Holder<V>> registry(Codec<V> codec, Supplier<ResourceKey<Registry<V>>> key) {
        return new Instance<Holder<V>>(() -> RegistryFileCodec.create((key.get()), (Codec)codec));
    }

    public record Instance<V>(Supplier<Codec<V>> supplier) implements LazyCodec<V>
    {
        @Override
        public Codec<V> get() {
            return this.supplier.get();
        }

        @Override
        public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
            return this.get().encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
            return this.get().decode(ops, input);
        }
    }
}
