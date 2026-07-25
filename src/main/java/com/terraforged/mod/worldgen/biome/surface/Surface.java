package com.terraforged.mod.worldgen.biome.surface;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.biome.HeightClimateZones;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import javax.annotation.Nullable;
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
                  // Cap fill depth so cave mouths aren't densified into 1×1 pillars.
                  int solidY = mutableblockpos.getY();
                  int fillFloor = Math.max(solidY, k - 10);
                  int wx = chunk.getPos().getMinBlockX() + j;
                  int wz = chunk.getPos().getMinBlockZ() + i;
                  for (; k > fillFloor; k--) {
                     chunk.setBlockState(mutableblockpos.setY(k), cliffMix(blockstate, wx, k, wz), false);
                  }
               }
            }
         }
      }
      // Alpine stone-cap / treeline strip runs after features (BiomeGenerator.decorate).
   }

   /**
    * High peaks: strip trees leftovers already handled in decor; stone-cap removes dirt/grass
    * in the top ~13% of world height. Treeline vegetation skip is in PositionSampler.
    */
   public static void applyAlpineZones(TerrainData terrainData, ChunkAccess chunk, ChunkGenerator generator) {
      if (terrainData == null || chunk == null) {
         return;
      }
      int maxY = terrainData.getLevels().maxY;
      MutableBlockPos pos = new MutableBlockPos();
      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            float heightNoise = HeightClimateZones.noiseFromScaled(terrainData.getHeight().get(lx, lz), maxY);
            if (!HeightClimateZones.isStoneCap(heightNoise) && !HeightClimateZones.isAboveTreeline(heightNoise)) {
               continue;
            }
            int surface = chunk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
            int floor = chunk.getHeight(Types.OCEAN_FLOOR_WG, lx, lz);
            if (HeightClimateZones.isAboveTreeline(heightNoise)) {
               stripVegetationColumn(chunk, pos, lx, lz, floor, surface);
            }
            if (HeightClimateZones.isStoneCap(heightNoise) && floor >= generator.getSeaLevel()) {
               stripDirtToStone(chunk, pos, lx, lz, floor);
            }
         }
      }
   }

   private static void stripVegetationColumn(ChunkAccess chunk, MutableBlockPos pos, int lx, int lz, int floor, int surface) {
      int top = Math.max(surface, floor + 1);
      int bottom = Math.max(chunk.getMinBuildHeight(), floor);
      for (int y = top; y >= bottom; y--) {
         BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
         if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.VINE)
               || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)
               || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.CACTUS)
               || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.DEAD_BUSH)
               || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
               || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
               || state.is(Blocks.DANDELION) || state.is(Blocks.POPPY)
               || state.is(Blocks.AZALEA) || state.is(Blocks.FLOWERING_AZALEA)
               || state.is(Blocks.OAK_SAPLING) || state.is(Blocks.BIRCH_SAPLING)
               || state.is(Blocks.SPRUCE_SAPLING) || state.is(Blocks.JUNGLE_SAPLING)
               || state.is(Blocks.ACACIA_SAPLING) || state.is(Blocks.DARK_OAK_SAPLING)) {
            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
         }
      }
   }

   private static void stripDirtToStone(ChunkAccess chunk, MutableBlockPos pos, int lx, int lz, int floorY) {
      BlockState stone = findLocalStone(chunk, pos, lx, lz, floorY);
      int minY = Math.max(chunk.getMinBuildHeight(), floorY - 8);
      for (int y = floorY; y >= minY; y--) {
         BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
         if (state.isAir() || !state.getFluidState().isEmpty()) {
            continue;
         }
         if (isErodible(state) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
               || state.is(Blocks.MYCELIUM) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)
               || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) {
            chunk.setBlockState(pos, stone, false);
         } else if (!isErodible(state)) {
            // Hit real stone/deepslate — stop.
            break;
         }
      }
   }

   private static BlockState findLocalStone(ChunkAccess chunk, MutableBlockPos pos, int lx, int lz, int floorY) {
      int minY = Math.max(chunk.getMinBuildHeight(), floorY - 24);
      for (int y = floorY; y >= minY; y--) {
         BlockState state = chunk.getBlockState(pos.set(lx, y, lz));
         if (state.isAir() || isErodible(state) || state.is(BlockTags.SNOW) || !state.getFluidState().isEmpty()) {
            continue;
         }
         if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
               || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
               || state.is(Blocks.TUFF) || state.is(Blocks.CALCITE)
               || state.is(Blocks.COBBLESTONE) || state.is(Blocks.BLACKSTONE)) {
            return state;
         }
         // First non-dirt solid under the crust — "any stone" per alpine rule.
         return state;
      }
      return Blocks.STONE.defaultBlockState();
   }

   /**
    * When sea level floods land biomes, vanilla surface rules often leave grass/podzol/mycelium
    * on the solid floor under water. Replace those with dirt/gravel/sand by depth.
    */
   public static void fixUnderwaterSurface(ChunkAccess chunk, ChunkGenerator generator) {
      int sea = generator.getSeaLevel();
      MutableBlockPos pos = new MutableBlockPos();

      for (int dz = 0; dz < 16; dz++) {
         for (int dx = 0; dx < 16; dx++) {
            int floor = chunk.getHeight(Types.OCEAN_FLOOR_WG, dx, dz);
            if (floor >= sea || floor < chunk.getMinBuildHeight()) {
               continue;
            }
            BlockState fluid = chunk.getBlockState(pos.set(dx, floor + 1, dz));
            if (fluid.getFluidState().isEmpty()) {
               // No water directly above — may still be a dry ledge under a flooded overhang.
               boolean watered = false;
               int maxY = Math.min(sea, floor + 8);
               for (int y = floor + 1; y <= maxY; y++) {
                  if (!chunk.getBlockState(pos.set(dx, y, dz)).getFluidState().isEmpty()) {
                     watered = true;
                     break;
                  }
               }
               if (!watered) {
                  continue;
               }
            }

            BlockState top = chunk.getBlockState(pos.set(dx, floor, dz));
            BlockState replacement = underwaterReplacement(top, sea - floor);
            if (replacement != null) {
               chunk.setBlockState(pos, replacement, false);
            }
         }
      }
   }

   @Nullable
   protected static BlockState underwaterReplacement(BlockState top, int depthBelowSea) {
      if (top.is(Blocks.GRASS_BLOCK)
         || top.is(Blocks.PODZOL)
         || top.is(Blocks.MYCELIUM)
         || top.is(Blocks.DIRT_PATH)
         || top.is(Blocks.FARMLAND)
         || top.is(Blocks.SNOW_BLOCK)
         || top.is(Blocks.SNOW)) {
         if (depthBelowSea >= 8) {
            return Blocks.GRAVEL.defaultBlockState();
         }
         if (depthBelowSea >= 3) {
            return Blocks.DIRT.defaultBlockState();
         }
         return Blocks.SAND.defaultBlockState();
      }
      // Coarse dirt / rooted dirt also look wrong as an exposed seafloor.
      if (top.is(Blocks.COARSE_DIRT) || top.is(Blocks.ROOTED_DIRT)) {
         return depthBelowSea >= 5 ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT.defaultBlockState();
      }
      return null;
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

   /**
    * Break up monolithic cliff columns: gravel + grey stone suite (andesite-heavy)
    * via cheap per-block hash noise.
    */
   protected static BlockState cliffMix(BlockState base, int worldX, int y, int worldZ) {
      if (base == null || base.isAir() || base.getBlock() instanceof LiquidBlock) {
         return base;
      }
      // Keep unusual solids (basalt, terracotta…) mostly intact.
      Block block = base.getBlock();
      boolean commonStone = block == Blocks.STONE
            || block == Blocks.COBBLESTONE
            || block == Blocks.MOSSY_COBBLESTONE
            || block == Blocks.ANDESITE
            || block == Blocks.DIORITE
            || block == Blocks.GRANITE
            || block == Blocks.DEEPSLATE
            || block == Blocks.TUFF;
      if (!commonStone && !base.is(BlockTags.BASE_STONE_OVERWORLD)) {
         return base;
      }
      int h = worldX * 374761393 + y * 668265263 + worldZ * 1274126177;
      h = (h ^ (h >>> 13)) * 1274126177;
      h ^= h >>> 16;
      int roll = h & 255;
      // ~70% replaced — andesite is the main stone accent (grey palette with gravel).
      if (roll < 22) {
         return Blocks.GRAVEL.defaultBlockState();
      }
      if (roll < 40) {
         return Blocks.COBBLESTONE.defaultBlockState();
      }
      if (roll < 48) {
         return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
      }
      if (roll < 100) {
         return Blocks.ANDESITE.defaultBlockState();
      }
      if (roll < 132) {
         return Blocks.DIORITE.defaultBlockState();
      }
      if (roll < 160) {
         return Blocks.GRANITE.defaultBlockState();
      }
      if (roll < 178) {
         return Blocks.TUFF.defaultBlockState();
      }
      if (roll < 190 && y < 0) {
         return Blocks.DEEPSLATE.defaultBlockState();
      }
      return base;
   }
}
