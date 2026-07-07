package com.terraforged.mod.internal.probe;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;

/** Kill-switch for the in-mod world probe / inspector (off by default). */
public final class TfProbeConfig {
    private TfProbeConfig() {
    }

    public static boolean enabled() {
        return TFCaveBiomeConfig.INSTANCE != null && TFCaveBiomeConfig.INSTANCE.enableInspector;
    }
}
