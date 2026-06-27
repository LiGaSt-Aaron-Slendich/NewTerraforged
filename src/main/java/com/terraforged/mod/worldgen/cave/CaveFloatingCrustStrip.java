package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Removes grass/logs/leaves and other surface crust left in mega/giga cave columns after decoration.
 */
public final class CaveFloatingCrustStrip {
    private static final int MIN_CAVE_AIR_RUN = 3;

    private CaveFloatingCrustStrip() {
    }

    public static void stripMegaGigaChunk(ChunkAccess chunk, CarverColumnCache columns) {
        if (columns == null || !columns.anyMegaGiga()) {
            return;
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!columns.isMegaGigaZone(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                if (!CaveFloatingCrustStrip.columnHasCaveAir(chunk, lx, lz, minY, Math.min(maxY, surface - 2))) {
                    continue;
                }
                for (int y = Math.min(maxY, surface - 1); y >= minY; --y) {
                    CaveFloatingCrustStrip.stripIfCaveVegetation(chunk, pos, lx, y, lz, minY);
                }
            }
        }
    }

    private static boolean columnHasCaveAir(ChunkAccess chunk, int lx, int lz, int minY, int scanTop) {
        int run = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= scanTop; ++y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                if (++run >= MIN_CAVE_AIR_RUN) {
                    return true;
                }
                continue;
            }
            run = 0;
        }
        return false;
    }

    private static void stripIfCaveVegetation(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int lx, int y, int lz, int minY) {
        pos.set(lx, y, lz);
        BlockState state = chunk.getBlockState(pos);
        if (!CaveFloatingCrustStrip.isCaveVegetationBlock(state)) {
            return;
        }
        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
    }

    private static boolean isCaveVegetationBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT)
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING);
    }
}
