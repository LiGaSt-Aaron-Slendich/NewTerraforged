package com.terraforged.mod.worldgen.biome.surface;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;

/**
 * Clears thin freestanding / hanging rock pillars left when the steep cliff fill
 * densifies a column that cave carve later opens around.
 */
public final class CaveMouthPillarCleanup {
    private CaveMouthPillarCleanup() {
    }

    public static void apply(ChunkAccess chunk, ChunkGenerator generator) {
        if (chunk == null || generator == null) {
            return;
        }
        MutableBlockPos pos = new MutableBlockPos();
        int sea = generator.getSeaLevel();
        int minY = chunk.getMinBuildHeight();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int surface = chunk.getHeight(Types.OCEAN_FLOOR_WG, lx, lz);
                if (surface < sea) {
                    continue;
                }
                int bottom = Math.max(minY + 1, surface - 32);
                for (int y = surface; y >= bottom; y--) {
                    BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.SNOW)
                            || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)) {
                        continue;
                    }
                    int horizAir = 0;
                    if (isAirInChunk(chunk, lx - 1, y, lz)) {
                        horizAir++;
                    }
                    if (isAirInChunk(chunk, lx + 1, y, lz)) {
                        horizAir++;
                    }
                    if (isAirInChunk(chunk, lx, y, lz - 1)) {
                        horizAir++;
                    }
                    if (isAirInChunk(chunk, lx, y, lz + 1)) {
                        horizAir++;
                    }
                    boolean aboveAir = isAirInChunk(chunk, lx, y + 1, lz);
                    boolean belowAir = y > minY && isAirInChunk(chunk, lx, y - 1, lz);
                    // Freestanding pillar in an opening, or floating stub.
                    boolean clear = (horizAir >= 3 && (aboveAir || belowAir))
                            || (horizAir >= 2 && aboveAir && belowAir);
                    if (clear) {
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static boolean isAirInChunk(ChunkAccess chunk, int lx, int y, int lz) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
            return false;
        }
        if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = chunk.getBlockState(new MutableBlockPos(lx, y, lz));
        return state.isAir() || !state.getFluidState().isEmpty();
    }
}
