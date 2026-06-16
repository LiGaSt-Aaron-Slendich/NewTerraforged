package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Targeted post-decorate guard for chunks where carving encroached on the surface crust.
 * Replaces the old per-chunk integrity scan for the common case.
 */
public final class CaveSurfaceProximityGuard {
    private CaveSurfaceProximityGuard() {
    }

    public static void repair(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, TerrainData terrain) {
        if (carver == null || !carver.hasSurfaceRisk()) {
            return;
        }
        CaveDecorationSanitizer.sanitizeRiskColumns(chunk, carver, generator);
        CaveChunkSurfaceRepair.stripRiskSurfacePillars(chunk, carver);
        CaveChunkSurfaceRepair.restoreRiverDepressions(chunk, carver, generator, terrain);
        ChunkUtil.refreshHeightmaps(chunk);
    }
}
