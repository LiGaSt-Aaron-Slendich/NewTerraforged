package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
            TerraForged.LOG.info("[DynamicTreesCompat] Dynamic Trees detected — fungal cave VEGETAL_DECORATION after cover");
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
        BlockPos treeAir = DynamicTreesCompat.resolveTreeAirAnchor(chunk, floorAnchor);
        if (treeAir == null) {
            return;
        }
        int lx = treeAir.getX() & 0xF;
        int lz = treeAir.getZ() & 0xF;
        BlockState soil = chunk.getBlockState(new BlockPos(lx, treeAir.getY() - 1, lz));
        if (!DynamicTreesCompat.canAcceptRootyDirt(soil)) {
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
        long baseSeed = random.setDecorationSeed(region.getSeed(), treeAir.getX(), treeAir.getZ());
        for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
            Holder placed = stage.get(featureIndex);
            if (!DynamicTreesCompat.isDynamicTreesFeature((Holder<PlacedFeature>)placed)) continue;
            random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
            if (!FeaturePlacement.place((Holder<PlacedFeature>)placed, placement, (ChunkGenerator)generator, (Random)random, treeAir, true)) continue;
            return;
        }
    }

    private static BlockPos resolveTreeAirAnchor(ChunkAccess chunk, BlockPos hint) {
        int lx = hint.getX() & 0xF;
        int lz = hint.getZ() & 0xF;
        int startY = Math.min(hint.getY() + 2, chunk.getHighestSectionPosition() + 15);
        for (int y = startY; y >= chunk.getMinBuildHeight() + 1; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir() || !FeaturePlacement.hasStableGround(chunk, lx, y, lz, 1)) continue;
            return new BlockPos(hint.getX(), y, hint.getZ());
        }
        return null;
    }

    private static boolean canAcceptRootyDirt(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.DIRT) || state.is(Blocks.MYCELIUM) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MOSS_BLOCK)) {
            return true;
        }
        String name = state.getBlock().getDescriptionId().toLowerCase();
        return name.contains("mycel") || name.contains("bioshroom") || name.contains("fung") && name.contains("block") || name.contains("mulch") || name.contains("peat");
    }
}
