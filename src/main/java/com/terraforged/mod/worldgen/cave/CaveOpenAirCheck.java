package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveOpenAirCheck {
    private static final int DEEP_SHELTER_DEPTH = 32;
    /** Underground mega/giga: blocks of surface crust where cave decor must not run. */
    private static final int UNDERGROUND_SURFACE_FORBIDDEN_DEPTH = 20;
    private static final int MEGA_GIGA_SURFACE_FORBIDDEN_DEPTH = 24;

    private CaveOpenAirCheck() {
    }

    public static boolean isInUndergroundSurfaceForbiddenZone(ChunkAccess chunk, int lx, int y, int lz) {
        return CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, false);
    }

    public static boolean isInUndergroundSurfaceForbiddenZone(ChunkAccess chunk, int lx, int y, int lz, boolean megaGiga) {
        int depth = megaGiga ? MEGA_GIGA_SURFACE_FORBIDDEN_DEPTH : UNDERGROUND_SURFACE_FORBIDDEN_DEPTH;
        int localSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        return y >= localSurface - depth;
    }

    public static boolean isSunFloor(ChunkAccess chunk, int lx, int y, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y < surface - 4) {
            return false;
        }
        return CaveOpenAirCheck.isColumnOpenToSky(chunk, lx, y, lz, surface);
    }

    public static boolean isOpenAir(ChunkAccess chunk, int lx, int y, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y >= surface) {
            return true;
        }
        if (y < surface - 48) {
            return false;
        }
        return CaveOpenAirCheck.isColumnOpenToSky(chunk, lx, y, lz, surface);
    }

    public static boolean isIgnoredCover(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.is(BlockTags.LEAVES)) {
            return true;
        }
        return state.is(BlockTags.LOGS);
    }

    private static boolean isColumnOpenToSky(ChunkAccess chunk, int lx, int y, int lz, int surface) {
        for (int dy = y + 1; dy <= surface; ++dy) {
            if (CaveOpenAirCheck.isIgnoredCover(chunk.getBlockState(new BlockPos(lx, dy, lz)))) continue;
            return false;
        }
        return true;
    }
}
