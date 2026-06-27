package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveChunkSurfaceRepair {
    private static final int SURFACE_LIFT = 16;
    /** Max height change per column during noise repair — avoids chunk-edge cliffs. */
    private static final int MAX_REPAIR_DELTA = 2;

    private CaveChunkSurfaceRepair() {
    }

    public static int[][] readGroundHeights(ChunkAccess chunk, CarverChunk carver) {
        int[][] heights = new int[16][16];
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                heights[lx][lz] = CaveChunkSurfaceRepair.findGroundHeight(chunk, lx, lz);
            }
        }
        return heights;
    }

    /** Top solid block within the surface shell band only — never scans into cave volumes. */
    public static int findSurfaceShellTop(ChunkAccess chunk, int lx, int lz) {
        int minY = CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz);
        int maxY = CaveChunkSurfaceBounds.bandMaxY(chunk, lx, lz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; --y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (CaveChunkSurfaceRepair.isPillarOrLeak(state)) {
                continue;
            }
            return y;
        }
        return CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz);
    }

    /** @deprecated use {@link #findSurfaceShellTop} for integrity passes */
    public static int findGroundHeight(ChunkAccess chunk, int lx, int lz) {
        return CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
    }

    public static void repairNoiseSurface(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, boolean aggressive) {
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
        ChunkUtil.refreshHeightmaps(chunk);
        int[][] heights = CaveChunkSurfaceRepair.readGroundHeights(chunk, carver);
        int[][] target = CaveChunkSurfaceRepair.computeTargetHeights(chunk, carver, region, heights, aggressive);
        CaveChunkSurfaceRepair.applyTargetHeights(chunk, carver, generator, heights, target);
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
        ChunkUtil.refreshHeightmaps(chunk);
    }

    public static void stripSurfacePillars(ChunkAccess chunk, CarverChunk carver) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int shell = CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
                int yMin = CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz);
                int yMax = CaveChunkSurfaceBounds.bandMaxY(chunk, lx, lz);
                for (int y = yMax; y >= yMin; --y) {
                    if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                        continue;
                    }
                    if (y <= shell && !CaveChunkSurfaceRepair.isPillarOrLeak(chunk.getBlockState(pos.set(lx, y, lz)))) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    /** Ensures overworld grass on failed chunks — red tint comes from corrupted_chunks biome effects. */
    public static void paintCorruptedMarkerSurface(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int sea = generator.getSeaLevel();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int shell = CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
                if (shell <= sea || !CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, shell, lz)) {
                    continue;
                }
                CaveChunkSurfaceRepair.stripColumnAbove(chunk, lx, lz, carver);
                pos.set(lx, shell, lz);
                BlockState existing = chunk.getBlockState(pos);
                if (existing.isAir() || !existing.getFluidState().isEmpty()) {
                    if (shell > chunk.getMinBuildHeight()) {
                        chunk.setBlockState(pos.set(lx, shell - 1, lz), Blocks.DIRT.defaultBlockState(), false);
                    }
                    chunk.setBlockState(pos.set(lx, shell, lz), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                    continue;
                }
                if (!existing.is(Blocks.GRASS_BLOCK) && !existing.is(Blocks.DIRT) && !existing.is(Blocks.PODZOL) && !existing.is(Blocks.MYCELIUM)) {
                    chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                }
            }
        }
        ChunkUtil.refreshHeightmaps(chunk);
    }

    private static void stripColumnAbove(ChunkAccess chunk, int lx, int lz, CarverChunk carver) {
        int shell = CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int yMin = CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz);
        int yMax = CaveChunkSurfaceBounds.bandMaxY(chunk, lx, lz);
        for (int y = yMax; y > shell; --y) {
            if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                continue;
            }
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
        }
    }

    private static int[][] computeTargetHeights(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, int[][] heights, boolean aggressive) {
        int[][] target = new int[16][16];
        boolean chessboard = CaveChunkCorruptionChecker.detectChessboardNoise(heights, carver);
        int chunkAverage = CaveChunkSurfaceRepair.averageGroundHeight(heights, carver);
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    target[lx][lz] = heights[lx][lz];
                    continue;
                }
                if (aggressive && chessboard) {
                    int delta = chunkAverage - heights[lx][lz];
                    if (Math.abs(delta) > MAX_REPAIR_DELTA) {
                        delta = Integer.signum(delta) * MAX_REPAIR_DELTA;
                    }
                    target[lx][lz] = heights[lx][lz] + delta;
                    continue;
                }
                target[lx][lz] = CaveChunkSurfaceRepair.medianGroundHeight(region, chunk, carver, heights, lx, lz, aggressive ? 2 : 1);
            }
        }
        return target;
    }

    private static int averageGroundHeight(int[][] heights, CarverChunk carver) {
        long sum = 0L;
        int count = 0;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                sum += heights[lx][lz];
                ++count;
            }
        }
        if (count == 0) {
            return heights[8][8];
        }
        return (int)Math.round((double)sum / (double)count);
    }

    private static int medianGroundHeight(WorldGenLevel region, ChunkAccess chunk, CarverChunk carver, int[][] localHeights, int lx, int lz, int radius) {
        int[] samples = new int[(radius * 2 + 1) * (radius * 2 + 1)];
        int count = 0;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int wx = chunkX + lx + dx;
                int wz = chunkZ + lz + dz;
                int nx = lx + dx;
                int nz = lz + dz;
                if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                    if (carver.isEntranceColumn(nx, nz)) {
                        continue;
                    }
                    samples[count++] = localHeights[nx][nz];
                    continue;
                }
                if (region == null) {
                    continue;
                }
                samples[count++] = CaveChunkSurfaceRepair.groundHeightAt(region, wx, wz);
            }
        }
        if (count == 0) {
            return localHeights[lx][lz];
        }
        java.util.Arrays.sort(samples, 0, count);
        return samples[count / 2];
    }

    private static int groundHeightAt(WorldGenLevel region, int wx, int wz) {
        BlockPos pos = new BlockPos(wx, 0, wz);
        if (!region.hasChunkAt(pos)) {
            return 64;
        }
        ChunkAccess neighbor = region.getChunk(pos);
        return CaveChunkSurfaceRepair.findSurfaceShellTop(neighbor, wx & 0xF, wz & 0xF);
    }

    private static void applyTargetHeights(ChunkAccess chunk, CarverChunk carver, Generator generator, int[][] from, int[][] to) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int sea = generator.getSeaLevel();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int surface = CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz);
                int current = from[lx][lz];
                int target = Math.max(to[lx][lz], CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz));
                target = Math.min(target, surface + 1);
                if (Math.abs(target - current) > MAX_REPAIR_DELTA) {
                    target = current + Integer.signum(target - current) * MAX_REPAIR_DELTA;
                }
                if (current == target || target <= sea) {
                    continue;
                }
                if (target > current) {
                    for (int y = current + 1; y <= target; ++y) {
                        if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                            continue;
                        }
                        pos.set(lx, y, lz);
                        chunk.setBlockState(pos, y == target ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), false);
                    }
                    continue;
                }
                for (int y = current; y > target; --y) {
                    if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
                if (CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, target, lz)) {
                    pos.set(lx, target, lz);
                    if (chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static boolean isPillarOrLeak(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
            return true;
        }
        return CaveDecorationSanitizer.isCaveLeakBlock(state);
    }
}
