package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * LiGaSt hierarchical surface-leak scan: grid 10 → 4 → 2 → per-column.
 * See {@code docs/LiGaSt-integrity-algorithm.md}.
 */
final class LiGaStIntegrityScan {
    private static final int[] STAGES = new int[]{10, 4, 2, 1};
    private static final int SURFACE_LIFT = 12;
    private static final int MIN_DEFECT_COLUMNS = 6;

    private LiGaStIntegrityScan() {
    }

    static boolean scanUndergroundFeaturesOnSurface(ChunkAccess chunk, CarverChunk carver, Generator generator, boolean broadDetection) {
        BitSet suspicious = new BitSet(256);
        for (int stage = 0; stage < STAGES.length; ++stage) {
            int stride = STAGES[stage];
            BitSet next = new BitSet(256);
            boolean anyHit = false;
            if (stage == 0) {
                for (int lx = 0; lx < 16; lx += stride) {
                    for (int lz = 0; lz < 16; lz += stride) {
                        if (LiGaStIntegrityScan.columnDefect(chunk, carver, generator, lx, lz, broadDetection)) {
                            LiGaStIntegrityScan.mark(next, lx, lz, stride);
                            anyHit = true;
                        }
                    }
                }
                if (!anyHit) {
                    return false;
                }
                suspicious = next;
                continue;
            }
            int defectColumns = 0;
            for (int lx = 0; lx < 16; ++lx) {
                for (int lz = 0; lz < 16; ++lz) {
                    if (!suspicious.get(lx + lz * 16)) {
                        continue;
                    }
                    if (stride > 1 && (lx % stride != 0 || lz % stride != 0)) {
                        continue;
                    }
                    if (!LiGaStIntegrityScan.columnDefect(chunk, carver, generator, lx, lz, broadDetection)) {
                        continue;
                    }
                    anyHit = true;
                    if (stride > 1) {
                        LiGaStIntegrityScan.mark(next, lx, lz, stride);
                    } else {
                        ++defectColumns;
                        if (!broadDetection || defectColumns >= MIN_DEFECT_COLUMNS) {
                            return true;
                        }
                    }
                }
            }
            if (!anyHit) {
                return false;
            }
            if (stride > 1) {
                suspicious = next;
            }
        }
        return false;
    }

    private static boolean columnDefect(ChunkAccess chunk, CarverChunk carver, Generator generator, int lx, int lz, boolean broadDetection) {
        if (carver != null && carver.isEntranceColumn(lx, lz)) {
            return false;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int wx = chunkX + lx;
        int wz = chunkZ + lz;
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
        int yTop = LiGaStIntegrityScan.findSurfaceColumnTop(chunk, lx, lz, surface);
        int yBottom = Math.max(surface, yTop - SURFACE_LIFT);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = yTop; y >= yBottom; --y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (broadDetection) {
                if (CaveDecorationSanitizer.isCorruptedSurfaceBlock(chunk, lx, y, lz, state, surfaceBiome)) {
                    return true;
                }
            } else if (CaveDecorationSanitizer.isUnresolvedSurfaceDefect(chunk, lx, y, lz, state)) {
                return true;
            }
        }
        return false;
    }

    private static int findSurfaceColumnTop(ChunkAccess chunk, int lx, int lz, int surface) {
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, surface + SURFACE_LIFT);
        for (int y = maxY; y >= surface; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                return y;
            }
        }
        return surface;
    }

    private static void mark(BitSet grid, int lx, int lz, int margin) {
        int half = Math.max(1, margin);
        for (int dx = -half; dx <= half; ++dx) {
            for (int dz = -half; dz <= half; ++dz) {
                int x = lx + dx;
                int z = lz + dz;
                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    grid.set(x + z * 16);
                }
            }
        }
    }
}
