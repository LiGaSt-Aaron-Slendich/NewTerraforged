package com.terraforged.mod.worldgen.biome.biomes;

import com.terraforged.mod.TerraForged;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public record ModBiome(ResourceKey<Biome> key, Supplier<Biome> factory) {
    public Biome create() {
        return this.factory.get();
    }

    public static ModBiome of(String name, ResourceKey<Biome> parent, Consumer<Biome.BiomeBuilder> modifier) {
        ResourceKey key = ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)TerraForged.location(name));
        Supplier<Biome> factory = ModBiome.copyFactory(parent, modifier);
        return new ModBiome((ResourceKey<Biome>)key, factory);
    }

    private static Supplier<Biome> copyFactory(ResourceKey<Biome> parent, Consumer<Biome.BiomeBuilder> modifier) {
        return () -> {
            Biome.BiomeBuilder builder = ModBiome.builderOf(parent);
            modifier.accept(builder);
            return builder.build();
        };
    }

    private static Biome.BiomeBuilder builderOf(ResourceKey<Biome> parent) {
        Biome biome = (Biome)BuiltinRegistries.BIOME.getOrThrow(parent);
        Holder holder = BuiltinRegistries.BIOME.getHolderOrThrow(parent);
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();
        builder.downfall(biome.getDownfall());
        builder.biomeCategory(Biome.getBiomeCategory((Holder)holder));
        builder.temperature(biome.getBaseTemperature());
        builder.mobSpawnSettings(biome.getMobSettings());
        builder.precipitation(biome.getPrecipitation());
        builder.specialEffects(biome.getSpecialEffects());
        builder.generationSettings(biome.getGenerationSettings());
        return builder;
    }

    public static Biome create(ResourceKey<Biome> parent, Consumer<Biome.BiomeBuilder> modifier) {
        Biome.BiomeBuilder builder = ModBiome.builderOf(parent);
        modifier.accept(builder);
        return builder.build();
    }
}
