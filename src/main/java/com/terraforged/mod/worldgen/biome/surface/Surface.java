package com.terraforged.mod.worldgen.biome.surface;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.biome.HeightClimateZones;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
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
                  Holder<Biome> biome = chunk.getNoiseBiome(wx >> 2, k >> 2, wz >> 2);
                  for (; k > fillFloor; k--) {
                     chunk.setBlockState(mutableblockpos.setY(k), cliffMix(blockstate, wx, k, wz, biome), false);
                  }
               }
            }
         }
      }
      // Alpine stone-cap / treeline strip runs after features (BiomeGenerator.decorate).
   }

   /**
    * High peaks: stone-cap removes dirt/grass in the top ~13% of world height.
    * Treeline vegetation skip is in PositionSampler only — do <b>not</b> strip
    * logs/leaves here (that left Dynamic Trees canopies floating without trunks).
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
            if (!HeightClimateZones.isStoneCap(heightNoise)) {
               continue;
            }
            int floor = chunk.getHeight(Types.OCEAN_FLOOR_WG, lx, lz);
            if (floor >= generator.getSeaLevel()) {
               stripDirtToStone(chunk, pos, lx, lz, floor);
            }
         }
      }
   }

   // Vegetation strip removed — see applyAlpineZones javadoc.

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
    * Break up monolithic cliff columns with a <b>biome</b>-matched palette
    * (grey mountains, orange badlands biomes, dark volcanic, sandy beaches).
    * Terrain type alone was painting mesa terracotta into taiga mountain walls.
    */
   protected static BlockState cliffMix(BlockState base, int worldX, int y, int worldZ, @Nullable Holder<Biome> biome) {
      if (base == null || base.isAir() || base.getBlock() instanceof LiquidBlock) {
         return base;
      }
      CliffPalette palette = cliffPalette(biome);
      if (!isCliffMixable(base, palette)) {
         return base;
      }
      int h = worldX * 374761393 + y * 668265263 + worldZ * 1274126177;
      h = (h ^ (h >>> 13)) * 1274126177;
      h ^= h >>> 16;
      int roll = h & 255;
      return switch (palette) {
         case GREY -> mixGrey(base, roll, y);
         case ORANGE -> mixOrange(base, roll);
         case VOLCANIC -> mixVolcanic(base, roll);
         case SAND -> mixSand(base, roll);
      };
   }

   private enum CliffPalette {
      GREY,
      ORANGE,
      VOLCANIC,
      SAND
   }

   private static CliffPalette cliffPalette(@Nullable Holder<Biome> biome) {
      if (biome == null) {
         return CliffPalette.GREY;
      }
      if (biome.is(BiomeTags.IS_BADLANDS)) {
         return CliffPalette.ORANGE;
      }
      if (biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN)) {
         return CliffPalette.SAND;
      }
      ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
      String path = id != null ? id.getPath() : "";
      if (path.contains("volcan") || path.contains("basalt") || path.contains("ash")) {
         return CliffPalette.VOLCANIC;
      }
      if (path.contains("badland") || path.contains("mesa") || path.contains("outback")
            || path.contains("terracotta") || path.contains("canyon")) {
         return CliffPalette.ORANGE;
      }
      if (path.contains("beach") || path.contains("dune") || path.contains("shore")) {
         return CliffPalette.SAND;
      }
      return CliffPalette.GREY;
   }

   private static boolean isCliffMixable(BlockState base, CliffPalette palette) {
      Block block = base.getBlock();
      if (palette == CliffPalette.ORANGE) {
         return block == Blocks.STONE
               || block == Blocks.TERRACOTTA
               || block == Blocks.ORANGE_TERRACOTTA
               || block == Blocks.RED_TERRACOTTA
               || block == Blocks.YELLOW_TERRACOTTA
               || block == Blocks.BROWN_TERRACOTTA
               || block == Blocks.WHITE_TERRACOTTA
               || block == Blocks.LIGHT_GRAY_TERRACOTTA
               || block == Blocks.RED_SANDSTONE
               || block == Blocks.SMOOTH_RED_SANDSTONE
               || block == Blocks.SANDSTONE
               || base.is(BlockTags.BASE_STONE_OVERWORLD)
               || base.is(BlockTags.TERRACOTTA);
      }
      if (palette == CliffPalette.VOLCANIC) {
         return block == Blocks.STONE
               || block == Blocks.BASALT
               || block == Blocks.BLACKSTONE
               || block == Blocks.ANDESITE
               || block == Blocks.MAGMA_BLOCK
               || block == Blocks.GRAVEL
               || base.is(BlockTags.BASE_STONE_OVERWORLD);
      }
      if (palette == CliffPalette.SAND) {
         return block == Blocks.STONE
               || block == Blocks.SANDSTONE
               || block == Blocks.SMOOTH_SANDSTONE
               || block == Blocks.SAND
               || block == Blocks.GRAVEL
               || base.is(BlockTags.BASE_STONE_OVERWORLD)
               || base.is(BlockTags.SAND);
      }
      // GREY — mountains / hills / torridonian / default
      return block == Blocks.STONE
            || block == Blocks.COBBLESTONE
            || block == Blocks.MOSSY_COBBLESTONE
            || block == Blocks.ANDESITE
            || block == Blocks.TUFF
            || block == Blocks.DEEPSLATE
            || block == Blocks.GRAVEL
            || base.is(BlockTags.BASE_STONE_OVERWORLD);
   }

   /** Grey mountain palette — no pink granite / white diorite. */
   private static BlockState mixGrey(BlockState base, int roll, int y) {
      if (roll < 24) {
         return Blocks.GRAVEL.defaultBlockState();
      }
      if (roll < 48) {
         return Blocks.COBBLESTONE.defaultBlockState();
      }
      if (roll < 56) {
         return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
      }
      if (roll < 120) {
         return Blocks.ANDESITE.defaultBlockState();
      }
      if (roll < 150) {
         return Blocks.TUFF.defaultBlockState();
      }
      if (roll < 168 && y < 0) {
         return Blocks.DEEPSLATE.defaultBlockState();
      }
      if (roll < 190) {
         return Blocks.STONE.defaultBlockState();
      }
      return base;
   }

   /** Badlands / mesa — warm terracotta + red sandstone. */
   private static BlockState mixOrange(BlockState base, int roll) {
      if (roll < 18) {
         return Blocks.RED_SAND.defaultBlockState();
      }
      if (roll < 40) {
         return Blocks.RED_SANDSTONE.defaultBlockState();
      }
      if (roll < 56) {
         return Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
      }
      if (roll < 100) {
         return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      }
      if (roll < 130) {
         return Blocks.RED_TERRACOTTA.defaultBlockState();
      }
      if (roll < 155) {
         return Blocks.YELLOW_TERRACOTTA.defaultBlockState();
      }
      if (roll < 175) {
         return Blocks.BROWN_TERRACOTTA.defaultBlockState();
      }
      if (roll < 190) {
         return Blocks.TERRACOTTA.defaultBlockState();
      }
      return base;
   }

   /** Volcano cones — dark basalt / blackstone. */
   private static BlockState mixVolcanic(BlockState base, int roll) {
      if (roll < 20) {
         return Blocks.GRAVEL.defaultBlockState();
      }
      if (roll < 70) {
         return Blocks.BASALT.defaultBlockState();
      }
      if (roll < 110) {
         return Blocks.BLACKSTONE.defaultBlockState();
      }
      if (roll < 145) {
         return Blocks.ANDESITE.defaultBlockState();
      }
      if (roll < 160) {
         return Blocks.MAGMA_BLOCK.defaultBlockState();
      }
      if (roll < 185) {
         return Blocks.SMOOTH_BASALT.defaultBlockState();
      }
      return base;
   }

   /** Beach / sandy cliffs. */
   private static BlockState mixSand(BlockState base, int roll) {
      if (roll < 30) {
         return Blocks.SAND.defaultBlockState();
      }
      if (roll < 70) {
         return Blocks.SANDSTONE.defaultBlockState();
      }
      if (roll < 100) {
         return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
      }
      if (roll < 130) {
         return Blocks.GRAVEL.defaultBlockState();
      }
      if (roll < 160) {
         return Blocks.STONE.defaultBlockState();
      }
      return base;
   }
}
