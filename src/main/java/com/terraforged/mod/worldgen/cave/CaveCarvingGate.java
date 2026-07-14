package com.terraforged.mod.worldgen.cave;

/**
 * Master switch for the NoiseCave carving pipeline.
 * When {@link #deferBlockCarveUntilAfterRiverFill} is true, the AIR step only prepares column metadata;
 * river void fill runs first in decorate (plugs terrain bug shafts), then block carve, then features.
 */
public final class CaveCarvingGate {
    public static boolean enabled = true;
    public static boolean deferBlockCarveUntilAfterRiverFill = true;

    private CaveCarvingGate() {
    }

    public static boolean isEnabled() {
        return CaveCarvingGate.enabled;
    }
}
