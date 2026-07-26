package com.terraforged.mod.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;

public interface LazyCodec<V> extends Codec<V>, Supplier<Codec<V>> {
   Codec<V> get();

   <T> DataResult<T> encode(V var1, DynamicOps<T> var2, T var3);

   <T> DataResult<Pair<V, T>> decode(DynamicOps<T> var1, T var2);

   static <V> LazyCodec<V> of(Supplier<Codec<V>> supplier) {
      return new LazyCodec.Instance<>(Suppliers.memoize(supplier::get));
   }

   static <V> LazyCodec<V> record(Function<com.mojang.serialization.codecs.RecordCodecBuilder.Instance<V>, ? extends App<Mu<V>, V>> builder) {
      return new LazyCodec.Instance<>(Suppliers.memoize(() -> RecordCodecBuilder.create(builder)));
   }

   static <V> LazyCodec<Holder<V>> registry(Codec<V> codec, Supplier<ResourceKey<Registry<V>>> key) {
      return new LazyCodec.Instance<>(() -> RegistryFileCodec.create(key.get(), codec));
   }

   public record Instance<V>(Supplier<Codec<V>> supplier) implements LazyCodec<V> {
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
