package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Stock TerraForged cave feature pass. Origins must stay underground — near exits
 * unguarded placement punches dripstone/vines/etc. into daylight.
 */
public class NoiseCaveDecorator {
   /** Min blocks below surface before cave features may place. */
   private static final int MIN_SURFACE_DEPTH = 14;

   public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, NoiseCave config) {
      BiomeList biomelist = carver.getBiomes(config);
      if (biomelist != null) {
         int i = chunk.getPos().getMinBlockX();
         int j = chunk.getPos().getMinBlockZ();
         int k = config.getHeight(i, j);
         int originY = resolveUndergroundOrigin(chunk, i, j, k);
         if (originY < chunk.getMinBuildHeight() + 4) {
            return;
         }
         BlockPos blockpos = new BlockPos(i, originY, j);
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));

         for (int l = 0; l < biomelist.size(); l++) {
            Holder<Biome> holder = biomelist.get(l);
            decorate(blockpos, region, generator, ((Biome)holder.value()).getGenerationSettings(), worldgenrandom);
         }
      }
   }

   /**
    * Prefer the configured cave height when deep enough; otherwise search for a deeper
    * air-over-solid floor. Skip the column entirely when nothing stays underground.
    */
   private static int resolveUndergroundOrigin(ChunkAccess chunk, int worldX, int worldZ, int preferredY) {
      int lx = worldX & 15;
      int lz = worldZ & 15;
      int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
      int maxY = Math.min(preferredY, surface - MIN_SURFACE_DEPTH);
      int minY = Math.max(chunk.getMinBuildHeight() + 4, surface - 320);
      if (maxY < minY) {
         return Integer.MIN_VALUE;
      }
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      if (preferredY <= surface - MIN_SURFACE_DEPTH && isFloorAir(chunk, pos, worldX, preferredY, worldZ)) {
         return preferredY;
      }
      for (int y = maxY; y >= minY; y--) {
         if (isFloorAir(chunk, pos, worldX, y, worldZ)) {
            return y;
         }
      }
      return Integer.MIN_VALUE;
   }

   private static boolean isFloorAir(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z) {
      if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) {
         return false;
      }
      return !chunk.getBlockState(pos.set(x, y - 1, z)).isAir();
   }

   public static void decorate(BlockPos pos, WorldGenLevel region, Generator generator, BiomeGenerationSettings settings, WorldgenRandom random) {
      List<HolderSet<PlacedFeature>> list = settings.features();
      long i = random.setDecorationSeed(region.getSeed(), pos.getX(), pos.getZ());

      for (int j = 0; j < list.size(); j++) {
         if (j >= Decoration.LOCAL_MODIFICATIONS.ordinal()) {
            HolderSet<PlacedFeature> holderset = list.get(j);

            for (int k = 0; k < holderset.size(); k++) {
               random.setFeatureSeed(i, k, j);
               PlacedFeature placedfeature = (PlacedFeature)holderset.get(k).value();
               placedfeature.placeWithBiomeCheck(region, generator, random, pos);
            }
         }
      }
   }
}
