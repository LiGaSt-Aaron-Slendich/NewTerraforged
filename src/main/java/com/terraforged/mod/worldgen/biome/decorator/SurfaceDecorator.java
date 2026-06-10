package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class SurfaceDecorator {
    public void decorate(ChunkAccess chunk, WorldGenRegion region, Generator generator) {
        WorldGenerationContext context = new WorldGenerationContext((ChunkGenerator)generator, (LevelHeightAccessor)region);
        NoiseChunk noiseChunk = NoiseChunkUtil.getNoiseChunk(chunk, generator);
        NoiseGeneratorSettings settings = (NoiseGeneratorSettings)generator.getVanillaGen().getSettings().value();
        SurfaceRules.RuleSource surfaceRules = settings.surfaceRule();
        Registry<Biome> biomes = generator.getBiomeSource().getRegistry();
        BiomeManager biomeManager = region.getBiomeManager();
        SurfaceSystem surfaceSystem = generator.getVanillaGen().getSurfaceSystem();
        surfaceSystem.buildSurface(biomeManager, biomes, false, context, chunk, noiseChunk, surfaceRules);
    }

    public void decoratePost(ChunkAccess chunk, Generator generator) {
        TerrainData chunkData = generator.getChunkData(chunk.getPos());
        Surface.apply(chunkData, chunk, generator);
    }
}
