package com.terraforged.mod.util.seed;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.util.seed.Seedable;

public interface ContextSeedable<T>
extends Seedable<T> {
    default public <V> V withSeed(long seed, V value, Class<V> type) {
        try {
            DataValue data = Cereal.serialize(value);
            Context context = new Context();
            context.getData().add("seed", seed);
            return Cereal.deserialize(data.asObj(), type, context);
        }
        catch (Throwable t) {
            throw new Error("Failed to reseed value: " + value, t);
        }
    }
}
