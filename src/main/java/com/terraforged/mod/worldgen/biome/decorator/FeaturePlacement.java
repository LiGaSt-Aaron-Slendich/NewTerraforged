package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class FeaturePlacement {
    private static final Set<PlacementModifierType<?>> STRIP_BIOME_FILTER = Set.of(PlacementModifierType.BIOME_FILTER);

    private FeaturePlacement() {
    }

    public static boolean hasStableGround(BlockGetter level, BlockPos pos, int minSolidDepth) {
        return FeaturePlacement.hasStableGround(level, pos.getX(), pos.getY(), pos.getZ(), minSolidDepth);
    }

    public static boolean hasStableGround(BlockGetter level, int x, int y, int z, int minSolidDepth) {
        if (y <= level.getMinBuildHeight()) {
            return false;
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int solid = 0;
        for (int dy = 1; dy <= minSolidDepth + 2; ++dy) {
            probe.set(x, y - dy, z);
            BlockState state = level.getBlockState((BlockPos)probe);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                solid = 0;
                continue;
            }
            if (!state.isSolidRender(level, (BlockPos)probe) || ++solid < minSolidDepth) continue;
            return true;
        }
        return false;
    }

    public static boolean hasStableCeiling(BlockGetter level, BlockPos pos, int minSolidDepth) {
        return FeaturePlacement.hasStableCeiling(level, pos.getX(), pos.getY(), pos.getZ(), minSolidDepth);
    }

    public static boolean hasStableCeiling(BlockGetter level, int x, int y, int z, int minSolidDepth) {
        if (y >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int solid = 0;
        for (int dy = 1; dy <= minSolidDepth + 2; ++dy) {
            probe.set(x, y + dy, z);
            BlockState state = level.getBlockState((BlockPos)probe);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                solid = 0;
                continue;
            }
            if (!state.isSolidRender(level, (BlockPos)probe) || ++solid < minSolidDepth) continue;
            return true;
        }
        return false;
    }

    public static boolean place(PlacedFeature placed, WorldGenLevel level, ChunkGenerator generator, Random random, BlockPos pos, boolean modBiome) {
        if (!modBiome) {
            try {
                return placed.placeWithBiomeCheck(level, generator, random, pos);
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        return FeaturePlacement.place((Holder<PlacedFeature>)Holder.direct(placed), level, generator, random, pos, true);
    }

    public static boolean place(Holder<PlacedFeature> holder, WorldGenLevel level, ChunkGenerator generator, Random random, BlockPos pos, boolean modBiome) {
        if (!modBiome) {
            try {
                return ((PlacedFeature)holder.value()).placeWithBiomeCheck(level, generator, random, pos);
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        try {
            if (((PlacedFeature)holder.value()).placeWithBiomeCheck(level, generator, random, pos)) {
                return true;
            }
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        PlacedFeature placed = VegetationFeatures.unwrap(holder, STRIP_BIOME_FILTER, true);
        return FeaturePlacement.placeUnwrapped(placed, level, generator, random, pos);
    }

    private static boolean placeUnwrapped(PlacedFeature placed, WorldGenLevel level, ChunkGenerator generator, Random random, BlockPos pos) {
        try {
            if (placed.place(level, generator, random, pos)) {
                return true;
            }
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        Holder configured = placed.feature();
        if (configured == null || configured.value() == null) {
            return false;
        }
        try {
            return ((ConfiguredFeature)configured.value()).place(level, generator, random, pos);
        }
        catch (RuntimeException ignored) {
            return false;
        }
    }
}
