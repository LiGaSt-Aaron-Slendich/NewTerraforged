package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Removes surface-imported trees and soil inside mega/giga cave volumes after decoration.
 * Does not target cave biome decor (giant mushrooms, bioshrooms, etc.).
 */
public final class CaveFloatingCrustStrip {
    private static final int MIN_CAVE_AIR_RUN = 3;
    private static final int TREE_PURGE_RADIUS = 6;
    private static final int SURFACE_LEAK_BAND = 28;
    private static final int STONE_SUPPORT_SCAN = 8;

    private CaveFloatingCrustStrip() {
    }

    public static void stripMegaGigaChunk(ChunkAccess chunk, CarverChunk carver, CarverColumnCache columns) {
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
                int surface = carver != null ? carver.cachedSurface(lx, lz) : maxY;
                int stripTop = Math.min(maxY, surface + 2);
                int stripBottom = Math.max(minY, surface - SURFACE_LEAK_BAND);
                for (int y = stripTop; y >= stripBottom; --y) {
                    if (!CaveFloatingCrustStrip.touchesCaveAir(caveAir, lx, y, lz)) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (CaveFloatingCrustStrip.shouldStrip(state, chunk, lx, y, lz)) {
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static boolean shouldStrip(BlockState state, ChunkAccess chunk, int lx, int y, int lz) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (CaveFloatingCrustStrip.isSurfaceSoil(state)) {
            return true;
        }
        if (!CaveFloatingCrustStrip.isSurfaceTreeBlock(state)) {
            return false;
        }
        return !CaveFloatingCrustStrip.hasStoneSupport(chunk, lx, y, lz);
    }

    private static boolean isSurfaceSoil(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT);
    }

    private static boolean isSurfaceTreeBlock(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS) || state.is(Blocks.VINE) || state.is(Blocks.COCOA)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING);
    }

    private static boolean hasStoneSupport(ChunkAccess chunk, int lx, int y, int lz) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy <= STONE_SUPPORT_SCAN; ++dy) {
            int below = y - dy;
            if (below < chunk.getMinBuildHeight()) {
                return false;
            }
            pos.set(lx, below, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF) || state.is(Blocks.GRANITE)
                    || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE) || state.is(Blocks.CALCITE)
                    || state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.NETHERRACK)
                    || state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                return true;
            }
            if (CaveFloatingCrustStrip.isSurfaceSoil(state) || CaveFloatingCrustStrip.isSurfaceTreeBlock(state)) {
                continue;
            }
            return true;
        }
        return false;
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
}
