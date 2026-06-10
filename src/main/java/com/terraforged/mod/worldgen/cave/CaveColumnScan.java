package com.terraforged.mod.worldgen.cave;

import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class CaveColumnScan {
    private CaveColumnScan() {
    }

    public static int findLowestFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY, y -> true);
    }

    public static int findLowestFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY, IntPredicate yGuard) {
        int best = -1;
        for (int y = minY; y <= maxY; ++y) {
            BlockPos pos;
            if (!yGuard.test(y) || !chunk.getBlockState(pos = new BlockPos(lx, y, lz)).isAir() || y <= minY || chunk.getBlockState(pos.below()).isAir()) continue;
            best = y;
        }
        return best;
    }

    public static int findTopValidFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY, IntPredicate yGuard) {
        for (int y = maxY; y >= minY; --y) {
            BlockPos pos;
            if (!yGuard.test(y) || !chunk.getBlockState(pos = new BlockPos(lx, y, lz)).isAir() || y <= minY || chunk.getBlockState(pos.below()).isAir()) continue;
            return y;
        }
        return -1;
    }

    public static int findCeilingAboveFloor(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        int start = floorY + 5;
        int limit = Math.min(maxY, floorY + 72);
        for (int y = start; y <= limit; ++y) {
            BlockPos pos = new BlockPos(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir() || y + 1 > maxY || chunk.getBlockState(pos.above()).isAir()) continue;
            return y;
        }
        return -1;
    }
}
