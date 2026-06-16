package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class CaveChunkIntegrityPass {
    private static final Set<Long> PROCESSED = ConcurrentHashMap.newKeySet();

    private CaveChunkIntegrityPass() {
    }

    public static void runOnce(ChunkAccess chunk, WorldGenLevel region, StructureFeatureManager structures, Generator generator, CarverChunk carver, FeatureDecorator featureDecorator, SurfaceDecorator surfaceDecorator, CompletableFuture<TerrainData> terrainFuture) {
        if (chunk == null || carver == null || !carver.hasSurfaceRisk()) {
            return;
        }
        long chunkKey = chunk.getPos().toLong();
        if (!PROCESSED.add(chunkKey)) {
            return;
        }
        CaveChunkCorruptionReport report = CaveChunkCorruptionChecker.scan(chunk, carver, generator);
        if (!report.corrupted()) {
            return;
        }
        int cx = chunk.getPos().getMiddleBlockX();
        int cz = chunk.getPos().getMiddleBlockZ();
        CaveChunkIntegrityPass.log("Surface-risk chunk corrupted (" + cx + ", " + cz + ") — " + report.detail());
        CaveChunkIntegrityPass.log("Restoring order... (" + cx + ", " + cz + ")");
        boolean restored = CaveChunkOrderRestorer.restore(chunk, carver, generator, region, structures, featureDecorator, surfaceDecorator, terrainFuture, report);
        if (restored) {
            CaveChunkIntegrityPass.log("Order restored... (" + cx + ", " + cz + ")");
            return;
        }
        CaveChunkCorruptionReport remaining = CaveChunkCorruptionChecker.verify(chunk, carver, generator);
        String detail = remaining.corrupted() ? remaining.detail() : report.detail();
        CaveChunkIntegrityPass.log("Restoration failed... (" + cx + ", " + cz + ") — " + detail);
    }

    private static void log(String message) {
        TerraForged.LOG.info("[ChunkIntegrity] {}", message);
        System.out.println("[NewTerraforged] " + message);
    }
}
