package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Per-Y strata fill for river void plugs: samples cover from neighboring columns/chunks,
 * with separate surface vs subsurface palettes.
 */
public final class RiverVoidStrataFill {
    private static final int SAMPLE_RADIUS = 8;
    private static final int ACCENT_MASK = 7;

    private RiverVoidStrataFill() {
    }

    static final class LayerPalette {
        final BlockState primary;
        final BlockState[] accents;

        LayerPalette(BlockState primary, BlockState[] accents) {
            this.primary = primary;
            this.accents = accents;
        }
    }

    public static Map<Integer, LayerPalette> newLayerCache() {
        return new HashMap<>();
    }

    public static BlockState pickStoneFill(BlockGetter level, ChunkAccess chunk, int lx, int y, int lz, int seed,
            Map<Integer, LayerPalette> layerCache) {
        int wx = chunk.getPos().getMinBlockX() + lx;
        int wz = chunk.getPos().getMinBlockZ() + lz;
        LayerPalette palette = layerCache.computeIfAbsent(y,
                ly -> RiverVoidStrataFill.sampleLayer(level, chunk, wx, wz, ly));
        if (palette.accents.length == 0) {
            return palette.primary;
        }
        int h = NoiseUtil.hash2D(seed ^ y * 0x1F711F, lx, lz);
        if ((h & ACCENT_MASK) == 0) {
            return palette.accents[(h >>> 4) % palette.accents.length];
        }
        return palette.primary;
    }

