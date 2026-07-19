package com.terraforged.mod.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.terraforged.mod.hooks.RegistryAccessUtil;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;

public interface WorldGenCodec<V> extends Codec<V> {
   MapCodec<RegistryAccess> CODEC = new MapCodec<RegistryAccess>() {
      public <T> Stream<T> keys(DynamicOps<T> ops) {
         return Stream.empty();
      }

      public <T> DataResult<RegistryAccess> decode(DynamicOps<T> ops, MapLike<T> input) {
         Optional<RegistryAccess> optional = RegistryAccessUtil.getRegistryAccess(ops);
         return optional.isEmpty() ? DataResult.error("Invalid ops") : DataResult.success(optional.get());
      }

      public <T> RecordBuilder<T> encode(RegistryAccess input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
         // Keep seed/levels already written into prefix. A fresh MapBuilder dropped them and
         // made CreateWorldScreen WorldGenSettings round-trip encode to null → datapack validation fail.
         return prefix;
      }
   };

   <T> V decode(DynamicOps<T> var1, T var2, RegistryAccess var3);

   <T> T encode(V var1, DynamicOps<T> var2);

   default <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
      Optional<RegistryAccess> optional = RegistryAccessUtil.getRegistryAccess(ops);
      if (optional.isEmpty()) {
         return DataResult.error("Invalid ops");
      } else {
         V v = this.decode(ops, input, optional.get());
         return DataResult.success(Pair.of(v, input));
      }
   }

   default <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
      return DataResult.success(this.encode(input, ops));
   }
}
