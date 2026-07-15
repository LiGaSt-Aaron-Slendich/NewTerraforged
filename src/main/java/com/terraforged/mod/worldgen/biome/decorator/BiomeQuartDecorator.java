package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CavePlacementFilter;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * TerraBlender-style chunk decoration: features at surface positions with {@code placeWithBiomeCheck},
 * one pass per decoration stage (P4#21).
 */
public final class BiomeQuartDecorator {
    private static final int GRID = 2;

    private BiomeQuartDecorator() {
    }

    public static void decorateStages(long seed, int fromStage, int toStage, ChunkAccess chunk, WorldGenLevel level,
            Generator generator, WorldgenRandom random, StructureFeatureManager structures, FeatureDecorator decorator,
            boolean placeStructures) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        for (int stage = fromStage; stage <= toStage; ++stage) {
            if (placeStructures) {
                VanillaDecorator.decorateStructuresOnly(seed, stage, chunk, level, generator, random, structures, decorator);
            }
            int structureOffset = placeStructures ? decorator.getStageStructures(stage).size() : 0;
            for (int dz = 0; dz < 16; dz += BiomeQuartDecorator.GRID) {
                for (int dx = 0; dx < 16; dx += BiomeQuartDecorator.GRID) {
                    int x = chunkMinX + dx;
                    int z = chunkMinZ + dz;
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                    if (y <= generator.getSeaLevel()) {
                        continue;
                    }
                    BlockPos origin = new BlockPos(x, y, z);
                    Holder<Biome> biome = level.getBiome(origin);
                    if (CaveBiomeIds.isUndergroundBiome(biome)) {
                        continue;
                    }
                    HolderSet<PlacedFeature> features = decorator.getStageFeatures(stage, biome.value());
                    if (features == null || features.size() == 0) {
                        continue;
                    }
                    if (CavePlacementFilter.shouldSkipTree(generator, chunk, x, y, z)) {
                        continue;
                    }
                    BiomeQuartDecorator.placeFeatures(seed, structureOffset, stage, origin, chunk, level, generator, random, features);
                }
            }
        }
    }

    private static void placeFeatures(long seed, int offset, int stage, BlockPos origin, ChunkAccess chunk,
            WorldGenLevel level, Generator generator, WorldgenRandom random, HolderSet<PlacedFeature> features) {
        int wx = origin.getX();
        int wz = origin.getZ();
        boolean megaCave = MegaCaveStructureFilter.isInMegaOrGigaCave(generator, wx, wz);
        for (int i = 0; i < features.size(); ++i) {
            Holder<PlacedFeature> feature = features.get(i);
            if (megaCave && VanillaDecorator.isBlockedMegaGigaFeature(feature)) {
                continue;
            }
            if (FeatureMassClassifier.isTree(feature) && (megaCave || !FeaturePlacement.hasStableGround(level, origin, 2))) {
                continue;
            }
            random.setFeatureSeed(seed, offset + i, stage);
            try {
                feature.value().placeWithBiomeCheck(level, (ChunkGenerator) generator, (Random) random, origin);
            }
            catch (RuntimeException ignored) {
            }
        }
    }
}
