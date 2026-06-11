package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class CaveTunnelRiverDecorator {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    private static final int STEP = 2;
    private static final int MIN_MOUNTAIN_RELIEF = 38;
    private static final int SURFACE_WATER_SEARCH = 14;
    private static final int MAX_RISE_PUNCH = 5;
    private static final int BASIN_DEPTH_MIN = 3;
    private static final int LAKE_LOOKAHEAD = 24;
    private static final int DRAIN_TUNNEL_LENGTH = 14;
    private static final int CHANNEL_HALF_WIDTH = 1;
    private static final int CHANNEL_DEPTH = 2;
    private static final int PUNCH_HALF_WIDTH = 1;
    private static final int PUNCH_HEIGHT = 3;

    private CaveTunnelRiverDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        int chunkMaxZ;
        int chunkMinZ;
        int chunkMaxX;
        int chunkMinX;
        int exitZ;
        int mouthZ;
        int mouthX;
        if (!carver.hasTunnelRiver()) {
            return;
        }
        int seed = (int)generator.getSeed();
        if (!CaveTunnelRiverDecorator.qualifiesMountainMassif(generator, seed, mouthX = carver.tunnelMouthX(), mouthZ = carver.tunnelMouthZ())) {
            return;
        }
        int exitX = carver.tunnelExitX();
        if (!CaveTunnelRiverDecorator.segmentIntersectsChunk(mouthX, mouthZ, exitX, exitZ = carver.tunnelExitZ(), chunkMinX = chunk.getPos().getMinBlockX(), chunkMaxX = chunkMinX + 15, chunkMinZ = chunk.getPos().getMinBlockZ(), chunkMaxZ = chunkMinZ + 15)) {
            return;
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        float dx = exitX - mouthX;
        float dz = exitZ - mouthZ;
        float len = (float)Math.sqrt(dx * dx + dz * dz);
        if (len < 8.0f) {
            return;
        }
        float fx = dx / len;
        float fz = dz / len;
        float perpX = -fz;
        float perpZ = fx;
        int steps = Math.min(512, Math.round(len / 2.0f) + 1);
        CaveTunnelRiverDecorator.seedSurfacePool(region, generator, mouthX, mouthZ, minY);
        int riverY = CaveTunnelRiverDecorator.resolveRiverLevel(region, generator, mouthX, mouthZ, minY, maxY);
        if (riverY < 0) {
            return;
        }
        int step = 0;
        while (step <= steps) {
            boolean inChunk;
            int z;
            int x = mouthX + Math.round(fx * 2.0f * (float)step);
            int floorY = CaveTunnelRiverDecorator.findWalkableFloor(region, x, z = mouthZ + Math.round(fz * 2.0f * (float)step), minY, maxY);
            if (floorY < 0) {
                floorY = CaveTunnelRiverDecorator.findLowestAir(region, x, z, minY, maxY);
            }
            boolean bl = inChunk = x >= chunkMinX && x <= chunkMaxX && z >= chunkMinZ && z <= chunkMaxZ;
            if (floorY < 0) {
                if (riverY >= 0 && inChunk) {
                    CaveTunnelRiverDecorator.carveChannelToCavity(region, x, z, riverY, perpX, perpZ, minY, maxY);
                    if (CaveTunnelRiverDecorator.hasSolidBed(region, x, riverY - 1, z)) {
                        CaveTunnelRiverDecorator.carveForcedTunnel(region, carver, x, z, riverY, perpX, perpZ, seed, step, minY, maxY);
                    }
                }
                ++step;
                continue;
            }
            if (riverY < 0) {
                riverY = floorY;
            }
            int rise = floorY - riverY;
            if (rise <= 0) {
                if (floorY < riverY) {
                    riverY = floorY;
                }
                if (inChunk) {
                    CaveTunnelRiverDecorator.carveOpenChannel(region, carver, x, z, riverY, perpX, perpZ, seed, step, minY, maxY);
                }
                ++step;
                continue;
            }
            BasinScan basin = CaveTunnelRiverDecorator.scanBasin(step, steps, riverY, mouthX, mouthZ, fx, fz, region, minY, maxY);
            if (basin.basin) {
                for (int ls = step; ls <= basin.barrierStep; ++ls) {
                    boolean localInChunk;
                    int lx = mouthX + Math.round(fx * 2.0f * (float)ls);
                    int lz = mouthZ + Math.round(fz * 2.0f * (float)ls);
                    boolean bl2 = localInChunk = lx >= chunkMinX && lx <= chunkMaxX && lz >= chunkMinZ && lz <= chunkMaxZ;
                    if (!localInChunk) continue;
                    int localFloor = CaveTunnelRiverDecorator.findWalkableFloor(region, lx, lz, minY, maxY);
                    int lakeBottom = CaveTunnelRiverDecorator.findDeepestFloor(region, lx, lz, minY, maxY);
                    if (lakeBottom < 0) {
                        lakeBottom = localFloor;
                    }
                    if (localFloor < 0 || localFloor >= riverY) continue;
                    CaveTunnelRiverDecorator.fillLakeColumn(region, carver, lx, lz, lakeBottom, riverY, minY, maxY);
                }
                if (inChunk) {
                    CaveTunnelRiverDecorator.carvePunchThrough(region, carver, x, z, riverY, perpX, perpZ, floorY, seed, step, minY, maxY);
                }
                int drainEnd = Math.min(steps, basin.barrierStep + 14);
                for (int ds = basin.barrierStep + 1; ds <= drainEnd; ++ds) {
                    int dxw = mouthX + Math.round(fx * 2.0f * (float)ds);
                    int dzw = mouthZ + Math.round(fz * 2.0f * (float)ds);
                    boolean drainInChunk = dxw >= chunkMinX && dxw <= chunkMaxX && dzw >= chunkMinZ && dzw <= chunkMaxZ;
                    int drainFloor = CaveTunnelRiverDecorator.findWalkableFloor(region, dxw, dzw, minY, maxY);
                    if (drainFloor >= 0 && drainFloor < riverY) {
                        riverY = drainFloor;
                    }
                    if (!drainInChunk) continue;
                    CaveTunnelRiverDecorator.carveDrainTunnel(region, carver, dxw, dzw, riverY, perpX, perpZ, seed, ds, minY, maxY);
                }
                step = drainEnd + 1;
                continue;
            }
            if (rise <= 5) {
                if (inChunk) {
                    CaveTunnelRiverDecorator.carvePunchThrough(region, carver, x, z, riverY, perpX, perpZ, floorY, seed, step, minY, maxY);
                }
            } else if (inChunk) {
                CaveTunnelRiverDecorator.carvePunchThrough(region, carver, x, z, riverY, perpX, perpZ, floorY, seed, step, minY, maxY);
            }
            ++step;
        }
        if (exitX >= chunkMinX && exitX <= chunkMaxX && exitZ >= chunkMinZ && exitZ <= chunkMaxZ) {
            CaveTunnelRiverDecorator.seedSurfaceOutlet(region, generator, exitX, exitZ);
        }
    }

    private static BasinScan scanBasin(int startStep, int steps, int riverY, int mouthX, int mouthZ, float fx, float fz, WorldGenLevel region, int minY, int maxY) {
        int end = Math.min(startStep + 24, steps);
        int minFloor = riverY;
        int minStep = startStep;
        int barrierStep = startStep;
        for (int i = startStep; i <= end; ++i) {
            int z;
            int x = mouthX + Math.round(fx * 2.0f * (float)i);
            int floor = CaveTunnelRiverDecorator.findWalkableFloor(region, x, z = mouthZ + Math.round(fz * 2.0f * (float)i), minY, maxY);
            if (floor < 0 || floor >= minFloor) continue;
            minFloor = floor;
            minStep = i;
        }
        if (riverY - minFloor < 3) {
            return new BasinScan(false, minFloor, minStep, startStep);
        }
        int barrier = -1;
        for (int i = minStep + 1; i <= end; ++i) {
            int z;
            int x = mouthX + Math.round(fx * 2.0f * (float)i);
            int floor = CaveTunnelRiverDecorator.findWalkableFloor(region, x, z = mouthZ + Math.round(fz * 2.0f * (float)i), minY, maxY);
            if (floor < 0 || floor <= riverY + 1) continue;
            barrier = i;
            break;
        }
        boolean isBasin = barrier > minStep && riverY - minFloor >= 3;
        return new BasinScan(isBasin, minFloor, minStep, isBasin ? barrier : startStep);
    }

    private static void carveOpenChannel(WorldGenLevel region, CarverChunk carver, int wx, int wz, int waterY, float perpX, float perpZ, int seed, int step, int minY, int maxY) {
        for (int w = -1; w <= 1; ++w) {
            int px = wx + Math.round(perpX * (float)w);
            int pz = wz + Math.round(perpZ * (float)w);
            for (int d = 0; d < 2; ++d) {
                CaveTunnelRiverDecorator.carveToAir(region, px, waterY - 1 - d, pz);
            }
            CaveTunnelRiverDecorator.setBedBlock(region, px, waterY - 1, pz, seed, px, pz, step);
            CaveTunnelRiverDecorator.setWater(region, new BlockPos(px, waterY, pz));
            CaveTunnelRiverDecorator.paintRiverColumn(region, carver, px, pz, waterY, minY, maxY);
        }
    }

    private static void carvePunchThrough(WorldGenLevel region, CarverChunk carver, int wx, int wz, int tunnelY, float perpX, float perpZ, int obstructFloorY, int seed, int step, int minY, int maxY) {
        int height = Math.min(3, Math.max(2, obstructFloorY - tunnelY + 1));
        for (int w = -1; w <= 1; ++w) {
            int dy;
            int px = wx + Math.round(perpX * (float)w);
            int pz = wz + Math.round(perpZ * (float)w);
            for (dy = 0; dy < height; ++dy) {
                CaveTunnelRiverDecorator.carveToAir(region, px, tunnelY + dy, pz);
            }
            for (dy = -1; dy >= -2; --dy) {
                CaveTunnelRiverDecorator.carveToAir(region, px, tunnelY + dy, pz);
            }
            CaveTunnelRiverDecorator.setBedBlock(region, px, tunnelY - 1, pz, seed, px, pz, step);
            CaveTunnelRiverDecorator.setWater(region, new BlockPos(px, tunnelY, pz));
            CaveTunnelRiverDecorator.paintRiverColumn(region, carver, px, pz, tunnelY, minY, maxY);
        }
    }

    private static void carveDrainTunnel(WorldGenLevel region, CarverChunk carver, int wx, int wz, int waterY, float perpX, float perpZ, int seed, int step, int minY, int maxY) {
        for (int w = -1; w <= 1; ++w) {
            int px = wx + Math.round(perpX * (float)w);
            int pz = wz + Math.round(perpZ * (float)w);
            CaveTunnelRiverDecorator.carveToAir(region, px, waterY, pz);
            CaveTunnelRiverDecorator.carveToAir(region, px, waterY + 1, pz);
            CaveTunnelRiverDecorator.setBedBlock(region, px, waterY - 1, pz, seed, px, pz, step);
            CaveTunnelRiverDecorator.setWater(region, new BlockPos(px, waterY, pz));
            if ((step & 3) == 0 && waterY > region.getMinBuildHeight() + 8) {
                CaveTunnelRiverDecorator.carveToAir(region, px, waterY - 1, pz);
                CaveTunnelRiverDecorator.setWater(region, new BlockPos(px, waterY - 1, pz));
            }
            CaveTunnelRiverDecorator.paintRiverColumn(region, carver, px, pz, waterY, minY, maxY);
        }
    }

    private static void fillLakeColumn(WorldGenLevel region, CarverChunk carver, int wx, int wz, int floorY, int surfaceY, int minY, int maxY) {
        if (floorY < 0 || surfaceY <= floorY) {
            return;
        }
        for (int y = floorY + 1; y <= surfaceY; ++y) {
            CaveTunnelRiverDecorator.setWater(region, new BlockPos(wx, y, wz));
        }
        CaveTunnelRiverDecorator.setBedBlock(region, wx, floorY, wz, 0, wx, wz, 0);
        CaveTunnelRiverDecorator.paintRiverColumn(region, carver, wx, wz, surfaceY, minY, maxY);
    }

    private static void paintRiverColumn(WorldGenLevel region, CarverChunk carver, int wx, int wz, int waterY, int minY, int maxY) {
        Holder<Biome> fallback;
        int cx = wx >> 4;
        int cz = wz >> 4;
        if (!region.hasChunk(cx, cz)) {
            return;
        }
        ChunkAccess chunk = region.getChunk(cx, cz);
        int lx = wx & 0xF;
        int lz = wz & 0xF;
        Holder<Biome> chamber = null;
        for (int probe = waterY + 2; probe <= Math.min(waterY + 40, maxY); probe += 2) {
            Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, probe, lz);
            if (painted != null && CaveBiomeIds.isUndergroundBiome(painted)) {
                chamber = painted;
                break;
            }
            Holder<Biome> resolved = carver.resolveBiome(chunk, lx, probe, lz);
            if (!CaveBiomeIds.isUndergroundBiome(resolved)) continue;
            chamber = resolved;
            break;
        }
        if (chamber == null && CaveBiomeIds.isUndergroundBiome(fallback = carver.resolveBiome(chunk, lx, Math.max(minY, waterY - 6), lz))) {
            chamber = fallback;
        }
        if (chamber == null) {
            return;
        }
        for (int y = waterY; y <= maxY; ++y) {
            BlockState state = region.getBlockState(new BlockPos(wx, y, wz));
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                if (y <= waterY + 2) continue;
                break;
            }
            CarverChunk.writeBiomeAt(chunk, lx, y, lz, chamber);
        }
    }

    private static void carveToAir(WorldGenLevel region, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = region.getBlockState(pos);
        if (state.isAir() || state.getFluidState().is((Fluid)Fluids.WATER)) {
            return;
        }
        if (state.is(Blocks.BEDROCK)) {
            return;
        }
        region.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static void setBedBlock(WorldGenLevel region, int x, int y, int z, int seed, int saltX, int saltZ, int saltStep) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = region.getBlockState(pos);
        if (state.isAir() || state.getFluidState().is((Fluid)Fluids.WATER)) {
            float n = NoiseUtil.valCoord2D(seed ^ 0x7F4A7C15, saltX, saltZ + saltStep * 31);
            region.setBlock(pos, n > 0.35f ? GRAVEL : COBBLE, 2);
        }
    }

    private static int resolveRiverLevel(WorldGenLevel region, Generator generator, int mouthX, int mouthZ, int minY, int maxY) {
        int floor = CaveTunnelRiverDecorator.findWalkableFloor(region, mouthX, mouthZ, minY, maxY);
        if (floor >= 0) {
            return floor;
        }
        floor = CaveTunnelRiverDecorator.findLowestAir(region, mouthX, mouthZ, minY, maxY);
        if (floor >= 0 && CaveTunnelRiverDecorator.hasSolidBed(region, mouthX, floor - 1, mouthZ)) {
            return floor;
        }
        return -1;
    }

    private static void carveChannelToCavity(WorldGenLevel region, int wx, int wz, int waterY, float perpX, float perpZ, int minY, int maxY) {
        for (int w = -1; w <= 1; ++w) {
            int px = wx + Math.round(perpX * (float)w);
            int pz = wz + Math.round(perpZ * (float)w);
            for (int y = waterY + 2; y >= minY; --y) {
                BlockPos pos = new BlockPos(px, y, pz);
                BlockState state = region.getBlockState(pos);
                if (state.isAir()) {
                    break;
                }
                if (state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK)) {
                    region.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                if (y <= waterY - 1 && CaveTunnelRiverDecorator.hasSolidBed(region, px, y, pz)) {
                    break;
                }
            }
        }
    }

    static boolean hasSolidBed(WorldGenLevel region, int x, int y, int z) {
        if (y < region.getMinBuildHeight()) {
            return false;
        }
        BlockState state = region.getBlockState(new BlockPos(x, y, z));
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static int findLowestAir(WorldGenLevel region, int wx, int wz, int minY, int maxY) {
        for (int y = minY; y <= maxY; ++y) {
            if (!region.getBlockState(new BlockPos(wx, y, wz)).isAir()) continue;
            return y;
        }
        return -1;
    }

    private static void carveForcedTunnel(WorldGenLevel region, CarverChunk carver, int wx, int wz, int tunnelY, float perpX, float perpZ, int seed, int step, int minY, int maxY) {
        for (int w = -1; w <= 1; ++w) {
            int px = wx + Math.round(perpX * (float)w);
            int pz = wz + Math.round(perpZ * (float)w);
            for (int dy = -2; dy <= 2; ++dy) {
                int y = tunnelY + dy;
                if (y < minY || y > maxY) continue;
                CaveTunnelRiverDecorator.carveToAir(region, px, y, pz);
            }
            if (!CaveTunnelRiverDecorator.hasSolidBed(region, px, tunnelY - 1, pz)) {
                continue;
            }
            CaveTunnelRiverDecorator.setBedBlock(region, px, tunnelY - 1, pz, seed, px, pz, step);
            CaveTunnelRiverDecorator.setWater(region, new BlockPos(px, tunnelY, pz));
            CaveTunnelRiverDecorator.paintRiverColumn(region, carver, px, pz, tunnelY, minY, maxY);
        }
    }

    private static int findWalkableFloor(WorldGenLevel region, int wx, int wz, int minY, int maxY) {
        int best = -1;
        for (int y = minY; y <= maxY; ++y) {
            BlockPos pos = new BlockPos(wx, y, wz);
            if (!region.getBlockState(pos).isAir() || y <= minY || region.getBlockState(pos.below()).isAir()) continue;
            best = y;
        }
        return best;
    }

    private static int findDeepestFloor(WorldGenLevel region, int wx, int wz, int minY, int maxY) {
        for (int y = minY; y <= maxY; ++y) {
            BlockPos pos = new BlockPos(wx, y, wz);
            if (!region.getBlockState(pos).isAir() || y <= minY || region.getBlockState(pos.below()).isAir()) continue;
            return y;
        }
        return -1;
    }

    static boolean segmentIntersectsChunk(int x0, int z0, int x1, int z1, int minX, int maxX, int minZ, int maxZ) {
        if (x0 >= minX && x0 <= maxX && z0 >= minZ && z0 <= maxZ) {
            return true;
        }
        if (x1 >= minX && x1 <= maxX && z1 >= minZ && z1 <= maxZ) {
            return true;
        }
        int samples = 32;
        for (int i = 0; i <= samples; ++i) {
            float t = (float)i / (float)samples;
            int sx = Math.round((float)x0 + (float)(x1 - x0) * t);
            int sz = Math.round((float)z0 + (float)(z1 - z0) * t);
            if (sx < minX || sx > maxX || sz < minZ || sz > maxZ) continue;
            return true;
        }
        return false;
    }

    static boolean qualifiesMountainMassif(Generator generator, int seed, int wx, int wz) {
        CaveType type = CaveSystemGrid.dominantType(seed, wx, wz);
        int cx = CaveSystemGrid.snapCenter(wx, type);
        int cz = CaveSystemGrid.snapCenter(wz, type);
        int radius = CaveSystemGrid.caveRadius(type) / 2;
        int minS = Integer.MAX_VALUE;
        int maxS = Integer.MIN_VALUE;
        int sea = generator.getSeaLevel();
        for (int ox = -radius; ox <= radius; ox += 16) {
            for (int oz = -radius; oz <= radius; oz += 16) {
                int sx = cx + ox;
                int sz = cz + oz;
                int s = generator.getOceanFloorHeight(sx, sz);
                if (s <= sea + 4) continue;
                minS = Math.min(minS, s);
                maxS = Math.max(maxS, s);
            }
        }
        return minS != Integer.MAX_VALUE && maxS - minS >= 38;
    }

    private static void seedSurfacePool(WorldGenLevel region, Generator generator, int wx, int wz, int floorY) {
        int sy;
        int bestX = wx;
        int bestZ = wz;
        boolean found = false;
        for (int ox = -14; ox <= 14; ++ox) {
            for (int oz = -14; oz <= 14; ++oz) {
                int surface;
                BlockPos pos;
                int sx = wx + ox;
                int sz = wz + oz;
                if (generator.getOceanFloorHeight(sx, sz) <= generator.getSeaLevel() + 2 || !region.getBlockState(pos = new BlockPos(sx, (surface = region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sx, sz)) - 1, sz)).getFluidState().is((Fluid)Fluids.WATER)) continue;
                bestX = sx;
                bestZ = sz;
                found = true;
                break;
            }
            if (found) break;
        }
        int surface = region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bestX, bestZ);
        for (int dy = 0; dy <= 4 && (sy = surface - dy) > floorY; ++dy) {
            BlockPos pos = new BlockPos(bestX, sy, bestZ);
            BlockState state = region.getBlockState(pos);
            if (state.getFluidState().is((Fluid)Fluids.WATER)) {
                return;
            }
            if (!state.isAir() && !state.canBeReplaced((Fluid)Fluids.WATER)) continue;
            region.setBlock(pos, WATER, 2);
        }
    }

    private static void seedSurfaceOutlet(WorldGenLevel region, Generator generator, int wx, int wz) {
        int sy;
        int surface = region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
        int sea = generator.getSeaLevel();
        for (int dy = 0; dy <= 3 && (sy = surface - dy) > sea; ++dy) {
            BlockPos pos = new BlockPos(wx, sy, wz);
            BlockState state = region.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced((Fluid)Fluids.WATER)) continue;
            region.setBlock(pos, WATER, 2);
        }
    }

    static void setWater(WorldGenLevel region, BlockPos pos) {
        if (CaveTunnelRiverDecorator.canPlaceWater(region, pos)) {
            region.setBlock(pos, WATER, 2);
        }
    }

    static boolean canPlaceWater(WorldGenLevel region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        return state.isAir() || state.canBeReplaced((Fluid)Fluids.WATER) || state.getFluidState().is((Fluid)Fluids.WATER);
    }

    private record BasinScan(boolean basin, int minFloor, int minStep, int barrierStep) {
    }
}
