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
     * Post-carve river/lake surface water top-up ({@link #restoreRiverDepressions}).
     * Does not carve crust, reshape beds, or run void-fill plugs — those masked the root cut bug.
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
        ChunkUtil.refreshHeightmaps(chunk);
    }

    /** Horizontal reach of void fill around river/lake water columns (world-space, includes neighbor chunks). */
    private static final int NEAR_RIVER_FILL_RADIUS = 18;
    /** Valley / confluence influence from TerraForged river noise (matches NoiseGenerator gate). */
    static final float RIVER_INFLUENCE_NOISE = 0.75f;

    /** Max blocks above channel water that shore crust fill may bridge (not whole hillsides). */
    private static final int SHORE_CRUST_BRIDGE = 3;

    /**
     * Fills air in actual river/lake bed columns ({@code river==0}) and land within
     * {@link #NEAR_RIVER_FILL_RADIUS} of a bed column. Does not target whole river biomes.
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
                if (terrain.getTerrain().get(lx, lz).isRiver() || terrain.getTerrain().get(lx, lz).isLake()) {
                    continue;
                }
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int refWaterY = fillCtx.nearestRefWaterY(wx, wz);
                if (refWaterY < 0) {
                    continue;
                }
                int groundTop = CaveChunkSurfaceRepair.findNaturalGroundTop(chunk, lx, lz);
                int scanTop = Math.max(groundTop, refWaterY);
                if (!CaveChunkSurfaceRepair.hasAnyAirInColumnBand(chunk, lx, lz, scanTop, minY)) {
                    continue;
                }
                if (CaveChunkSurfaceRepair.columnHasProtectedBlocks(chunk, lx, lz, scanTop, minY)) {
                    continue;
                }
                int bedY = terrain.getHeight(lx, lz);
                int plugTop = CaveChunkSurfaceRepair.computeShorePlugTop(chunk, lx, lz, refWaterY, sea, groundTop);
                if (plugTop < 0) {
                    continue;
                }
                plugTops[lx][lz] = plugTop;
                CaveChunkSurfaceRepair.plugColumnVoids(chunk, level, lx, lz, bedY, plugTop, plugTop, sea, minY, false, gravel,
                        water, pos, fillSeed, layerCache);
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

    private static int resolveRiverSurfaceY(TerrainData terrain, int lx, int lz, int sea) {
        return Math.max(TerrainLevels.getWaterLevel(lx, lz, sea, terrain), terrain.getBaseHeight(lx, lz));
    }

    private static int computeShorePlugTop(ChunkAccess chunk, int lx, int lz, int refWaterY, int sea, int groundTop) {
        int top = refWaterY;
        int plugFloor = top > sea + 4 ? Math.max(chunk.getMinBuildHeight(), sea) : chunk.getMinBuildHeight();
        if (groundTop > refWaterY && groundTop <= refWaterY + SHORE_CRUST_BRIDGE
                && CaveChunkSurfaceRepair.hasAnyAirInColumnBand(chunk, lx, lz, groundTop, plugFloor)) {
            top = groundTop;
        }
        return top;
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
            if (!chunk.getBlockState(pos).isAir()) {
                continue;
            }
            if (waterColumn && y > bedY && y <= waterY) {
                BlockState fill = y == waterY ? (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0) : water;
                chunk.setBlockState(pos, fill, false);
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
                    plugTop = CaveChunkSurfaceRepair.computeShorePlugTop(chunk, lx, lz, refWaterY, sea, groundTop);
                    if (plugTop < 0 || !CaveChunkSurfaceRepair.hasAnyAirInColumnBand(chunk, lx, lz, plugTop, minY)) {
                        continue;
                    }
                }
                int plugFloor = plugTop > sea + 4 ? Math.max(minY, sea) : minY;
                if (plugTop < plugFloor) {
                    continue;
                }
                boolean nearChannel = fillCtx.nearestRefWaterY(wx, wz) >= 0;
                for (int y = plugTop; y >= plugFloor; --y) {
                    pos.set(lx, y, lz);
                    if (!chunk.getBlockState(pos).isAir()) {
                        continue;
                    }
                    if (!nearChannel && !CaveChunkSurfaceRepair.hasAdjacentRiverFill(chunk, level, lx, y, lz, neighbor)) {
                        continue;
                    }
                    chunk.setBlockState(pos,
                            RiverVoidStrataFill.pickStoneFill(level, chunk, lx, y, lz, fillSeed, layerCache), false);
                }
            }
        }
    }

    private static boolean hasAdjacentRiverFill(ChunkAccess chunk, BlockGetter level, int lx, int y, int lz,
            BlockPos.MutableBlockPos neighbor) {
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
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
                return true;
            }
            if (RiverVoidStrataFill.isStrataCandidate(state)) {
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
            BlockState fill = y == waterY ? (BlockState)water.setValue((Property)LiquidBlock.LEVEL, 0) : water;
            if (chunk.getBlockState(pos).isAir()) {
                chunk.setBlockState(pos, fill, false);
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
