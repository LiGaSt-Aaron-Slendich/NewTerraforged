package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveCarvingGate;
import com.terraforged.mod.worldgen.cave.CaveChunkIntegrityPass;
import com.terraforged.mod.worldgen.cave.RiverShoreBiomeClip;
import com.terraforged.mod.worldgen.cave.CaveChunkSurfaceRepair;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.NoiseCaveGenerator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.noise.Module;
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

    @org.jetbrains.annotations.Nullable
    public CarverChunk buildDiagnosticCarver(int seed, ChunkAccess chunk, Generator generator) {
        return this.noiseCaveGenerator.buildDiagnosticCarver(seed, chunk, generator);
    }

    public NoiseCave[] orderedCarveConfigs() {
        return this.noiseCaveGenerator.orderedCarveConfigs();
    }

    public boolean isCarveConfigEnabled(NoiseCave config) {
        return this.noiseCaveGenerator.isCarveConfigEnabled(config);
    }

    public Module carveModifierFor(NoiseCave config) {
        return this.noiseCaveGenerator.modifierFor(config);
    }

    public void carve(long seed, ChunkAccess chunk, WorldGenRegion region, BiomeManager biomes, GenerationStep.Carving step, Generator generator) {
        if (step != GenerationStep.Carving.AIR) {
            return;
        }
        TerrainData terrain = generator.getChunkDataIfReady(chunk.getPos());
        if (terrain == null) {
            terrain = generator.getChunkData(chunk.getPos());
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
        WorldGenLevel featureLevel = ChunkScopedWorldGenLevel.wrap(region, chunk, ChunkScopedWorldGenLevel.FEATURE_PLACEMENT_RADIUS);
        this.featureDecorator.decorate(chunk, featureLevel, structures, terrainFuture, generator, false);
        Surface.smoothWater(chunk, region, terrain);
        Surface.applyPost(chunk, terrain, generator);
        CarverChunk carver = this.noiseCaveGenerator.peekCarver(chunk.getPos());
        CaveChunkSurfaceRepair.restoreRiverDepressions(chunk, carver, generator, terrain, region);
        RiverShoreBiomeClip.clip(chunk, generator, terrain);
        this.featureDecorator.placeStructures(chunk, featureLevel, structures, generator);
        if (CaveCarvingGate.deferBlockCarveUntilAfterRiverFill) {
            this.noiseCaveGenerator.applyCarveBlocks(chunk, generator);
            carver = this.noiseCaveGenerator.peekCarver(chunk.getPos());
        }
        this.noiseCaveGenerator.decorateVolume(chunk, scoped, generator);
        Surface.repairExposedCover(chunk, region, generator, terrain, carver);
        this.noiseCaveGenerator.decorateEntrances(chunk, scoped, generator);
        CaveChunkIntegrityPass.runOnce(chunk, scoped, structures, generator, carver, this.featureDecorator, this.surfaceDecorator, terrainFuture);
        this.noiseCaveGenerator.finishDecorate(chunk, generator);
        ChunkUtil.refreshHeightmaps(chunk);
    }
}
