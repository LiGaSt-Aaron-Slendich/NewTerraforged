package com.terraforged.mod.worldgen.biome.vegetation;

import com.terraforged.mod.data.ModVegetations;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;

public class BiomeVegetationManager {
   private final RegistryAccess access;
   private final VegetationConfig[] configs;
   private final Map<Holder<Biome>, BiomeVegetation> vegetation = new ConcurrentHashMap<>();

   public BiomeVegetationManager(RegistryAccess access) {
      this.access = access;
      this.configs = ModVegetations.getVegetation(access);
   }

   public BiomeVegetation getVegetation(Holder<Biome> biome) {
      return this.vegetation.computeIfAbsent(biome, this::compute);
   }

   private BiomeVegetation compute(Holder<Biome> biome) {
      VegetationConfig vegetationconfig = getConfig(biome, this.configs);
      VegetationFeatures vegetationfeatures = VegetationFeatures.create((Biome)biome.value(), this.access, vegetationconfig);
      return new BiomeVegetation(vegetationconfig, vegetationfeatures);
   }

   private static VegetationConfig getConfig(Holder<Biome> biome, VegetationConfig[] configs) {
      for (VegetationConfig vegetationconfig : configs) {
         if (biome.is(vegetationconfig.biomes())) {
            return vegetationconfig;
         }
      }

      return VegetationConfig.NONE;
   }
}
