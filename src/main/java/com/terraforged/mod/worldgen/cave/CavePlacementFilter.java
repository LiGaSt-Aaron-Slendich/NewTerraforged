package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CavePlacementFilter {
    private static final int MIN_DEPTH_BELOW_SURFACE = 6;
    private static final int MIN_OPEN_AIR_ABOVE = 3;

    private CavePlacementFilter() {
    }

    public static boolean shouldSkipTree(Generator generator, ChunkAccess chunk, int x, int y, int z) {
        int lx = x & 0xF;
        int lz = z & 0xF;
        CarverChunk carver = generator.peekCaveCarver(chunk.getPos());
        if (carver != null && carver.isColumnCacheReady() && carver.columnCache().skipTree(lx, lz)) {
            return true;
        }
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        if (MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z)) {
            if (CavePlacementFilter.isInsideCaveVolume(chunk, lx, y, lz, surface)) {
                return true;
            }
        }
        if (CavePlacementFilter.hasPaintedCaveBiomeAt(chunk, lx, y, lz)) {
            return true;
        }
        if (!CavePlacementFilter.isUndergroundCave(chunk, lx, y, lz)) {
            return false;
        }
        return MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z) || CavePlacementFilter.hasOpenCaveAir(chunk, lx, y, lz);
    }

    private static boolean isInsideCaveVolume(ChunkAccess chunk, int lx, int y, int lz, int surface) {
        if (y >= surface - 2) {
            return false;
        }
        if (CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz)) {
            return true;
        }
        return CavePlacementFilter.hasOpenCaveAir(chunk, lx, y, lz);
    }

    private static boolean hasPaintedCaveBiomeAt(ChunkAccess chunk, int lx, int y, int lz) {
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted == null) {
            return false;
        }
        return CaveBiomeIds.isModCaveBiome(painted) || CaveBiomeIds.isUndergroundBiome(painted);
    }

    static boolean isUndergroundCave(ChunkAccess chunk, int lx, int y, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        if (y >= surface - MIN_DEPTH_BELOW_SURFACE) {
            return false;
        }
        return CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz);
    }

    private static boolean hasOpenCaveAir(ChunkAccess chunk, int lx, int y, int lz) {
        BlockState state;
        int air = 0;
        for (int dy = 1; dy <= 8 && ((state = chunk.getBlockState(new BlockPos(lx, y + dy, lz))).isAir() || !state.getFluidState().isEmpty()); ++dy) {
            if (++air < MIN_OPEN_AIR_ABOVE) {
                continue;
            }
            return true;
        }
        return false;
    }
}
