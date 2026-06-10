package com.terraforged.mod.data.codec;

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
import com.terraforged.mod.data.util.DataUtil;
import java.util.function.Consumer;

public record SpecCodec<V>(DataSpec<V> spec) implements Codec<V>
{
    public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
        try {
            JsonElement json = (JsonElement)ops.convertTo((DynamicOps)JsonOps.INSTANCE, input);
            DataValue data = DataUtil.toData(json);
            V result = this.spec.deserialize(data.asObj());
            return DataResult.success((Pair<V, T>) (Object) Pair.of(result, input));
        }
        catch (Throwable t) {
            t.printStackTrace();
            return DataResult.error((String)t.getMessage());
        }
    }

    public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
        try {
            DataValue data = this.spec.serialize(input);
            JsonElement json = DataUtil.toJson(data);
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
        }
        catch (Throwable t) {
            t.printStackTrace();
            return DataResult.error((String)t.getMessage());
        }
    }

    public static <T> Codec<T> of(String name) {
        return SpecCodec.of((DataSpec<T>) (Object) DataSpecs.getSpec(name));
    }

    public static <T> Codec<T> of(DataSpec<T> spec) {
        return new SpecCodec<T>(spec);
    }

    public static <T> Codec<T> create(Class<T> type, DataFactory<T> factory, Consumer<DataSpec.Builder<T>> consumer) {
        DataSpec.Builder<T> builder = DataSpec.builder(type, factory);
        consumer.accept(builder);
        return SpecCodec.of(builder.build());
    }

    public static <T> Codec<T> create(String name, Class<T> type, DataFactory<T> factory, Consumer<DataSpec.Builder<T>> consumer) {
        DataSpec.Builder<T> builder = DataSpec.builder(name, type, factory);
        consumer.accept(builder);
        return SpecCodec.of(builder.build());
    }
}
