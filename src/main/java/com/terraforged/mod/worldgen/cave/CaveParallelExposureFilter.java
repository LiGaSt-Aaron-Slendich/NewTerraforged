package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Optional anti-groove envelope for mega/giga caves. Disabled by default — full carve volume
 * is restored first; re-enable entrance-only suppression once carving is verified in-game.
 */
public final class CaveParallelExposureFilter {
    static final float MAX_EXPOSED_FRACTION = 0.30f;

    private CaveParallelExposureFilter() {
    }

    static void build(CarverColumnCache columns, int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config) {
        if (!columns.anyMegaGiga() || columns.isEnvelopeBuilt()) {
            return;
        }
        columns.markEnvelopeBuilt();
    }
}
