package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.noise.Module;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Detects flat walls and interior monolith pillars beside open cave space, then force-carves through them.
 */
public final class CaveFlatWallRepair {
    private static final int SCAN_DEPTH = 112;
    private static final int MIN_OPEN_AIR = 8;
    private static final int MAX_WALL_AIR = 10;
    private static final int MIN_ASYMMETRY = 6;
    private static final int MIN_OPEN_NEIGHBORS = 2;
    private static final int REPAIR_PASSES = 2;
    private static final int REPAIR_PASSES_DECORATE = 1;
    private static final int BORDER_DEPTH = 2;

    private CaveFlatWallRepair() {
    }

    public static void afterCarve(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor) {
        CaveFlatWallRepair.runRepair(seed, chunk, carver, generator, caves, modifierFor, null, false);
    }

    public static void withNeighbors(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor) {
        CaveFlatWallRepair.runRepair(seed, chunk, carver, generator, caves, modifierFor, region, true, REPAIR_PASSES_DECORATE);
    }

    /** Late pass after cave volume decoration — catches walls exposed once neighbors finished. */
    public static void afterDecorate(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor) {
        if (carver == null || !carver.isColumnCacheReady() || !carver.columnCache().anyMegaGiga()) {
            return;
        }
        CaveFlatWallRepair.runRepair(seed, chunk, carver, generator, caves, modifierFor, region, false, REPAIR_PASSES_DECORATE);
    }

    private static void runRepair(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region, boolean includeInterior) {
        CaveFlatWallRepair.runRepair(seed, chunk, carver, generator, caves, modifierFor, region, includeInterior, REPAIR_PASSES);
    }

