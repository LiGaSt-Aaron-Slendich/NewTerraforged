package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Underground-only cleanup: floating cave vegetation and blocks painted for a different cave biome.
 */
public final class CaveFeatureRestorer {
    private static final int MAX_SUPPORT_SCAN = 32;
    private static final int MIN_FLOOR_DEPTH = 2;
    private static final int MAX_AIR_BELOW_SUPPORT = 2;
    private static final int MIN_CAVE_AIR = 10;
    private static final int MAX_TREE_CLUSTER = 4096;
    private static final int SURFACE_CRUST = 2;
    private static final int CAVE_SCAN_ABOVE_FLOOR = 128;
    private static final int[] NEIGHBOR_OFFSETS = new int[]{-1, 0, 0, 1, 0, 0, 0, -1, 0, 0, 1, 0, 0, 0, -1, 0, 0, 1};

    private CaveFeatureRestorer() {
    }

    public static int restore(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null || !carver.isColumnCacheReady()) {
            return 0;
        }
        int removed = 0;
        removed += CaveFeatureRestorer.removeWrongBiomeVegetation(chunk, carver);
        removed += CaveFeatureRestorer.removeFloatingClusters(chunk);
        removed += CaveFeatureRestorer.removeUnsupportedVegetation(chunk);
        removed += CaveFeatureRestorer.removeUnsupportedLogs(chunk);
        removed += CaveFeatureRestorer.removeFloatingClusters(chunk);
        return removed;
    }

    private static int removeWrongBiomeVegetation(ChunkAccess chunk, CarverChunk carver) {
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = CaveFeatureRestorer.caveScanRange(chunk, lx, lz);
                if (range == null) {
                    continue;
                }
                for (int y = range[1]; y >= range[0]; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!CaveFeatureRestorer.isRemovableFeature(state)) {
                        continue;
                    }
                    Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
                    if (painted == null || !CaveBiomeIds.isModCaveBiome(painted)) {
                        continue;
                    }
                    Holder<Biome> expected = carver.resolveBiome(chunk, lx, y, lz);
                    if (CaveBiomeIds.sameBiomeKey(painted, expected)) {
                        continue;
                    }
                    chunk.setBlockState(pos, air, false);
                    ++removed;
                }
            }
        }
        return removed;
    }

    private static int removeFloatingClusters(ChunkAccess chunk) {
        LongOpenHashSet processed = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int removed = 0;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = CaveFeatureRestorer.caveScanRange(chunk, lx, lz);
                if (range == null) {
                    continue;
                }
                int yBottom = range[0];
                int yTop = range[1];
                for (int y = yBottom; y <= yTop; ++y) {
                    long key = CaveFeatureRestorer.pack(chunk, lx, y, lz);
                    if (processed.contains(key)) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!CaveFeatureRestorer.isTreeMaterial(state)) {
                        continue;
                    }
                    LongOpenHashSet seen = new LongOpenHashSet();
                    List<long[]> component = new ArrayList<>();
                    queue.clear();
                    seen.add(key);
                    processed.add(key);
                    queue.enqueue(key);
                    component.add(new long[]{lx, y, lz});
                    boolean grounded = CaveFeatureRestorer.isLogBlock(state)
                            ? CaveFeatureRestorer.hasCaveTreeGrounding(chunk, lx, y, lz, range)
                            : CaveFeatureRestorer.hasCaveVegetationGrounding(chunk, lx, y, lz, range);
                    int count = 1;
                    while (!queue.isEmpty() && count < MAX_TREE_CLUSTER) {
                        long current = queue.dequeueLong();
                        int cx = CaveFeatureRestorer.unpackX(current);
                        int cy = CaveFeatureRestorer.unpackY(chunk, current);
                        int cz = CaveFeatureRestorer.unpackZ(current);
                        for (int i = 0; i < NEIGHBOR_OFFSETS.length; i += 3) {
                            int nx = cx + NEIGHBOR_OFFSETS[i];
                            int ny = cy + NEIGHBOR_OFFSETS[i + 1];
                            int nz = cz + NEIGHBOR_OFFSETS[i + 2];
                            if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16 || ny < yBottom || ny > yTop) {
                                continue;
                            }
                            long next = CaveFeatureRestorer.pack(chunk, nx, ny, nz);
                            if (seen.contains(next)) {
                                continue;
                            }
                            pos.set(nx, ny, nz);
                            BlockState neighbor = chunk.getBlockState(pos);
                            if (!CaveFeatureRestorer.isTreeMaterial(neighbor) && !CaveFeatureRestorer.isVegetationBlock(neighbor)) {
                                continue;
                            }
                            seen.add(next);
                            processed.add(next);
                            queue.enqueue(next);
                            component.add(new long[]{nx, ny, nz});
                            ++count;
                            if (!grounded) {
                                if (CaveFeatureRestorer.isLogBlock(neighbor)) {
                                    grounded = CaveFeatureRestorer.hasCaveTreeGrounding(chunk, nx, ny, nz, range);
                                } else if (CaveFeatureRestorer.hasCaveVegetationGrounding(chunk, nx, ny, nz, range)) {
                                    grounded = true;
                                }
                            }
                        }
                    }
                    if (grounded) {
                        continue;
                    }
                    for (long[] block : component) {
                        pos.set((int)block[0], (int)block[1], (int)block[2]);
                        chunk.setBlockState(pos, air, false);
                        ++removed;
                    }
                }
            }
        }
        return removed;
    }

    private static int removeUnsupportedVegetation(ChunkAccess chunk) {
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = CaveFeatureRestorer.caveScanRange(chunk, lx, lz);
                if (range == null) {
                    continue;
                }
                for (int y = range[1]; y >= range[0]; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!CaveFeatureRestorer.isVegetationBlock(state) || CaveFeatureRestorer.isLogBlock(state)) {
                        continue;
                    }
                    if (CaveFeatureRestorer.hasCaveVegetationGrounding(chunk, lx, y, lz, range)) {
                        continue;
                    }
                    for (int cy = y; cy <= range[1]; ++cy) {
                        pos.set(lx, cy, lz);
                        BlockState above = chunk.getBlockState(pos);
                        if (!CaveFeatureRestorer.isVegetationBlock(above) || CaveFeatureRestorer.isLogBlock(above)) {
                            if (cy == y) {
                                break;
                            }
                            continue;
                        }
                        chunk.setBlockState(pos, air, false);
                        ++removed;
                    }
                }
            }
        }
        return removed;
    }

    /** Removes log columns/stubs that lost their canopy but still float above the cave floor. */
    private static int removeUnsupportedLogs(ChunkAccess chunk) {
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = CaveFeatureRestorer.caveScanRange(chunk, lx, lz);
                if (range == null) {
                    continue;
                }
                for (int y = range[1]; y >= range[0]; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!CaveFeatureRestorer.isLogBlock(state)) {
                        continue;
                    }
                    if (CaveFeatureRestorer.hasCaveTreeGrounding(chunk, lx, y, lz, range)) {
                        continue;
                    }
                    for (int cy = y; cy <= range[1]; ++cy) {
                        pos.set(lx, cy, lz);
                        BlockState above = chunk.getBlockState(pos);
                        if (!CaveFeatureRestorer.isLogBlock(above)) {
                            if (cy == y) {
                                break;
                            }
                            continue;
                        }
                        chunk.setBlockState(pos, air, false);
                        ++removed;
                    }
                }
            }
        }
        return removed;
    }

    /** Returns [bottomY, topY] underground scan band, or null when the column has no cave volume. */
    static int[] caveScanRange(ChunkAccess chunk, int lx, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minY = Math.max(chunk.getMinBuildHeight() + 8, surface - CAVE_SCAN_ABOVE_FLOOR);
        int topAir = -1;
        int bottomAir = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int air = 0;
        for (int y = surface; y >= minY; --y) {
            if (!chunk.getBlockState(pos.set(lx, y, lz)).isAir()) {
                continue;
            }
            ++air;
            topAir = y;
            bottomAir = Math.min(bottomAir, y);
        }
        if (air < MIN_CAVE_AIR || topAir < 0) {
            return null;
        }
        int scanTop = Math.min(topAir, surface - SURFACE_CRUST);
        if (scanTop < bottomAir) {
            scanTop = surface - SURFACE_CRUST;
        }
        if (scanTop < bottomAir) {
            return null;
        }
        return new int[]{bottomAir, scanTop};
    }

    private static boolean isRemovableFeature(BlockState state) {
        return CaveFeatureRestorer.isVegetationBlock(state) || CaveFeatureRestorer.isTreeMaterial(state);
    }

    private static boolean isTreeMaterial(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS);
    }

    private static boolean isLogBlock(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static boolean isVegetationBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return CaveFeatureRestorer.isTreeMaterial(state) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)
                || state.is(Blocks.COCOA) || state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER) || state.is(Blocks.GLOW_LICHEN)
                || state.is(Blocks.VINE) || state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT)
                || state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT)
                || state.is(Blocks.SPORE_BLOSSOM) || state.is(Blocks.HANGING_ROOTS)
                || state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM) || state.is(Blocks.SMALL_DRIPLEAF)
                || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.SHROOMLIGHT) || state.is(Blocks.WARPED_WART_BLOCK) || state.is(Blocks.NETHER_WART_BLOCK);
    }

    private static boolean hasCaveVegetationGrounding(ChunkAccess chunk, int lx, int y, int lz, int[] range) {
        return CaveFeatureRestorer.hasCaveTreeGrounding(chunk, lx, y, lz, range);
    }

    /** True when the block sits on a cave floor mass, not a thin shelf above open cavern air. */
    private static boolean hasCaveTreeGrounding(ChunkAccess chunk, int lx, int y, int lz, int[] range) {
        if (!CaveFeatureRestorer.hasConnectedFloorBelow(chunk, lx, y, lz, MIN_FLOOR_DEPTH)) {
            return false;
        }
        int supportBottom = y - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (supportBottom >= range[0] && CaveFeatureRestorer.isSupportColumnBlock(chunk.getBlockState(pos.set(lx, supportBottom, lz)))) {
            --supportBottom;
        }
        ++supportBottom;
        int airBelow = 0;
        for (int by = supportBottom - 1; by >= Math.max(range[0], supportBottom - 16); --by) {
            BlockState state = chunk.getBlockState(pos.set(lx, by, lz));
            if (state.isAir()) {
                if (++airBelow > MAX_AIR_BELOW_SUPPORT) {
                    return false;
                }
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                return false;
            }
            if (state.isSolidRender((BlockGetter)chunk, pos)) {
                return true;
            }
            return false;
        }
        return airBelow <= MAX_AIR_BELOW_SUPPORT;
    }

    private static boolean isSupportColumnBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return CaveFeatureRestorer.isLogBlock(state) || !CaveFeatureRestorer.isVegetationBlock(state);
    }

    private static boolean hasConnectedFloorBelow(ChunkAccess chunk, int lx, int y, int lz, int minSolidDepth) {
        int floorY = y - 1;
        if (floorY <= chunk.getMinBuildHeight()) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState floor = chunk.getBlockState(pos.set(lx, floorY, lz));
        if (floor.isAir() || !floor.getFluidState().isEmpty() || !floor.isSolidRender((BlockGetter)chunk, pos)) {
            return false;
        }
        return CaveFeatureRestorer.countSolidDepth(chunk, lx, floorY, lz, minSolidDepth) >= minSolidDepth;
    }

    private static int countSolidDepth(ChunkAccess chunk, int lx, int startY, int lz, int minSolidDepth) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int solid = 0;
        for (int y = startY; y >= Math.max(chunk.getMinBuildHeight(), startY - minSolidDepth - 4); --y) {
            BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                break;
            }
            if (state.isSolidRender((BlockGetter)chunk, pos) && ++solid >= minSolidDepth) {
                return solid;
            }
        }
        return solid;
    }

    private static long pack(ChunkAccess chunk, int x, int y, int z) {
        return (long)(y - chunk.getMinBuildHeight()) << 8 | (long)x << 4 | (long)z;
    }

    private static int unpackX(long key) {
        return (int)(key >> 4 & 0xFL);
    }

    private static int unpackY(ChunkAccess chunk, long key) {
        return (int)(key >> 8) + chunk.getMinBuildHeight();
    }

    private static int unpackZ(long key) {
        return (int)(key & 0xFL);
    }
}
