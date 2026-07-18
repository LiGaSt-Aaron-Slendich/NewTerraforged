package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;

public class SurfaceDecorator {
   public void decorate(ChunkAccess chunk, WorldGenRegion region, Generator generator) {
      WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(generator, region);
      NoiseChunk noisechunk = NoiseChunkUtil.getNoiseChunk(chunk, generator);
      Registry<Biome> registry = generator.getBiomeSource().getRegistry();
      BiomeManager biomemanager = region.getBiomeManager();
      SurfaceSystem surfacesystem = generator.getVanillaGen().getSurfaceSystem();
      RuleSource rulesource = ((NoiseGeneratorSettings)generator.getVanillaGen().getSettings().value()).surfaceRule();
      surfacesystem.buildSurface(biomemanager, registry, false, worldgenerationcontext, chunk, noisechunk, rulesource);
   }

   public void decoratePost(ChunkAccess chunk, Generator generator) {
      TerrainData terraindata = generator.getChunkData(chunk.getPos());
      Surface.apply(terraindata, chunk, generator);
   }
}
