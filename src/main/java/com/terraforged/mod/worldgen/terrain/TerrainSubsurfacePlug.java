package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CaveCarvingGate;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Safety net when {@link CaveCarvingGate} is off: closes underground air between terrain fill
 * top and the natural ground crust. Skips logs/leaves when locating crust but still plugs open
 * pockets under forests (tree columns are not skipped wholesale).
 */
public final class TerrainSubsurfacePlug {
    private static final int MIN_PLUG_GAP = 2;

    private TerrainSubsurfacePlug() {
    }

    public static void plugTerrainHollow(ChunkAccess chunk, TerrainData terrain, Generator generator) {
        if (terrain == null || CaveCarvingGate.isEnabled()) {
            return;
        }
        int sea = generator.getSeaLevel();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int solidY = terrain.getHeight(lx, lz);
                int waterY = TerrainLevels.getWaterLevel(lx, lz, sea, terrain);
                int fillTop = Math.max(solidY, waterY);
                int groundY = TerrainSubsurfacePlug.findNaturalGroundY(chunk, lx, lz, fillTop);
                if (groundY - fillTop < MIN_PLUG_GAP) {
                    continue;
                }
                for (int y = fillTop + 1; y < groundY; ++y) {
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

    /** Lowest grass/dirt/stone crust under any vegetation, scanning from the surface down. */
    private static int findNaturalGroundY(ChunkAccess chunk, int lx, int lz, int fillTop) {
        int scanTop = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        scanTop = Math.min(chunk.getMaxBuildHeight() - 1, Math.max(scanTop, fillTop + 1));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = scanTop; y > fillTop; --y) {
            BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                return y;
            }
            if (TerrainSubsurfacePlug.isVegetation(state)) {
                continue;
            }
            return y;
        }
        return fillTop;
    }

    private static boolean isVegetation(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS) || state.is(Blocks.VINE) || state.is(Blocks.COCOA)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING);
    }
}
