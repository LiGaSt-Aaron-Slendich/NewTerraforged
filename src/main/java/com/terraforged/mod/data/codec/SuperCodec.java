package com.terraforged.mod.data.codec;

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
import com.terraforged.mod.data.util.DataUtil;
import java.util.function.Supplier;

public record SuperCodec<V>(Class<V> type, Supplier<Void> validator) implements Codec<V>
{
    private static final Supplier<Void> NOOP_VALIDATOR = () -> null;

    public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
        try {
            this.validator.get();
            JsonElement json = (JsonElement)ops.convertTo((DynamicOps)JsonOps.INSTANCE, input);
            DataValue data = DataUtil.toData(json);
            V result = Cereal.deserialize(data.asObj(), this.type, Context.NONE);
            return DataResult.success((Pair<V, T>) (Object) Pair.of(result, input));
        }
        catch (Throwable t) {
            t.printStackTrace();
            return DataResult.error((String)t.getMessage());
        }
    }

    public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
        try {
            this.validator.get();
            DataValue data = Cereal.serialize(input, Context.NONE);
            JsonElement json = DataUtil.toJson(data);
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
        }
        catch (Throwable t) {
            t.printStackTrace();
            return DataResult.error((String)t.getMessage());
        }
    }

    private static Supplier<Void> getValidator(Class<?> type) {
        return Suppliers.memoize(() -> {
            if (DataSpecs.getSubSpec(type) == null) {
                throw new IllegalStateException("No sub-spec for type: " + type);
            }
            return null;
        });
    }

    public static <T> SuperCodec<T> of(Class<T> type) {
        return new SuperCodec<T>(type, SuperCodec.getValidator(type));
    }

    public static <T> SuperCodec<T> withoutValidator(Class<T> type) {
        return new SuperCodec<T>(type, NOOP_VALIDATOR);
    }
}
