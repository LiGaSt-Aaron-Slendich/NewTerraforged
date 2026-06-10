package com.terraforged.mod.data.codec;

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

public interface WorldGenCodec<V>
extends Codec<V> {
    public static final MapCodec<RegistryAccess> CODEC = new MapCodec<RegistryAccess>(){

        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        public <T> DataResult<RegistryAccess> decode(DynamicOps<T> ops, MapLike<T> input) {
            Optional<RegistryAccess> access = RegistryAccessUtil.getRegistryAccess(ops);
            if (access.isEmpty()) {
                return DataResult.error((String)"Invalid ops");
            }
            return DataResult.success(access.get());
        }

        public <T> RecordBuilder<T> encode(RegistryAccess input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return new RecordBuilder.MapBuilder(ops);
        }
    };

    public <T> V decode(DynamicOps<T> var1, T var2, RegistryAccess var3);

    public <T> T encode(V var1, DynamicOps<T> var2);

    default public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
        Optional<RegistryAccess> access = RegistryAccessUtil.getRegistryAccess(ops);
        if (access.isEmpty()) {
            return DataResult.error((String)"Invalid ops");
        }
        V result = this.decode(ops, input, access.get());
        return DataResult.success((Pair<V, T>) (Object) Pair.of(result, input));
    }

    default public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
        return DataResult.success(this.encode(input, ops));
    }
}
