package com.terraforged.mod.registry;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.lazy.LazyRegistry;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public interface ModRegistry {
   LazyRegistry<ClimateType> CLIMATE = TerraForged.registry("worldgen/climate");
   LazyRegistry<NoiseCave> CAVE = TerraForged.registry("worldgen/cave");
   LazyRegistry<TerrainNoise> TERRAIN = TerraForged.registry("worldgen/terrain/noise");
   LazyRegistry<TerrainType> TERRAIN_TYPE = TerraForged.registry("worldgen/terrain/type");
   LazyRegistry<VegetationConfig> VEGETATION = TerraForged.registry("worldgen/vegetation");

   static <T> T[] entries(RegistryAccess access, ResourceKey<Registry<T>> key, IntFunction<T[]> arrayFunc) {
      return entries(access.ownedRegistryOrThrow(key), arrayFunc);
   }

   static <T> T[] entries(Registry<T> registry, IntFunction<T[]> arrayFunc) {
      return registry.entrySet().stream().sorted(Comparator.comparing(e -> ((ResourceKey)e.getKey()).location())).map(Entry::getValue).toArray(arrayFunc);
   }
}
