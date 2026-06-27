package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveChunkIntegrityPass;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.NoiseCaveGenerator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;

public class BiomeGenerator {
    private final SurfaceDecorator surfaceDecorator;
    private final FeatureDecorator featureDecorator;
    private final NoiseCaveGenerator noiseCaveGenerator;

    public BiomeGenerator(long seed, RegistryAccess access) {
        this.surfaceDecorator = new SurfaceDecorator();
        this.featureDecorator = new FeatureDecorator(access);
        this.noiseCaveGenerator = new NoiseCaveGenerator(seed, access);
    }

    public BiomeGenerator(long seed, BiomeGenerator other) {
        this.surfaceDecorator = other.surfaceDecorator;
        this.featureDecorator = other.featureDecorator;
        this.noiseCaveGenerator = new NoiseCaveGenerator(seed, other.noiseCaveGenerator);
    }

    public void surface(ChunkAccess chunk, WorldGenRegion region, Generator generator) {
        this.surfaceDecorator.decorate(chunk, region, generator);
        ChunkUtil.refreshHeightmaps(chunk);
        this.surfaceDecorator.decoratePost(chunk, generator);
    }

    public CaveEntranceClaims getCaveEntranceClaims() {
        return this.noiseCaveGenerator.getEntranceClaims();
    }

    public CarverChunk peekCaveCarver(net.minecraft.world.level.ChunkPos pos) {
        return this.noiseCaveGenerator.peekCarver(pos);
    }

    public void carve(long seed, ChunkAccess chunk, WorldGenRegion region, BiomeManager biomes, GenerationStep.Carving step, Generator generator) {
        if (step != GenerationStep.Carving.AIR) {
            return;
        }
        this.noiseCaveGenerator.carve(chunk, generator);
    }

    public void decorate(ChunkAccess chunk, WorldGenLevel region, StructureFeatureManager structures, Generator generator) {
        TerrainData terrain = generator.getChunkDataIfReady(chunk.getPos());
        CompletableFuture<TerrainData> terrainFuture;
        if (terrain != null) {
            terrainFuture = CompletableFuture.completedFuture(terrain);
        } else {
            terrainFuture = generator.getChunkDataAsync(chunk.getPos());
            terrain = terrainFuture.join();
        }
        WorldGenLevel scoped = ChunkScopedWorldGenLevel.wrap(region, chunk, 2);
        this.featureDecorator.decorate(chunk, ChunkScopedWorldGenLevel.wrap(region, chunk, 1), structures, terrainFuture, generator);
        this.noiseCaveGenerator.decorateVolume(chunk, scoped, generator);
        Surface.smoothWater(chunk, region, terrain);
        Surface.applyPost(chunk, terrain, generator);
        CarverChunk carver = this.noiseCaveGenerator.peekCarver(chunk.getPos());
        Surface.repairExposedCover(chunk, region, generator, terrain, carver);
        this.noiseCaveGenerator.decorateEntrances(chunk, scoped, generator);
        CaveChunkIntegrityPass.runOnce(chunk, scoped, structures, generator, carver, this.featureDecorator, this.surfaceDecorator, terrainFuture);
        this.noiseCaveGenerator.finishDecorate(chunk, generator);
        ChunkUtil.refreshHeightmaps(chunk);
    }
}
