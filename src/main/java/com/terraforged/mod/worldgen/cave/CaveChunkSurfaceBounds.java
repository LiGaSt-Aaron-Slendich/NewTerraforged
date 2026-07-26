package com.terraforged.mod.worldgen.cave;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Limits chunk-integrity phase 1 to the overworld surface shell — never cave volumes below.
 */
public final class CaveChunkSurfaceBounds {
    static final int SURFACE_CRUST = 3;
    static final int SURFACE_LIFT = 16;

    private CaveChunkSurfaceBounds() {
    }

    public static int surfaceY(ChunkAccess chunk, int lx, int lz) {
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
    }

    public static int bandMinY(ChunkAccess chunk, int lx, int lz) {
        return Math.max(chunk.getMinBuildHeight(), CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz) - SURFACE_CRUST);
    }

    public static int bandMaxY(ChunkAccess chunk, int lx, int lz) {
        return Math.min(chunk.getMaxBuildHeight() - 1, CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz) + SURFACE_LIFT);
    }

    /** True when phase-1 may modify blocks at this position. */
    public static boolean mayModify(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz) {
        if (carver != null && carver.isEntranceColumn(lx, lz) && y < CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz) - CaveUndergroundGuard.ENTRANCE_BIOME_DEPTH) {
            return false;
        }
        int surface = CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz);
        if (y > surface + SURFACE_LIFT || y < surface - SURFACE_CRUST) {
            return false;
        }
        if (y <= surface && !CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz)) {
            return false;
        }
        if (carver != null && carver.isColumnCacheReady() && carver.columnCache().forbidsUndergroundWrite(lx, y, lz, chunk, carver.isEntranceColumn(lx, lz))) {
            return false;
        }
        return true;
    }

    public static boolean isInSurfaceBand(ChunkAccess chunk, int lx, int y, int lz) {
        return y >= CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz) && y <= CaveChunkSurfaceBounds.bandMaxY(chunk, lx, lz);
    }
}
