package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

public class CarverUtil {
   public static void applyCarvers(long seed, ChunkAccess centerChunk, WorldGenRegion region, BiomeManager biomeManager, Carving step, Generator generator) {
      biomeManager = biomeManager.withDifferentSource(generator);
      ChunkPos chunkpos = centerChunk.getPos();
      WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
      NoiseChunk noisechunk = NoiseChunkUtil.getNoiseChunk(centerChunk, generator);
      CarvingContext carvingcontext = generator.getVanillaGen().createCarvingContext(region, centerChunk, noisechunk);
      Aquifer aquifer = noisechunk.aquifer();
      CarvingMask carvingmask = ((ProtoChunk)centerChunk).getOrCreateCarvingMask(step);

      for (int i = -8; i <= 8; i++) {
         for (int j = -8; j <= 8; j++) {
            ChunkAccess chunkaccess = region.getChunk(chunkpos.x + i, chunkpos.z + j);
            ChunkPos chunkpos1 = chunkaccess.getPos();
            int k = chunkpos1.getMinBlockX();
            int l = chunkpos1.getMinBlockZ();
            Holder<Biome> holder = biomeManager.getNoiseBiomeAtQuart(k >> 2, 0, l >> 2);
            BiomeGenerationSettings biomegenerationsettings = ((Biome)holder.value()).getGenerationSettings();
            List<Holder<ConfiguredWorldCarver<?>>> list = (List<Holder<ConfiguredWorldCarver<?>>>)biomegenerationsettings.getCarvers(step);

            for (int i1 = 0; i1 < list.size(); i1++) {
               ConfiguredWorldCarver<?> configuredworldcarver = (ConfiguredWorldCarver<?>)list.get(i1).value();
               worldgenrandom.setLargeFeatureSeed(seed + i1, chunkpos1.x, chunkpos1.z);
               if (worldgenrandom.nextFloat() < 0.5F && configuredworldcarver.isStartChunk(worldgenrandom)) {
                  configuredworldcarver.carve(carvingcontext, centerChunk, biomeManager::getBiome, worldgenrandom, aquifer, chunkpos1, carvingmask);
               }
            }
         }
      }
   }
}
