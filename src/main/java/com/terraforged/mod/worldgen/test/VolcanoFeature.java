package com.terraforged.mod.worldgen.test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class VolcanoFeature extends Feature<VolcanoConfig> {
   protected final ThreadLocal<Volcano.Cache> localCache = ThreadLocal.withInitial(Volcano.Cache::new);

   public VolcanoFeature() {
      super(VolcanoConfig.CODEC);
   }

   public boolean place(FeaturePlaceContext<VolcanoConfig> context) {
      return true;
   }

   private static void fillColumn(int x, int z, int height, int surface, Volcano.Value value, VolcanoConfig config, ChunkAccess chunk, BlockState filler) {
      BlockState blockstate = getFluid(value.hash);
      int i = getFluidLevel(value.hash, config);

      for (int j = height; j > surface; j--) {
         int k = chunk.getSectionIndex(j);
         LevelChunkSection levelchunksection = chunk.getSection(k);
         BlockState blockstate1 = filler.isAir() && j <= i ? blockstate : filler;
         levelchunksection.setBlockState(x, j & 15, z, blockstate1, false);
      }
   }

   private static int getFluidLevel(long hash, VolcanoConfig config) {
      double d0 = config.fluidLevel().get(Volcano.Noise.rand(hash, 33199));
      return Volcano.toHeightValue(d0);
   }

   private static BlockState getFluid(long hash) {
      double d0 = Volcano.Noise.rand(hash, 39761);
      return d0 < 0.5 ? Blocks.WATER.defaultBlockState() : Blocks.LAVA.defaultBlockState();
   }

   private static boolean test(int x, int z, FeaturePlaceContext<VolcanoConfig> context) {
      int i = context.chunkGenerator().getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, context.level());
      return i < 180;
   }
}
