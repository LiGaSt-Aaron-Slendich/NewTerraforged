package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class NoiseCaveCarver {
   private static final int CHUNK_AREA = 256;

   public static void carve(ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, boolean carve) {
      MutableBlockPos mutableblockpos = new MutableBlockPos();
      int i = generator.getMinY();
      int j = chunk.getPos().getMinBlockX();
      int k = chunk.getPos().getMinBlockZ();

      for (int l = 0; l < 256; l++) {
         int i1 = l & 15;
         int j1 = l >> 4;
         int k1 = j + i1;
         int l1 = k + j1;
         int i2 = getSurface(k1, l1, chunk, generator, carver);
         int j2 = config.getHeight(k1, l1);
         float f = carver.modifier.getValue(k1, l1);
         int k2 = config.getCavernSize(k1, l1, f);
         if (k2 != 0) {
            int l2 = config.getFloorDepth(k1, l1, k2);
            int i3 = MathUtil.clamp(j2 + k2, i, i2);
            int j3 = MathUtil.clamp(j2 - l2, i, i2);
            if (i3 - j3 >= 2) {
               Holder<Biome> holder = carver.getBiome(k1, l1, config, generator);
               if (carve) {
                  carve(chunk, holder, i1, j1, j3, i3, i2, mutableblockpos);
               }
            }
         }
      }
   }

   private static void carve(ChunkAccess chunk, Holder<Biome> biome, int dx, int dz, int bottom, int top, int surface, MutableBlockPos pos) {
      BlockState blockstate = Blocks.AIR.defaultBlockState();
      int i = dx >> 2;
      int j = dz >> 2;
      int k = surface - 16 >> 2;

      for (int l = bottom; l <= top; l++) {
         pos.set(dx, l, dz);
         if (chunk.getBlockState(pos).getFluidState().isEmpty()) {
            chunk.setBlockState(pos, blockstate, false);
            if (l >> 2 < k) {
               int i1 = (l & 15) >> 2;
               int j1 = chunk.getSectionIndex(l);
               LevelChunkSection levelchunksection = chunk.getSection(j1);
               levelchunksection.getBiomes().getAndSetUnchecked(i, i1, j, biome);
            }
         }
      }
   }

   private static int getSurface(int x, int z, ChunkAccess chunk, Generator generator, CarverChunk carverChunk) {
      float f = carverChunk.getCarvingMask(x, z);
      int i = chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z) - 1;
      if (i > generator.getSeaLevel() || i < generator.getSeaLevel() - 16) {
         i += 9;
      }

      return i - NoiseUtil.floor(16.0F * f);
   }
}
