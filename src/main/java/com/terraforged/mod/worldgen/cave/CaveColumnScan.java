package com.terraforged.mod.worldgen.cave;

import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Column air/floor scans for cave biome paint. Tall worlds (640–1060) make naive
 * full-height sweeps expensive — prefer surface-relative windows when possible.
 */
public final class CaveColumnScan {
    /** Max depth below surface for floor hunts in tall overworld columns. */
    public static final int SURFACE_SCAN_DEPTH = 320;

    private CaveColumnScan() {
    }

    public static int findLowestFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY, y -> true);
    }

    public static int findLowestFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY, IntPredicate yGuard) {
        int best = -1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; ++y) {
            if (!yGuard.test(y)) {
                continue;
            }
            pos.set(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir() || y <= minY) {
                continue;
            }
            pos.set(lx, y - 1, lz);
            if (chunk.getBlockState(pos).isAir()) {
                continue;
            }
            best = y;
        }
        return best;
    }

    public static int findTopValidFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY, IntPredicate yGuard) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; --y) {
            if (!yGuard.test(y)) {
                continue;
            }
            pos.set(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir() || y <= minY) {
                continue;
            }
            pos.set(lx, y - 1, lz);
            if (chunk.getBlockState(pos).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    /**
     * Surface-relative floor find for tall worlds: scan at most {@link #SURFACE_SCAN_DEPTH}
     * below the column surface instead of the full build height.
     */
    public static int findFloorNearSurface(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        if (surface < minY + 4) {
            surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
        }
        int scanMax = Math.min(maxY, Math.max(minY + 4, surface - 2));
        int scanMin = Math.max(minY, scanMax - SURFACE_SCAN_DEPTH);
        // Prefer top floor in the window (active chamber), not the absolute deepest pocket.
        int top = findTopValidFloor(chunk, lx, lz, scanMin, scanMax, y -> true);
        if (top >= 0) {
            return top;
        }
        return findLowestFloor(chunk, lx, lz, scanMin, scanMax);
    }

    public static int findCeilingAboveFloor(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        int start = floorY + 5;
        int limit = Math.min(maxY, floorY + 72);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = start; y <= limit; ++y) {
            pos.set(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir()) {
                continue;
            }
            if (y + 1 > maxY) {
                continue;
            }
            pos.set(lx, y + 1, lz);
            if (chunk.getBlockState(pos).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    /** Full air-pocket height at lx/lz containing block y (for chamber-fit checks). */
    public static int measureAirColumnSpan(ChunkAccess chunk, int lx, int y, int lz, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        pos.set(lx, y, lz);
        if (!chunk.getBlockState(pos).isAir()) {
            return 0;
        }
        int bottom = y;
        while (bottom > minY) {
            pos.set(lx, bottom - 1, lz);
            if (!chunk.getBlockState(pos).isAir()) {
                break;
            }
            --bottom;
        }
        int top = y;
        while (top < maxY) {
            pos.set(lx, top + 1, lz);
            if (!chunk.getBlockState(pos).isAir()) {
                break;
            }
            ++top;
        }
        return top - bottom + 1;
    }
}
