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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveBiomeFeatureRunner {
    private CaveBiomeFeatureRunner() {
    }

    public static void decorateFungalCover(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), floorAnchor.getX(), floorAnchor.getZ());
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(floorAnchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!CaveBiomeFeatureRunner.isFungalCoverFeature((Holder<PlacedFeature>)placed) || !CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 400, stageIndex);
                if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
                return;
            }
        }
    }

    private static boolean isFungalCoverFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("cover") || path.contains("carpet") || path.contains("mycelium") || path.contains("coarse") || path.contains("moss") && path.contains("patch") || path.contains("ground") || path.contains("spread") || path.contains("floor");
    }

    public static void decorateFloorAndCeiling(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random, boolean includeTrees) {
        BlockPos ceilAnchor;
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        CaveBiomeFeatureRunner.decorateScatter(floorAnchor, false, chunk, region, placement, generator, biome, settings, random);
        if (includeTrees) {
            CaveBiomeFeatureRunner.decorateTrees(floorAnchor, chunk, placement, generator, biome, settings, random);
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            CaveBiomeFeatureRunner.decorateVolcanicVents(floorAnchor, chunk, carver, region, placement, generator, biome, settings, random);
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorAnchor.getY() + 4, maxY);
        if (ceilY > floorAnchor.getY() + 5 && CaveBiomeFeatureRunner.mayPlace(chunk, carver, ceilAnchor = new BlockPos(floorAnchor.getX(), ceilY, floorAnchor.getZ()), generator, biome)) {
            Holder<Biome> ceilBiome = carver.resolveBiome(chunk, lx, ceilY, lz);
            if (!CaveBiomeIds.isModCaveBiome(ceilBiome)) {
                ceilBiome = biome;
            }
            WorldGenLevel ceilPlacement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, ceilBiome, carver);
            BiomeGenerationSettings ceilSettings = ((Biome)ceilBiome.value()).getGenerationSettings();
            CaveBiomeFeatureRunner.decorateScatter(ceilAnchor, true, chunk, region, ceilPlacement, generator, ceilBiome, ceilSettings, random);
        }
    }

    private static void decorateScatter(BlockPos anchor, boolean ceiling, ChunkAccess chunk, WorldGenLevel underground, WorldGenLevel biomePlacement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(underground.getSeed(), anchor.getX(), anchor.getZ());
        int scatterBudget = CaveBiomeFeatureRunner.scatterBudgetFor(biome);
        int placedCount = 0;
        int attempts = 0;
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                if (!CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || DynamicTreesCompat.isLoaded() && CaveBiomeIds.isFungalCaveBiome(biome) && (DynamicTreesCompat.isDynamicTreesFeature((Holder<PlacedFeature>)placed) || !ceiling && CaveBiomeFeatureRunner.isVanillaFungalScatter((Holder<PlacedFeature>)placed)) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome((Holder<PlacedFeature>)placed, biome) || !CaveBiomeFeatureRunner.matchesAnchor((Holder<PlacedFeature>)placed, ceiling) || FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || !ceiling && !CaveBiomeFeatureRunner.allowsFloorScatter((Holder<PlacedFeature>)placed, biome) || !ceiling && CaveFeatureFilters.requiresSolidFloor((Holder<PlacedFeature>)placed) && !CaveFeaturePlacement.hasSolidFloorBelow(chunk, anchor) || ceiling && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, anchor.getX(), anchor.getY(), anchor.getZ(), 1)) continue;
                random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
                CaveFeatureRules.Anchor kind = ceiling ? CaveFeatureRules.Anchor.CEILING : CaveFeatureRules.Anchor.FLOOR;
                BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(anchor, kind, false);
                ++attempts;
                if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, biomePlacement, (ChunkGenerator)generator, (Random)random, placePos, true) || ++placedCount < scatterBudget) continue;
                return;
            }
        }
        if (!ceiling && placedCount == 0 && attempts > 0) {
            CaveBiomeFeatureRunner.decorateScatterFallback(anchor, chunk, underground, generator, biome, settings, random);
        }
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

    private static int scatterBudgetFor(Holder<Biome> biome) {
        if (CaveBiomeIds.isFungalCaveBiome(biome)) {
            return DynamicTreesCompat.isLoaded() ? 3 : 4;
        }
        return Integer.MAX_VALUE;
    }

    private static void decorateVolcanicVents(BlockPos floorAnchor, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, WorldGenLevel placement, Generator generator, Holder<Biome> biome, BiomeGenerationSettings settings, WorldgenRandom random) {
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor) || random.nextFloat() > 0.78f) {
            return;
        }
        int lx = floorAnchor.getX() & 0xF;
        int lz = floorAnchor.getZ() & 0xF;
        int floorY = floorAnchor.getY();
        for (int dy = 0; dy >= -6; --dy) {
            int y = floorY + dy;
            if (y <= chunk.getMinBuildHeight() || !FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, y, lz, 2)) continue;
            floorY = y;
            break;
        }
        if (!FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, floorY, lz, 2)) {
            return;
        }
        List stages = settings.features();
        long baseSeed = random.setDecorationSeed(region.getSeed(), floorAnchor.getX(), floorAnchor.getZ());
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(floorAnchor.getX(), floorY, floorAnchor.getZ()), CaveFeatureRules.Anchor.FLOOR, false);
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder placed = stage.get(featureIndex);
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null || !CaveBiomeFeatureRunner.isVolcanicVentFeature(id.getPath()) || !CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome)) continue;
                random.setFeatureSeed(baseSeed, featureIndex + 500, stageIndex);
                if (FeaturePlacement.place((Holder<PlacedFeature>)placed, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) {
                    return;
                }
            }
        }
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
        if (FeatureMassClassifier.isCeilingScatter(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed)) {
            return false;
        }
        return !CaveBiomeFeatureRunner.isCeilingPath(placed) || FeatureMassClassifier.isDualSurfaceFeature(placed);
    }

    private static boolean isCeilingPath(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("ceiling") || path.contains("hanging") || path.contains("cobweb") || path.contains("icicle") || path.contains("/ice/") || path.contains("prismarite") || path.contains("prismoss");
    }
}
