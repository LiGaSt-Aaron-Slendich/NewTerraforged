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
import net.minecraft.world.level.material.Fluids;

/**
 * Carves a shallow sulfur river channel in painted transition cells and fills it with water
 * (yellow-tinted via biome special effects on {@code newterraforged:cave_sulfur_river}).
 */
public final class CaveSulfurRiverDecorator {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final int GRID = 4;
    private static final int BASIN_DEPTH = 3;
    private static final int CHANNEL_HALF = 2;

    private CaveSulfurRiverDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int seed = (int)generator.getSeed();
        for (int lx = 0; lx < 16; lx += GRID) {
            for (int lz = 0; lz < 16; lz += GRID) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0) {
                    continue;
                }
                Holder<Biome> biome = carver.resolveBiome(chunk, lx, floorY, lz);
                if (!CaveBiomeIds.isSulfurRiverBiome(biome)) {
                    continue;
                }
                float riverNoise = NoiseUtil.valCoord2D(seed ^ 0x5EC0012, wx, wz);
                if (riverNoise < -0.15f) {
                    continue;
                }
                int waterY = floorY - BASIN_DEPTH + 1;
                carveBasin(region, carver, chunk, lx, lz, floorY, waterY, minY, maxY, seed, wx, wz);
            }
        }
    }

    private static void carveBasin(WorldGenLevel region, CarverChunk carver, ChunkAccess chunk, int lx, int lz, int floorY, int waterY, int minY, int maxY, int seed, int wx, int wz) {
        for (int dx = -CHANNEL_HALF; dx <= CHANNEL_HALF; ++dx) {
            for (int dz = -CHANNEL_HALF; dz <= CHANNEL_HALF; ++dz) {
                int plx = lx + dx;
                int plz = lz + dz;
                if (plx < 0 || plx >= 16 || plz < 0 || plz >= 16) {
                    continue;
                }
                float edge = NoiseUtil.sqrt((float)(dx * dx + dz * dz));
                if (edge > (float)CHANNEL_HALF + 0.35f) {
                    continue;
                }
                int localFloor = findFloor(chunk, plx, plz, minY, maxY);
                if (localFloor < 0) {
                    continue;
                }
                int baseY = Math.min(localFloor, floorY);
                for (int y = baseY; y > baseY - BASIN_DEPTH && y > minY; --y) {
                    BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + plx, y, chunk.getPos().getMinBlockZ() + plz);
                    BlockState state = region.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (y <= waterY) {
                        region.setBlock(pos, WATER, 2);
                    } else {
                        region.setBlock(pos, GRAVEL, 2);
                    }
                }
                BlockPos surface = new BlockPos(chunk.getPos().getMinBlockX() + plx, waterY, chunk.getPos().getMinBlockZ() + plz);
                if (region.getBlockState(surface).isAir()) {
                    region.setBlock(surface, WATER, 2);
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
