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
    private static final String[] BLOCKED_PATH = new String[]{"lake_lava", "lake_lava_underground", "lake_lava_surface", "spring_lava", "spring_water", "geode", "amethyst", "monster_room", "fossil", "dungeon_extra", "dungeon", "ore_", "disk_sand", "disk_clay", "disk_gravel", "freeze_top_layer", "dead_leaves", "dead_log", "dead_leaf", "dead/", "fallen_log", "deadwood", "dead_fungus", "dead_tree"};

    private CaveFeatureFilters() {
    }

    public static boolean isModCaveDecorationStage(int stageIndex) {
        return stageIndex == GenerationStep.Decoration.RAW_GENERATION.ordinal()
                || stageIndex == GenerationStep.Decoration.LAKES.ordinal()
                || stageIndex == GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal()
                || stageIndex == GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal()
                || stageIndex == GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal()
                || stageIndex == GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
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
        if (CaveFeatureFilters.isForeignWildNatureFeature(fNs, fPath, bPath, biome)) {
            return false;
        }
        if ("minecraft".equals(fNs)) {
            return CaveFeatureFilters.isVanillaCaveHelper(fPath, bPath);
        }
        if (!fNs.equals(bNs)) {
            if (CaveFeatureFilters.isVolcanicAccentFeature(fPath) && CaveFeatureFilters.allowsCrossModVolcanicAccent(bPath, fPath, biome)) {
                return true;
            }
            if (bPath.contains("scorching") && (fPath.contains("ash") || fPath.contains("scorching") || fPath.contains("vent") || fPath.contains("volcanic"))) {
                return true;
            }
            if (CaveFeatureFilters.matchesEmburBiomeFeature(fPath, bPath) && "byg".equals(fNs)) {
                return true;
            }
            if (CaveFeatureFilters.isPrismachasmFeature(fPath) && bPath.contains("prismachasm")) {
                return true;
            }
            if (CaveBiomeIds.isFungalCaveBiome(biome) && (fPath.contains("fungal") || fPath.contains("bioshroom") || fPath.contains("mushroom") || fPath.contains("mycel"))) {
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
        if (theme != null && (fPath.contains(theme) || "embur".equals(theme) && fPath.contains("bog"))) {
            return true;
        }
        if (CaveFeatureFilters.matchesEmburBiomeFeature(fPath, bPath)) {
            return true;
        }
        if (CaveFeatureFilters.matchesScorchingBiomeFeature(fPath, bPath)) {
            return true;
        }
        String slug = CaveFeatureFilters.biomeFeatureSlug(bPath);
        if (slug.length() >= 3 && (fPath.contains(slug) || fPath.contains("cave/" + slug))) {
            return true;
        }
        if (fPath.contains("cave/") && CaveFeatureFilters.sharesCaveSlug(bPath, fPath)) {
            return true;
        }
        return CaveFeatureFilters.isSameNamespaceBiomeFeature(fNs, bNs, fPath, bPath);
    }

    private static boolean isSameNamespaceBiomeFeature(String fNs, String bNs, String fPath, String bPath) {
        if (!fNs.equals(bNs) || fPath.contains("deferred_feature") || fPath.contains("/global/")) {
            return false;
        }
        String biomeLeaf = bPath.contains("/") ? bPath.substring(bPath.lastIndexOf(47) + 1) : bPath;
        if (biomeLeaf.length() >= 4 && fPath.contains(biomeLeaf.replace("_caves", "").replace("_cave", ""))) {
            return true;
        }
        return fPath.contains("cave/") || fPath.contains("/cave/") || fPath.contains("underground");
    }

    private static boolean matchesScorchingBiomeFeature(String fPath, String bPath) {
        if (!bPath.contains("scorching") && !bPath.contains("brimstone") && !bPath.contains("volcanic") && !bPath.contains("mantle")) {
            return false;
        }
        return fPath.contains("scorch") || fPath.contains("charred") || fPath.contains("ash") && !fPath.contains("smolder") || fPath.contains("vent") || fPath.contains("basalt") || fPath.contains("magma") || fPath.contains("volcanic") || fPath.contains("brimstone") || fPath.contains("frostfire_patch") || fPath.contains("yellowstone");
    }

    public static boolean isDeferredOrGlobalFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("deferred_feature") || path.contains("/global/") || path.contains("global/raw");
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
        if (CaveFeatureFilters.isDeadWoodFeature(fPath)) {
            return true;
        }
        String bPath = ((ResourceKey<Biome>) biomeKey.get()).location().getPath().toLowerCase();
        if (bPath.contains("mantle") && CaveFeatureFilters.isForeignVentFeature(fPath)) {
            return true;
        }
        if (bPath.contains("brimstone") && CaveFeatureFilters.isForeignVentFeature(fPath) && !fPath.contains("ash_vent")) {
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
        if (fPath.contains("fungal") && !bPath.contains("fungal") && !bPath.contains("bioshroom")) {
            return true;
        }
        if (fPath.contains("bioshroom") && !bPath.contains("bioshroom") && !bPath.contains("fungal")) {
            return true;
        }
        if (fPath.contains("thermal") && !bPath.contains("thermal")) {
            return true;
        }
        if (fPath.contains("frostfire") && !bPath.contains("frostfire")) {
            return true;
        }
        if ((fPath.contains("ash_vent") || fPath.contains("volcanic") || fPath.contains("scorching")) && !bPath.contains("scorching") && !bPath.contains("volcanic") && !bPath.contains("ash") && !bPath.contains("brimstone")) {
            return true;
        }
        if (fPath.contains("glowshroom") && !bPath.contains("glowshroom") && !bPath.contains("fungal") && !bPath.contains("mycotoxic")) {
            return true;
        }
        if (CaveFeatureFilters.isForeignForestInVolcanicBiome(fPath, bPath)) {
            return true;
        }
        return false;
    }

    private static boolean isForeignForestInVolcanicBiome(String fPath, String bPath) {
        if (!CaveFeatureFilters.isVolcanicBiomePath(bPath) && !bPath.contains("scorching")) {
            return false;
        }
        if (fPath.contains("scorching") || fPath.contains("scorch") || fPath.contains("charred") || fPath.contains("ash") && !fPath.contains("smolder")) {
            return false;
        }
        return fPath.contains("smolder") || fPath.contains("forest") && !fPath.contains("underground") || fPath.contains("leaf") || fPath.contains("canopy") || fPath.contains("redwood") || fPath.contains("nightshade") && !bPath.contains("nightshade") || fPath.contains("tree") && !fPath.contains("charred");
    }

    private static boolean matchesEmburBiomeFeature(String fPath, String bPath) {
        if (!bPath.contains("embur") && !bPath.contains("embur_bog")) {
            return false;
        }
        return fPath.contains("embur") || fPath.contains("bog") || fPath.contains("peat") || fPath.contains("mulch") || fPath.contains("mycelium") || fPath.contains("mushroom") && !fPath.contains("nether") || fPath.contains("fungus") && !fPath.contains("nether");
    }

    public static boolean isForeignWildNatureFeature(String fNs, String fPath, String bPath, Holder<Biome> biome) {
        if (!"wildernature".equals(fNs) && !"wildnature".equals(fNs)) {
            return false;
        }
        if (fPath.contains("glowshroom")) {
            return !bPath.contains("glowshroom") && !CaveBiomeIds.isFungalCaveBiome(biome);
        }
        if (fPath.contains("mushroom") || fPath.contains("shroom") || fPath.contains("fungus") || fPath.contains("mycel")) {
            return !CaveBiomeIds.isFungalCaveBiome(biome);
        }
        String slug = CaveFeatureFilters.biomeFeatureSlug(bPath);
        return slug.length() < 3 || !fPath.contains(slug);
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
        if (biomePath.contains("scorching")) {
            return "scorching";
        }
        if (biomePath.contains("volcanic") || biomePath.contains("ash")) {
            return "volcanic";
        }
        if (biomePath.contains("bioshroom")) {
            return "fungal";
        }
        return null;
    }

    private static boolean isVolcanicBiomePath(String biomePath) {
        return biomePath.contains("thermal") || biomePath.contains("mantle") || biomePath.contains("brimstone") || biomePath.contains("magma") || biomePath.contains("scorching") || biomePath.contains("volcanic");
    }

    private static boolean isVolcanicAccentFeature(String fPath) {
        return fPath.contains("ash_vent") || fPath.contains("ash") || fPath.contains("geyser") || fPath.contains("/vents") || fPath.contains("magma") || fPath.contains("basalt") || fPath.contains("lava_drip") || fPath.contains("scorching") || fPath.contains("volcanic");
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
        return biomePath.contains("cave") || biomePath.contains("cavern") || biomePath.contains("grotto") || biomePath.contains("hypogeal") || biomePath.contains("prismachasm") || biomePath.contains("undergarden") || biomePath.contains("mycotoxic") || biomePath.contains("scorching") || biomePath.contains("bioshroom") || biomePath.contains("embur") || biomePath.contains("crimson_gardens") || biomePath.contains("shattered_glacier") || biomePath.contains("nightshade") || biomePath.contains("quartz_desert") || biomePath.contains("ancient_delta") || biomePath.contains("brimstone");
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
        if (CaveFeatureFilters.isDeadWoodFeature(path)) {
            return false;
        }
        if (CaveFeatureFilters.isBlockedLushFeature(path) && !CaveFeatureFilters.isCoverFeaturePath(path)) {
            return false;
        }
        if (CaveFeatureFilters.isMegaHandledColumnFeature(path, biome)) {
            return false;
        }
        if (CaveFeatureFilters.isHeavyDripstoneFeature(path) && !CaveFeatureFilters.allowsDripstoneFeatures(biome)) {
            return false;
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome) && path.contains("cave/stone/")) {
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
        if (CaveFeatureFilters.isForeignWildNatureFeature(id.getNamespace(), path, biome.unwrapKey().map(key -> key.location().getPath().toLowerCase()).orElse(""), biome)) {
            return false;
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
        if (CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome)) {
            return path.contains("grass") || path.contains("flower") || path.contains("bamboo") || path.contains("vine") || path.contains("jungle") || path.contains("patch");
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            if (path.contains("tall_grass") || path.contains("bamboo") || path.contains("flower_jungle")) {
                return false;
            }
            return path.contains("grass") || path.contains("moss") && path.contains("patch") || path.contains("bioshroom") && (path.contains("grass") || path.contains("moss") || path.contains("block"));
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return path.contains("scorch") || path.contains("charred") || path.contains("ash") || path.contains("vent") || path.contains("grass") || path.contains("fern") || path.contains("flower") || path.contains("patch") || path.contains("vine") || path.contains("moss") || path.contains("fungus") || path.contains("mushroom");
        }
        if (CaveBiomeIds.isPrismachasmBiome(biome) || CaveBiomeIds.isCrystalCaveBiome(biome)) {
            return path.contains("prism") || path.contains("hyssop") || path.contains("crystal") || path.contains("moss") || path.contains("grass") || path.contains("patch") || path.contains("cluster") || path.contains("lichen") || path.contains("amethyst");
        }
        return false;
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
        if (id != null && CaveFeatureFilters.isBlockedLushFeature(id.getPath().toLowerCase()) && !CaveFeatureFilters.isCoverFeaturePath(id.getPath())) {
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
        if (path.contains("geode") || path.contains("mega_geode") || path.contains("crystal_geode")) {
            return true;
        }
        if (CaveFeatureFilters.isBlockedLushFeature(path) && !CaveFeatureFilters.isCoverFeaturePath(path)) {
            return true;
        }
        for (String blocked : BLOCKED_PATH) {
            if (!path.contains(blocked)) continue;
            if (biome != null && CaveBiomeIds.isCrystalCaveBiome(biome) && path.contains("cave/crystal") && blocked.equals("amethyst") && !path.contains("geode")) continue;
            return true;
        }
        return false;
    }

    public static boolean isCoverFeaturePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String lower = path.toLowerCase();
        if (lower.contains("vent") || lower.contains("geyser") || lower.contains("geode") || lower.contains("monster_room")) {
            return false;
        }
        return lower.contains("cover") || lower.contains("carpet") || lower.contains("replacer") || lower.contains("tiles") || lower.contains("fuck_art") || lower.contains("/split/") || lower.contains("ground") || lower.contains("spread") || lower.contains("floor") && !lower.contains("underfloor") || lower.contains("coarse") || lower.contains("mycelium") || lower.contains("scorch") || lower.contains("charred") || lower.contains("ash") && lower.contains("patch") || lower.contains("frostfire_patch") || lower.contains("moss") && (lower.contains("patch") || lower.contains("block")) || lower.contains("calcite") || lower.contains("prismoss") && !lower.contains("cluster") || lower.contains("bioshroom") && (lower.contains("block") || lower.contains("grass") || lower.contains("moss"));
    }

    public static boolean isDeadWoodFeature(String path) {
        return path.contains("dead_leaves") || path.contains("dead_log") || path.contains("dead_leaf") || path.contains("dead/") || path.contains("/dead/") || path.contains("fallen_log") || path.contains("deadwood") || path.contains("dead_fungus") || path.contains("dead_tree");
    }

    private static boolean isBlockedLushFeature(String path) {
        return path.contains("lush_caves") || path.contains("blooming_caves") || path.contains("spore_blossom") || path.contains("azalea_tree") || path.contains("moss_vegetation") || path.contains("moss_patch") || path.contains("moss_carpet");
    }

    private static boolean isMegaHandledColumnFeature(String path, Holder<Biome> biome) {
        if (path.contains("prismarite") || path.contains("prismoss") || path.contains("hyssop")) {
            return biome.unwrapKey().map(key -> key.location().getPath().toLowerCase().contains("prismachasm")).orElse(false) == false;
        }
        return CaveFeatureFilters.isHeavyDripstoneFeature(path);
    }

    public static boolean isHeavyDripstoneFeature(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.contains("large_dripstone") || path.contains("pointed_dripstone") || path.contains("dripstone_cluster") || path.contains("stalactite") || path.contains("cave/ice/icicle");
    }

    public static boolean allowsDripstoneFeatures(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath().toLowerCase();
            return path.contains("dripstone") || path.contains("karst") || path.contains("grotto") || path.contains("tuff_cave") || path.contains("tuff_caves") || path.contains("limestone") || path.contains("icicle");
        }).orElse(false);
    }

    private static boolean isVolumeStage(int stageIndex) {
        return stageIndex == GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal() || stageIndex == GenerationStep.Decoration.VEGETAL_DECORATION.ordinal() || stageIndex == GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal();
    }
}