    private static void runRepair(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region, boolean includeInterior, int maxPasses) {
        if (carver == null || !carver.isColumnCacheReady()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        if (!CaveFlatWallRepair.shouldRun(columns, chunk)) {
            return;
        }
        int total = 0;
        for (int pass = 0; pass < maxPasses; ++pass) {
            int repaired = 0;
            repaired += CaveFlatWallRepair.repairBorderColumns(seed, chunk, carver, generator, caves, modifierFor, region);
            if (includeInterior) {
                repaired += CaveFlatWallRepair.repairInteriorMonoliths(seed, chunk, carver, generator, caves, modifierFor, region);
            }
            total += repaired;
            if (repaired == 0) {
                break;
            }
        }
        if (total > 0) {
            ChunkUtil.refreshHeightmaps(chunk);
            CaveSurfaceBiomeRestorer.restore(chunk, generator, carver);
        }
    }

    private static boolean shouldRun(CarverColumnCache columns, ChunkAccess chunk) {
        int open = CaveFlatWallRepair.countOpenColumns(chunk);
        if (open < 2) {
            return false;
        }
        if (columns.anyMegaGiga()) {
            return open >= 3;
        }
        int cx = chunk.getPos().getMiddleBlockX();
        int cz = chunk.getPos().getMiddleBlockZ();
        if (CaveSystemBounds.isWithinFootprint(cx, cz, CaveType.MEGA) || CaveSystemBounds.isWithinFootprint(cx, cz, CaveType.GIGA)) {
            return open >= 3;
        }
        return open >= 3;
    }

    private static int countOpenColumns(ChunkAccess chunk) {
        int open = 0;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (CaveFlatWallRepair.columnAir(chunk, lx, lz) >= MIN_OPEN_AIR) {
                    ++open;
                }
            }
        }
        return open;
    }

    private static int repairBorderColumns(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region) {
        CarverColumnCache columns = carver.columnCache();
        int repaired = 0;
        for (int edge = 0; edge < 4; ++edge) {
            for (int depth = 0; depth < BORDER_DEPTH; ++depth) {
                for (int i = 0; i < 16; ++i) {
                    int lx;
                    int lz;
                    int inwardX;
                    int inwardZ;
                    Direction outward;
                    switch (edge) {
                        case 0 -> {
                            lx = depth;
                            lz = i;
                            inwardX = 1;
                            inwardZ = 0;
                            outward = Direction.WEST;
                        }
                        case 1 -> {
                            lx = 15 - depth;
                            lz = i;
                            inwardX = -1;
                            inwardZ = 0;
                            outward = Direction.EAST;
                        }
                        case 2 -> {
                            lx = i;
                            lz = depth;
                            inwardX = 0;
                            inwardZ = 1;
                            outward = Direction.NORTH;
                        }
                        default -> {
                            lx = i;
                            lz = 15 - depth;
                            inwardX = 0;
                            inwardZ = -1;
                            outward = Direction.SOUTH;
                        }
                    }
                    if (!CaveFlatWallRepair.isWallCandidate(chunk, carver, generator, columns, caves, modifierFor, region, seed, lx, lz, inwardX, inwardZ, outward)) {
                        continue;
                    }
                    if (CaveFlatWallRepair.tryRepairColumn(seed, chunk, carver, generator, caves, modifierFor, region, columns, lx, lz)) {
                        ++repaired;
                    }
                }
            }
        }
        return repaired;
    }

    private static int repairInteriorMonoliths(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region) {
        CarverColumnCache columns = carver.columnCache();
        int repaired = 0;
        for (int lx = BORDER_DEPTH; lx < 16 - BORDER_DEPTH; ++lx) {
            for (int lz = BORDER_DEPTH; lz < 16 - BORDER_DEPTH; ++lz) {
                if (!CaveFlatWallRepair.isInteriorMonolith(chunk, carver, generator, columns, caves, modifierFor, region, seed, lx, lz)) {
                    continue;
                }
                if (CaveFlatWallRepair.tryRepairColumn(seed, chunk, carver, generator, caves, modifierFor, region, columns, lx, lz)) {
                    ++repaired;
                }
            }
        }
        return repaired;
    }

    private static boolean isInteriorMonolith(ChunkAccess chunk, CarverChunk carver, Generator generator, CarverColumnCache columns, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region, int seed, int lx, int lz) {
        if (columns.oceanBlocked(lx, lz)) {
            return false;
        }
        int selfAir = CaveFlatWallRepair.columnAir(chunk, lx, lz);
        if (selfAir >= MIN_OPEN_AIR) {
            return false;
        }
        int openNeighbors = 0;
        int bestNeighborAir = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int nx = lx + dir.getStepX();
            int nz = lz + dir.getStepZ();
            int neighborAir;
            if (nx >= 0 && nx <= 15 && nz >= 0 && nz <= 15) {
                neighborAir = CaveFlatWallRepair.columnAir(chunk, nx, nz);
            } else if (region != null) {
                int wx = chunk.getPos().getMinBlockX() + nx;
                int wz = chunk.getPos().getMinBlockZ() + nz;
                neighborAir = CaveFlatWallRepair.columnAir(region, wx, wz);
            } else {
                continue;
            }
            if (neighborAir >= MIN_OPEN_AIR) {
                ++openNeighbors;
                bestNeighborAir = Math.max(bestNeighborAir, neighborAir);
            }
        }
        if (openNeighbors >= MIN_OPEN_NEIGHBORS && bestNeighborAir - selfAir >= MIN_ASYMMETRY) {
            return true;
        }
        NoiseCave config = CaveFlatWallRepair.pickConfig(columns, caves, generator);
        if (config == null) {
            return false;
        }
        Module modifier = modifierFor.apply(config);
        int wx = chunk.getPos().getMinBlockX() + lx;
        int wz = chunk.getPos().getMinBlockZ() + lz;
        CaveColumnSimulator.Sample sample = CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, wx, wz);
        return sample != null && selfAir <= MAX_WALL_AIR && openNeighbors >= 1;
    }

    private static boolean isWallCandidate(ChunkAccess chunk, CarverChunk carver, Generator generator, CarverColumnCache columns, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region, int seed, int lx, int lz, int inwardX, int inwardZ, Direction outward) {
        if (columns.oceanBlocked(lx, lz)) {
            return false;
        }
        int edgeAir = CaveFlatWallRepair.columnAir(chunk, lx, lz);
        if (edgeAir >= MIN_OPEN_AIR) {
            return false;
        }
        int inwardLx = lx + inwardX;
        int inwardLz = lz + inwardZ;
        if (inwardLx >= 0 && inwardLx <= 15 && inwardLz >= 0 && inwardLz <= 15) {
            int inwardAir = CaveFlatWallRepair.columnAir(chunk, inwardLx, inwardLz);
            if (inwardAir >= MIN_OPEN_AIR && inwardAir - edgeAir >= MIN_ASYMMETRY) {
                return true;
            }
        }
        if (region != null) {
            for (int step = 1; step <= 2; ++step) {
                int wx = chunk.getPos().getMinBlockX() + lx + outward.getStepX() * step;
                int wz = chunk.getPos().getMinBlockZ() + lz + outward.getStepZ() * step;
                int neighborAir = CaveFlatWallRepair.columnAir(region, wx, wz);
                if (neighborAir >= MIN_OPEN_AIR && neighborAir - edgeAir >= MIN_ASYMMETRY) {
                    return true;
                }
            }
        }
        return CaveFlatWallRepair.isInteriorMonolith(chunk, carver, generator, columns, caves, modifierFor, region, seed, lx, lz);
    }

    private static boolean tryRepairColumn(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave[] caves, Function<NoiseCave, Module> modifierFor, WorldGenLevel region, CarverColumnCache columns, int lx, int lz) {
        NoiseCave config = CaveFlatWallRepair.pickConfig(columns, caves, generator);
        if (config == null) {
            return false;
        }
        Module modifier = modifierFor.apply(config);
        if (NoiseCaveCarver.forceCarveMegaGigaColumn(seed, chunk, carver, generator, config, modifier, lx, lz)) {
            return true;
        }
        return CaveFlatWallRepair.brutePunchColumn(chunk, carver, generator, config, modifier, seed, lx, lz, region);
    }

    private static NoiseCave pickConfig(CarverColumnCache columns, NoiseCave[] caves, Generator generator) {
        NoiseCave picked = NoiseCaveGenerator.representativeMegaGiga(columns, caves);
        if (picked != null) {
            return picked;
        }
        if (columns.hasGiga()) {
            return CaveLocator.findConfig(generator, CaveType.GIGA);
        }
        if (columns.hasMega()) {
            return CaveLocator.findConfig(generator, CaveType.MEGA);
        }
        for (NoiseCave cave : caves) {
            if (cave.getType() == CaveType.GIGA) {
                return cave;
            }
        }
        for (NoiseCave cave : caves) {
            if (cave.getType() == CaveType.MEGA) {
                return cave;
            }
        }
        return null;
    }

    private static int columnAir(ChunkAccess chunk, int lx, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minY = chunk.getMinBuildHeight();
        int yTop = Math.max(minY, surface - 2);
        int yBottom = Math.max(minY, surface - SCAN_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int air = 0;
        for (int y = yTop; y >= yBottom; --y) {
            if (chunk.getBlockState(pos.set(lx, y, lz)).isAir()) {
                ++air;
            }
        }
        return air;
    }

    private static int columnAir(LevelAccessor level, int wx, int wz) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
        int minY = level.getMinBuildHeight();
        int yTop = Math.max(minY, surface - 2);
        int yBottom = Math.max(minY, surface - SCAN_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int air = 0;
        for (int y = yTop; y >= yBottom; --y) {
            if (level.getBlockState(pos.set(wx, y, wz)).isAir()) {
                ++air;
            }
        }
        return air;
    }

    private static boolean brutePunchColumn(ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, Module modifier, int seed, int lx, int lz, WorldGenLevel region) {
        int wx = chunk.getPos().getMinBlockX() + lx;
        int wz = chunk.getPos().getMinBlockZ() + lz;
        int bottom;
        int top;
        int[] neighborChamber = CaveFlatWallRepair.findNeighborChamberBounds(chunk, region, lx, lz);
        if (neighborChamber != null) {
            bottom = neighborChamber[0];
            top = neighborChamber[1];
        } else {
            CaveColumnSimulator.Sample sample = CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, wx, wz);
            if (sample != null) {
                bottom = sample.floorY();
                top = sample.ceilingY();
            } else {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int center = config.getHeight(seed, wx, wz);
                bottom = Math.max(chunk.getMinBuildHeight(), center - 6);
                top = Math.min(surface - 2, center + 6);
            }
        }
        if (top - bottom < 3) {
            return false;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int y = bottom; y <= top; ++y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            chunk.setBlockState(pos, air, false);
            ++cleared;
        }
        if (cleared > 0) {
            carver.markBiomeRestoreColumn(lx, lz);
        }
        return cleared > 0;
    }

    /** Match chamber height from the most open cardinal neighbor. */
    private static int[] findNeighborChamberBounds(ChunkAccess chunk, WorldGenLevel region, int lx, int lz) {
        int bestAir = 0;
        int bestFloor = Integer.MAX_VALUE;
        int bestCeil = Integer.MIN_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int nx = lx + dir.getStepX();
            int nz = lz + dir.getStepZ();
            int wx = chunk.getPos().getMinBlockX() + nx;
            int wz = chunk.getPos().getMinBlockZ() + nz;
            int[] bounds;
            if (nx >= 0 && nx <= 15 && nz >= 0 && nz <= 15) {
                bounds = CaveFlatWallRepair.scanChamberBounds(chunk, nx, nz);
            } else if (region != null) {
                bounds = CaveFlatWallRepair.scanChamberBounds(region, wx, wz);
            } else {
                continue;
            }
            if (bounds == null) {
                continue;
            }
            int airSpan = bounds[1] - bounds[0];
            if (airSpan <= bestAir) {
                continue;
            }
            bestAir = airSpan;
            bestFloor = bounds[0];
            bestCeil = bounds[1];
        }
        if (bestAir < 3) {
            return null;
        }
        return new int[]{bestFloor, bestCeil};
    }

    private static int[] scanChamberBounds(ChunkAccess chunk, int lx, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minY = chunk.getMinBuildHeight();
        int yTop = Math.max(minY, surface - 2);
        int yBottom = Math.max(minY, surface - SCAN_DEPTH);
        int floor = Integer.MAX_VALUE;
        int ceil = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = yBottom; y <= yTop; ++y) {
            if (!chunk.getBlockState(pos.set(lx, y, lz)).isAir()) {
                continue;
            }
            floor = Math.min(floor, y);
            ceil = Math.max(ceil, y);
        }
        if (floor == Integer.MAX_VALUE || ceil - floor < 2) {
            return null;
        }
        return new int[]{floor, ceil};
    }

    private static int[] scanChamberBounds(LevelAccessor level, int wx, int wz) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
        int minY = level.getMinBuildHeight();
        int yTop = Math.max(minY, surface - 2);
        int yBottom = Math.max(minY, surface - SCAN_DEPTH);
        int floor = Integer.MAX_VALUE;
        int ceil = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = yBottom; y <= yTop; ++y) {
            if (!level.getBlockState(pos.set(wx, y, wz)).isAir()) {
                continue;
            }
            floor = Math.min(floor, y);
            ceil = Math.max(ceil, y);
        }
        if (floor == Integer.MAX_VALUE || ceil - floor < 2) {
            return null;
        }
        return new int[]{floor, ceil};
    }
}
