package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.terraforged.mod.data.codec.LazyCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.LinkedHashMap;
import net.minecraft.resources.ResourceLocation;

public class ClimateType {
    public static final String IGNORE = "forge:registry_name";
    public static final Codec<ClimateType> CODEC = LazyCodec.of(() -> new Codec<ClimateType>(){

        public <T> DataResult<Pair<ClimateType, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).map(map -> {
                Object2FloatOpenHashMap weights = new Object2FloatOpenHashMap();
                map.entries().forEach(e -> {
                    String name = (String)ops.getStringValue(e.getFirst()).result().orElseThrow();
                    if (name.equals(ClimateType.IGNORE)) {
                        return;
                    }
                    float weight = ((Number)ops.getNumberValue(e.getSecond()).result().orElseThrow()).floatValue();
                    weights.put(new ResourceLocation(name), weight);
                });
                return new ClimateType((Object2FloatMap<ResourceLocation>)weights);
            }).map(weights -> Pair.of(weights, input));
        }

        public <T> DataResult<T> encode(ClimateType input, DynamicOps<T> ops, T prefix) {
            LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
            for (Object2FloatMap.Entry entry : input.weights.object2FloatEntrySet()) {
                map.put(ops.createString(((ResourceLocation)entry.getKey()).toString()), ops.createFloat(entry.getFloatValue()));
            }
            @SuppressWarnings("unchecked")
            T mapOut = (T) (Object) ops.createMap((java.util.Map) map);
            return DataResult.success(mapOut);
        }
    });
    private final Object2FloatMap<ResourceLocation> weights;

    public ClimateType(Object2FloatMap<ResourceLocation> weights) {
        this.weights = weights;
    }

    public Object2FloatMap<ResourceLocation> getWeights() {
        return this.weights;
    }
}
