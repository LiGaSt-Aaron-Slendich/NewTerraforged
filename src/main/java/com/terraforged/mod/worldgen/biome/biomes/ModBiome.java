package com.terraforged.mod.worldgen.biome.biomes;

import com.terraforged.mod.TerraForged;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeBuilder;

public record ModBiome(ResourceKey<Biome> key, Supplier<Biome> factory) {
   public Biome create() {
      return this.factory.get();
   }

   public static ModBiome of(String name, ResourceKey<Biome> parent, Consumer<BiomeBuilder> modifier) {
      ResourceKey<Biome> resourcekey = ResourceKey.create(Registry.BIOME_REGISTRY, TerraForged.location(name));
      Supplier<Biome> supplier = copyFactory(parent, modifier);
      return new ModBiome(resourcekey, supplier);
   }

   private static Supplier<Biome> copyFactory(ResourceKey<Biome> parent, Consumer<BiomeBuilder> modifier) {
      return () -> {
         BiomeBuilder biomebuilder = builderOf(parent);
         modifier.accept(biomebuilder);
         return biomebuilder.build();
      };
   }

   private static BiomeBuilder builderOf(ResourceKey<Biome> parent) {
      Biome biome = (Biome)BuiltinRegistries.BIOME.getOrThrow(parent);
      Holder<Biome> holder = BuiltinRegistries.BIOME.getHolderOrThrow(parent);
      BiomeBuilder biomebuilder = new BiomeBuilder();
      biomebuilder.downfall(biome.getDownfall());
      biomebuilder.biomeCategory(Biome.getBiomeCategory(holder));
      biomebuilder.temperature(biome.getBaseTemperature());
      biomebuilder.mobSpawnSettings(biome.getMobSettings());
      biomebuilder.precipitation(biome.getPrecipitation());
      biomebuilder.specialEffects(biome.getSpecialEffects());
      biomebuilder.generationSettings(biome.getGenerationSettings());
      return biomebuilder;
   }
}
