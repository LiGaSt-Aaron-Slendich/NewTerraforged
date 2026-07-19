package com.terraforged.mod.internal.probe;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.internal.probe.client.InspectorClient;

public final class ProbeClientHooks {
    private ProbeClientHooks() {
    }

    static void init() {
        if (!TfProbeConfig.enabled()) {
            return;
        }
        InspectorClient.bootstrap();
        TerraForged.LOG.info("[probe] Inspector client hooks registered");
    }
}
