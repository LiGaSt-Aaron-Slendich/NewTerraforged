package com.terraforged.mod.codec;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.spec.DataSpecs;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.util.DataUtil;
import java.util.function.Consumer;

public record SpecCodec<V>(DataSpec<V> spec) implements Codec<V> {
   public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
      try {
         JsonElement jsonelement = (JsonElement)ops.convertTo(JsonOps.INSTANCE, input);
         DataValue datavalue = DataUtil.toData(jsonelement);
         V v = this.spec.deserialize(datavalue.asObj());
         return DataResult.success(Pair.of(v, input));
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return DataResult.error(throwable.getMessage());
      }
   }

   public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
      try {
         DataValue datavalue = this.spec.serialize(input);
         JsonElement jsonelement = DataUtil.toJson(datavalue);
         T t = (T)JsonOps.INSTANCE.convertTo(ops, jsonelement);
         return DataResult.success(t);
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return DataResult.error(throwable.getMessage());
      }
   }

   public static <T> Codec<T> of(String name) {
      return of((DataSpec<T>)DataSpecs.getSpec(name));
   }

   public static <T> Codec<T> of(DataSpec<T> spec) {
      return new SpecCodec<>(spec);
   }

   public static <T> Codec<T> create(Class<T> type, DataFactory<T> factory, Consumer<DataSpec.Builder<T>> consumer) {
      DataSpec.Builder<T> builder = DataSpec.builder(type, factory);
      consumer.accept(builder);
      return of(builder.build());
   }

   public static <T> Codec<T> create(String name, Class<T> type, DataFactory<T> factory, Consumer<DataSpec.Builder<T>> consumer) {
      DataSpec.Builder<T> builder = DataSpec.builder(name, type, factory);
      consumer.accept(builder);
      return of(builder.build());
   }
}
