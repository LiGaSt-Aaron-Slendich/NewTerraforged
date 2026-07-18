package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.terraforged.mod.codec.LazyCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2FloatMap.Entry;
import java.util.LinkedHashMap;
import net.minecraft.resources.ResourceLocation;

public class ClimateType {
   public static final String IGNORE = "forge:registry_name";
   public static final Codec<ClimateType> CODEC = LazyCodec.of(() -> new Codec<ClimateType>() {
      public <T> DataResult<Pair<ClimateType, T>> decode(DynamicOps<T> ops, T input) {
         return ops.getMap(input).map(map -> {
            Object2FloatOpenHashMap<ResourceLocation> object2floatopenhashmap = new Object2FloatOpenHashMap();
            map.entries().forEach(e -> {
               String s = (String)ops.getStringValue(e.getFirst()).result().orElseThrow();
               if (!s.equals("forge:registry_name")) {
                  float f = ((Number)ops.getNumberValue(e.getSecond()).result().orElseThrow()).floatValue();
                  object2floatopenhashmap.put(new ResourceLocation(s), f);
               }
            });
            return new ClimateType(object2floatopenhashmap);
         }).map(weights -> Pair.of(weights, input));
      }

      public <T> DataResult<T> encode(ClimateType input, DynamicOps<T> ops, T prefix) {
         LinkedHashMap<T, T> linkedhashmap = new LinkedHashMap<>();
         ObjectIterator objectiterator = input.weights.object2FloatEntrySet().iterator();

         while (objectiterator.hasNext()) {
            Entry<ResourceLocation> entry = (Entry<ResourceLocation>)objectiterator.next();
            linkedhashmap.put((T)ops.createString(((ResourceLocation)entry.getKey()).toString()), (T)ops.createFloat(entry.getFloatValue()));
         }

         return DataResult.success(ops.createMap(linkedhashmap));
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
