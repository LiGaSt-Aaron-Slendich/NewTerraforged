package com.terraforged.mod.data;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome.BiomeCategory;

public interface ModClimates extends ModRegistry {
   float RARE = 1.0F;
   float NORMAL = 5.0F;

   static void register() {
      Registry<Biome> registry = BuiltinRegistries.BIOME;
      List<Holder<Biome>> list = BiomeUtil.getOverworldBiomes(registry);

      for (BiomeType biometype : BiomeType.values()) {
         ModRegistries.register(CLIMATE, biometype.name().toLowerCase(Locale.ROOT), ModClimates.Factory.create(biometype, list, registry));
      }
   }

   public static class Factory {
      static ClimateType create(BiomeType type, List<Holder<Biome>> biomes, Registry<Biome> registry) {
         Object2FloatOpenHashMap<ResourceLocation> object2floatopenhashmap = new Object2FloatOpenHashMap();

         for (Holder<Biome> holder : biomes) {
            BiomeType biometype = BiomeUtil.getType(holder);
            if (biometype != null && biometype == type) {
               ResourceKey<Biome> resourcekey = (ResourceKey<Biome>)holder.unwrapKey().orElseThrow();
               object2floatopenhashmap.put(resourcekey.location(), getWeight(resourcekey, holder));
            }
         }

         return new ClimateType(object2floatopenhashmap);
      }

      static float getWeight(ResourceKey<Biome> key, Holder<Biome> biome) {
         if (Biome.getBiomeCategory(biome) == BiomeCategory.MUSHROOM) {
            return 1.0F;
         } else {
            return key == Biomes.ICE_SPIKES ? 1.0F : 5.0F;
         }
      }
   }
}
