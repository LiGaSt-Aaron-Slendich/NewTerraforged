package com.terraforged.mod.worldgen.biome.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

public class BufferedBiomeManager extends DelegateBiomeManager {
   private static final ThreadLocal<BufferedBiomeManager> LOCAL_BIOME_MANAGER = ThreadLocal.withInitial(BufferedBiomeManager::new);
   protected int misses;
   protected int requests;
   protected ChunkPos chunkPos;
   protected final Holder<Biome>[] buffer = new Holder[256];
   protected final MutableBlockPos pos = new MutableBlockPos();

   void set(ChunkPos chunkPos, BiomeManager biomeManager) {
      this.misses = 0;
      this.requests = 0;
      this.chunkPos = chunkPos;
      this.setDelegate(biomeManager);
      int i = chunkPos.getMinBlockX();
      int j = chunkPos.getMinBlockZ();

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            this.pos.set(i + l, 64, j + k);
            Holder<Biome> holder = biomeManager.getBiome(this.pos);
            this.buffer[index(l, k)] = holder;
         }
      }
   }

   @Override
   public Holder<Biome> getBiome(BlockPos pos) {
      this.requests++;
      int i = pos.getX() >> 4;
      int j = pos.getZ() >> 4;
      if (i == this.chunkPos.x && j == this.chunkPos.z) {
         int k = pos.getX() - this.chunkPos.getMinBlockX();
         int l = pos.getZ() - this.chunkPos.getMinBlockZ();
         return this.buffer[index(k, l)];
      } else {
         this.misses++;
         return this.delegate.getBiome(pos);
      }
   }

   public void report() {
   }

   private static int index(int dx, int dz) {
      return dz << 4 | dx;
   }

   public static BufferedBiomeManager assign(ChunkPos chunkPos, BiomeManager biomeManager) {
      BufferedBiomeManager bufferedbiomemanager = LOCAL_BIOME_MANAGER.get();
      bufferedbiomemanager.set(chunkPos, biomeManager);
      return bufferedbiomemanager;
   }
}
