package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CavePlacementFilter {
    private static final int MIN_DEPTH_BELOW_SURFACE = 6;
    private static final int MIN_OPEN_AIR_ABOVE = 4;

    private CavePlacementFilter() {
    }

    public static boolean shouldSkipTree(Generator generator, ChunkAccess chunk, int x, int y, int z) {
        int lx = x & 0xF;
        int lz = z & 0xF;
        CarverChunk carver = generator.peekCaveCarver(chunk.getPos());
        if (carver != null && carver.isColumnCacheReady() && carver.columnCache().skipTree(lx, lz)) {
            return true;
        }
        Holder<net.minecraft.world.level.biome.Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted != null && CaveBiomeIds.isModCaveBiome(painted)) {
            return true;
        }
        for (int dy = -2; dy <= 2; ++dy) {
            painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y + dy, lz);
            if (painted != null && CaveBiomeIds.isModCaveBiome(painted)) {
                return true;
            }
        }
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
        if (floor < surface - MIN_DEPTH_BELOW_SURFACE && y <= floor + 1 && CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz)) {
            return true;
        }
        if (MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z)) {
            if (CavePlacementFilter.hasOpenCaveAir(chunk, lx, y, lz)) {
                return true;
            }
            if (y <= surface && CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz)) {
                return true;
            }
        }
        if (!CavePlacementFilter.isUndergroundCave(chunk, lx, y, lz)) {
            return false;
        }
        return MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z) || CavePlacementFilter.hasOpenCaveAir(chunk, lx, y, lz);
    }

    static boolean isUndergroundCave(ChunkAccess chunk, int lx, int y, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        if (y >= surface - 6) {
            return false;
        }
        return CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz);
    }

    private static boolean hasOpenCaveAir(ChunkAccess chunk, int lx, int y, int lz) {
        BlockState state;
        int air = 0;
        for (int dy = 1; dy <= 6 && ((state = chunk.getBlockState(new BlockPos(lx, y + dy, lz))).isAir() || !state.getFluidState().isEmpty()); ++dy) {
            if (++air < 4) continue;
            return true;
        }
        return false;
    }
}
