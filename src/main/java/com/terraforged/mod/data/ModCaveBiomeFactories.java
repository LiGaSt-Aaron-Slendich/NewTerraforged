package com.terraforged.mod.data;

import com.terraforged.mod.worldgen.biome.biomes.ModBiome;
import com.terraforged.mod.worldgen.cave.CaveBiomeFeaturePresets;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;

public final class ModCaveBiomeFactories {
    public static final String THERMAL_SPRINGS = "cave_thermal_springs";
    public static final String UNDERGROUND_JUNGLE = "cave_underground_jungle";
    public static final String STEAMING_JUNGLE = "cave_steaming_jungle";
    private static final ResourceKey<Biome> PRESET_THERMAL = ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)new ResourceLocation("terralith", "yellowstone"));
    private static final ResourceKey<Biome> PRESET_JUNGLE = ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)new ResourceLocation("terralith", "cave/underground_jungle"));
    private static final ResourceKey<Biome> PRESET_STEAMING = ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)new ResourceLocation("terralith", "cave/thermal_caves"));
    private static final ResourceKey<Biome> FEATURE_JUNGLE = Biomes.JUNGLE;

    private ModCaveBiomeFactories() {
    }

    public static Biome thermalSprings() {
        return ModCaveBiomeFactories.fromCavePreset(PRESET_THERMAL, (ResourceKey<Biome>)Biomes.DRIPSTONE_CAVES, CaveBiomeFeaturePresets.Profile.THERMAL, builder -> {
            builder.temperature(0.9f);
            builder.downfall(0.4f);
        });
    }

    public static Biome undergroundJungle() {
        return ModBiome.create((ResourceKey<Biome>)Biomes.JUNGLE, builder -> {
            builder.biomeCategory(Biome.BiomeCategory.UNDERGROUND);
            builder.generationSettings(CaveBiomeFeaturePresets.build((ResourceKey<Biome>)Biomes.JUNGLE, (ResourceKey<Biome>)Biomes.JUNGLE, CaveBiomeFeaturePresets.Profile.JUNGLE));
            builder.temperature(0.72f);
            builder.downfall(0.85f);
        });
    }

    public static Biome steamingJungle() {
        return ModCaveBiomeFactories.fromCavePreset(PRESET_STEAMING, FEATURE_JUNGLE, CaveBiomeFeaturePresets.Profile.STEAMING, builder -> {
            builder.temperature(0.81f);
            builder.downfall(0.65f);
        });
    }

    private static Biome fromCavePreset(ResourceKey<Biome> preset, ResourceKey<Biome> fallback, CaveBiomeFeaturePresets.Profile profile, Consumer<Biome.BiomeBuilder> tweak) {
        ResourceKey<Biome> parent = ModCaveBiomeFactories.resolvePreset(preset, fallback);
        ResourceKey<Biome> featureSource = BuiltinRegistries.BIOME.containsKey(preset.location()) ? preset : fallback;
        BiomeGenerationSettings features = CaveBiomeFeaturePresets.build(featureSource, fallback, profile);
        return ModBiome.create(parent, builder -> {
            builder.biomeCategory(Biome.BiomeCategory.UNDERGROUND);
            builder.generationSettings(features);
            tweak.accept((Biome.BiomeBuilder)builder);
        });
    }

    private static ResourceKey<Biome> resolvePreset(ResourceKey<Biome> preset, ResourceKey<Biome> fallback) {
        if (BuiltinRegistries.BIOME.containsKey(preset.location())) {
            return preset;
        }
        return fallback;
    }
}
