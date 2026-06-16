package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Shallow sulfur river channel in painted transition cells — flat floors only, filled with water
 * (yellow-tinted via biome special effects on {@code newterraforged:cave_sulfur_river}).
 */
public final class CaveSulfurRiverDecorator {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final int GRID = 5;
    private static final int BASIN_DEPTH = 4;
    private static final int CHANNEL_HALF = 2;
    private static final int MAX_LOCAL_SLOPE = 2;

    private CaveSulfurRiverDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        if (!carver.columnCache().anyMegaGiga()) {
            return;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int seed = (int)generator.getSeed();
        CarverColumnCache columns = carver.columnCache();
        for (int lx = 2; lx < 16; lx += GRID) {
            for (int lz = 2; lz < 16; lz += GRID) {
                if (columns.localSurfaceSlope(lx, lz, MAX_LOCAL_SLOPE + 1)) {
                    continue;
                }
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = CaveSulfurRiverDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0) {
                    continue;
                }
                Holder<Biome> biome = carver.resolveBiome(chunk, lx, floorY, lz);
                if (!CaveBiomeIds.isSulfurRiverBiome(biome)) {
                    continue;
                }
                float riverNoise = NoiseUtil.valCoord2D(seed ^ 0x5EC0012, wx, wz);
                if (riverNoise < -0.35f) {
                    continue;
                }
                CaveSulfurRiverDecorator.carveBasin(region, chunk, lx, lz, floorY, minY, maxY);
            }
        }
    }

    private static void carveBasin(WorldGenLevel region, ChunkAccess chunk, int lx, int lz, int floorY, int minY, int maxY) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int dx = -CHANNEL_HALF; dx <= CHANNEL_HALF; ++dx) {
            for (int dz = -CHANNEL_HALF; dz <= CHANNEL_HALF; ++dz) {
                int plx = lx + dx;
                int plz = lz + dz;
                if (plx < 0 || plx >= 16 || plz < 0 || plz >= 16) {
                    continue;
                }
                float edge = NoiseUtil.sqrt((float)(dx * dx + dz * dz));
                if (edge > (float)CHANNEL_HALF + 0.4f) {
                    continue;
                }
                int localFloor = CaveSulfurRiverDecorator.findFloor(chunk, plx, plz, minY, maxY);
                if (localFloor < 0) {
                    continue;
                }
                int waterY = localFloor;
                int carveBottom = Math.max(minY + 1, localFloor - BASIN_DEPTH);
                for (int y = localFloor; y >= carveBottom; --y) {
                    BlockPos pos = new BlockPos(chunkX + plx, y, chunkZ + plz);
                    BlockState state = region.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (y < waterY) {
                        region.setBlock(pos, GRAVEL, 2);
                    }
                }
                for (int y = waterY; y <= localFloor + 1 && y <= maxY; ++y) {
                    BlockPos pos = new BlockPos(chunkX + plx, y, chunkZ + plz);
                    if (region.getBlockState(pos).isAir()) {
                        region.setBlock(pos, WATER, 3);
                    }
                }
            }
        }
    }

    private static int findFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        for (int y = maxY; y >= minY; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                continue;
            }
            if (y <= minY || chunk.getBlockState(new BlockPos(lx, y - 1, lz)).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }
}
