package com.terraforged.mod.worldgen.biome.surface;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class Surface {
   protected static final TagKey<Block> ERODIBLE = BlockTags.DIRT;

   public static void apply(TerrainData terrainData, ChunkAccess chunk, ChunkGenerator generator) {
      float f = 55.0F * (generator.getGenDepth() / 255.0F);
      MutableBlockPos mutableblockpos = new MutableBlockPos();

      for (int i = 0; i < 16; i++) {
         for (int j = 0; j < 16; j++) {
            int k = chunk.getHeight(Types.OCEAN_FLOOR_WG, j, i);
            float f1 = terrainData.getGradient(j, i, f);
            if (k >= generator.getSeaLevel() && !(f1 < 0.6F)) {
               BlockState blockstate = findSolid(mutableblockpos.set(j, k, i), chunk);
               if (blockstate != null) {
                  for (int l = mutableblockpos.getY(); k > l; k--) {
                     chunk.setBlockState(mutableblockpos.setY(k), blockstate, false);
                  }
               }
            }
         }
      }
   }

   public static void applyPost(ChunkAccess chunk, TerrainData terrainData, ChunkGenerator generator) {
      float f = 70.0F * (generator.getGenDepth() / 255.0F);
      MutableBlockPos mutableblockpos = new MutableBlockPos();

      for (int i = 0; i < 16; i++) {
         for (int j = 0; j < 16; j++) {
            int k = chunk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, j, i) + 1;
            mutableblockpos.set(j, k, i);
            BlockState blockstate = chunk.getBlockState(mutableblockpos);
            float f1 = terrainData.getGradient(j, i, f);
            if (f1 < 0.625F) {
               if (blockstate.getBlock() instanceof SnowLayerBlock) {
                  smoothSnow(mutableblockpos, blockstate, chunk, terrainData);
               }
            } else {
               if (blockstate.isAir()) {
                  blockstate = chunk.getBlockState(mutableblockpos.setY(k - 1));
               }

               if (blockstate.is(BlockTags.SNOW)) {
                  erodeSnow(mutableblockpos, chunk);
               }
            }
         }
      }
   }

   public static void smoothWater(ChunkAccess chunk, WorldGenLevel region, TerrainData terrainData) {
      MutableBlockPos mutableblockpos = new MutableBlockPos();
      int i = chunk.getPos().getMinBlockX();
      int j = chunk.getPos().getMinBlockZ();
      BlockState blockstate = (BlockState)Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 2);

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            if (isSmoothable(l, k, terrainData)) {
               int i1 = i + l;
               int j1 = j + k;
               int k1 = chunk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, l, k);
               BlockState blockstate1 = chunk.getBlockState(mutableblockpos.set(i1, k1, j1));
               if (blockstate1.is(Blocks.WATER)
                  && (Integer)blockstate1.getValue(LiquidBlock.LEVEL) == 0
                  && shouldSmooth(i1, k1, j1, chunk, region, mutableblockpos)) {
                  chunk.setBlockState(mutableblockpos.set(i1, k1, j1), blockstate, false);
               }
            }
         }
      }
   }

   protected static boolean shouldSmooth(int x, int y, int z, ChunkAccess chunk, WorldGenLevel region, MutableBlockPos pos) {
      int i = 6;
      int j = i * i;

      for (int k = -i; k <= i; k++) {
         for (int l = -i; l <= i; l++) {
            int i1 = l * l + k * k;
            if (i1 != 0 && i1 <= j) {
               pos.set(x + l, y, z + k);
               BlockGetter blockgetter = (BlockGetter)(sameChunk(pos, chunk.getPos()) ? chunk : region);
               BlockState blockstate = blockgetter.getBlockState(pos);
               if (blockstate.isAir()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   protected static boolean isSmoothable(int x, int z, TerrainData terrainData) {
      float f = terrainData.getRiver().get(x, z);
      Terrain terrain = terrainData.getTerrain().get(x, z);
      return (terrain.isRiver() || terrain.isLake()) && f == 0.0F;
   }

   protected static void smoothSnow(MutableBlockPos pos, BlockState state, ChunkAccess chunk, TerrainData terrain) {
      float f = terrain.getHeight().get(pos.getX(), pos.getZ());
      float f1 = f - terrain.getLevels().getHeight(f);
      int i = 1 + NoiseUtil.floor(f1 * 7.9999F);
      state = (BlockState)state.setValue(SnowLayerBlock.LAYERS, i);
      chunk.setBlockState(pos, state, false);
   }

   protected static void erodeSnow(MutableBlockPos pos, ChunkAccess chunk) {
      chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
      int i = pos.getY() - 1;
      int j = Math.max(pos.getY() - 15, 0);

      for (int k = i; k > j; k--) {
         pos.setY(k);
         BlockState blockstate = chunk.getBlockState(pos);
         if (!isErodible(blockstate)) {
            return;
         }

         chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
      }
   }

   public static boolean isErodible(BlockState state) {
      return state.is(ERODIBLE) || state.is(BlockTags.SNOW);
   }

   protected static boolean sameChunk(BlockPos pos, ChunkPos chunk) {
      return pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
   }

   protected static BlockState findSolid(MutableBlockPos pos, ChunkAccess chunk) {
      BlockState blockstate = chunk.getBlockState(pos);
      if (!isErodible(blockstate)) {
         return null;
      } else {
         int i = pos.getY() - 1;

         for (int j = Math.max(0, pos.getY() - 20); i > j; i--) {
            blockstate = chunk.getBlockState(pos.setY(i));
            if (!isErodible(blockstate)) {
               return blockstate;
            }
         }

         return null;
      }
   }
}
