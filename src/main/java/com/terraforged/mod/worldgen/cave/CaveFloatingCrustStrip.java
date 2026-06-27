package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Removes surface vegetation and tree parts inside mega/giga cave volumes after decoration.
 * Trees span multiple columns — purge uses a horizontal radius, not single-column strips.
 */
public final class CaveFloatingCrustStrip {
    private static final int MIN_CAVE_AIR_RUN = 3;
    private static final int TREE_PURGE_RADIUS = 6;

    private CaveFloatingCrustStrip() {
    }

    public static void stripMegaGigaChunk(ChunkAccess chunk, CarverColumnCache columns) {
        if (columns == null || !columns.anyMegaGiga()) {
            return;
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        boolean[][] caveAir = CaveFloatingCrustStrip.buildCaveAirMask(chunk, columns, minY, maxY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!caveAir[lx][lz]) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                for (int y = Math.min(maxY, surface - 1); y >= minY; --y) {
                    if (!CaveFloatingCrustStrip.touchesCaveAir(caveAir, lx, lz, y)) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (CaveFloatingCrustStrip.isCaveVegetationBlock(state)) {
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static boolean[][] buildCaveAirMask(ChunkAccess chunk, CarverColumnCache columns, int minY, int scanTop) {
        boolean[][] mask = new boolean[16][16];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!columns.isMegaGigaZone(lx, lz)) {
                    continue;
                }
                int run = 0;
                for (int y = minY; y <= scanTop; ++y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        if (++run >= MIN_CAVE_AIR_RUN) {
                            mask[lx][lz] = true;
                            break;
                        }
                        continue;
                    }
                    run = 0;
                }
            }
        }
        return mask;
    }

    private static boolean touchesCaveAir(boolean[][] caveAir, int lx, int y, int lz) {
        for (int dx = -TREE_PURGE_RADIUS; dx <= TREE_PURGE_RADIUS; ++dx) {
            for (int dz = -TREE_PURGE_RADIUS; dz <= TREE_PURGE_RADIUS; ++dz) {
                int nx = lx + dx;
                int nz = lz + dz;
                if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16 || !caveAir[nx][nz]) {
                    continue;
                }
                if (dx * dx + dz * dz > TREE_PURGE_RADIUS * TREE_PURGE_RADIUS) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isCaveVegetationBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT)
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)
                || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK);
    }
}
