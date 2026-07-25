package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.util.map.FloatMap;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetation;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import com.terraforged.mod.worldgen.biome.viability.ViabilityContext;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class SamplerContext {
   private static final ThreadLocal<SamplerContext> LOCAL_CONTEXT = ThreadLocal.withInitial(SamplerContext::new);
   public ChunkAccess chunk;
   public WorldGenLevel region;
   public Generator generator;
   public WorldgenRandom random;
   public Biome biome;
   public VegetationConfig vegetation;
   public VegetationFeatures features;
   public float maxViability = 0.0F;
   public final FloatMap viability = new FloatMap();
   public final BiomeList biomeList = new BiomeList();
   public final ViabilityContext viabilityContext = new ViabilityContext();
   public final MutableBlockPos pos = new MutableBlockPos();

   public int getHeight(int x, int z) {
      return this.chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z);
   }

   public Holder<Biome> getBiome(int x, int y, int z) {
      return this.region.getBiome(this.pos.set(x, y, z));
   }

   public TerrainData terrainData() {
      return this.viabilityContext.getTerrain();
   }

   public SamplerContext reset() {
      this.biomeList.reset();
      return this;
   }

   public void push(Biome biome, BiomeVegetation vegetation) {
      this.maxViability = 0.0F;
      this.biome = biome;
      this.vegetation = vegetation.config;
      this.features = vegetation.features;
   }

   public static SamplerContext get() {
      return LOCAL_CONTEXT.get().reset();
   }

   /** Same thread-local instance without clearing biomeList / maps. */
   public static SamplerContext current() {
      return LOCAL_CONTEXT.get();
   }
}
