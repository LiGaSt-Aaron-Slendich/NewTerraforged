package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.util.ChunkBiomePaint;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
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
      int seed = Seeds.get(generator.getSeed());
      CaveType configType = config.getType();
      boolean megaGiga = configType != null && configType.isMegaOrGiga();

      if (!carver.isColumnCacheReady()) {
         carver.prepareColumnCache(seed, chunk, generator);
      }
      CarverColumnCache columns = carver.columnCache();
      Registry<Biome> biomes = generator.getBiomeSource().getRegistry();

      for (int l = 0; l < 256; l++) {
         int i1 = l & 15;
         int j1 = l >> 4;
         int k1 = j + i1;
         int l1 = k + j1;

         if (megaGiga && !columns.matches(configType, i1, j1)) {
            continue;
         }
         if (megaGiga && columns.oceanBlocked(i1, j1)) {
            continue;
         }

         int i2 = getSurface(k1, l1, chunk, generator, carver, columns, i1, j1);
         float f = megaGiga
            ? CaveNoise.sampleMerged(carver.modifier, seed, k1, l1)
            : carver.modifier.getValue(k1, l1);
         int j2 = config.getHeight(k1, l1);
         int k2 = config.getCavernSize(k1, l1, f);
         if (k2 != 0) {
            int l2 = config.getFloorDepth(k1, l1, k2);
            int i3 = MathUtil.clamp(j2 + k2, i, i2);
            int j3 = MathUtil.clamp(j2 - l2, i, i2);
            if (i3 - j3 >= 2) {
               int sampleY = (j3 + i3) >> 1;
               Holder<Biome> holder = ChunkBiomePaint.sanitize(carver.getBiome(k1, l1, sampleY, config, generator), biomes);
               if (carve) {
                  int paintSkip = megaGiga ? 6 : 8;
                  carve(chunk, holder, i1, j1, j3, i3, i2, paintSkip, mutableblockpos, biomes);
               }
            }
         }
      }
   }

   private static void carve(
      ChunkAccess chunk, Holder<Biome> biome, int dx, int dz, int bottom, int top, int surface,
      int surfaceBiomeSkip, MutableBlockPos pos, Registry<Biome> biomes
   ) {
      BlockState blockstate = Blocks.AIR.defaultBlockState();
      int i = dx >> 2;
      int j = dz >> 2;
      int paintCeiling = surface - Math.max(3, surfaceBiomeSkip);

      for (int l = bottom; l <= top; l++) {
         pos.set(dx, l, dz);
         if (chunk.getBlockState(pos).getFluidState().isEmpty()) {
            chunk.setBlockState(pos, blockstate, false);
            // Keep overworld quarts in the surface crust at cave mouths — paint only deeper.
            if (l >= paintCeiling) {
               continue;
            }
            int i1 = (l & 15) >> 2;
            int j1 = chunk.getSectionIndex(l);
            if (j1 >= 0 && j1 < chunk.getSectionsCount()) {
               LevelChunkSection levelchunksection = chunk.getSection(j1);
               ChunkBiomePaint.set(levelchunksection, i, i1, j, biome, biomes);
            }
         }
      }
   }

   private static int getSurface(
      int x, int z, ChunkAccess chunk, Generator generator, CarverChunk carverChunk, CarverColumnCache columns, int dx, int dz
   ) {
      float f = carverChunk.getCarvingMask(x, z);
      int i;
      if (carverChunk.isColumnCacheReady()) {
         i = columns.surfaceY(dx, dz) - 1;
      } else {
         i = chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z) - 1;
      }
      if (i > generator.getSeaLevel() || i < generator.getSeaLevel() - 16) {
         i += 9;
      }

      return i - NoiseUtil.floor(16.0F * f);
   }
}
