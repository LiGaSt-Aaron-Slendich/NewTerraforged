package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.fml.ModList;

public final class DynamicTreesCompat {
    private static final String MOD_ID = "dynamictrees";
    private static Boolean loaded;

    private DynamicTreesCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null && (loaded = Boolean.valueOf(ModList.get().isLoaded(MOD_ID))).booleanValue()) {
            TerraForged.LOG.info("[DynamicTreesCompat] Dynamic Trees detected \u0432\u0402\u201d surface + fungal cave VEGETAL_DECORATION");
        }
        return loaded;
    }

    public static boolean isDynamicTreesFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        return id != null && MOD_ID.equals(id.getNamespace());
    }

    public static boolean useCaveVegetationPass(Holder<Biome> biome) {
        if (!DynamicTreesCompat.isLoaded()) {
            return false;
        }
        return CaveBiomeIds.isFungalCaveBiome(biome);
    }

    public static int maxCaveDecorationStage(Holder<Biome> biome) {
        if (DynamicTreesCompat.useCaveVegetationPass(biome)) {
            return GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        }
        return GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
    }

    public static void decorateFungalCave(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos floorAnchor, WorldgenRandom random) {
        List stages;
        if (!DynamicTreesCompat.useCaveVegetationPass(biome)) {
            return;
        }
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, floorAnchor)) {
            return;
        }
        int stageIndex = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        if (stageIndex >= (stages = ((Biome)biome.value()).getGenerationSettings().features()).size()) {
            return;
        }
        HolderSet stage = (HolderSet)stages.get(stageIndex);
        if (stage == null || stage.size() == 0) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        long baseSeed = random.setDecorationSeed(region.getSeed(), floorAnchor.getX(), floorAnchor.getZ());
        BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(floorAnchor, CaveFeatureRules.Anchor.FLOOR, false);
        for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
            Holder placed = stage.get(featureIndex);
            if (!DynamicTreesCompat.isDynamicTreesFeature((Holder<PlacedFeature>)placed)) continue;
            random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
            if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, placement, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
            return;
        }
    }
}
