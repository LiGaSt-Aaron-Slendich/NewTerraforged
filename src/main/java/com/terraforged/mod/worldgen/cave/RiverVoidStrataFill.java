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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Per-Y strata fill for river void plugs: samples cover stone from neighboring columns/chunks,
 * uses the dominant block per layer, and sprinkles secondary types via deterministic noise.
 */
public final class RiverVoidStrataFill {
    private static final int SAMPLE_RADIUS = 24;
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
        LayerPalette palette = layerCache.computeIfAbsent(y, ly -> RiverVoidStrataFill.sampleLayer(level, wx, wz, ly));
        if (palette.accents.length == 0) {
            return palette.primary;
        }
        int h = NoiseUtil.hash2D(seed ^ y * 0x1F711F, lx, lz);
        if ((h & ACCENT_MASK) == 0) {
            return palette.accents[(h >>> 4) % palette.accents.length];
        }
        return palette.primary;
    }

    private static LayerPalette sampleLayer(BlockGetter level, int centerX, int centerZ, int y) {
        Map<Block, Integer> counts = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; ++dz) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; ++dx) {
                pos.set(centerX + dx, y, centerZ + dz);
                BlockState state = level.getBlockState(pos);
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

    /** Natural stone / gravel cover only — no ladders, rails, vegetation, or other specials. */
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
                || state.is(Blocks.SMOOTH_SANDSTONE);
    }
}
