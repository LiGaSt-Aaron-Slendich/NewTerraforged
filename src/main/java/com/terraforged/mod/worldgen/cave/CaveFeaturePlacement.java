package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
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
        int ox = Math.round(nx * 3.0f);
        int oz = Math.round(nz * 3.0f);
        if (ox == 0 && oz == 0) {
            ox = featureIndex % 3 - 1;
            oz = stageIndex % 3 - 1;
        }
        return base.offset(ox, 0, oz);
    }

    public static BlockPos validateScatterPlacement(ChunkAccess chunk, BlockPos airAnchor, CaveFeatureRules.Anchor anchor, long scatterSeed, int featureIndex, int stageIndex, int minFloorDepth) {
        BlockPos placePos = CaveFeaturePlacement.resolveScatterPos(airAnchor, anchor, scatterSeed, featureIndex, stageIndex);
        int ox = placePos.getX() - airAnchor.getX();
        int oz = placePos.getZ() - airAnchor.getZ();
        BlockPos offsetAir = airAnchor.offset(ox, 0, oz);
        offsetAir = CaveFeaturePlacement.snapScatterAirAnchor(chunk, offsetAir);
        int lx = offsetAir.getX() & 0xF;
        int lz = offsetAir.getZ() & 0xF;
        int y = offsetAir.getY();
        if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
            return null;
        }
        if (anchor == CaveFeatureRules.Anchor.FLOOR) {
            if (minFloorDepth > 0 && !CaveFeaturePlacement.hasConnectedFloor(chunk, offsetAir, minFloorDepth)) {
                return null;
            }
            if (!FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, y, lz, 1)) {
                return null;
            }
        } else if (!CaveFeaturePlacement.hasConnectedCeiling(chunk, offsetAir, 2)) {
            return null;
        }
        return CaveFeaturePlacement.resolveWorldPos(offsetAir, anchor, false);
    }

    /** Walk down to the nearest walkable floor air above solid ground (slopes / stepped terrain). */
    public static BlockPos snapScatterAirAnchor(ChunkAccess chunk, BlockPos airAnchor) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        int startY = Math.min(airAnchor.getY() + 2, chunk.getMaxBuildHeight() - 2);
        for (int y = startY; y >= chunk.getMinBuildHeight() + 1; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                continue;
            }
            if (FeaturePlacement.hasStableGround((BlockGetter)chunk, lx, y, lz, 1)) {
                return new BlockPos(airAnchor.getX(), y, airAnchor.getZ());
            }
        }
        return airAnchor;
    }

    public static BlockPos resolveWorldPos(BlockPos airAnchor, CaveFeatureRules.Anchor anchor, boolean topLayer) {
        return switch (anchor) {
            default -> throw new IncompatibleClassChangeError();
            case FLOOR -> airAnchor.below();
            case CEILING -> airAnchor.above();
        };
    }

    public static boolean hasConnectedFloor(ChunkAccess chunk, BlockPos airAnchor, int minSolidDepth) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        int floorY = airAnchor.getY() - 1;
        if (floorY <= chunk.getMinBuildHeight()) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState floor = chunk.getBlockState(pos.set(lx, floorY, lz));
        if (floor.isAir() || !floor.getFluidState().isEmpty() || !floor.isSolidRender((BlockGetter)chunk, pos)) {
            return false;
        }
        int solid = 1;
        for (int dy = 2; dy <= minSolidDepth + 4; ++dy) {
            int y = airAnchor.getY() - dy;
            if (y <= chunk.getMinBuildHeight()) {
                break;
            }
            BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return false;
            }
            if (state.isSolidRender((BlockGetter)chunk, pos) && ++solid >= minSolidDepth) {
                return true;
            }
        }
        return solid >= minSolidDepth;
    }

    public static boolean hasConnectedCeiling(ChunkAccess chunk, BlockPos airAnchor, int minSolidDepth) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        int ceilingY = airAnchor.getY() + 1;
        if (ceilingY >= chunk.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState ceiling = chunk.getBlockState(pos.set(lx, ceilingY, lz));
        if (ceiling.isAir() || !ceiling.getFluidState().isEmpty() || !ceiling.isSolidRender((BlockGetter)chunk, pos)) {
            return false;
        }
        int solid = 1;
        for (int dy = 2; dy <= minSolidDepth + 4; ++dy) {
            int y = airAnchor.getY() + dy;
            if (y >= chunk.getMaxBuildHeight()) {
                break;
            }
            BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return false;
            }
            if (state.isSolidRender((BlockGetter)chunk, pos) && ++solid >= minSolidDepth) {
                return true;
            }
        }
        return solid >= minSolidDepth;
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
