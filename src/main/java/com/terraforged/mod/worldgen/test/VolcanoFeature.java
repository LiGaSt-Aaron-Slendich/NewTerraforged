package com.terraforged.mod.worldgen.test;

import com.terraforged.mod.worldgen.test.Volcano;
import com.terraforged.mod.worldgen.test.VolcanoConfig;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class VolcanoFeature
extends Feature<VolcanoConfig> {
    protected final ThreadLocal<Volcano.Cache> localCache = ThreadLocal.withInitial(Volcano.Cache::new);

    public VolcanoFeature() {
        super(VolcanoConfig.CODEC);
    }

    public boolean place(FeaturePlaceContext<VolcanoConfig> context) {
        return true;
    }

    private static void fillColumn(int x, int z, int height, int surface, Volcano.Value value, VolcanoConfig config, ChunkAccess chunk, BlockState filler) {
        BlockState lava = VolcanoFeature.getFluid(value.hash);
        int fluidLevel = VolcanoFeature.getFluidLevel(value.hash, config);
        for (int y = height; y > surface; --y) {
            int index = chunk.getSectionIndex(y);
            LevelChunkSection section = chunk.getSection(index);
            BlockState block = filler.isAir() && y <= fluidLevel ? lava : filler;
            section.setBlockState(x, y & 0xF, z, block, false);
        }
    }

    private static int getFluidLevel(long hash, VolcanoConfig config) {
        double height = config.fluidLevel().get(Volcano.Noise.rand(hash, 33199));
        return Volcano.toHeightValue(height);
    }

    private static BlockState getFluid(long hash) {
        double noise = Volcano.Noise.rand(hash, 39761);
        return noise < 0.5 ? Blocks.WATER.defaultBlockState() : Blocks.LAVA.defaultBlockState();
    }

    private static boolean test(int x, int z, FeaturePlaceContext<VolcanoConfig> context) {
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, (LevelHeightAccessor)context.level());
        return y < 180;
    }
}
