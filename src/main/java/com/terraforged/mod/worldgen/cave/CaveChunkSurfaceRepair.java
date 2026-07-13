package com.terraforged.mod.worldgen.cave;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

public final class CaveChunkSurfaceRepair {
    private static final int SURFACE_LIFT = 16;
    /** Max height change per column during noise repair — avoids chunk-edge cliffs. */
    private static final int MAX_REPAIR_DELTA = 2;
    /**
     * Post-carve river/lake repair ({@link #restoreRiverDepressions}): water top-up + seam fill only.
     * Does not carve crust or reshape beds (trim/sync removed as root cut bug).
     */
    public static boolean riverDepressionRestoreEnabled = true;

    private CaveChunkSurfaceRepair() {
    }

    public static boolean isRiverDepressionRestoreEnabled() {
        return CaveChunkSurfaceRepair.riverDepressionRestoreEnabled;
    }

    public static int[][] readGroundHeights(ChunkAccess chunk, CarverChunk carver) {
        return CaveChunkSurfaceRepair.readGroundHeights(chunk, carver, null, null);
    }

    public static int[][] readGroundHeights(ChunkAccess chunk, CarverChunk carver, TerrainData terrain, Generator generator) {
        int[][] heights = new int[16][16];
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                heights[lx][lz] = CaveChunkSurfaceRepair.resolveRepairHeight(chunk, carver, terrain, generator, lx, lz);
            }
        }
        return heights;
    }

    static boolean isRiverBedColumn(TerrainData terrain, int lx, int lz) {
        Terrain type = terrain.getTerrain().get(lx, lz);
        return (type.isRiver() || type.isLake()) && terrain.getRiver().get(lx, lz) == 0.0f;
    }

    private static int resolveRepairHeight(ChunkAccess chunk, CarverChunk carver, TerrainData terrain, Generator generator, int lx, int lz) {
        if (terrain != null && CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
            return CaveChunkSurfaceRepair.landReferenceHeight(terrain, lx, lz, generator != null ? generator.getSeaLevel() : 62);
        }
        return CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
    }

    private static int landReferenceHeight(TerrainData terrain, int lx, int lz, int sea) {
        int best = Integer.MIN_VALUE;
        for (int dz = -2; dz <= 2; ++dz) {
            for (int dx = -2; dx <= 2; ++dx) {
                int nx = lx + dx;
                int nz = lz + dz;
                if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) {
                    continue;
                }
                if (CaveChunkSurfaceRepair.isRiverBedColumn(terrain, nx, nz)) {
                    continue;
                }
                best = Math.max(best, terrain.getHeight(nx, nz));
            }
        }
        if (best == Integer.MIN_VALUE) {
            best = Math.max(terrain.getHeight(lx, lz), terrain.getBaseHeight(lx, lz));
        }
        return Math.max(best, sea);
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
        CaveChunkSurfaceRepair.repairNoiseSurface(chunk, carver, generator, region, null, aggressive);
    }

    public static void repairNoiseSurface(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, TerrainData terrain, boolean aggressive) {
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
        ChunkUtil.refreshHeightmaps(chunk);
        int[][] heights = CaveChunkSurfaceRepair.readGroundHeights(chunk, carver, terrain, generator);
        int[][] target = CaveChunkSurfaceRepair.computeTargetHeights(chunk, carver, region, terrain, generator, heights, aggressive);
        CaveChunkSurfaceRepair.applyTargetHeights(chunk, carver, generator, terrain, heights, target);
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
        ChunkUtil.refreshHeightmaps(chunk);
    }

    /** True when mega/synapse opened air under a river bed — needs bed plug, not depression carve. */
    public static boolean riverDepressionSkippedDueToCaveAir(ChunkAccess chunk, int lx, int lz, int bedY, int waterY) {
        return CaveChunkSurfaceRepair.hasCaveChamberBelow(chunk, lx, lz, bedY);
    }

    /**
     * River bed from terrain noise can sit at sea level on high rivers while {@code waterY} follows
     * {@code baseHeight} — carving down to that bed opens a shaft to Y=62 with carving disabled.
     */
    private static int resolveRiverBedY(TerrainData terrain, int lx, int lz, int waterY, int sea) {
        int bedY = terrain.getHeight(lx, lz);
        if (waterY <= sea + 2) {
            return bedY;
        }
        int maxDepth = Math.max(2, Math.min(6, (waterY - sea) / 8 + 2));
        int minBed = waterY - maxDepth;
        if (bedY < minBed) {
            return minBed;
        }
        return bedY;
    }

    /**
     * After cave carving breached a river column: plug the visible shaft (often down to sea level)
     * and rebuild bed + water. This is what masked surface tunnels before we disabled the pass.
     */
    private static void restoreRiverBedAfterCaveBreached(ChunkAccess chunk, CarverChunk carver, int lx, int lz,
            int bedY, int waterY, int sea, BlockState water, BlockPos.MutableBlockPos pos, BlockGetter sampler,
            int fillSeed, Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache) {
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        int plugFloor = Math.max(chunk.getMinBuildHeight(), sea);
        for (int y = waterY; y >= plugFloor; --y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (y == waterY) {
                chunk.setBlockState(pos, (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0), false);
                continue;
            }
            if (y > bedY) {
                if (state.isAir()) {
                    chunk.setBlockState(pos, water, false);
                }
                continue;
            }
            if (y == bedY) {
                if (state.isAir()) {
                    chunk.setBlockState(pos, gravel, false);
                }
                continue;
            }
            if (state.isAir()) {
                chunk.setBlockState(pos, RiverVoidStrataFill.pickStoneFill(sampler, chunk, lx, y, lz, fillSeed, layerCache),
                        false);
            }
        }
    }

    /** Carve river/lake beds from terrain data after flat surface repair — keeps channels from leaking. */
    public static void restoreRiverDepressions(ChunkAccess chunk, CarverChunk carver, Generator generator, TerrainData terrain) {
        CaveChunkSurfaceRepair.restoreRiverDepressions(chunk, carver, generator, terrain, chunk, false);
    }

    public static void restoreRiverDepressions(ChunkAccess chunk, CarverChunk carver, Generator generator, TerrainData terrain,
            BlockGetter sampler) {
        CaveChunkSurfaceRepair.restoreRiverDepressions(chunk, carver, generator, terrain, sampler, false);
    }

    private static void restoreRiverDepressions(ChunkAccess chunk, CarverChunk carver, Generator generator, TerrainData terrain,
            BlockGetter sampler, boolean preCarve) {
        if (preCarve) {
            return;
        }
        if (!CaveChunkSurfaceRepair.riverDepressionRestoreEnabled) {
            return;
        }
        if (terrain == null) {
            return;
        }
        BlockGetter level = sampler != null ? sampler : chunk;
        int fillSeed = (int)generator.getSeed() ^ (int)(chunk.getPos().toLong() >>> 32) ^ (int)chunk.getPos().toLong();
        Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache = RiverVoidStrataFill.newLayerCache();
        int sea = generator.getSeaLevel();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                if (!CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
                    continue;
                }
                int waterY = TerrainLevels.getWaterLevel(lx, lz, sea, terrain);
                int bedY = terrain.getHeight(lx, lz);
                CaveChunkSurfaceRepair.refillWaterOnly(chunk, carver, lx, lz, bedY, waterY, water, pos);
                pos.set(lx, waterY, lz);
                BlockState surface = chunk.getBlockState(pos);
                if (surface.isAir()) {
                    chunk.setBlockState(pos, (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0), false);
                }
            }
        }
        CaveChunkSurfaceRepair.plugRiverChannelVoids(chunk, carver, generator, terrain, level, fillSeed, layerCache);
        ChunkUtil.refreshHeightmaps(chunk);
    }

    /** Horizontal reach of void fill around river/lake water columns (world-space, includes neighbor chunks). */
    private static final int NEAR_RIVER_FILL_RADIUS = 18;
    /** Tighter radius for aggressive shore seam fill — subsurface/undercut only, not channel width. */
    private static final int SHORE_SEAM_RADIUS = 10;
    private static final int SHORE_SEAM_RADIUS_SQ = SHORE_SEAM_RADIUS * SHORE_SEAM_RADIUS;
    /** Valley / confluence influence from TerraForged river noise (matches NoiseGenerator gate). */
    static final float RIVER_INFLUENCE_NOISE = 0.75f;

    /**
     * Fills seams in river/lake bed columns ({@code river==0}), river/lake banks, and land within
     * {@link #NEAR_RIVER_FILL_RADIUS} of a bed column.
     */
    public static void plugRiverChannelVoids(ChunkAccess chunk, CarverChunk carver, Generator generator, TerrainData terrain,
            BlockGetter sampler, int fillSeed, Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache) {
        if (terrain == null) {
            return;
        }
        if (!RiverVoidFillContext.chunkNeedsFill(generator, terrain, chunk)) {
            return;
        }
        BlockGetter level = sampler != null ? sampler : chunk;
        if (layerCache == null) {
            layerCache = RiverVoidStrataFill.newLayerCache();
        }
        RiverVoidFillContext fillCtx = new RiverVoidFillContext(generator, chunk, terrain, level, generator.getSeaLevel(),
                NEAR_RIVER_FILL_RADIUS);
        int sea = generator.getSeaLevel();
        int minY = chunk.getMinBuildHeight();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[][] plugTops = new int[16][16];
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                plugTops[lx][lz] = Integer.MIN_VALUE;
            }
        }
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                if (!CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
                    continue;
                }
                int waterY = CaveChunkSurfaceRepair.resolveRiverSurfaceY(terrain, lx, lz, sea);
                int bedY = CaveChunkSurfaceRepair.resolveRiverBedY(terrain, lx, lz, waterY, sea);
                plugTops[lx][lz] = waterY;
                CaveChunkSurfaceRepair.plugColumnVoids(chunk, level, lx, lz, bedY, waterY, waterY, sea, minY, true, gravel,
                        water, pos, fillSeed, layerCache);
            }
        }
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                if (CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
                    continue;
                }
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int refWaterY = fillCtx.nearestRefWaterY(wx, wz);
                if (refWaterY < 0) {
                    continue;
                }
                int groundTop = CaveChunkSurfaceRepair.findNaturalGroundTop(chunk, lx, lz);
                int targetSurfaceY = CaveChunkSurfaceRepair.resolveBankSurfaceY(terrain, lx, lz, refWaterY, sea);
                int plugFloor = CaveChunkSurfaceRepair.resolveShorePlugFloor(refWaterY, sea, minY);
                if (targetSurfaceY < plugFloor) {
                    continue;
                }
                int shoreDistSq = fillCtx.nearestShoreDistSq(wx, wz);
                boolean nearShoreSeam = shoreDistSq <= CaveChunkSurfaceRepair.SHORE_SEAM_RADIUS_SQ;
                int columnWaterTop = CaveChunkSurfaceRepair.findColumnWaterTop(chunk, lx, lz, plugFloor,
                        chunk.getMaxBuildHeight() - 1);
                int scanTop = Math.max(Math.max(targetSurfaceY, groundTop), columnWaterTop);
                int gapTop = scanTop;
                int gapBottom = plugFloor;
                if (nearShoreSeam) {
                    gapTop = Math.max(gapTop, refWaterY + 2);
                    gapBottom = Math.max(plugFloor, refWaterY - 4);
                }
                boolean needsRaise = targetSurfaceY > groundTop;
                boolean needsGap = CaveChunkSurfaceRepair.hasFillableGapInColumnBand(chunk, lx, lz, gapTop, gapBottom)
                        || CaveChunkSurfaceRepair.hasUndercutShelf(chunk, lx, lz, groundTop, gapBottom)
                        || nearShoreSeam && CaveChunkSurfaceRepair.hasSubsurfaceShoreVoid(chunk, lx, lz, groundTop,
                                gapBottom, refWaterY);
                boolean needsFloatingWater = CaveChunkSurfaceRepair.hasFloatingWaterVoid(chunk, lx, lz, gapBottom, gapTop);
                if (!needsRaise && !needsGap && !needsFloatingWater) {
                    continue;
                }
                int fillTop = Math.max(targetSurfaceY,
                        CaveChunkSurfaceRepair.findColumnWaterTop(chunk, lx, lz, gapBottom, gapTop));
                if (CaveChunkSurfaceRepair.columnHasProtectedBlocks(chunk, lx, lz, fillTop, plugFloor)) {
                    continue;
                }
                plugTops[lx][lz] = fillTop;
                CaveChunkSurfaceRepair.restoreBankColumnToTerrain(chunk, level, lx, lz, plugFloor, targetSurfaceY, fillTop,
                        refWaterY, wx, wz, fillSeed, layerCache, pos);
            }
        }
        CaveChunkSurfaceRepair.sealHorizontalRiverGaps(chunk, level, generator, terrain, fillCtx, sea, minY, fillSeed,
                layerCache, plugTops);
        ChunkUtil.refreshHeightmaps(chunk);
    }

    /** Natural surface for shore fill — grass/dirt/stone, never logs/leaves canopy. */
    private static int findNaturalGroundTop(ChunkAccess chunk, int lx, int lz) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; --y) {
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                continue;
            }
            if (CaveChunkSurfaceRepair.isPillarOrLeak(state)) {
                continue;
            }
            return y;
        }
        return CaveChunkSurfaceBounds.surfaceY(chunk, lx, lz);
    }

    private static boolean hasAnyAirInColumnBand(ChunkAccess chunk, int lx, int lz, int topY, int bottomY) {
        int minY = Math.max(chunk.getMinBuildHeight(), Math.min(topY, bottomY));
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, Math.max(topY, bottomY));
        for (int y = maxY; y >= minY; --y) {
            if (chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gap detection for river seam fill — air or subsurface water trapped under shore crust.
     * Open channel water (air above) is not a fillable gap.
     */
    private static boolean hasFillableGapInColumnBand(ChunkAccess chunk, int lx, int lz, int topY, int bottomY) {
        int minY = Math.max(chunk.getMinBuildHeight(), Math.min(topY, bottomY));
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, Math.max(topY, bottomY));
        for (int y = maxY; y >= minY; --y) {
            if (CaveChunkSurfaceRepair.isFillableGapBlock(chunk, lx, y, lz)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFillableGapBlock(ChunkAccess chunk, int lx, int y, int lz) {
        BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().is(Fluids.WATER)) {
            return false;
        }
        if (CaveChunkSurfaceRepair.hasSolidCrustAbove(chunk, lx, y, lz)) {
            return true;
        }
        int below = y - 1;
        return below >= chunk.getMinBuildHeight()
                && chunk.getBlockState(new BlockPos(lx, below, lz)).isAir();
    }

    /** Blocks that may be replaced by fill — air and seam water only, not open river surface. */
    private static boolean shouldPlaceFillBlock(ChunkAccess chunk, int lx, int y, int lz) {
        BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().is(Fluids.WATER)) {
            return false;
        }
        return CaveChunkSurfaceRepair.hasSolidCrustAbove(chunk, lx, y, lz);
    }

    /** Water layer sitting over air — void under hanging water (screen 2). */
    private static boolean hasFloatingWaterVoid(ChunkAccess chunk, int lx, int lz, int floorY, int topY) {
        int minY = Math.max(chunk.getMinBuildHeight(), floorY);
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, topY);
        for (int y = maxY; y > minY; --y) {
            BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
            if (!state.getFluidState().is(Fluids.WATER)) {
                continue;
            }
            if (chunk.getBlockState(new BlockPos(lx, y - 1, lz)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static int findColumnWaterTop(ChunkAccess chunk, int lx, int lz, int floorY, int topY) {
        int minY = Math.max(chunk.getMinBuildHeight(), floorY);
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, topY);
        int waterTop = minY;
        for (int y = minY; y <= maxY; ++y) {
            if (chunk.getBlockState(new BlockPos(lx, y, lz)).getFluidState().is(Fluids.WATER)) {
                waterTop = y;
            }
        }
        return waterTop;
    }

    /** Grass/dirt shelf with only air/water beneath down to plug floor. */
    private static boolean hasUndercutShelf(ChunkAccess chunk, int lx, int lz, int groundTop, int plugFloor) {
        if (groundTop <= plugFloor) {
            return false;
        }
        boolean sawGap = false;
        for (int y = groundTop - 1; y >= plugFloor; --y) {
            if (CaveChunkSurfaceRepair.isFillableGapBlock(chunk, lx, y, lz)) {
                sawGap = true;
                continue;
            }
            BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
            if (!state.getFluidState().isEmpty()) {
                continue;
            }
            if (!state.isAir()) {
                return sawGap;
            }
        }
        return sawGap;
    }

    /** Air pocket under bank surface within a few blocks below water — common carved shore seam. */
    private static boolean hasSubsurfaceShoreVoid(ChunkAccess chunk, int lx, int lz, int groundTop, int plugFloor,
            int refWaterY) {
        if (refWaterY < 0 || groundTop <= plugFloor) {
            return false;
        }
        int bandTop = Math.min(groundTop, refWaterY + 1);
        int bandBottom = Math.max(plugFloor, refWaterY - 3);
        for (int y = bandTop; y >= bandBottom; --y) {
            if (CaveChunkSurfaceRepair.isFillableGapBlock(chunk, lx, y, lz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Horizontal air/water at the channel surface — filling this with solid narrows the river (old aggressive bug).
     */
    private static boolean isOpenWaterLevelGap(ChunkAccess chunk, BlockGetter level, int lx, int y, int lz, int wx, int wz,
            int refWaterY) {
        if (refWaterY < 0 || y > refWaterY || y < refWaterY - 1) {
            return false;
        }
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            int nx = lx + offset[0];
            int nz = lz + offset[1];
            BlockState neighbor;
            if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                neighbor = chunk.getBlockState(new BlockPos(nx, y, nz));
            } else {
                neighbor = level.getBlockState(new BlockPos(wx + offset[0], y, wz + offset[1]));
            }
            if (!neighbor.getFluidState().is(Fluids.WATER)) {
                continue;
            }
            if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                if (!CaveChunkSurfaceRepair.hasSolidCrustAbove(chunk, nx, y, nz)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean mayFillShoreBlock(ChunkAccess chunk, BlockGetter level, int lx, int y, int lz, int wx, int wz,
            int refWaterY) {
        if (!CaveChunkSurfaceRepair.shouldPlaceFillBlock(chunk, lx, y, lz)) {
            return false;
        }
        return !CaveChunkSurfaceRepair.isOpenWaterLevelGap(chunk, level, lx, y, lz, wx, wz, refWaterY);
    }
    private static boolean hasSolidCrustAbove(ChunkAccess chunk, int lx, int y, int lz) {
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, y + 6);
        for (int cy = y + 1; cy <= maxY; ++cy) {
            BlockState above = chunk.getBlockState(new BlockPos(lx, cy, lz));
            if (above.isAir() || above.getFluidState().is(Fluids.WATER)) {
                continue;
            }
            if (!above.getFluidState().isEmpty()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static int resolveRiverSurfaceY(TerrainData terrain, int lx, int lz, int sea) {
        return Math.max(TerrainLevels.getWaterLevel(lx, lz, sea, terrain), terrain.getBaseHeight(lx, lz));
    }

    /** Target surface from TerraForged terrain height — the yellow-line bank, not flat water level. */
    private static int resolveBankSurfaceY(TerrainData terrain, int lx, int lz, int refWaterY, int sea) {
        int terrainY = terrain.getHeight(lx, lz);
        return Math.max(terrainY, refWaterY);
    }

    private static int resolveShorePlugFloor(int refWaterY, int sea, int minY) {
        return refWaterY > sea + 4 ? Math.max(minY, sea) : minY;
    }

    /**
     * Rebuild bank column up to engine terrain height — restores slope (green check), not a flat water-level shelf (red).
     */
    private static void restoreBankColumnToTerrain(ChunkAccess chunk, BlockGetter sampler, int lx, int lz, int floorY,
            int targetSurfaceY, int fillTop, int refWaterY, int wx, int wz, int fillSeed,
            Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache, BlockPos.MutableBlockPos pos) {
        BlockState surfaceCover = RiverVoidStrataFill.sampleSurfaceCover(sampler, chunk, lx, lz, targetSurfaceY);
        BlockState subsurfaceFill = RiverVoidStrataFill.sampleSubsurfaceFill(sampler, chunk, lx, lz, targetSurfaceY);
        int surfaceY = targetSurfaceY;
        for (int y = Math.min(fillTop, targetSurfaceY + 1); y >= Math.max(floorY, targetSurfaceY - 1); --y) {
            if (CaveChunkSurfaceRepair.mayFillShoreBlock(chunk, sampler, lx, y, lz, wx, wz, refWaterY)) {
                surfaceY = y;
                break;
            }
        }
        for (int y = floorY; y <= fillTop; ++y) {
            pos.set(lx, y, lz);
            BlockState current = chunk.getBlockState(pos);
            if (y == surfaceY) {
                if (current.isAir() || !current.getFluidState().isEmpty()) {
                    chunk.setBlockState(pos, surfaceCover, false);
                }
                continue;
            }
            if (!CaveChunkSurfaceRepair.mayFillShoreBlock(chunk, sampler, lx, y, lz, wx, wz, refWaterY)) {
                continue;
            }
            BlockState fill;
            if (y >= surfaceY - 4) {
                fill = subsurfaceFill;
            } else {
                fill = RiverVoidStrataFill.pickStoneFill(sampler, chunk, lx, y, lz, fillSeed, layerCache);
            }
            chunk.setBlockState(pos, fill, false);
        }
    }

    private static boolean columnHasProtectedBlocks(ChunkAccess chunk, int lx, int lz, int topY, int bottomY) {
        int minY = Math.max(chunk.getMinBuildHeight(), Math.min(topY, bottomY));
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, Math.max(topY, bottomY));
        for (int y = maxY; y >= minY; --y) {
            if (CaveChunkSurfaceRepair.isProtectedFromRiverFill(chunk.getBlockState(new BlockPos(lx, y, lz)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProtectedFromRiverFill(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.CROPS) || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.BEDS) || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.WOOL) || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH) || state.is(Blocks.CHEST) || state.is(Blocks.BARREL)
                || state.is(Blocks.COMPOSTER) || state.is(Blocks.CAULDRON) || state.is(Blocks.LECTERN)
                || state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.BANNERS) || state.is(BlockTags.SIGNS)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.LOOM)
                || state.is(Blocks.CARTOGRAPHY_TABLE) || state.is(Blocks.FLETCHING_TABLE)
                || state.is(Blocks.STONECUTTER) || state.is(Blocks.GRINDSTONE);
    }

    private static void plugColumnVoids(ChunkAccess chunk, BlockGetter sampler, int lx, int lz, int bedY, int plugTop, int waterY,
            int sea, int minY, boolean waterColumn, BlockState gravel, BlockState water, BlockPos.MutableBlockPos pos,
            int fillSeed, Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache) {
        int plugFloor = minY;
        if (plugTop > sea + 4) {
            plugFloor = Math.max(minY, sea);
        }
        if (plugTop < plugFloor) {
            return;
        }
        if (!waterColumn && CaveChunkSurfaceRepair.columnHasProtectedBlocks(chunk, lx, lz, plugTop, plugFloor)) {
            return;
        }
        for (int y = plugTop; y >= plugFloor; --y) {
            pos.set(lx, y, lz);
            if (!CaveChunkSurfaceRepair.shouldPlaceFillBlock(chunk, lx, y, lz)) {
                continue;
            }
            if (waterColumn && y > bedY && y <= waterY) {
                BlockState current = chunk.getBlockState(pos);
                if (current.isAir()) {
                    BlockState fill = y == waterY ? (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0) : water;
                    chunk.setBlockState(pos, fill, false);
                } else {
                    chunk.setBlockState(pos, gravel, false);
                }
            } else if (waterColumn && y == bedY) {
                chunk.setBlockState(pos, gravel, false);
            } else {
                chunk.setBlockState(pos,
                        RiverVoidStrataFill.pickStoneFill(sampler, chunk, lx, y, lz, fillSeed, layerCache), false);
            }
        }
    }

    /** Closes horizontal air pockets between filled river columns (confluences / shore seams). */
    private static void sealHorizontalRiverGaps(ChunkAccess chunk, BlockGetter level, Generator generator, TerrainData terrain,
            RiverVoidFillContext fillCtx, int sea, int minY, int fillSeed,
            Map<Integer, RiverVoidStrataFill.LayerPalette> layerCache, int[][] plugTops) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int plugTop = plugTops[lx][lz];
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                if (plugTop == Integer.MIN_VALUE) {
                    int refWaterY = fillCtx.nearestRefWaterY(wx, wz);
                    if (refWaterY < 0) {
                        continue;
                    }
                    if (CaveChunkSurfaceRepair.columnHasProtectedBlocks(chunk, lx, lz,
                            CaveChunkSurfaceRepair.findNaturalGroundTop(chunk, lx, lz), minY)) {
                        continue;
                    }
                    int groundTop = CaveChunkSurfaceRepair.findNaturalGroundTop(chunk, lx, lz);
                    int targetSurfaceY = CaveChunkSurfaceRepair.resolveBankSurfaceY(terrain, lx, lz, refWaterY, sea);
                    int plugFloor = CaveChunkSurfaceRepair.resolveShorePlugFloor(refWaterY, sea, minY);
                    plugTop = targetSurfaceY;
                    if (plugTop < plugFloor || (!CaveChunkSurfaceRepair.hasFillableGapInColumnBand(chunk, lx, lz, plugTop, plugFloor)
                            && plugTop <= groundTop)) {
                        continue;
                    }
                }
                int refWaterYSeal = fillCtx.nearestRefWaterY(wx, wz);
                int plugFloor = refWaterYSeal >= 0
                        ? CaveChunkSurfaceRepair.resolveShorePlugFloor(refWaterYSeal, sea, minY)
                        : (plugTop > sea + 4 ? Math.max(minY, sea) : minY);
                if (plugTop < plugFloor) {
                    continue;
                }
                int shoreDistSq = fillCtx.nearestShoreDistSq(wx, wz);
                boolean nearShoreSeam = shoreDistSq <= CaveChunkSurfaceRepair.SHORE_SEAM_RADIUS_SQ;
                int targetSurfaceY = refWaterYSeal >= 0
                        ? CaveChunkSurfaceRepair.resolveBankSurfaceY(terrain, lx, lz, refWaterYSeal, sea)
                        : plugTop;
                BlockState surfaceCover = RiverVoidStrataFill.sampleSurfaceCover(level, chunk, lx, lz, targetSurfaceY);
                BlockState subsurfaceFill = RiverVoidStrataFill.sampleSubsurfaceFill(level, chunk, lx, lz, targetSurfaceY);
                int sealTop = nearShoreSeam && refWaterYSeal >= 0 ? Math.max(plugTop, refWaterYSeal + 2) : plugTop;
                int sealBottom = nearShoreSeam && refWaterYSeal >= 0
                        ? Math.max(plugFloor, refWaterYSeal - 4)
                        : plugFloor;
                for (int y = sealTop; y >= sealBottom; --y) {
                    pos.set(lx, y, lz);
                    if (!CaveChunkSurfaceRepair.mayFillShoreBlock(chunk, level, lx, y, lz, wx, wz, refWaterYSeal)) {
                        continue;
                    }
                    if (refWaterYSeal < 0 && !CaveChunkSurfaceRepair.hasAdjacentRiverFill(chunk, level, lx, y, lz,
                            refWaterYSeal, neighbor)) {
                        continue;
                    }
                    if (refWaterYSeal >= 0 && y >= refWaterYSeal - 1
                            && !CaveChunkSurfaceRepair.hasAdjacentShoreFill(chunk, level, lx, y, lz, refWaterYSeal,
                                    neighbor)) {
                        continue;
                    }
                    BlockState fill;
                    if (y >= targetSurfaceY - 4) {
                        fill = y >= targetSurfaceY ? surfaceCover : subsurfaceFill;
                    } else {
                        fill = RiverVoidStrataFill.pickStoneFill(level, chunk, lx, y, lz, fillSeed, layerCache);
                    }
                    chunk.setBlockState(pos, fill, false);
                }
            }
        }
    }

    private static boolean hasAdjacentRiverFill(ChunkAccess chunk, BlockGetter level, int lx, int y, int lz,
            int refWaterY, BlockPos.MutableBlockPos neighbor) {
        return CaveChunkSurfaceRepair.hasAdjacentShoreFill(chunk, level, lx, y, lz, refWaterY, neighbor);
    }

    /** Cardinal + diagonal neighbors with water, strata, or shore fill — subsurface seam closure. */
    private static boolean hasAdjacentShoreFill(ChunkAccess chunk, BlockGetter level, int lx, int y, int lz, int refWaterY,
            BlockPos.MutableBlockPos neighbor) {
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        boolean subsurface = refWaterY >= 0 && y < refWaterY - 1;
        for (int[] offset : offsets) {
            int nx = lx + offset[0];
            int nz = lz + offset[1];
            if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                neighbor.set(nx, y, nz);
            } else {
                neighbor.set(chunk.getPos().getMinBlockX() + nx, y, chunk.getPos().getMinBlockZ() + nz);
            }
            BlockState state;
            if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                state = chunk.getBlockState(neighbor);
            } else {
                state = level.getBlockState(neighbor);
            }
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty() && state.getFluidState().is(Fluids.WATER)) {
                if (subsurface || offset[0] == 0 || offset[1] == 0) {
                    return true;
                }
                continue;
            }
            if (RiverVoidStrataFill.isStrataCandidate(state) || RiverVoidStrataFill.isSubsurfaceFillCandidate(state)
                    || RiverVoidStrataFill.isSurfaceCoverCandidate(state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mayModifyRiverColumn(int lx, int y, int lz, int bedY, int waterY) {
        return y > bedY && y <= waterY;
    }

    private static boolean mayModifyRiver(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, int waterY) {
        if (carver != null && carver.isEntranceColumn(lx, lz)) {
            return false;
        }
        return y >= waterY - CaveChunkSurfaceBounds.SURFACE_CRUST && y <= waterY;
    }

    private static boolean hasCaveChamberBelow(ChunkAccess chunk, int lx, int lz, int bedY) {
        return CaveChunkSurfaceRepair.hasAirInColumnBand(chunk, lx, lz, bedY - 1, Math.max(chunk.getMinBuildHeight(), bedY - 48));
    }

    private static boolean hasAirInColumnBand(ChunkAccess chunk, int lx, int lz, int topY, int bottomY) {
        int minY = Math.max(chunk.getMinBuildHeight(), Math.min(topY, bottomY));
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, Math.max(topY, bottomY));
        int airRun = 0;
        for (int y = maxY; y >= minY; --y) {
            BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
            if (state.isAir()) {
                if (++airRun >= 2) {
                    return true;
                }
                continue;
            }
            airRun = 0;
        }
        return false;
    }

    /** Remove lifted surface soil above the water line — never opens rock below the channel band. */
    private static void trimRiverCrustAboveWater(ChunkAccess chunk, CarverChunk carver, int lx, int lz, int waterY, int shellTop, BlockPos.MutableBlockPos pos) {
        for (int y = shellTop; y > waterY; --y) {
            if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                continue;
            }
            pos.set(lx, y, lz);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (!CaveChunkSurfaceRepair.isRiverCrustBlock(state)) {
                continue;
            }
            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
        }
    }

    /**
     * Sync bed + water in the channel band only ({@code bedY+1..waterY}). Replaces misplaced
     * soil/stone in-band with water instead of carving a deep air shaft.
     */
    private static void syncRiverChannelBand(ChunkAccess chunk, CarverChunk carver, int lx, int lz, int bedY, int waterY, BlockState water, BlockPos.MutableBlockPos pos) {
        if (waterY <= bedY) {
            CaveChunkSurfaceRepair.refillWaterOnly(chunk, carver, lx, lz, bedY, waterY, water, pos);
            return;
        }
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        for (int y = bedY + 1; y <= waterY; ++y) {
            if (!CaveChunkSurfaceRepair.mayModifyRiverColumn(lx, y, lz, bedY, waterY)) {
                continue;
            }
            pos.set(lx, y, lz);
            BlockState fill = y == waterY ? (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0) : water;
            chunk.setBlockState(pos, fill, false);
        }
        if (CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, bedY, lz)) {
            pos.set(lx, bedY, lz);
            BlockState bed = chunk.getBlockState(pos);
            if (bed.isAir()) {
                chunk.setBlockState(pos, gravel, false);
            }
        }
    }

    private static boolean isRiverCrustBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS);
    }

    private static void refillWaterOnly(ChunkAccess chunk, CarverChunk carver, int lx, int lz, int bedY, int waterY, BlockState water, BlockPos.MutableBlockPos pos) {
        for (int y = bedY + 1; y <= waterY; ++y) {
            if (!CaveChunkSurfaceRepair.mayModifyRiver(chunk, carver, lx, y, lz, waterY)) {
                continue;
            }
            pos.set(lx, y, lz);
            BlockState current = chunk.getBlockState(pos);
            if (current.isAir()) {
                BlockState fill = y == waterY ? (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0) : water;
                chunk.setBlockState(pos, fill, false);
            } else if (current.getFluidState().is(Fluids.WATER)
                    && CaveChunkSurfaceRepair.hasSolidCrustAbove(chunk, lx, y, lz)) {
                chunk.setBlockState(pos, Blocks.GRAVEL.defaultBlockState(), false);
            }
        }
    }

    public static void stripSurfacePillars(ChunkAccess chunk, CarverChunk carver) {
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver, false);
    }

    public static void stripRiskSurfacePillars(ChunkAccess chunk, CarverChunk carver) {
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver, true);
    }

    private static void stripSurfacePillars(ChunkAccess chunk, CarverChunk carver, boolean riskOnly) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                if (riskOnly && (carver == null || !carver.isSurfaceRiskColumn(lx, lz))) {
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

    private static int[][] computeTargetHeights(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, TerrainData terrain, Generator generator, int[][] heights, boolean aggressive) {
        int[][] target = new int[16][16];
        boolean chessboard = CaveChunkCorruptionChecker.detectChessboardNoise(heights, carver);
        int chunkAverage = CaveChunkSurfaceRepair.averageGroundHeight(heights, carver);
        int sea = generator != null ? generator.getSeaLevel() : 62;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    target[lx][lz] = heights[lx][lz];
                    continue;
                }
                if (terrain != null && CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
                    target[lx][lz] = CaveChunkSurfaceRepair.landReferenceHeight(terrain, lx, lz, sea);
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

    private static void applyTargetHeights(ChunkAccess chunk, CarverChunk carver, Generator generator, TerrainData terrain, int[][] from, int[][] to) {
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
