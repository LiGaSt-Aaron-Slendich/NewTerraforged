package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
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

    /** Re-run full surface rules + cover after integrity repair — does not touch terrain geometry. */
    public void refreshAfterIntegrity(ChunkAccess chunk, WorldGenLevel region, Generator generator, TerrainData terrain, CarverChunk carver) {
        if (!(region instanceof WorldGenRegion worldGenRegion)) {
            return;
        }
        this.decorate(chunk, worldGenRegion, generator);
        ChunkUtil.refreshHeightmaps(chunk);
        this.decoratePost(chunk, generator);
        if (terrain != null) {
            Surface.smoothWater(chunk, region, terrain);
            Surface.applyPost(chunk, terrain, generator);
            Surface.repairExposedCover(chunk, region, generator, terrain, carver);
        }
    }
}
