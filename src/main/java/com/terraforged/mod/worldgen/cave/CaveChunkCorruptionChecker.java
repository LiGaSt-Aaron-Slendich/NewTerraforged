package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveChunkCorruptionChecker {
    private static final int SURFACE_LIFT = 12;
    private static final int SURFACE_CRUST = 3;
    /** Fraction of interior columns that must match a chessboard/grid signature. */
    private static final float NOISE_RATIO_THRESHOLD = 0.42f;
    /** Minimum height delta between parity groups to count as carve grid noise. */
    private static final int CHESS_PARITY_DELTA = 5;
    /** Neighbor height jump that reads as a 1-block grid step. */
    private static final int GRID_STEP = 1;

    /** Minimum surface columns with cave-painted leaks before a chunk is flagged. */
    private static final int MIN_DEFECT_COLUMNS = 6;

    private CaveChunkCorruptionChecker() {
    }

    public static CaveChunkCorruptionReport scan(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        CaveChunkCorruptionReport report = CaveChunkCorruptionReport.clean();
        if (CaveChunkCorruptionChecker.scanUndergroundFeaturesOnSurface(chunk, carver, generator, true)) {
            report.add(CaveChunkCorruptionReport.Issue.UNDERGROUND_FEATURES);
        }
        return report;
    }

    /** Post-restore check — only unresolved cave leaks, not legit surface vegetation. */
    public static CaveChunkCorruptionReport verify(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        CaveChunkCorruptionReport report = CaveChunkCorruptionReport.clean();
        if (CaveChunkCorruptionChecker.scanUndergroundFeaturesOnSurface(chunk, carver, generator, false)) {
            report.add(CaveChunkCorruptionReport.Issue.UNDERGROUND_FEATURES);
        }
        if (CaveChunkCorruptionChecker.scanSurfaceNoise(chunk, carver)) {
            report.add(CaveChunkCorruptionReport.Issue.NOISE);
        }
        return report;
    }

    private static boolean scanUndergroundFeaturesOnSurface(ChunkAccess chunk, CarverChunk carver, Generator generator, boolean broadDetection) {
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int defectColumns = 0;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                int yTop = CaveChunkCorruptionChecker.findSurfaceColumnTop(chunk, lx, lz, surface);
                int yBottom = Math.max(surface, yTop - SURFACE_LIFT);
                boolean columnDefect = false;
                for (int y = yTop; y >= yBottom; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (broadDetection) {
                        if (CaveDecorationSanitizer.isCorruptedSurfaceBlock(chunk, lx, y, lz, state, surfaceBiome)) {
                            columnDefect = true;
                            break;
                        }
                        continue;
                    }
                    if (CaveDecorationSanitizer.isUnresolvedSurfaceDefect(chunk, lx, y, lz, state)) {
                        columnDefect = true;
                        break;
                    }
                }
                if (columnDefect) {
                    ++defectColumns;
                    if (!broadDetection || defectColumns >= MIN_DEFECT_COLUMNS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** @deprecated use {@link CaveDecorationSanitizer#isCorruptedSurfaceBlock} */
    static boolean isSurfaceLeak(ChunkAccess chunk, int lx, int y, int lz, BlockState state, Holder<Biome> surfaceBiome) {
        return CaveDecorationSanitizer.isCorruptedSurfaceBlock(chunk, lx, y, lz, state, surfaceBiome);
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

    static boolean scanSurfaceNoise(ChunkAccess chunk, CarverChunk carver) {
        int[][] heights = CaveChunkSurfaceRepair.readGroundHeights(chunk, carver);
        return CaveChunkCorruptionChecker.detectChessboardNoise(heights, carver)
                || CaveChunkCorruptionChecker.detectGridNoise(heights, carver);
    }

    static boolean detectChessboardNoise(int[][] heights, CarverChunk carver) {
        int scored = 0;
        int total = 0;
        int evenSum = 0;
        int oddSum = 0;
        int evenCount = 0;
        int oddCount = 0;
        for (int lx = 2; lx < 14; ++lx) {
            for (int lz = 2; lz < 14; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                ++total;
                int center = heights[lx][lz];
                if (((lx + lz) & 1) == 0) {
                    evenSum += center;
                    ++evenCount;
                } else {
                    oddSum += center;
                    ++oddCount;
                }
                int north = heights[lx][lz - 1];
                int south = heights[lx][lz + 1];
                int west = heights[lx - 1][lz];
                int east = heights[lx + 1][lz];
                int diagA = heights[lx - 1][lz - 1];
                int diagB = heights[lx + 1][lz + 1];
                int maxCard = Math.max(Math.max(Math.abs(center - north), Math.abs(center - south)), Math.max(Math.abs(center - west), Math.abs(center - east)));
                int maxDiag = Math.max(Math.max(Math.abs(center - diagA), Math.abs(center - heights[lx + 1][lz - 1])), Math.max(Math.abs(center - diagB), Math.abs(center - heights[lx - 1][lz + 1])));
                if (maxDiag >= 2 && maxCard <= 1) {
                    ++scored;
                }
            }
        }
        if (total == 0) {
            return false;
        }
        int evenAvg = evenCount == 0 ? 0 : evenSum / evenCount;
        int oddAvg = oddCount == 0 ? 0 : oddSum / oddCount;
        boolean paritySplit = Math.abs(evenAvg - oddAvg) >= CHESS_PARITY_DELTA;
        return paritySplit && (float)scored / (float)total >= NOISE_RATIO_THRESHOLD;
    }

    private static boolean detectGridNoise(int[][] heights, CarverChunk carver) {
        int scored = 0;
        int total = 0;
        for (int lx = 2; lx < 14; ++lx) {
            for (int lz = 2; lz < 14; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                ++total;
                int center = heights[lx][lz];
                int north = heights[lx][lz - 1];
                int south = heights[lx][lz + 1];
                int west = heights[lx - 1][lz];
                int east = heights[lx + 1][lz];
                boolean axisGrid = Math.abs(center - north) == GRID_STEP && Math.abs(center - south) == GRID_STEP
                        && Math.abs(center - west) <= GRID_STEP && Math.abs(center - east) <= GRID_STEP;
                boolean crossGrid = Math.abs(center - west) == GRID_STEP && Math.abs(center - east) == GRID_STEP
                        && Math.abs(center - north) <= GRID_STEP && Math.abs(center - south) <= GRID_STEP;
                if (axisGrid || crossGrid) {
                    ++scored;
                }
            }
        }
        return total > 0 && (float)scored / (float)total >= NOISE_RATIO_THRESHOLD;
    }
}
