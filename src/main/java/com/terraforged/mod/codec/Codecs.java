package com.terraforged.mod.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Codecs {
   public static <A> MapCodec<A> opt(String name, A defaultValue, Codec<A> codec) {
      return Codec.optionalField(name, codec).xmap(o -> o.orElse(defaultValue), a -> Optional.ofNullable(a));
   }

   public static <V> JsonElement encode(V v, Codec<V> codec) {
      return codec.encodeStart(JsonOps.INSTANCE, v).result().filter(JsonElement::isJsonObject).<JsonElement>map(JsonElement::getAsJsonObject).orElse(null);
   }

   public static <V> V modify(V v, Codec<V> codec, UnaryOperator<JsonObject> modifier) {
      JsonElement jsonelement = encode(v, codec);
      if (jsonelement == null) {
         return v;
      } else {
         JsonObject jsonobject = modifier.apply(jsonelement.getAsJsonObject());
         return codec.decode(JsonOps.INSTANCE, jsonobject).result().<V>map(Pair::getFirst).orElse(v);
      }
   }
}
