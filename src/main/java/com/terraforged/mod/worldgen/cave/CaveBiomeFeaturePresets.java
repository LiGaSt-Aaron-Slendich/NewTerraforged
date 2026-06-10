package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveBiomeFeaturePresets {
    private CaveBiomeFeaturePresets() {
    }

    public static BiomeGenerationSettings build(ResourceKey<Biome> preset, ResourceKey<Biome> featureFallback, Profile profile) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        CaveBiomeFeaturePresets.appendFiltered(builder, preset, profile);
        if (profile != Profile.THERMAL) {
            CaveBiomeFeaturePresets.appendFiltered(builder, featureFallback, profile);
        }
        return builder.build();
    }

    private static void appendFiltered(BiomeGenerationSettings.Builder builder, ResourceKey<Biome> key, Profile profile) {
        BiomeGenerationSettings settings = CaveBiomeFeaturePresets.readSettings(key);
        for (int i = 0; i < settings.features().size(); ++i) {
            GenerationStep.Decoration step = GenerationStep.Decoration.values()[i];
            for (Holder<PlacedFeature> feature : settings.features().get(i)) {
                if (!CaveBiomeFeaturePresets.allows((Holder<PlacedFeature>)feature, profile)) continue;
                builder.addFeature(step, feature);
            }
        }
    }

    private static BiomeGenerationSettings readSettings(ResourceKey<Biome> key) {
        if (BuiltinRegistries.BIOME.containsKey(key.location())) {
            return ((Biome)BuiltinRegistries.BIOME.getOrThrow(key)).getGenerationSettings();
        }
        return ((Biome)BuiltinRegistries.BIOME.getOrThrow(Biomes.PLAINS)).getGenerationSettings();
    }

    static boolean allows(Holder<PlacedFeature> placed, Profile profile) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (CaveBiomeFeaturePresets.isBlocked(path)) {
            return false;
        }
        switch (profile) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case THERMAL: 
            case JUNGLE: 
            case STEAMING: 
        }
        return !CaveBiomeFeaturePresets.isBlocked(path);
    }

    private static boolean isBlocked(String path) {
        return path.contains("monster_room") || path.contains("fossil") || path.contains("geode") || path.contains("amethyst") || path.startsWith("ore_") || path.contains("/ore_") || path.contains("lake_lava") || path.contains("lake_water") || path.contains("spring_lava") || path.contains("spring_water") || path.contains("disk_sand") || path.contains("disk_gravel") || path.contains("freeze_top") || path.contains("dripleaf") || path.contains("spore_blossom") || path.contains("azalea") || path.contains("lush_caves") || path.contains("blooming_caves");
    }

    private static boolean allowsThermal(String path) {
        return path.contains("yellowstone/") || path.contains("ash_vent") || path.contains("geyser") || path.contains("underwater_magma") || path.contains("glow_lichen") || path.contains("thermal/");
    }

    private static boolean allowsJungle(String path) {
        if (path.contains("yellowstone/") || path.contains("frostfire") || path.contains("fungal")) {
            return false;
        }
        return path.contains("jungle") || path.contains("bamboo") || path.contains("vines") || path.contains("classic_vines") || path.contains("patch_grass") || path.contains("patch_tall_grass") || path.contains("flower_warm") || path.contains("flower_default") || path.contains("flower_jungle") || path.contains("patch_sugar_cane") || path.contains("patch_melon") || path.contains("trees_jungle") || path.contains("trees_sparse_jungle") || path.contains("trees_hawaii") || path.contains("mushroom") || path.contains("disk_clay") || path.contains("glow_lichen");
    }

    public static enum Profile {
        THERMAL,
        JUNGLE,
        STEAMING;

    }
}
