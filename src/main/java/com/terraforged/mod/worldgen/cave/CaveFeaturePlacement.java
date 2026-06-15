package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.noise.util.NoiseUtil;
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

    public static BlockPos resolveScatterPos(BlockPos airAnchor, CaveFeatureRules.Anchor anchor, long scatterSeed, int featureIndex, int stageIndex) {
        BlockPos base = CaveFeaturePlacement.resolveWorldPos(airAnchor, anchor, false);
        float nx = NoiseUtil.valCoord2D((int)(scatterSeed ^ (long)featureIndex), airAnchor.getX() + stageIndex * 3, airAnchor.getZ());
        float nz = NoiseUtil.valCoord2D((int)(scatterSeed ^ (long)stageIndex ^ 0x9E3779B9L), airAnchor.getX(), airAnchor.getZ() + featureIndex * 5);
        int ox = Math.round(nx * 5.0f);
        int oz = Math.round(nz * 5.0f);
        if (ox == 0 && oz == 0) {
            ox = featureIndex % 3 - 1;
            oz = stageIndex % 3 - 1;
        }
        return base.offset(ox, 0, oz);
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
        for (int dy = 0; dy >= -8; --dy) {
            int y = airAnchor.getY() + dy;
            if (y <= chunk.getMinBuildHeight()) {
                break;
            }
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                continue;
            }
            if (FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, y, lz, 1)) {
                return true;
            }
        }
        return false;
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
