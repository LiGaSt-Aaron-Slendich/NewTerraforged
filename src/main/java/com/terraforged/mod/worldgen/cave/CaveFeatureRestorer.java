package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fast underground cleanup: strip cave trees and fix floating / embedded mushrooms.
 */
public final class CaveFeatureRestorer {
    private static final int MIN_CAVE_AIR = 10;
    private static final int SURFACE_CRUST = 2;
    /** Only scan this many blocks above the lowest cave air — mushrooms sit on the floor band. */
    private static final int FLOOR_BAND = 48;
    private static final int MIN_FLOOR_DEPTH = 2;
    private static final int MAX_AIR_BELOW_SUPPORT = 2;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private CaveFeatureRestorer() {
    }

    public static int restore(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null || !carver.isColumnCacheReady()) {
            return 0;
        }
        CarverColumnCache columns = carver.columnCache();
        if (!columns.anyMegaGiga() && !columns.anySynapseEligible()) {
            return 0;
        }
        int[][] ranges = CaveFeatureRestorer.buildScanRanges(chunk);
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = ranges[lx | lz << 4];
                if (range == null) {
                    continue;
                }
                int mushroomTop = Math.min(range[1], range[0] + FLOOR_BAND);
                for (int y = range[0]; y <= range[1]; ++y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (CaveFeatureRestorer.isTreeMaterial(state)) {
                        chunk.setBlockState(pos, AIR, false);
                        ++removed;
                        continue;
                    }
                    if (y > mushroomTop || !CaveFeatureRestorer.isMushroomOrVegetation(state)) {
                        continue;
                    }
                    if (CaveFeatureRestorer.isEmbeddedInStone(chunk, lx, y, lz, state)
                            || !CaveFeatureRestorer.hasCaveFloorSupport(chunk, lx, y, lz, range[0])) {
                        chunk.setBlockState(pos, AIR, false);
                        ++removed;
                    }
                }
            }
        }
        return removed;
    }

    private static int[][] buildScanRanges(ChunkAccess chunk) {
        int[][] ranges = new int[256][];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int minY = Math.max(chunk.getMinBuildHeight() + 8, surface - 96);
                int topAir = -1;
                int bottomAir = Integer.MAX_VALUE;
                int air = 0;
                for (int y = surface; y >= minY; --y) {
                    if (!chunk.getBlockState(pos.set(lx, y, lz)).isAir()) {
                        continue;
                    }
                    ++air;
                    topAir = y;
                    bottomAir = Math.min(bottomAir, y);
                }
                if (air < MIN_CAVE_AIR || topAir < 0) {
                    continue;
                }
                int scanTop = Math.min(topAir, surface - SURFACE_CRUST);
                if (scanTop < bottomAir) {
                    scanTop = surface - SURFACE_CRUST;
                }
                if (scanTop >= bottomAir) {
                    ranges[lx | lz << 4] = new int[]{bottomAir, scanTop};
                }
            }
        }
        return ranges;
    }

    private static boolean isTreeMaterial(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS);
    }

    private static boolean isMushroomOrVegetation(BlockState state) {
        if (state.isAir() || CaveFeatureRestorer.isTreeMaterial(state)) {
            return false;
        }
        return state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.SHROOMLIGHT)
                || state.is(Blocks.WARPED_WART_BLOCK) || state.is(Blocks.NETHER_WART_BLOCK)
                || state.is(Blocks.HANGING_ROOTS) || state.is(Blocks.SPORE_BLOSSOM)
                || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.VINE);
    }

    private static boolean isEmbeddedInStone(ChunkAccess chunk, int lx, int y, int lz, BlockState state) {
        if (!CaveFeatureRestorer.isMushroomBlock(state)) {
            return false;
        }
        int solidSides = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            BlockState neighbor = chunk.getBlockState(pos.set(lx + offset[0], y + offset[1], lz + offset[2]));
            if (neighbor.isAir() || CaveFeatureRestorer.isMushroomBlock(neighbor)) {
                continue;
            }
            if (neighbor.isSolidRender((BlockGetter)chunk, pos)) {
                ++solidSides;
            }
        }
        return solidSides >= 2;
    }

    private static boolean isMushroomBlock(BlockState state) {
        return state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.SHROOMLIGHT);
    }

    private static boolean hasCaveFloorSupport(ChunkAccess chunk, int lx, int y, int lz, int caveFloor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int below = y - 1;
        if (below < caveFloor) {
            return false;
        }
        BlockState floor = chunk.getBlockState(pos.set(lx, below, lz));
        if (floor.isAir() || !floor.getFluidState().isEmpty() || !floor.isSolidRender((BlockGetter)chunk, pos)) {
            return false;
        }
        int solid = 1;
        for (int dy = 2; dy <= MIN_FLOOR_DEPTH + 2; ++dy) {
            int by = y - dy;
            if (by < chunk.getMinBuildHeight()) {
                break;
            }
            BlockState state = chunk.getBlockState(pos.set(lx, by, lz));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                break;
            }
            if (state.isSolidRender((BlockGetter)chunk, pos) && ++solid >= MIN_FLOOR_DEPTH) {
                break;
            }
        }
        if (solid < MIN_FLOOR_DEPTH) {
            return false;
        }
        int supportBottom = below;
        while (supportBottom >= caveFloor && CaveFeatureRestorer.isSupportColumn(chunk.getBlockState(pos.set(lx, supportBottom, lz)))) {
            --supportBottom;
        }
        ++supportBottom;
        int airBelow = 0;
        for (int by = supportBottom - 1; by >= Math.max(caveFloor, supportBottom - 12); --by) {
            if (chunk.getBlockState(pos.set(lx, by, lz)).isAir() && ++airBelow > MAX_AIR_BELOW_SUPPORT) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSupportColumn(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.LOGS) || !CaveFeatureRestorer.isMushroomOrVegetation(state) && !CaveFeatureRestorer.isTreeMaterial(state);
    }
}