    /** Top block from intact neighbor columns — terracotta, grass, podzol, etc. */
    public static BlockState sampleSurfaceCover(BlockGetter level, ChunkAccess chunk, int lx, int lz, int nearY) {
        Map<Block, Integer> counts = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = chunk.getPos().getMinBlockX() + lx;
        int cz = chunk.getPos().getMinBlockZ() + lz;
        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz += 2) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx += 2) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int nx = lx + dx;
                int nz = lz + dz;
                int ny = RiverVoidStrataFill.neighborSurfaceY(level, chunk, nx, nz, cx + dx, cz + dz, nearY);
                if (Math.abs(ny - nearY) > 6) {
                    continue;
                }
                pos.set(nx >= 0 && nx < 16 && nz >= 0 && nz < 16 ? nx : cx + dx, ny, nz >= 0 && nz < 16 ? nz : cz + dz);
                BlockState state = nx >= 0 && nx < 16 && nz >= 0 && nz < 16 ? chunk.getBlockState(pos) : level.getBlockState(pos);
                if (RiverVoidStrataFill.isSurfaceCoverCandidate(state)) {
                    counts.merge(state.getBlock(), 1, Integer::sum);
                }
            }
        }
        if (counts.isEmpty()) {
            pos.set(lx, nearY, lz);
            BlockState local = chunk.getBlockState(pos);
            if (RiverVoidStrataFill.isSurfaceCoverCandidate(local)) {
                return local;
            }
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey().defaultBlockState();
    }

    /** Block directly under neighbor surface — differs from surface in many biomes. */
    public static BlockState sampleSubsurfaceFill(BlockGetter level, ChunkAccess chunk, int lx, int lz, int nearY) {
        Map<Block, Integer> counts = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = chunk.getPos().getMinBlockX() + lx;
        int cz = chunk.getPos().getMinBlockZ() + lz;
        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz += 2) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx += 2) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int nx = lx + dx;
                int nz = lz + dz;
                int wx = cx + dx;
                int wz = cz + dz;
                int surfaceY = RiverVoidStrataFill.neighborSurfaceY(level, chunk, nx, nz, wx, wz, nearY);
                if (Math.abs(surfaceY - nearY) > 8) {
                    continue;
                }
                pos.set(wx, surfaceY - 1, wz);
                BlockState under;
                if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                    under = chunk.getBlockState(pos.set(nx, surfaceY - 1, nz));
                } else {
                    under = level.getBlockState(pos);
                }
                if (RiverVoidStrataFill.isSubsurfaceFillCandidate(under)) {
                    counts.merge(under.getBlock(), 1, Integer::sum);
                }
            }
        }
        if (counts.isEmpty()) {
            BlockState surface = RiverVoidStrataFill.sampleSurfaceCover(level, chunk, lx, lz, nearY);
            if (RiverVoidStrataFill.isSubsurfaceFillCandidate(surface)) {
                return surface;
            }
            if (surface.is(Blocks.GRASS_BLOCK) || surface.is(Blocks.PODZOL) || surface.is(Blocks.MYCELIUM)) {
                return Blocks.DIRT.defaultBlockState();
            }
            return surface;
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey().defaultBlockState();
    }

    private static int neighborSurfaceY(BlockGetter level, ChunkAccess chunk, int lx, int lz, int wx, int wz, int fallback) {
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
            return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        }
        if (level instanceof WorldGenLevel world) {
            return world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
        }
        return fallback;
    }

    private static LayerPalette sampleLayer(BlockGetter level, ChunkAccess chunk, int centerX, int centerZ, int y) {
        Map<Block, Integer> counts = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; ++dz) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; ++dx) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                pos.set(wx, y, wz);
                BlockState state = wx >= chunkMinX && wx < chunkMaxX && wz >= chunkMinZ && wz < chunkMaxZ
                        ? chunk.getBlockState(pos)
                        : level.getBlockState(pos);
                if (!RiverVoidStrataFill.isStrataCandidate(state)) {
                    continue;
                }
                counts.merge(state.getBlock(), 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            BlockState fallback = RiverVoidStrataFill.fallbackNear(level, centerX, y, centerZ, pos);
            return new LayerPalette(fallback, new BlockState[0]);
        }
        List<Map.Entry<Block, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort(Comparator.comparingInt(Map.Entry<Block, Integer>::getValue).reversed());
        BlockState primary = ranked.get(0).getKey().defaultBlockState();
        int accentCount = Math.min(2, ranked.size() - 1);
        BlockState[] accents = new BlockState[accentCount];
        for (int i = 0; i < accentCount; ++i) {
            accents[i] = ranked.get(i + 1).getKey().defaultBlockState();
        }
        return new LayerPalette(primary, accents);
    }

    private static BlockState fallbackNear(BlockGetter level, int centerX, int y, int centerZ, BlockPos.MutableBlockPos pos) {
        pos.set(centerX, y, centerZ);
        BlockState local = level.getBlockState(pos);
        if (RiverVoidStrataFill.isStrataCandidate(local)) {
            return local.getBlock().defaultBlockState();
        }
        for (int dy = 1; dy <= 4; ++dy) {
            pos.set(centerX, y - dy, centerZ);
            BlockState below = level.getBlockState(pos);
            if (RiverVoidStrataFill.isStrataCandidate(below)) {
                return below.getBlock().defaultBlockState();
            }
            pos.set(centerX, y + dy, centerZ);
            BlockState above = level.getBlockState(pos);
            if (RiverVoidStrataFill.isStrataCandidate(above)) {
                return above.getBlock().defaultBlockState();
            }
        }
        return Blocks.STONE.defaultBlockState();
    }

    static boolean isSurfaceCoverCandidate(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
            return false;
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || RiverVoidStrataFill.isTerracotta(state)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE);
    }

    static boolean isSubsurfaceFillCandidate(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
            return false;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            return false;
        }
        return RiverVoidStrataFill.isStrataCandidate(state) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.CLAY) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || RiverVoidStrataFill.isTerracotta(state);
    }

    /** Strata / deep fill — stone, terracotta bands, sandstone, etc. */
    static boolean isStrataCandidate(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CLIMBABLE) || state.is(BlockTags.RAILS) || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.BANNERS) || state.is(BlockTags.CANDLES)
                || state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.SIGNS)) {
            return false;
        }
        if (state.is(Blocks.LADDER) || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SCAFFOLDING) || state.is(Blocks.CHAIN) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.CLAY)
                || RiverVoidStrataFill.isTerracotta(state);
    }

    private static boolean isTerracotta(BlockState state) {
        return state.is(Blocks.TERRACOTTA) || state.is(Blocks.WHITE_TERRACOTTA) || state.is(Blocks.ORANGE_TERRACOTTA)
                || state.is(Blocks.YELLOW_TERRACOTTA) || state.is(Blocks.BROWN_TERRACOTTA) || state.is(Blocks.RED_TERRACOTTA)
                || state.is(Blocks.LIGHT_GRAY_TERRACOTTA) || state.is(Blocks.GRAY_TERRACOTTA)
                || state.is(Blocks.BLACK_TERRACOTTA) || state.is(Blocks.BLUE_TERRACOTTA)
                || state.is(Blocks.GREEN_TERRACOTTA) || state.is(Blocks.CYAN_TERRACOTTA)
                || state.is(Blocks.LIGHT_BLUE_TERRACOTTA) || state.is(Blocks.LIME_TERRACOTTA)
                || state.is(Blocks.MAGENTA_TERRACOTTA) || state.is(Blocks.PINK_TERRACOTTA)
                || state.is(Blocks.PURPLE_TERRACOTTA);
    }
}
