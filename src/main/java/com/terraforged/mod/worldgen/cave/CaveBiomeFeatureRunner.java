package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.compat.DynamicTreesCompat;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveBiomeFeatureRunner {
    private static final int MAX_SCATTER_ATTEMPTS = 64;

    private CaveBiomeFeatureRunner() {
    }

    public static void decorateFungalCover(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        CaveBiomeFeatureRunner.decorateThemedCover(chunk, carver, region, generator, biome, floorAnchor, random, CaveBiomeFeatureRunner::isFungalCoverFeature, 0);
    }

    public static void decorateCrystalCover(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        CaveBiomeFeatureRunner.decorateThemedCover(chunk, carver, region, generator, biome, floorAnchor, random, CaveBiomeFeatureRunner::isCrystalCoverFeature, 100);
    }

    public static void decorateBiomeCover(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            CaveBiomeFeatureRunner.decorateFungalCover(chunk, carver, region, generator, biome, floorAnchor, random);
            return;
        }
        if (CaveBiomeIds.isCrystalCaveBiome(biome) && !CaveBiomeIds.isPrismachasmBiome(biome) && !CaveBiomeIds.isSkyrisCaveBiome(biome)) {
            CaveBiomeFeatureRunner.decorateCrystalCover(chunk, carver, region, generator, biome, floorAnchor, random);
            return;
        }
        java.util.function.Predicate<Holder<PlacedFeature>> matcher;
        int seedSalt;
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            matcher = CaveBiomeFeatureRunner::isScorchingCoverFeature;
            seedSalt = 200;
        } else if (CaveBiomeIds.isPrismachasmBiome(biome)) {
            matcher = CaveBiomeFeatureRunner::isPrismachasmCoverFeature;
            seedSalt = 250;
        } else if (CaveBiomeIds.isSkyrisCaveBiome(biome)) {
            matcher = CaveBiomeFeatureRunner::isSkyrisCoverFeature;
            seedSalt = 255;
        } else {
            matcher = CaveBiomeFeatureRunner::isGenericCaveCoverFeature;
            seedSalt = 300;
        }
        CaveBiomeFeatureRunner.decorateThemedCover(chunk, carver, region, generator, biome, floorAnchor, random, matcher, seedSalt);
    }

    private static void decorateThemedCover(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random, java.util.function.Predicate<Holder<PlacedFeature>> matcher, int seedSalt) {
        floorAnchor = CaveFloorCover.prepare(chunk, carver, biome, floorAnchor);
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), floorAnchor.getX(), floorAnchor.getZ());
        int placed = 0;
        int coverBudget = CaveBiomeFeatureRunner.coverBudgetFor(biome);
        int[][] offsets = CaveBiomeFeatureRunner.coverOffsets(coverBudget);
        block0: for (int[] offset : offsets) {
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(floorAnchor.offset(offset[0], 0, offset[1]), CaveFeatureRules.Anchor.FLOOR, false);
            if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, placePos)) continue;
            for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
                HolderSet stage;
                if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
                for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                    Holder placedFeature = stage.get(featureIndex);
                    if (!matcher.test((Holder<PlacedFeature>)placedFeature) || !CaveBiomeFeatureRunner.isCoverFeatureAllowed((Holder<PlacedFeature>)placedFeature, biome)) continue;
                    random.setFeatureSeed(baseSeed, featureIndex + seedSalt + offset[0] * 7 + offset[1] * 13, stageIndex);
                    if (!FeaturePlacement.place((Holder<PlacedFeature>)placedFeature, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
                    if (++placed >= coverBudget) {
                        break block0;
                    }
                    continue block0;
                }
            }
        }
        if (placed == 0 && !CaveBiomeFeatureRunner.decorateCoverFallback(chunk, carver, region, generator, biome, floorAnchor, random, placement, settings, baseSeed, seedSalt)) {
            CaveBiomeFeatureRunner.decorateStoneCoverFallback(chunk, placement, generator, biome, floorAnchor, settings, random, baseSeed, seedSalt);
        }
        if (placed == 0 && CaveFloorCover.appliesTo(biome)) {
            CaveFloorCover.prepare(chunk, carver, biome, floorAnchor);
        }
    }

    private static boolean isCoverFeatureAllowed(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null || CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return false;
        }
        boolean coverPath = CaveFeatureFilters.isCoverFeaturePath(id.getPath());
        if (!CaveFeatureFilters.isModCaveFeatureAllowed(placed, biome) && !coverPath) {
            return false;
        }
        return CaveFeatureFilters.belongsToModCaveBiome(placed, biome);
    }

    private static int coverBudgetFor(Holder<Biome> biome) {
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            return 14;
        }
        if (CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 12;
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return 11;
        }
        if (CaveBiomeIds.isModCaveBiome(biome)) {
            return 10;
        }
        return 8;
    }

    private static int[][] coverOffsets(int budget) {
        if (budget >= 6) {
            return new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}, {2, 0}, {0, 2}, {-2, 0}, {0, -2}, {2, 1}, {-2, -1}, {1, 2}, {-1, -2}};
        }
        if (budget >= 5) {
            return new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        }
        if (budget >= 4) {
            return new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {0, 2}};
        }
        return new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    }

    private static void decorateStoneCoverFallback(ChunkAccess chunk, WorldGenLevel placement, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, BiomeGenerationSettings settings, WorldgenRandom random, long baseSeed, int seedSalt) {
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        List stages = settings.features();
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(floorAnchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placedFeature = stage.get(featureIndex);
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placedFeature);
                if (id == null || CaveFeatureFilters.isDeferredOrGlobalFeature((Holder<PlacedFeature>)placedFeature) || CaveFeatureFilters.isForbiddenForCaveBiome((Holder<PlacedFeature>)placedFeature, biome)) continue;
                String path = id.getPath().toLowerCase();
                if (!path.contains("patch") && !path.contains("spread") && !path.contains("ground") && !path.contains("moss") && !path.contains("cover") && !path.contains("carpet") && !path.contains("scorch") && !path.contains("ash") && !path.contains("mycelium")) continue;
                if (!CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placedFeature, biome)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + seedSalt + 950, stageIndex);
                if (FeaturePlacement.place((Holder<PlacedFeature>)placedFeature, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) {
                    return;
                }
            }
        }
    }

    private static boolean decorateCoverFallback(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random, WorldGenLevel placement, BiomeGenerationSettings settings, long baseSeed, int seedSalt) {
        List stages = settings.features();
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(floorAnchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placedFeature = stage.get(featureIndex);
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placedFeature);
                if (id == null || !CaveFeatureFilters.isCoverFeaturePath(id.getPath()) || !CaveBiomeFeatureRunner.isCoverFeatureAllowed((Holder<PlacedFeature>)placedFeature, biome)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + seedSalt + 900, stageIndex);
                if (FeaturePlacement.place((Holder<PlacedFeature>)placedFeature, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isFungalCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("cover") || path.contains("carpet") || path.contains("mycelium") || path.contains("coarse") || path.contains("moss") && path.contains("patch") || path.contains("ground") || path.contains("spread") || path.contains("floor") || path.contains("fungal") || path.contains("mushroom") && !path.contains("nether") || path.contains("bioshroom") && (path.contains("block") || path.contains("grass") || path.contains("moss") || path.contains("cover"));
    }

    private static boolean isCrystalCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("geode")) {
            return false;
        }
        return path.contains("cover") || path.contains("carpet") || path.contains("floor") || path.contains("spread") || path.contains("calcite") || path.contains("moss") && path.contains("patch") || path.contains("crystal") && (path.contains("block") || path.contains("floor") || path.contains("patch")) || path.contains("replacer") || path.contains("tiles") || path.contains("fuck_art") || path.contains("/split/");
    }

    private static boolean isScorchingCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("vent") || path.contains("geyser") || path.contains("geode")) {
            return false;
        }
        return path.contains("scorch") || path.contains("ash") || path.contains("charred") || path.contains("basalt_strip") || path.contains("magma_strip") || path.contains("frostfire_patch") || path.contains("yellowstone") && (path.contains("floor") || path.contains("cover") || path.contains("patch") || path.contains("spread")) || path.contains("cover") || path.contains("carpet") || path.contains("floor") || path.contains("spread") || path.contains("replacer") || path.contains("tiles");
    }

    private static boolean isPrismachasmCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("prismoss") || path.contains("prismarite") && !path.contains("cluster") || path.contains("hyssop") || path.contains("cover") || path.contains("carpet") || path.contains("floor") || path.contains("spread");
    }

    private static boolean isSkyrisCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("geode")) {
            return false;
        }
        return path.contains("skyris") || path.contains("cover") || path.contains("carpet") || path.contains("floor") || path.contains("spread") || path.contains("moss") && path.contains("patch") || path.contains("grass") || path.contains("fern");
    }

    private static boolean isEmburCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("geode") || path.contains("vent") || path.contains("geyser")) {
            return false;
        }
        return path.contains("embur") || path.contains("bog") || path.contains("peat") || path.contains("mulch") || path.contains("mycelium") || path.contains("mushroom") && !path.contains("nether") || path.contains("cover") || path.contains("carpet") || path.contains("floor") || path.contains("spread") || path.contains("ground") || path.contains("moss") && path.contains("patch");
    }

    private static boolean isGenericCaveCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("geode") || path.contains("vent") || path.contains("geyser") || path.contains("monster_room")) {
            return false;
        }
        if (path.contains("column") && !path.contains("cover")) {
            return false;
        }
        return path.contains("cover") || path.contains("carpet") || path.contains("coarse") || path.contains("mycelium") || path.contains("replacer") || path.contains("tiles") || path.contains("fuck_art") || path.contains("/split/") || path.contains("ground") || path.contains("spread") || path.contains("floor") || path.contains("moss_block") || path.contains("sculk") || path.contains("frostfire_patch") || path.contains("snow") && path.contains("patch") || path.contains("moss") && path.contains("patch") || path.contains("lichen") && path.contains("patch") || path.contains("ice/") && (path.contains("floor") || path.contains("cover") || path.contains("patch")) || path.contains("charred") || path.contains("scorch") && path.contains("patch") || path.contains("ash") && path.contains("patch") || path.contains("mulch") || path.contains("peat") || path.contains("bog") || path.contains("calcite") && path.contains("patch");
    }

    static boolean isBiomeCoverFeature(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            return CaveBiomeFeatureRunner.isFungalCoverFeature(placed);
        }
        if (CaveBiomeIds.isCrystalCaveBiome(biome) && !CaveBiomeIds.isPrismachasmBiome(biome)) {
            return CaveBiomeFeatureRunner.isCrystalCoverFeature(placed);
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return CaveBiomeFeatureRunner.isScorchingCoverFeature(placed);
        }
        if (CaveBiomeIds.isPrismachasmBiome(biome)) {
            return CaveBiomeFeatureRunner.isPrismachasmCoverFeature(placed);
        }
        if (CaveBiomeIds.isSkyrisCaveBiome(biome)) {
            return CaveBiomeFeatureRunner.isSkyrisCoverFeature(placed);
        }
        return CaveBiomeFeatureRunner.isGenericCaveCoverFeature(placed);
    }

    public static void decorateNativeUndergroundJungle(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random, boolean includeTrees) {
        BlockPos ceilAnchor;
        if (!CaveBiomeIds.isUndergroundJungleBiome(biome) || !CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        String biomeNs = biome.unwrapKey().map(key -> key.location().getNamespace()).orElse("");
        CaveBiomeFeatureRunner.decorateNativeScatter(floorAnchor, false, chunk, region, placement, generator, biome, biomeNs, settings, random, includeTrees);
        if (includeTrees) {
            CaveBiomeFeatureRunner.decorateNativeTrees(floorAnchor, chunk, placement, generator, biome, biomeNs, settings, random);
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorAnchor.getY() + 4, maxY);
        if (ceilY > floorAnchor.getY() + 5 && CaveBiomeFeatureRunner.mayPlaceCeiling(chunk, carver, ceilAnchor = new BlockPos(floorAnchor.getX(), ceilY, floorAnchor.getZ()), generator, biome)) {
            Holder<Biome> ceilBiome = carver.resolveBiome(chunk, lx, ceilY, lz);
            if (!CaveBiomeIds.isModCaveBiome(ceilBiome)) {
                ceilBiome = biome;
            }
            String ceilNs = ceilBiome.unwrapKey().map(key -> key.location().getNamespace()).orElse(biomeNs);
            WorldGenLevel ceilPlacement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, ceilBiome, carver);
            BiomeGenerationSettings ceilSettings = ((Biome)ceilBiome.value()).getGenerationSettings();
            CaveBiomeFeatureRunner.decorateNativeScatter(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, ceilNs, ceilSettings, random, false);
        }
    }

    public static void decorateLightFloorAndCeiling(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        CaveFeaturePlan.Cache planCache = new CaveFeaturePlan.Cache();
        CaveBiomeFeatureRunner.decorateScatter(floorAnchor, false, chunk, region, placement, generator, biome, settings, random, 8, 32);
        CaveBiomeFeatureRunner.decoratePlannedFeatures(floorAnchor, false, chunk, region, placement, generator, biome, random, planCache, 4);
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            CaveBiomeFeatureRunner.decorateVolcanicVents(floorAnchor, chunk, carver, region, placement, generator, biome, settings, random);
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorAnchor.getY() + 4, maxY);
        BlockPos ceilAnchor;
        if (ceilY > floorAnchor.getY() + 5 && CaveBiomeFeatureRunner.mayPlaceCeiling(chunk, carver, ceilAnchor = new BlockPos(floorAnchor.getX(), ceilY, floorAnchor.getZ()), generator, biome)) {
            Holder<Biome> ceilBiome = carver.resolveBiome(chunk, lx, ceilY, lz);
            if (!CaveBiomeIds.isModCaveBiome(ceilBiome)) {
                ceilBiome = biome;
            }
            WorldGenLevel ceilPlacement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, ceilBiome, carver);
            BiomeGenerationSettings ceilSettings = ((Biome)ceilBiome.value()).getGenerationSettings();
            CaveBiomeFeatureRunner.decorateScatter(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, ceilSettings, random, 6, 24);
            CaveBiomeFeatureRunner.decoratePlannedFeatures(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, random, planCache, 4);
        }
    }

    public static void decorateFloorAndCeiling(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random, boolean includeTrees) {
        BlockPos ceilAnchor;
        floorAnchor = CaveFloorCover.prepare(chunk, carver, biome, floorAnchor);
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        CaveFeaturePlan.Cache planCache = new CaveFeaturePlan.Cache();
        CaveBiomeFeatureRunner.decorateScatter(floorAnchor, false, chunk, region, placement, generator, biome, settings, random, 6, 28);
        CaveBiomeFeatureRunner.decoratePlannedFeatures(floorAnchor, false, chunk, region, placement, generator, biome, random, planCache, CaveBiomeIds.isCoverDenseCaveBiome(biome) ? 6 : 5);
        if (includeTrees) {
            CaveBiomeFeatureRunner.decorateTrees(floorAnchor, chunk, placement, generator, biome, settings, random);
        }
        if ((CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) && !MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, floorAnchor.getX(), floorAnchor.getY(), floorAnchor.getZ())) {
            CaveBiomeFeatureRunner.decorateVolcanicVents(floorAnchor, chunk, carver, region, placement, generator, biome, settings, random);
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorAnchor.getY() + 4, maxY);
        if (ceilY > floorAnchor.getY() + 5 && CaveBiomeFeatureRunner.mayPlaceCeiling(chunk, carver, ceilAnchor = new BlockPos(floorAnchor.getX(), ceilY, floorAnchor.getZ()), generator, biome)) {
            Holder<Biome> ceilBiome = carver.resolveBiome(chunk, lx, ceilY, lz);
            if (!CaveBiomeIds.isModCaveBiome(ceilBiome)) {
                ceilBiome = biome;
            }
            WorldGenLevel ceilPlacement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, ceilBiome, carver);
            BiomeGenerationSettings ceilSettings = ((Biome)ceilBiome.value()).getGenerationSettings();
            CaveBiomeFeatureRunner.decorateScatter(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, ceilSettings, random, 5, 22);
            CaveBiomeFeatureRunner.decoratePlannedFeatures(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, random, planCache, CaveBiomeIds.isCoverDenseCaveBiome(biome) ? 7 : 6);
        }
    }

    private static void decoratePlannedFeatures(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel region, WorldGenLevel placement, Generator generator, Holder<Biome> biome, WorldgenRandom random, CaveFeaturePlan.Cache planCache, int maxPlacements) {
        CaveFeatureRules.Anchor kind = ceiling ? CaveFeatureRules.Anchor.CEILING : CaveFeatureRules.Anchor.FLOOR;
        CaveFeaturePlan.StageFeature[] candidates = planCache.get(biome).forAnchor(kind);
        if (candidates.length == 0) {
            return;
        }
        candidates = CaveBiomeFeatureRunner.orderBiomeFeaturesFirst(candidates, biome);
        long baseSeed = random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
        int placed = 0;
        for (CaveFeaturePlan.StageFeature entry : candidates) {
            if (FeatureMassClassifier.spawnsSurfaceVegetation(entry.feature())) continue;
            if (!CaveFeatureFilters.isModCaveFeatureAllowed(entry.feature(), biome) || !CaveFeatureFilters.belongsToModCaveBiome(entry.feature(), biome) || CaveFeatureFilters.isForbiddenForCaveBiome(entry.feature(), biome)) continue;
            if (CaveBiomeFeatureRunner.isGlobalCaveHelper(entry.feature())) continue;
            if (!CaveBiomeFeatureRunner.matchesAnchor(entry.feature(), ceiling)) continue;
            if (!ceiling && CaveFeatureFilters.requiresSolidFloor(entry.feature()) && !CaveFeaturePlacement.hasSolidFloorBelow(chunk, anchor)) continue;
            if (ceiling && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, anchor.getX() & 0xF, anchor.getY(), anchor.getZ() & 0xF, 1)) continue;
            random.setFeatureSeed(baseSeed, entry.featureIndex() + (ceiling ? 800 : 700), entry.stageIndex());
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, kind, entry.topLayer());
            if (!FeaturePlacement.place(entry.feature(), placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
            if (++placed >= maxPlacements) {
                return;
            }
        }
        for (CaveFeaturePlan.StageFeature entry : candidates) {
            if (!CaveBiomeFeatureRunner.isGlobalCaveHelper(entry.feature())) continue;
            if (!CaveBiomeFeatureRunner.matchesAnchor(entry.feature(), ceiling)) continue;
            if (ceiling && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, anchor.getX() & 0xF, anchor.getY(), anchor.getZ() & 0xF, 1)) continue;
            random.setFeatureSeed(baseSeed, entry.featureIndex() + (ceiling ? 900 : 850), entry.stageIndex());
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, kind, entry.topLayer());
            if (!FeaturePlacement.place(entry.feature(), placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
            if (++placed >= maxPlacements) {
                return;
            }
        }
    }

    private static CaveFeaturePlan.StageFeature[] orderBiomeFeaturesFirst(CaveFeaturePlan.StageFeature[] candidates, Holder<Biome> biome) {
        CaveFeaturePlan.StageFeature[] copy = candidates.clone();
        java.util.Arrays.sort(copy, (a, b) -> {
            boolean aBio = CaveFeatureFilters.belongsToModCaveBiome(a.feature(), biome) && !CaveBiomeFeatureRunner.isGlobalCaveHelper(a.feature());
            boolean bBio = CaveFeatureFilters.belongsToModCaveBiome(b.feature(), biome) && !CaveBiomeFeatureRunner.isGlobalCaveHelper(b.feature());
            if (aBio != bBio) {
                return aBio ? -1 : 1;
            }
            return Integer.compare(CaveFeaturePlacement.massPriority(a.mass()), CaveFeaturePlacement.massPriority(b.mass()));
        });
        return copy;
    }

    private static boolean isGlobalCaveHelper(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if ("minecraft".equals(id.getNamespace())) {
            return path.contains("glow_lichen") || path.contains("classic_vines") || path.contains("vines") || path.contains("patch_");
        }
        return path.contains("glow_lichen") && !path.contains("cave/");
    }

    private static void decorateScatter(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel underground, WorldGenLevel biomePlacement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        CaveBiomeFeatureRunner.decorateScatter(anchor, ceiling, chunk, underground, biomePlacement, generator, biome, settings, random, -1, MAX_SCATTER_ATTEMPTS);
    }

    private static void decorateScatter(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel underground, WorldGenLevel biomePlacement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random, int scatterBudgetOverride, int maxAttempts) {
        int placed = CaveBiomeFeatureRunner.decorateScatterPass(anchor, ceiling, chunk, underground, biomePlacement, generator, biome, settings, random, scatterBudgetOverride, maxAttempts, true);
        if (placed == 0 || scatterBudgetOverride < 0) {
            CaveBiomeFeatureRunner.decorateScatterPass(anchor, ceiling, chunk, underground, biomePlacement, generator, biome, settings, random, scatterBudgetOverride, maxAttempts, false);
        }
    }

    private static int decorateScatterPass(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel underground, WorldGenLevel biomePlacement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random, int scatterBudgetOverride, int maxAttempts, boolean biomeOnly) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(underground.getSeed(), anchor.getX(), anchor.getZ());
        int scatterBudget = scatterBudgetOverride >= 0 ? scatterBudgetOverride : (ceiling ? CaveBiomeFeatureRunner.scatterCeilingBudgetFor(biome) : CaveBiomeFeatureRunner.scatterBudgetFor(biome));
        int mushroomBudget = CaveBiomeFeatureRunner.mushroomScatterBudget(biome);
        int placedCount = 0;
        int mushroomPlaced = 0;
        int attempts = 0;
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!ceiling && CaveBiomeFeatureRunner.isSmallMushroomScatter((Holder<PlacedFeature>)placed) && mushroomPlaced >= mushroomBudget) continue;
                if (!ceiling && CaveBiomeFeatureRunner.isLargeVerticalMushroom((Holder<PlacedFeature>)placed) && mushroomPlaced >= 1) continue;
                if (CaveFeatureFilters.isDeferredOrGlobalFeature((Holder<PlacedFeature>)placed) || !CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || DynamicTreesCompat.isLoaded() && CaveBiomeIds.isFungalCaveBiome(biome) && (DynamicTreesCompat.isDynamicTreesFeature((Holder<PlacedFeature>)placed) || !ceiling && CaveBiomeFeatureRunner.isVanillaFungalScatter((Holder<PlacedFeature>)placed)) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome((Holder<PlacedFeature>)placed, biome) || !CaveBiomeFeatureRunner.matchesAnchor((Holder<PlacedFeature>)placed, ceiling) || FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || !ceiling && !CaveBiomeFeatureRunner.allowsFloorScatter((Holder<PlacedFeature>)placed, biome) || !ceiling && CaveFeatureFilters.requiresSolidFloor((Holder<PlacedFeature>)placed) && !CaveFeaturePlacement.hasSolidFloorBelow(chunk, anchor) || ceiling && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, anchor.getX(), anchor.getY(), anchor.getZ(), 1)) continue;
                if (biomeOnly && CaveBiomeFeatureRunner.isGlobalCaveHelper((Holder<PlacedFeature>)placed)) continue;
                if (!biomeOnly && !CaveBiomeFeatureRunner.isGlobalCaveHelper((Holder<PlacedFeature>)placed)) continue;
                random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
                CaveFeatureRules.Anchor kind = ceiling ? CaveFeatureRules.Anchor.CEILING : CaveFeatureRules.Anchor.FLOOR;
                BlockPos placePos = CaveFeaturePlacement.resolveScatterPos(anchor, kind, baseSeed, featureIndex, stageIndex);
                if (++attempts > maxAttempts) {
                    return placedCount;
                }
                if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, biomePlacement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
                if (!ceiling && (CaveBiomeFeatureRunner.isSmallMushroomScatter((Holder<PlacedFeature>)placed) || CaveBiomeFeatureRunner.isLargeVerticalMushroom((Holder<PlacedFeature>)placed))) {
                    ++mushroomPlaced;
                }
                if (++placedCount < scatterBudget) continue;
                return placedCount;
            }
        }
        if (!ceiling && placedCount == 0 && attempts > 0 && biomeOnly) {
            return 0;
        }
        if (!ceiling && placedCount == 0 && attempts > 0 && !biomeOnly) {
            CaveBiomeFeatureRunner.decorateScatterFallback(anchor, chunk, underground, generator, biome, settings, random);
        }
        return placedCount;
    }

    private static void decorateScatterFallback(BlockPos anchor, ChunkAccess chunk, WorldGenLevel underground, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(underground.getSeed(), anchor.getX(), anchor.getZ());
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome((Holder<PlacedFeature>)placed, biome) || !CaveBiomeFeatureRunner.matchesAnchor((Holder<PlacedFeature>)placed, false) || FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 200, stageIndex);
                BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, CaveFeatureRules.Anchor.FLOOR, false);
                if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, underground, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
                return;
            }
        }
    }

    private static int scatterCeilingBudgetFor(Holder<Biome> biome) {
        if (CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 20;
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return 18;
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            return 12;
        }
        return 14;
    }

    private static int scatterBudgetFor(Holder<Biome> biome) {
        if (CaveBiomeIds.isCrystalCaveBiome(biome) && !CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 20;
        }
        if (CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 24;
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return 22;
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            return DynamicTreesCompat.isLoaded() ? 5 : 8;
        }
        return 12;
    }

    private static int mushroomScatterBudget(Holder<Biome> biome) {
        if (!CaveBiomeIds.isFungalCaveBiome(biome)) {
            return Integer.MAX_VALUE;
        }
        return 1;
    }

    private static boolean isLargeVerticalMushroom(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("huge") && (path.contains("mushroom") || path.contains("fungus") || path.contains("shroom"));
    }

    private static boolean isSmallMushroomScatter(Holder<PlacedFeature> placed) {
        if (FeatureMassClassifier.isTree(placed) || FeatureMassClassifier.isCaveFloorLarge(placed)) {
            return false;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("huge") || path.contains("big_") || path.contains("mega_") || path.contains("giant") || path.contains("colony") || path.contains("glowshroom_cap")) {
            return false;
        }
        return path.contains("patch_mushroom") || path.contains("mushroom") || path.contains("shroom") || path.contains("fungus");
    }

    private static void decorateNativeScatter(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel underground, WorldGenLevel biomePlacement, Generator generator, Holder<Biome> biome, String biomeNs, BiomeGenerationSettings settings, WorldgenRandom random, boolean includeTrees) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(underground.getSeed(), anchor.getX(), anchor.getZ());
        int scatterBudget = ceiling ? 4 : 8;
        int placedCount = 0;
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!CaveBiomeFeatureRunner.isNativeBiomeFeatureAllowed((Holder<PlacedFeature>)placed, biome, biomeNs) || !includeTrees && FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || !CaveBiomeFeatureRunner.matchesAnchor((Holder<PlacedFeature>)placed, ceiling) || !ceiling && CaveFeatureFilters.requiresSolidFloor((Holder<PlacedFeature>)placed) && !CaveFeaturePlacement.hasSolidFloorBelow(chunk, anchor) || ceiling && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, anchor.getX(), anchor.getY(), anchor.getZ(), 1)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 400, stageIndex);
                CaveFeatureRules.Anchor kind = ceiling ? CaveFeatureRules.Anchor.CEILING : CaveFeatureRules.Anchor.FLOOR;
                BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, kind, false);
                if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, biomePlacement, (ChunkGenerator)generator, (Random)random, placePos, true) || ++placedCount < scatterBudget) continue;
                return;
            }
        }
    }

    private static void decorateNativeTrees(BlockPos anchor, ChunkAccess chunk, WorldGenLevel region, Generator generator, Holder<Biome> biome, String biomeNs, BiomeGenerationSettings settings, WorldgenRandom random) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || !CaveBiomeFeatureRunner.isNativeBiomeFeatureAllowed((Holder<PlacedFeature>)placed, biome, biomeNs)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 500, stageIndex);
                FeaturePlacement.place((Holder<PlacedFeature>)placed, region, (ChunkGenerator)generator, (Random)random, placePos, true);
            }
        }
    }

    private static boolean isNativeBiomeFeatureAllowed(Holder<PlacedFeature> placed, Holder<Biome> biome, String biomeNs) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        String bPath = biome.unwrapKey().map(key -> key.location().getPath().toLowerCase()).orElse("");
        if (path.contains("monster_room") || path.contains("fossil") || path.contains("geode") || path.startsWith("ore_") || path.contains("/ore_") || path.contains("lake_lava") || path.contains("lake_water") || path.contains("spring_lava") || path.contains("spring_water") || path.contains("disk_sand") || path.contains("disk_gravel") || path.contains("freeze_top") || path.contains("dripleaf") || path.contains("spore_blossom") || path.contains("azalea") || path.contains("lush_caves") || path.contains("blooming_caves")) {
            return false;
        }
        if (CaveFeatureFilters.isForeignWildNatureFeature(id.getNamespace(), path, bPath, biome)) {
            return false;
        }
        if (path.contains("yellowstone/") || path.contains("frostfire") || path.contains("fungal") && !bPath.contains("fungal")) {
            return false;
        }
        String ns = id.getNamespace();
        return "minecraft".equals(ns) || ns.equals(biomeNs);
    }

    private static void decorateVolcanicVents(BlockPos floorAnchor, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, WorldGenLevel placement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        int naturalFloor = CaveBiomeFeatureRunner.findNaturalVentFloor(chunk, floorAnchor.getX() & 0xF, floorAnchor.getZ() & 0xF, floorAnchor.getY(), chunk.getMinBuildHeight());
        if (naturalFloor < 0) {
            return;
        }
        floorAnchor = new BlockPos(floorAnchor.getX(), naturalFloor, floorAnchor.getZ());
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int floorY = floorAnchor.getY();
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), floorAnchor.getX(), floorAnchor.getZ());
        int clusterSize = CaveBiomeIds.isScorchingCaveBiome(biome) ? 1 + random.nextInt(2) : 1;
        block0: for (int attempt = 0; attempt < clusterSize; ++attempt) {
            int ox = attempt == 0 ? 0 : -2 + random.nextInt(5);
            int oz = attempt == 0 ? 0 : -2 + random.nextInt(5);
            int px = floorAnchor.getX() + ox;
            int pz = floorAnchor.getZ() + oz;
            int plx = px & 0xF;
            int plz = pz & 0xF;
            int py = CaveBiomeFeatureRunner.findNaturalVentFloor(chunk, plx, plz, floorY, chunk.getMinBuildHeight());
            if (py < 0) continue;
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(px, py, pz), CaveFeatureRules.Anchor.FLOOR, false);
            for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
                HolderSet stage;
                if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
                for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                    Holder placedFeature = stage.get(featureIndex);
                    ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placedFeature);
                    if (id == null || !CaveBiomeFeatureRunner.isVolcanicVentFeature(id.getPath()) || !CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placedFeature, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placedFeature, biome)) continue;
                    random.setFeatureSeed(baseSeed, featureIndex + 500 + attempt * 17, stageIndex);
                    if (!FeaturePlacement.place((Holder<PlacedFeature>)placedFeature, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
                    continue block0;
                }
            }
        }
    }

    private static int findNaturalVentFloor(ChunkAccess chunk, int lx, int lz, int startY, int minY) {
        for (int y = startY; y >= minY && y >= startY - 8; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                continue;
            }
            BlockState below = chunk.getBlockState(new BlockPos(lx, y - 1, lz));
            if (below.isAir() || !below.isSolidRender((BlockGetter)chunk, new BlockPos(lx, y - 1, lz))) {
                continue;
            }
            if (CaveBiomeFeatureRunner.isNaturalStoneFloor(below)) {
                return y;
            }
        }
        return -1;
    }

    private static boolean isNaturalStoneFloor(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean isVolcanicVentFeature(String path) {
        String lower = path.toLowerCase();
        return lower.contains("ash_vent") || lower.contains("/vents") || lower.contains("geyser");
    }

    private static void decorateTrees(BlockPos anchor, ChunkAccess chunk, WorldGenLevel region, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || !CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 100, stageIndex);
                FeaturePlacement.place((Holder<PlacedFeature>)placed, region, (ChunkGenerator)generator, (Random)random, placePos, true);
            }
        }
    }

    static boolean mayPlace(ChunkAccess chunk, CarverChunk carver, BlockPos anchor, Generator generator, Holder<Biome> biome) {
        int lx = anchor.getX() & 0xF;
        int lz = anchor.getZ() & 0xF;
        boolean megaGiga = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, anchor.getX(), anchor.getY(), anchor.getZ());
        return CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, anchor.getY(), lz, biome, megaGiga, carver.isEntranceColumn(lx, lz));
    }

    static boolean mayPlaceCeiling(ChunkAccess chunk, CarverChunk carver, BlockPos anchor, Generator generator, Holder<Biome> biome) {
        int lx = anchor.getX() & 0xF;
        int lz = anchor.getZ() & 0xF;
        boolean megaGiga = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, anchor.getX(), anchor.getY(), anchor.getZ());
        return CaveUndergroundGuard.mayPlaceCeilingAnchorForBiome(chunk, carver, lx, anchor.getY(), lz, biome, megaGiga, carver.isEntranceColumn(lx, lz));
    }

    private static boolean allowsFloorScatter(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        ResourceLocation id;
        if (FeatureMassClassifier.spawnsSurfaceVegetation(placed)) {
            return CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome);
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome) && (id = FeatureMassClassifier.featurePath(placed)) != null) {
            String path = id.getPath().toLowerCase();
            if (path.contains("vine") || path.contains("lichen") || path.contains("glow_lichen")) {
                return false;
            }
            if (CaveBiomeFeatureRunner.isFungalCoverFeature(placed)) {
                return false;
            }
            return path.contains("mushroom") || path.contains("fungal") || path.contains("moss") || path.contains("patch") || path.contains("root") || path.contains("hanging");
        }
        if (CaveBiomeFeatureRunner.isBiomeCoverFeature(placed, biome)) {
            return false;
        }
        ResourceLocation scatterId = FeatureMassClassifier.featurePath(placed);
        if (scatterId != null) {
            String scatterPath = scatterId.getPath().toLowerCase();
            if (CaveBiomeFeatureRunner.isVolcanicVentFeature(scatterPath) || scatterPath.contains("geyser")) {
                return false;
            }
            if (FeatureMassClassifier.isCaveFloorLarge(placed) || scatterPath.contains("huge") || scatterPath.contains("mega_") || scatterPath.contains("colony")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isVanillaFungalScatter(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("mushroom") || path.contains("fungal");
    }

    private static boolean matchesAnchor(Holder<PlacedFeature> placed, boolean ceiling) {
        if (ceiling) {
            return FeatureMassClassifier.isCeilingScatter(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed) || CaveBiomeFeatureRunner.isCeilingPath(placed);
        }
        if (CaveBiomeFeatureRunner.isFrostfireFloorColumn(placed) || CaveBiomeFeatureRunner.isFloorClusterFeature(placed)) {
            return true;
        }
        if (FeatureMassClassifier.isCeilingScatter(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed)) {
            return false;
        }
        return !CaveBiomeFeatureRunner.isCeilingPath(placed) || FeatureMassClassifier.isDualSurfaceFeature(placed);
    }

    private static boolean isFloorClusterFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("hanging") || path.contains("stalactite") || path.contains("icicle")) {
            return false;
        }
        return path.contains("prismoss") || path.contains("hyssop") || path.contains("prismachasm") || path.contains("ash_vent") || path.contains("scorching") || path.contains("cluster") && !path.contains("ceiling") && !path.contains("hanging");
    }

    private static boolean isFrostfireFloorColumn(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("frostfire/columns") || path.contains("frostfire/pillar");
    }

    private static boolean isCeilingPath(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("ceiling") || path.contains("hanging") || path.contains("cobweb") || path.contains("icicle") || path.contains("/ice/") || path.contains("hanging_prismarite") || path.contains("stalactite") || path.contains("prismarite") && path.contains("cluster") || path.contains("bud") && path.contains("amethyst");
    }
}
