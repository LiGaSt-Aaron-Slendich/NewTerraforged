package com.terraforged.mod.worldgen.cave;

/**
 * Master switch for the NoiseCave carving pipeline (A/B diagnostics).
 * When {@code false}: no air volumes from synapse/mega/giga, grotto entrances, entrance carver,
 * or tunnel-river decorate — code stays in place, set {@link #enabled} to {@code true} to restore.
 */
public final class CaveCarvingGate {
    public static boolean enabled = false;

    private CaveCarvingGate() {
    }

    public static boolean isEnabled() {
        return CaveCarvingGate.enabled;
    }
}
