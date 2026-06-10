package com.terraforged.mod.data.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Codecs {
    public static <A> MapCodec<A> opt(String name, A defaultValue, Codec<A> codec) {
        return Codec.optionalField((String)name, codec).xmap(o -> o.orElse(defaultValue), a -> Optional.ofNullable(a));
    }

    public static <V> JsonElement encode(V v, Codec<V> codec) {
        return Codecs.encode(v, codec, (DynamicOps<JsonElement>)JsonOps.INSTANCE);
    }

    public static <V> JsonElement encode(V v, Codec<V> codec, DynamicOps<JsonElement> ops) {
        return (JsonElement)codec.encodeStart(ops, v).result().filter(j -> j.isJsonObject()).map(j -> j.getAsJsonObject()).orElseThrow();
    }

    public static <V> V modify(V v, Codec<V> codec, UnaryOperator<JsonObject> modifier) {
        JsonElement json = Codecs.encode(v, codec);
        if (json == null) {
            return v;
        }
        JsonObject result = (JsonObject)modifier.apply(json.getAsJsonObject());
        return codec.decode(JsonOps.INSTANCE, result).result().map(Pair::getFirst).orElse(v);
    }
}
