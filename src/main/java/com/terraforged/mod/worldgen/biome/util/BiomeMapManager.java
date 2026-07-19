package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.biome.util.SurfaceBiomeConfigLoader;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class BiomeMapManager {
   private static final BiomeType[] TYPES = BiomeType.values();
   private static final BiomeMapManager.BiomeTypeHolder[] HOLDERS = Stream.of(TYPES)
      .map(BiomeMapManager.BiomeTypeHolder::new)
      .toArray(BiomeMapManager.BiomeTypeHolder[]::new);
   private final Registry<Biome> biomes;
   private final Registry<ClimateType> climateTypes;
   private final List<Holder<Biome>> overworldBiomes;
   private final Map<BiomeType, WeightMap<Holder<Biome>>> biomeMap;

   public BiomeMapManager(RegistryAccess access) {
      this.biomes = access.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
      this.climateTypes = access.ownedRegistryOrThrow(ModRegistry.CLIMATE.get());
      this.overworldBiomes = getOverworldBiomes(this.biomes, this.climateTypes);
      this.biomeMap = this.buildBiomeMap();
   }

   public Holder<Biome> get(ResourceKey<Biome> key) {
      return this.biomes.getHolderOrThrow(key);
   }

   public Registry<Biome> getBiomes() {
      return this.biomes;
   }

   public List<Holder<Biome>> getOverworldBiomes() {
      return this.overworldBiomes;
   }

   public Map<BiomeType, WeightMap<Holder<Biome>>> getBiomeMap() {
      return this.biomeMap;
   }

   private Map<BiomeType, WeightMap<Holder<Biome>>> buildBiomeMap() {
      Map<BiomeType, Object2FloatMap<Holder<Biome>>> map = this.getWeightsMap();
      EnumMap<BiomeType, WeightMap<Holder<Biome>>> enummap = new EnumMap<>(BiomeType.class);

      for (Entry<BiomeType, Object2FloatMap<Holder<Biome>>> entry : map.entrySet()) {
         Holder<Biome>[] holder = (Holder<Biome>[])entry.getValue().keySet().toArray(Holder[]::new);
         float[] afloat = entry.getValue().values().toFloatArray();
         enummap.put(entry.getKey(), new WeightMap<>(holder, afloat));
      }

      return enummap;
   }

   private Map<BiomeType, Object2FloatMap<Holder<Biome>>> getWeightsMap() {
      HashMap<BiomeType, Object2FloatMap<Holder<Biome>>> hashmap = new HashMap<>();
      ObjectOpenHashSet<Holder<Biome>> objectopenhashset = new ObjectOpenHashSet();

      for (BiomeMapManager.BiomeTypeHolder biomemapmanager$biometypeholder : HOLDERS) {
         ClimateType climatetype = (ClimateType)this.climateTypes.get(biomemapmanager$biometypeholder.name);
         if (climatetype == null) {
            hashmap.put(biomemapmanager$biometypeholder.type(), newMutableWeightMap());
         } else {
            Object2FloatMap<Holder<Biome>> object2floatmap = getBiomeWeights(climatetype, this.biomes, objectopenhashset::add);
            hashmap.put(biomemapmanager$biometypeholder.type(), object2floatmap);
         }
      }

      for (Holder<Biome> holder : this.overworldBiomes) {
         if (!objectopenhashset.contains(holder)) {
            BiomeType biometype = BiomeUtil.getType(holder);
            if (biometype != null) {
               hashmap.computeIfAbsent(biometype, t -> new Object2FloatLinkedOpenHashMap()).put(holder, 1.0F);
            }
         }
      }

      // Overlay explicit surface-biomes.toml weights when present.
      SurfaceBiomeConfigLoader.ConfigOverlay overlay = SurfaceBiomeConfigLoader.load(this.biomes);
      for (var entry : overlay.explicit().entrySet()) {
         Object2FloatMap<Holder<Biome>> map = hashmap.computeIfAbsent(entry.getKey(), t -> newMutableWeightMap());
         for (var e : entry.getValue().object2FloatEntrySet()) {
            map.put(e.getKey(), e.getFloatValue());
         }
      }

      return hashmap;
   }

   private static Object2FloatMap<Holder<Biome>> getBiomeWeights(ClimateType type, Registry<Biome> biomes, Consumer<Holder<Biome>> registered) {
      Object2FloatMap<Holder<Biome>> object2floatmap = newMutableWeightMap();
      ObjectIterator objectiterator = type.getWeights().object2FloatEntrySet().iterator();

      while (objectiterator.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2FloatMap.Entry<ResourceLocation> entry = (it.unimi.dsi.fastutil.objects.Object2FloatMap.Entry<ResourceLocation>)objectiterator.next();
         ResourceLocation id = entry.getKey();
         // Skip missing biomes (standalone / optional biome mods) instead of crashing.
         var optionalBiome = biomes.getOptional(id);
         if (optionalBiome.isEmpty()) {
            continue;
         }
         var optionalKey = biomes.getResourceKey(optionalBiome.get());
         if (optionalKey.isEmpty()) {
            continue;
         }
         var optionalHolder = biomes.getHolder(optionalKey.get());
         if (optionalHolder.isEmpty()) {
            continue;
         }
         Holder<Biome> holder = optionalHolder.get();
         object2floatmap.put(holder, entry.getFloatValue());
         registered.accept(holder);
      }

      return object2floatmap;
   }

   private static List<Holder<Biome>> getOverworldBiomes(Registry<Biome> biomes, Registry<ClimateType> biomeTypes) {
      List<Holder<Biome>> list = BiomeUtil.getOverworldBiomes(biomes);
      ObjectOpenHashSet<Holder<Biome>> objectopenhashset = new ObjectOpenHashSet(list);

      for (ClimateType climatetype : biomeTypes) {
         ObjectIterator objectiterator = climatetype.getWeights().keySet().iterator();

         while (objectiterator.hasNext()) {
            ResourceLocation resourcelocation = (ResourceLocation)objectiterator.next();
            ResourceKey<Biome> resourcekey = ResourceKey.create(Registry.BIOME_REGISTRY, resourcelocation);
            var optionalHolder = biomes.getHolder(resourcekey);
            if (optionalHolder.isEmpty()) {
               continue;
            }
            Holder<Biome> holder = optionalHolder.get();
            if (objectopenhashset.add(holder)) {
               list.add(holder);
            }
         }
      }

      list.sort(BiomeUtil.BIOME_SORTER);
      return list;
   }

   private static Object2FloatMap<Holder<Biome>> newMutableWeightMap() {
      return new Object2FloatLinkedOpenHashMap();
   }

   private record BiomeTypeHolder(BiomeType type, ResourceLocation name) {
      public BiomeTypeHolder(BiomeType type) {
         this(type, TerraForged.location(type.name().toLowerCase(Locale.ROOT)));
      }
   }
}
