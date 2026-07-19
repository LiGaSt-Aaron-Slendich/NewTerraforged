package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Cave biome DeferredRegister wiring. Full factories restored in cave KEEP layer.
 * Phase-1 stub so identity/config boots without ModCaveBiomeFactories.
 */
public final class TFCaveBiomes {
    private TFCaveBiomes() {
    }

    public static void register(IEventBus modBus) {
        TerraForged.LOG.info("[TFCaveBiomes] Deferred until cave KEEP layer (ModCaveBiomeFactories)");
    }
}
