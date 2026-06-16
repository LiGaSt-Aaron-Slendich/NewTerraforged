package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class CaveChunkIntegrityPass {
    private CaveChunkIntegrityPass() {
    }

    /** Disabled — full chunk replay is too expensive and rarely fixes TerraForged crust issues. */
    public static void runOnce(ChunkAccess chunk, WorldGenLevel region, StructureFeatureManager structures, Generator generator, CarverChunk carver, FeatureDecorator featureDecorator, SurfaceDecorator surfaceDecorator, CompletableFuture<TerrainData> terrainFuture) {
    }
}
