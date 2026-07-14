package com.terraforged.mod.worldgen.cave;

/**
 * Master switch for the NoiseCave carving pipeline.
 * Block carve runs during the AIR carving step; river void fill runs in the same step immediately after.
 */
public final class CaveCarvingGate {
    public static boolean enabled = true;
    public static boolean deferBlockCarveUntilAfterRiverFill = false;

    private CaveCarvingGate() {
    }

    public static boolean isEnabled() {
        return CaveCarvingGate.enabled;
    }
}
