package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Closes the gap left by {@link ChunkUtil#fillChunk}: stone/water only up to terrain height,
 * while vanilla surface rules may build the crust a few blocks higher. Runs after buildSurface
 * and before features so trees/logs are never touched.
 */
public final class TerrainSubsurfacePlug {
    private static final int MIN_PLUG_GAP = 2;
    /** Max blocks above fill top to search for the post-surface crust. */
    private static final int MAX_CRUST_SCAN = 32;

    private TerrainSubsurfacePlug() {
    }

    /** After buildSurface, before trees — closes small fill-vs-crust gaps only. */
    public static void plugFillGap(ChunkAccess chunk, TerrainData terrain, Generator generator) {
        TerrainSubsurfacePlug.plugColumns(chunk, terrain, generator, false);
    }

    /**
     * After features/surface repair — closes larger hollows, but never touches columns
     * that already have logs/leaves (tree trunks would get stone infill).
     */
    public static void plugTerrainHollow(ChunkAccess chunk, TerrainData terrain, Generator generator) {
        TerrainSubsurfacePlug.plugColumns(chunk, terrain, generator, true);
    }

    private static void plugColumns(ChunkAccess chunk, TerrainData terrain, Generator generator, boolean skipVegetatedColumns) {
        if (terrain == null) {
            return;
        }
        int sea = generator.getSeaLevel();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (TerrainSubsurfacePlug.isRiverBedColumn(terrain, lx, lz)) {
                    continue;
                }
                int solidY = terrain.getHeight(lx, lz);
                int waterY = TerrainLevels.getWaterLevel(lx, lz, sea, terrain);
                int fillTop = Math.max(solidY, waterY);
                int scanTop = Math.min(chunk.getMaxBuildHeight() - 1, fillTop + MAX_CRUST_SCAN);
                if (skipVegetatedColumns && TerrainSubsurfacePlug.hasVegetationInBand(chunk, lx, lz, fillTop + 1, scanTop)) {
                    continue;
                }
                int crustY = TerrainSubsurfacePlug.findPostSurfaceCrust(chunk, lx, lz, fillTop, scanTop);
                if (crustY - fillTop < MIN_PLUG_GAP) {
                    continue;
                }
                for (int y = fillTop + 1; y < crustY; ++y) {
                    pos.set(lx, y, lz);
                    if (!chunk.getBlockState(pos).isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos, stone, false);
                }
            }
        }
        ChunkUtil.refreshHeightmaps(chunk);
    }

    /**
     * First surface-crust solid placed by surface rules above the fill top, or {@code fillTop}
     * when the column is already continuous stone to the crust.
     */
    private static int findPostSurfaceCrust(ChunkAccess chunk, int lx, int lz, int fillTop, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = fillTop + 1; y <= maxY; ++y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                return y;
            }
            if (TerrainSubsurfacePlug.isVegetation(state)) {
                return y;
            }
            if (TerrainSubsurfacePlug.isSurfaceCrust(state)) {
                return y;
            }
            if (y > fillTop + 1) {
                return y - 1;
            }
            return fillTop;
        }
        return fillTop;
    }

    private static boolean hasVegetationInBand(ChunkAccess chunk, int lx, int lz, int yMin, int yMax) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = yMin; y <= yMax; ++y) {
            if (TerrainSubsurfacePlug.isVegetation(chunk.getBlockState(pos.set(lx, y, lz)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSurfaceCrust(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.STONE) || state.is(BlockTags.DIRT);
    }

    private static boolean isVegetation(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS) || state.is(Blocks.VINE) || state.is(Blocks.COCOA)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING);
    }

    private static boolean isRiverBedColumn(TerrainData terrain, int lx, int lz) {
        Terrain type = terrain.getTerrain().get(lx, lz);
        return (type.isRiver() || type.isLake()) && terrain.getRiver().get(lx, lz) == 0.0f;
    }
}
