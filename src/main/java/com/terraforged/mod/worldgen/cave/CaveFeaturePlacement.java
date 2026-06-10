package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveFeaturePlacement {
    private CaveFeaturePlacement() {
    }

    public static boolean mayPlace(Holder<PlacedFeature> feature, CaveFeatureRules.Anchor anchor, BlockPos airAnchor, ChunkAccess chunk) {
        if (FeatureMassClassifier.isTree(feature)) {
            return false;
        }
        if (FeatureMassClassifier.classify(feature) == FeatureMass.BLOCKED) {
            return false;
        }
        int lx = airAnchor.getX() & 0xF;
        int ly = airAnchor.getY();
        int lz = airAnchor.getZ() & 0xF;
        return switch (anchor) {
            default -> throw new IncompatibleClassChangeError();
            case FLOOR -> FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, ly, lz, 1);
            case CEILING -> FeaturePlacement.hasStableCeiling((BlockGetter)chunk, lx, ly, lz, 1);
        };
    }

    public static BlockPos resolveWorldPos(BlockPos airAnchor, CaveFeatureRules.Anchor anchor, boolean topLayer) {
        return switch (anchor) {
            default -> throw new IncompatibleClassChangeError();
            case FLOOR -> airAnchor.below();
            case CEILING -> airAnchor.above();
        };
    }

    public static boolean hasSolidFloorBelow(ChunkAccess chunk, BlockPos airAnchor) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        return FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, airAnchor.getY(), lz, 1);
    }

    public static int massPriority(FeatureMass mass) {
        return switch (mass) {
            case LARGE -> 0;
            case MEDIUM -> 1;
            case SMALL -> 2;
            case SCATTER -> 3;
            default -> 4;
        };
    }
}
