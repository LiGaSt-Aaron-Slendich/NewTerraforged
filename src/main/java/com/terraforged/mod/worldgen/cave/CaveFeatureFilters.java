package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveFeatureFilters {
    private static final String[] BLOCKED_PATH = new String[]{"lake_lava", "lake_lava_underground", "lake_lava_surface", "spring_lava", "spring_water", "geode", "amethyst", "monster_room", "fossil", "dungeon_extra", "dungeon", "ore_", "disk_sand", "disk_clay", "disk_gravel", "freeze_top_layer"};

    private CaveFeatureFilters() {
    }

    public static boolean isModCaveDecorationStage(int stageIndex) {
        return stageIndex == GenerationStep.Decoration.RAW_GENERATION.ordinal() || stageIndex == GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal() || stageIndex == GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal() || stageIndex == GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal() || stageIndex == GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
    }

    public static boolean belongsToModCaveBiome(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        ResourceLocation featureId = FeatureMassClassifier.featurePath(placed);
        if (featureId == null) {
            return false;
        }
        Optional biomeKey = biome.unwrapKey();
        if (biomeKey.isEmpty()) {
            return false;
        }
        String fPath = featureId.getPath().toLowerCase();
        String bPath = ((ResourceKey<Biome>) biomeKey.get()).location().getPath().toLowerCase();
        String fNs = featureId.getNamespace();
        String bNs = ((ResourceKey<Biome>) biomeKey.get()).location().getNamespace();
        if (CaveFeatureFilters.isForeignThemeFeature(fPath, bPath)) {
            return false;
        }
        if ("minecraft".equals(fNs)) {
            return CaveFeatureFilters.isVanillaCaveHelper(fPath, bPath);
        }
        if (!fNs.equals(bNs)) {
            if (CaveFeatureFilters.isVolcanicAccentFeature(fPath) && CaveFeatureFilters.allowsCrossModVolcanicAccent(bPath, fPath, biome)) {
                return true;
            }
            if (CaveFeatureFilters.isPrismachasmFeature(fPath) && bPath.contains("prismachasm")) {
                return true;
            }
            String slug = CaveFeatureFilters.biomeFeatureSlug(bPath);
            return slug.length() >= 3 && fPath.contains(slug);
        }
        if (CaveFeatureFilters.isSharedCaveInfra(fPath)) {
            return true;
        }
        if (fPath.contains("fuck_art") || fPath.contains("/split/") || fPath.contains("/tiles")) {
            return "terralith".equals(bNs);
        }
        String theme = CaveFeatureFilters.extractCaveTheme(bPath);
        if (theme != null && fPath.contains(theme)) {
            return true;
        }
        String slug = CaveFeatureFilters.biomeFeatureSlug(bPath);
        if (slug.length() >= 3 && (fPath.contains(slug) || fPath.contains("cave/" + slug))) {
            return true;
        }
        return fPath.contains("cave/") && CaveFeatureFilters.sharesCaveSlug(bPath, fPath);
    }

    private static String biomeFeatureSlug(String biomePath) {
        int slash = biomePath.lastIndexOf(47);
        String name = slash >= 0 ? biomePath.substring(slash + 1) : biomePath;
        return name.replace("_caverns", "").replace("_caves", "").replace("_cave", "");
    }

    private static boolean sharesCaveSlug(String biomePath, String featurePath) {
        String slug = CaveFeatureFilters.biomeFeatureSlug(biomePath);
        if (slug.length() < 3) {
            return false;
        }
        int caveIdx = featurePath.indexOf("cave/");
        if (caveIdx < 0) {
            return featurePath.contains(slug);
        }
        String segment = featurePath.substring(caveIdx + 5);
        int next = segment.indexOf(47);
        String head = next >= 0 ? segment.substring(0, next) : segment;
        return head.contains(slug) || slug.contains(head);
    }

    public static boolean isForbiddenForCaveBiome(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        ResourceLocation featureId = FeatureMassClassifier.featurePath(placed);
        if (featureId == null) {
            return true;
        }
        Optional biomeKey = biome.unwrapKey();
        if (biomeKey.isEmpty()) {
            return false;
        }
        String fPath = featureId.getPath().toLowerCase();
        String bPath = ((ResourceKey<Biome>) biomeKey.get()).location().getPath().toLowerCase();
        if (bPath.contains("mantle") && CaveFeatureFilters.isForeignVentFeature(fPath)) {
            return true;
        }
        if (bPath.contains("brimstone") && CaveFeatureFilters.isForeignVentFeature(fPath)) {
            return true;
        }
        if (!bPath.contains("thermal") && !CaveBiomeIds.isModThermalPresetBiome(biome)) {
            return false;
        }
        if (CaveBiomeIds.isModThermalPresetBiome(biome)) {
            return false;
        }
        return fPath.contains("dripleaf") || fPath.contains("pitcher") || fPath.contains("sea_pickle") || fPath.contains("/lakes") || fPath.contains("lush_caves") || fPath.contains("moss_carpet") || fPath.contains("clay") && (fPath.contains("water") || fPath.contains("dripleaf"));
    }

    private static boolean isForeignThemeFeature(String fPath, String bPath) {
        if (fPath.contains("redstone") && !bPath.contains("redstone")) {
            return true;
        }
        if (!(!fPath.contains("crystal") || bPath.contains("crystal") || bPath.contains("prismachasm") || bPath.contains("prisma") || bPath.contains("subzero"))) {
            return true;
        }
        if (!(!fPath.contains("/ice/") && !fPath.contains("icicle") || bPath.contains("ice") || bPath.contains("icicle") || bPath.contains("frostfire") || bPath.contains("subzero") || bPath.contains("frost"))) {
            return true;
        }
        if ((fPath.contains("prismarite") || fPath.contains("prismachasm")) && !bPath.contains("prismachasm") && !bPath.contains("prisma")) {
            return true;
        }
        if ((fPath.contains("lush") || fPath.contains("azalea") || fPath.contains("spore_blossom")) && !bPath.contains("lush")) {
            return true;
        }
        if (fPath.contains("fungal") && !bPath.contains("fungal")) {
            return true;
        }
        if (fPath.contains("thermal") && !bPath.contains("thermal")) {
            return true;
        }
        return fPath.contains("frostfire") && !bPath.contains("frostfire");
    }

    private static boolean isSharedCaveInfra(String fPath) {
        return fPath.contains("/generic/") || fPath.contains("/stone/") || fPath.contains("/ice/");
    }

    private static String extractCaveTheme(String biomePath) {
        if (biomePath.contains("fungal")) {
            return "fungal";
        }
        if (biomePath.contains("frostfire")) {
            return "frostfire";
        }
        if (biomePath.contains("thermal_springs") || biomePath.contains("cave_thermal_springs")) {
            return "yellowstone";
        }
        if (biomePath.contains("thermal")) {
            return "thermal";
        }
        if (biomePath.contains("redstone")) {
            return "redstone";
        }
        if (biomePath.contains("crystal")) {
            return "crystal";
        }
        if (biomePath.contains("ice_cave") || biomePath.contains("icicle")) {
            return "ice";
        }
        if (biomePath.contains("frost_caves") || biomePath.contains("frost_cave")) {
            return "frost";
        }
        if (biomePath.contains("desert")) {
            return "desert";
        }
        if (biomePath.contains("lush")) {
            return "lush";
        }
        if (biomePath.contains("mossy")) {
            return "mossy";
        }
        if (biomePath.contains("magma")) {
            return "magma";
        }
        if (biomePath.contains("deep")) {
            return "deep";
        }
        if (biomePath.contains("subzero") || biomePath.contains("hypogeal")) {
            return "subzero";
        }
        if (biomePath.contains("prismachasm") || biomePath.contains("prisma")) {
            return "prismachasm";
        }
        if (biomePath.contains("mycotoxic")) {
            return "mycotoxic";
        }
        if (biomePath.contains("grotto")) {
            return "grotto";
        }
        if (biomePath.contains("mantle")) {
            return "mantle";
        }
        if (biomePath.contains("brimstone")) {
            return "brimstone";
        }
        if (biomePath.contains("infested")) {
            return "infested";
        }
        if (biomePath.contains("undergarden")) {
            return "undergarden";
        }
        if (biomePath.contains("jungle")) {
            return "jungle";
        }
        if (biomePath.contains("karst")) {
            return "karst";
        }
        if (biomePath.contains("limestone")) {
            return "limestone";
        }
        if (biomePath.contains("chalk")) {
            return "chalk";
        }
        if (biomePath.contains("sand")) {
            return "sand";
        }
        if (biomePath.contains("quartz")) {
            return "quartz";
        }
        if (biomePath.contains("embur")) {
            return "embur";
        }
        if (biomePath.contains("glowshroom")) {
            return "glowshroom";
        }
        if (biomePath.contains("steaming")) {
            return "steaming";
        }
        return null;
    }

    private static boolean isVolcanicBiomePath(String biomePath) {
        return biomePath.contains("thermal") || biomePath.contains("mantle") || biomePath.contains("brimstone") || biomePath.contains("magma");
    }

    private static boolean isVolcanicAccentFeature(String fPath) {
        return fPath.contains("ash_vent") || fPath.contains("geyser") || fPath.contains("/vents") || fPath.contains("magma") || fPath.contains("basalt_strip") || fPath.contains("lava_drip");
    }

    private static boolean allowsCrossModVolcanicAccent(String bPath, String fPath, Holder<Biome> biome) {
        if (!CaveFeatureFilters.isVolcanicBiomePath(bPath)) {
            return false;
        }
        return CaveFeatureFilters.isVolcanicAccentFeature(fPath);
    }

    private static boolean isForeignVentFeature(String fPath) {
        return fPath.contains("ash_vent") || fPath.contains("geyser") || fPath.contains("/vents");
    }

    private static boolean isVanillaCaveHelper(String fPath, String bPath) {
        if (fPath.contains("glow_lichen") || fPath.contains("classic_vines") || fPath.contains("vines")) {
            return true;
        }
        if (fPath.contains("dripstone") || fPath.contains("pointed_dripstone")) {
            return bPath.contains("dripstone") || bPath.contains("stone") || bPath.contains("empty");
        }
        return !fPath.contains("monster_room") && (fPath.contains("moss") || fPath.contains("cave"));
    }

    private static boolean isModCaveBiomePath(String biomePath) {
        return biomePath.contains("cave") || biomePath.contains("cavern") || biomePath.contains("grotto") || biomePath.contains("hypogeal") || biomePath.contains("prismachasm") || biomePath.contains("undergarden") || biomePath.contains("mycotoxic");
    }

    public static boolean isModCaveFeatureAllowed(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isBlockedPath(placed, biome)) {
            return false;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (CaveFeatureFilters.isBlockedLushFeature(path)) {
            return false;
        }
        if (CaveFeatureFilters.isMegaHandledColumnFeature(path, biome)) {
            return false;
        }
        if (path.contains("monster_room") || path.contains("fossil")) {
            return false;
        }
        if (path.contains("spring_lava") || path.contains("spring_water")) {
            return false;
        }
        if (path.contains("ore_infested")) {
            return true;
        }
        if (CaveFeatureFilters.allowsVegetation(biome, path)) {
            return true;
        }
        if (path.contains("grass") && !path.contains("seagrass") && !path.contains("sea_grass")) {
            return false;
        }
        if (path.startsWith("ore_") || path.contains("/ore_")) {
            return false;
        }
        return !FeatureMassClassifier.spawnsSurfaceVegetation(placed);
    }

    private static boolean allowsVegetation(Holder<Biome> biome, String path) {
        if (!(CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome))) {
            return false;
        }
        return path.contains("grass") || path.contains("flower") || path.contains("bamboo") || path.contains("vine") || path.contains("jungle") || path.contains("patch");
    }

    private static boolean isPrismachasmFeature(String fPath) {
        return fPath.contains("prismarite") || fPath.contains("prismoss") || fPath.contains("prisma") || fPath.contains("hyssop");
    }

    public static boolean requiresSolidFloor(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("lake") || path.contains("spring") || path.contains("puddle") || path.contains("pool") || path.contains("clay") || path.contains("water");
    }

    public static boolean isAllowedStage(int stageIndex, boolean modCaveBiome) {
        if (modCaveBiome) {
            return CaveFeatureFilters.isModCaveDecorationStage(stageIndex);
        }
        return stageIndex != GenerationStep.Decoration.LAKES.ordinal() && stageIndex != GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal();
    }

    public static boolean isAllowedFeature(Holder<PlacedFeature> placed, int stageIndex) {
        if (CaveFeatureFilters.isBlockedPath(placed, null)) {
            return false;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id != null && CaveFeatureFilters.isBlockedLushFeature(id.getPath().toLowerCase())) {
            return false;
        }
        return !CaveFeatureFilters.isVolumeFeature(placed, stageIndex) || CaveFeatureFilters.isAnchorOnlyFeature(placed);
    }

    public static boolean isVolumeFeature(Holder<PlacedFeature> placed, int stageIndex) {
        if (CaveFeatureFilters.isBlockedPath(placed, null)) {
            return false;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (CaveFeatureFilters.isVolumeStage(stageIndex)) {
            return path.contains("cave/") || path.contains("mushroom") || path.contains("fungal") || path.contains("glow_lichen") || path.contains("dripstone") || path.contains("dripleaf") || path.contains("lichen") || path.contains("hanging") || path.contains("stalactite") || path.contains("icicle") || path.contains("patch");
        }
        return false;
    }

    public static boolean isAnchorOnlyFeature(Holder<PlacedFeature> placed) {
        if (FeatureMassClassifier.isCaveFloorLarge(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed)) {
            return true;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("huge_") || path.contains("mega_geode") || path.contains("whole_");
    }

    private static boolean isBlockedPath(Holder<PlacedFeature> placed) {
        return CaveFeatureFilters.isBlockedPath(placed, null);
    }

    private static boolean isBlockedPath(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return true;
        }
        String path = id.getPath().toLowerCase();
        if (CaveFeatureFilters.isBlockedLushFeature(path)) {
            return true;
        }
        for (String blocked : BLOCKED_PATH) {
            if (!path.contains(blocked)) continue;
            return true;
        }
        return false;
    }

    private static boolean isBlockedLushFeature(String path) {
        return path.contains("lush_caves") || path.contains("blooming_caves") || path.contains("spore_blossom") || path.contains("azalea_tree") || path.contains("moss_vegetation") || path.contains("moss_patch") || path.contains("moss_carpet");
    }

    private static boolean isMegaHandledColumnFeature(String path, Holder<Biome> biome) {
        if (path.contains("prismarite") || path.contains("prismoss") || path.contains("hyssop")) {
            return biome.unwrapKey().map(key -> key.location().getPath().toLowerCase().contains("prismachasm")).orElse(false) == false;
        }
        return path.contains("large_dripstone") || path.contains("pointed_dripstone") || path.contains("dripstone_cluster") || path.contains("cave/crystal/") || path.contains("cave/ice/");
    }

    private static boolean isVolumeStage(int stageIndex) {
        return stageIndex == GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal() || stageIndex == GenerationStep.Decoration.VEGETAL_DECORATION.ordinal() || stageIndex == GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal();
    }
}
