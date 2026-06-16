package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Integrity2 — underground floating / wrong-biome feature cleanup via {@link CaveFeatureRestorer}.
 */
public final class CaveFeatureIntegrityPass {
    private CaveFeatureIntegrityPass() {
    }

    public static void runOnce(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (chunk == null || carver == null || !carver.isColumnCacheReady()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        if (!columns.anyMegaGiga() && !columns.anySynapseEligible()) {
            return;
        }
        int removed = CaveFeatureRestorer.restore(chunk, carver, generator);
        if (removed > 0) {
            TerraForged.LOG.debug("[FeatureIntegrity2] removed {} stray blocks in chunk {}", removed, chunk.getPos());
        }
    }
}
