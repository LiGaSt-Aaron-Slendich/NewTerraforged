package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.Module;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

public class CarverChunk {
   private Holder<Biome> cached;
   private int cachedX;
   private int cachedZ;
   private NoiseCave cachedConfig;
   private int biomeListIndex = -1;
   private final BiomeList[] biomeLists;
   private final Map<NoiseCave, BiomeList> biomes = new IdentityHashMap<>();
   public Module mask;
   public Module modifier;
   public Module megaModifier;
   public Module gigaModifier;
   public TerrainData terrainData;
   private final CarverColumnCache columns = new CarverColumnCache();
   private boolean columnsReady;

   public CarverChunk(int size) {
      this.biomeLists = new BiomeList[size];

      for (int i = 0; i < this.biomeLists.length; i++) {
         this.biomeLists[i] = new BiomeList();
      }
   }

   public CarverChunk reset() {
      this.cached = null;
      this.cachedConfig = null;
      this.biomes.clear();
      this.biomeListIndex = -1;
      this.columnsReady = false;
      this.megaModifier = null;
      this.gigaModifier = null;
      return this;
   }

   public void prepareColumnCache(int seed, ChunkAccess chunk, Generator generator) {
      if (!this.columnsReady) {
         this.columns.build(seed, chunk, this, generator);
         this.columnsReady = true;
      }
   }

   public boolean isColumnCacheReady() {
      return this.columnsReady;
   }

   CarverColumnCache columnCache() {
      return this.columns;
   }

   public void clearColumnCache() {
      this.columnsReady = false;
   }

   public int cachedSurface(int dx, int dz) {
      return this.columns.surfaceY(dx, dz);
   }

   public BiomeList getBiomes(NoiseCave config) {
      return this.biomes.get(config);
   }

   public Holder<Biome> getBiome(int x, int z, NoiseCave config, Generator generator) {
      int midY = this.getSurfaceY(x, z) - 24;
      return this.getBiome(x, z, midY, config, generator);
   }

   public Holder<Biome> getBiome(int x, int z, int blockY, NoiseCave config, Generator generator) {
      int i = x >> 2;
      int j = z >> 2;
      if (this.cached == null || i != this.cachedX || j != this.cachedZ || this.cachedConfig != config) {
         int surfaceY = this.getSurfaceY(x, z);
         Holder<Biome> surfaceBiome = generator.getBiomeSource().getNoiseBiome(x >> 2, surfaceY >> 2, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
         int cx = snapToCaveGrid(x, config);
         int cz = snapToCaveGrid(z, config);
         int radius = estimateCaveRadius(config);
         this.cached = generator.getBiomeSource()
            .getUnderGroundBiome(config.getSeed(), x, z, config.getType(), surfaceBiome, blockY, surfaceY, cx, cz, radius);
         if (this.cached == null) {
            this.cached = generator.getBiomeSource().getUnderGroundBiome(config.getSeed(), x, z, config.getType());
         }
         this.cachedX = i;
         this.cachedZ = j;
         this.cachedConfig = config;
         this.biomes.computeIfAbsent(config, c -> this.nextList()).add(this.cached);
      }

      return this.cached;
   }

   private int getSurfaceY(int x, int z) {
      if (this.columnsReady) {
         return this.columns.surfaceY(x & 0xF, z & 0xF);
      }
      if (this.terrainData == null) {
         return 64;
      }
      return this.terrainData.getHeight(x & 0xF, z & 0xF);
   }

   private static int estimateCaveRadius(NoiseCave config) {
      return switch (config.getType()) {
         case GIGA -> 400;
         case MEGA -> 250;
         default -> Math.max(64, config.getMaxY() - config.getMinY());
      };
   }

   private static int snapToCaveGrid(int coord, NoiseCave config) {
      int radius = estimateCaveRadius(config);
      int cell = radius * 2;
      return Math.floorDiv(coord, cell) * cell + radius;
   }

   /** TF118 river mask: {@code 1.0F - noise * river} (no *0.45). */
   public float getCarvingMask(int x, int z) {
      float f = this.mask.getValue(x, z);
      float f1 = this.terrainData.getRiver().get(x, z);
      return 1.0F - f * f1;
   }

   /** Debug probe: cave mask noise (does not change {@link #getCarvingMask}). */
   public float debugMaskNoise(int x, int z) {
      return this.mask == null ? 0.0F : this.mask.getValue(x, z);
   }

   /** Debug probe: river factor used by TF118 mask {@code 1 - mask * river}. */
   public float debugRiverNoise(int x, int z) {
      return this.terrainData == null ? 0.0F : this.terrainData.getRiver().get(x, z);
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
