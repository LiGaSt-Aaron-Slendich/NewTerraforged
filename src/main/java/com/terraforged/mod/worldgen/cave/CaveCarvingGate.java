package com.terraforged.mod.worldgen.cave;

/**
 * Master switch for the NoiseCave carving pipeline.
 * When {@link #deferBlockCarveUntilAfterRiverFill} is true, {@link com.terraforged.mod.worldgen.cave.NoiseCaveGenerator}
 * prepares column metadata during the carving step but places air only after river void fill in decorate.
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
