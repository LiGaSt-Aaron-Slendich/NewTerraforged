package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.CommonAPI;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource.Preset;

public class BiomeUtil {
   private static final Map<BiomeType, ResourceLocation> TYPE_NAMES = new EnumMap<>(BiomeType.class);
   private static final Comparator<ResourceKey<?>> KEY_COMPARATOR = Comparator.comparing(ResourceKey::location);
   public static Comparator<Holder<Biome>> BIOME_SORTER = (o1, o2) -> {
      ResourceKey<Biome> resourcekey = (ResourceKey<Biome>)o1.unwrapKey().orElseThrow();
      ResourceKey<Biome> resourcekey1 = (ResourceKey<Biome>)o2.unwrapKey().orElseThrow();
      Objects.requireNonNull(resourcekey);
      Objects.requireNonNull(resourcekey1);
      return KEY_COMPARATOR.compare(resourcekey, resourcekey1);
   };

   public static ResourceLocation getRegistryName(BiomeType type) {
      return TYPE_NAMES.get(type);
   }

   public static List<Holder<Biome>> getOverworldBiomes(RegistryAccess access) {
      return getOverworldBiomes(access.registryOrThrow(Registry.BIOME_REGISTRY));
   }

   public static List<Holder<Biome>> getOverworldBiomes(Registry<Biome> biomes) {
      Set<Holder<Biome>> set = getVanillaOverworldBiomes(biomes);
      BiomeMatcher biomematcher = CommonAPI.get().getOverworldMatcher();

      for (Holder<Biome> holder : biomes.asHolderIdMap()) {
         if (!set.contains(holder) && biomematcher.test(holder)) {
            set.add(holder);
         }
      }

      ArrayList<Holder<Biome>> arraylist = new ArrayList<>(set);
      arraylist.sort(BIOME_SORTER);
      return arraylist;
   }

   public static BiomeType getType(Holder<Biome> biome) {
      return switch (Biome.getBiomeCategory(biome)) {
         case MESA, DESERT -> BiomeType.DESERT;
         case PLAINS -> getByTemp((Biome)biome.value(), BiomeType.COLD_STEPPE, BiomeType.GRASSLAND, BiomeType.STEPPE);
         case TAIGA -> getByTemp((Biome)biome.value(), BiomeType.TUNDRA, BiomeType.TAIGA);
         case ICY -> BiomeType.TUNDRA;
         case SAVANNA -> BiomeType.SAVANNA;
         case JUNGLE -> BiomeType.TROPICAL_RAINFOREST;
         case FOREST -> getByRain((Biome)biome.value(), BiomeType.TUNDRA, BiomeType.TEMPERATE_RAINFOREST, BiomeType.TEMPERATE_FOREST);
         case MOUNTAIN -> BiomeType.ALPINE;
         default -> null;
      };
   }

   public static BiomeType getByRain(Biome biome, BiomeType frozen, BiomeType wetter, BiomeType dryer) {
      if (biome.getPrecipitation() == Precipitation.SNOW) {
         return frozen;
      } else {
         return biome.getDownfall() >= 0.8 ? wetter : dryer;
      }
   }

   public static BiomeType getByTemp(Biome biome, BiomeType colder, BiomeType warmer) {
      return biome.getPrecipitation() == Precipitation.SNOW ? colder : warmer;
   }

   public static BiomeType getByTemp(Biome biome, BiomeType cold, BiomeType temperate, BiomeType hot) {
      if (biome.getPrecipitation() == Precipitation.SNOW) {
         return cold;
      } else {
         return biome.getPrecipitation() != Precipitation.NONE && !(biome.getBaseTemperature() > 1.0) ? temperate : hot;
      }
   }

   private static Set<Holder<Biome>> getVanillaOverworldBiomes(Registry<Biome> biomes) {
      return new HashSet<>(Preset.OVERWORLD.biomeSource(biomes).possibleBiomes());
   }

   static {
      for (BiomeType biometype : BiomeType.values()) {
         TYPE_NAMES.put(biometype, TerraForged.location(biometype.name().toLowerCase(Locale.ROOT)));
      }
   }
}
