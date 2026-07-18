package com.terraforged.mod.codec;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpecs;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.util.DataUtil;
import java.util.function.Supplier;

public record SuperCodec<V>(Class<V> type, Supplier<Void> validator) implements Codec<V> {
   private static final Supplier<Void> NOOP_VALIDATOR = () -> null;

   public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
      try {
         this.validator.get();
         JsonElement jsonelement = (JsonElement)ops.convertTo(JsonOps.INSTANCE, input);
         DataValue datavalue = DataUtil.toData(jsonelement);
         V v = Cereal.deserialize(datavalue.asObj(), this.type, Context.NONE);
         return DataResult.success(Pair.of(v, input));
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return DataResult.error(throwable.getMessage());
      }
   }

   public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
      try {
         this.validator.get();
         DataValue datavalue = Cereal.serialize(input, Context.NONE);
         JsonElement jsonelement = DataUtil.toJson(datavalue);
         T t = (T)JsonOps.INSTANCE.convertTo(ops, jsonelement);
         return DataResult.success(t);
      } catch (Throwable throwable) {
         throwable.printStackTrace();
         return DataResult.error(throwable.getMessage());
      }
   }

   private static Supplier<Void> getValidator(Class<?> type) {
      return Suppliers.memoize(() -> {
         if (DataSpecs.getSubSpec(type) == null) {
            throw new IllegalStateException("No sub-spec for type: " + type);
         } else {
            return null;
         }
      });
   }

   public static <T> SuperCodec<T> of(Class<T> type) {
      return new SuperCodec<>(type, getValidator(type));
   }

   public static <T> SuperCodec<T> withoutValidator(Class<T> type) {
      return new SuperCodec<>(type, NOOP_VALIDATOR);
   }
}
