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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class NoiseCaveDecorator {
   public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, NoiseCave config) {
      BiomeList biomelist = carver.getBiomes(config);
      if (biomelist != null) {
         int i = chunk.getPos().getMinBlockX();
         int j = chunk.getPos().getMinBlockZ();
         int k = config.getHeight(i, j);
         BlockPos blockpos = new BlockPos(i, k, j);
         WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));

         for (int l = 0; l < biomelist.size(); l++) {
            Holder<Biome> holder = biomelist.get(l);
            decorate(blockpos, region, generator, ((Biome)holder.value()).getGenerationSettings(), worldgenrandom);
         }
      }
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
