package com.terraforged.mod.internal.probe;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.internal.probe.InspectorServerSession;
import net.minecraftforge.common.MinecraftForge;

/** Optional inspector module entry — no-op when {@link TfProbeConfig#enabled()} is false. */
public final class ProbeInspectorBootstrap {
    private static boolean initialized;

    private ProbeInspectorBootstrap() {
    }

    public static boolean isEnabled() {
        return TfProbeConfig.enabled();
    }

    public static void initCommon() {
        if (!TfProbeConfig.enabled() || initialized) {
            return;
        }
        initialized = true;
        MinecraftForge.EVENT_BUS.register(InspectorServerSession.class);
        TerraForged.LOG.info("[probe] Inspector module armed (enable_inspector=true in cave-biomes.toml)");
    }

    public static void initClient() {
        if (!TfProbeConfig.enabled()) {
            return;
        }
        ProbeClientHooks.init();
    }
}
