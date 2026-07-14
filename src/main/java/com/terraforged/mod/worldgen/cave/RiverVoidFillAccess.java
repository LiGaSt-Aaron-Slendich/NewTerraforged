package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Safe block/height reads during chunkgen — never touches WorldGenRegion out-of-bound chunks. */
final class RiverVoidFillAccess {
    private RiverVoidFillAccess() {
    }

    static boolean canRead(BlockGetter level, ChunkAccess chunk, int wx, int wz) {
        ChunkAccess owner = RiverVoidFillAccess.resolveChunk(level, chunk, wx, wz);
        return owner != null;
    }

    static BlockState blockState(BlockGetter level, ChunkAccess chunk, int x, int y, int z) {
        ChunkAccess owner = RiverVoidFillAccess.resolveChunk(level, chunk, x, z);
        if (owner == null) {
            return null;
        }
        int lx = x - owner.getPos().getMinBlockX();
        int lz = z - owner.getPos().getMinBlockZ();
        return owner.getBlockState(new BlockPos(lx, y, lz));
    }

    static int motionBlockingY(BlockGetter level, ChunkAccess chunk, int wx, int wz, int fallback) {
        ChunkAccess owner = RiverVoidFillAccess.resolveChunk(level, chunk, wx, wz);
        if (owner == null) {
            return fallback;
        }
        int lx = wx - owner.getPos().getMinBlockX();
        int lz = wz - owner.getPos().getMinBlockZ();
        return owner.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
    }

    private static ChunkAccess resolveChunk(BlockGetter level, ChunkAccess chunk, int wx, int wz) {
        int cx = wx >> 4;
        int cz = wz >> 4;
        if (chunk.getPos().x == cx && chunk.getPos().z == cz) {
            return chunk;
        }
        if (level instanceof WorldGenLevel world) {
            if (!world.hasChunk(cx, cz)) {
                return null;
            }
            return world.getChunk(cx, cz);
        }
        return chunk;
    }
}
