package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Local mega/giga envelope: marks inland slope columns where roof buffer relaxes (not zero)
 * without piercing the surface crust.
 */
public final class CaveParallelExposureFilter {
    static final float MAX_EXPOSED_FRACTION = 0.30f;
    private static final int MIN_LOCAL_SLOPE = 4;
    /** Fraction of qualifying inland slope columns that become local entrance slots. */
    private static final float ENTRANCE_SLOT = 0.07f;

    private CaveParallelExposureFilter() {
    }

    static void build(CarverColumnCache columns, int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config) {
        if (!columns.anyMegaGiga() || columns.isEnvelopeBuilt()) {
            if (columns.anyMegaGiga() && !columns.isEnvelopeBuilt()) {
                columns.markEnvelopeBuilt();
            }
            return;
        }
        if (config == null || !config.getType().isMegaOrGiga() || columns.nearSea() || columns.mayHaveRiver()) {
            columns.markEnvelopeBuilt();
            return;
        }
        columns.markEnvelopeBuilt();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int dx = 2; dx < 14; dx += 3) {
            for (int dz = 2; dz < 14; dz += 3) {
                if (columns.zone(dx, dz) == CarverColumnCache.ZONE_NONE) {
                    continue;
                }
                if (columns.nearRiver(dx, dz) || !columns.localSurfaceSlope(dx, dz, MIN_LOCAL_SLOPE)) {
                    continue;
                }
                int x = startX + dx;
                int z = startZ + dz;
                float pick = (NoiseUtil.valCoord2D((int)(seed ^ 0xE1A701L), x, z) + 1.0f) * 0.5f;
                if (pick > ENTRANCE_SLOT) {
                    continue;
                }
                columns.setEnvelopeFlag(dx, dz, CarverColumnCache.ENVELOPE_ENTRANCE);
            }
        }
    }
}
