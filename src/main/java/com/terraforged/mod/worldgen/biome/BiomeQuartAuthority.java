package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.worldgen.GenerationFeatureGates;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveSurfaceBiomeRestorer;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Single surface-quart authority before decoration (P4#22).
 * Carve may paint underground biomes; this pass restores surface band once — decor reads {@code level.getBiome} only.
 */
public final class BiomeQuartAuthority {
    private BiomeQuartAuthority() {
    }

    public static void finalizeForDecorate(ChunkAccess chunk, Generator generator, CarverChunk carver) {
        if (!GenerationFeatureGates.biomeQuartAuthorityEnabled) {
            return;
        }
        CaveSurfaceBiomeRestorer.restore(chunk, generator, carver);
        ChunkUtil.refreshHeightmaps(chunk);
    }
}
