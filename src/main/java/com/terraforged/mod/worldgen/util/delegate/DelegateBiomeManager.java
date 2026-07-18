package com.terraforged.mod.worldgen.util.delegate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeManager.NoiseBiomeSource;

public class DelegateBiomeManager extends BiomeManager {
   protected BiomeManager delegate;

   public DelegateBiomeManager() {
      super(null, 0L);
   }

   protected void set(BiomeManager biomeManager) {
      this.delegate = biomeManager;
   }

   public BiomeManager withDifferentSource(NoiseBiomeSource p_186688_) {
      this.delegate = this.delegate.withDifferentSource(p_186688_);
      return this;
   }

   public Holder<Biome> getBiome(BlockPos pos) {
      return this.delegate.getBiome(pos);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(double x, double y, double z) {
      return this.delegate.getNoiseBiomeAtPosition(x, y, z);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(BlockPos pos) {
      return this.delegate.getNoiseBiomeAtPosition(pos);
   }

   public Holder<Biome> getNoiseBiomeAtQuart(int x, int y, int z) {
      return this.delegate.getNoiseBiomeAtQuart(x, y, z);
   }
}
