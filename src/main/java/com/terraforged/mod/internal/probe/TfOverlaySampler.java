package com.terraforged.mod.internal.probe;

import com.terraforged.mod.worldgen.Generator;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Stub — full NewTF overlay sampler deferred on 1.19 until CarverChunk paint APIs return. */
public final class TfOverlaySampler {
    private TfOverlaySampler() {}

    public static List<TfOverlayColumn> sample(
            ServerLevel level,
            Generator generator,
            InspectorOverlayMode mode,
            BlockPos center,
            int radius
    ) {
        return Collections.emptyList();
    }
}
