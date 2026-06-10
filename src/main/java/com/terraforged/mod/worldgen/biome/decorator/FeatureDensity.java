package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.biome.decorator.FeatureDensityBudget;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FeatureDensity {
    private FeatureDensity() {
    }

    public static boolean tryPlace(PlacedFeature feature, FeatureDensityBudget budget, int localX, int localZ, WorldGenLevel level, ChunkGenerator generator, Random random, BlockPos pos, boolean modBiome) {
        FeatureMass mass = FeatureMassClassifier.classify(feature);
        if (mass == FeatureMass.BLOCKED || !budget.canPlace(mass, localX, localZ)) {
            return false;
        }
        if (FeatureMassClassifier.isTree(feature) && !FeaturePlacement.hasStableGround((BlockGetter)level, pos, 2)) {
            return false;
        }
        if (FeaturePlacement.place(feature, level, generator, random, pos, modBiome)) {
            budget.record(mass, localX, localZ);
            return true;
        }
        return false;
    }
}
