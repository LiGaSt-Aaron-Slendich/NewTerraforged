package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * @deprecated Use {@link CaveFeatureIntegrityPass} + {@link CaveFeatureRestorer}. Kept as no-op shim.
 */
@Deprecated
public final class CaveFloatingVegetationSanitizer {
    private CaveFloatingVegetationSanitizer() {
    }

    public static void sanitize(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        CaveFeatureIntegrityPass.runOnce(chunk, carver, generator);
    }
}
