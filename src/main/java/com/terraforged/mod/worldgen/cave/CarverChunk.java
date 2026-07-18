package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.Module;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class CarverChunk {
   private Holder<Biome> cached;
   private int cachedX;
   private int cachedZ;
   private int biomeListIndex = -1;
   private final BiomeList[] biomeLists;
   private final Map<NoiseCave, BiomeList> biomes = new IdentityHashMap<>();
   public Module mask;
   public Module modifier;
   public TerrainData terrainData;

   public CarverChunk(int size) {
      this.biomeLists = new BiomeList[size];

      for (int i = 0; i < this.biomeLists.length; i++) {
         this.biomeLists[i] = new BiomeList();
      }
   }

   public CarverChunk reset() {
      this.cached = null;
      this.biomes.clear();
      this.biomeListIndex = -1;
      return this;
   }

   public BiomeList getBiomes(NoiseCave config) {
      return this.biomes.get(config);
   }

   public Holder<Biome> getBiome(int x, int z, NoiseCave config, Generator generator) {
      int i = x >> 2;
      int j = z >> 2;
      if (this.cached == null || i != this.cachedX || j != this.cachedZ) {
         this.cached = generator.getBiomeSource().getUnderGroundBiome(config.getSeed(), x, z, config.getType());
         this.cachedX = i;
         this.cachedZ = j;
         this.biomes.computeIfAbsent(config, c -> this.nextList()).add(this.cached);
      }

      return this.cached;
   }

   public float getCarvingMask(int x, int z) {
      float f = this.mask.getValue(x, z);
      float f1 = this.terrainData.getRiver().get(x, z);
      return 1.0F - f * f1;
   }

   private BiomeList nextList() {
      int i = this.biomeListIndex + 1;
      if (i < this.biomeLists.length) {
         this.biomeListIndex = i;
         return this.biomeLists[i].reset();
      } else {
         return new BiomeList();
      }
   }
}
