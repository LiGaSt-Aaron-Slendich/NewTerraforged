package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Cave biome DeferredRegister. Full ModCaveBiomeFactories deferred with feature presets.
 */
public final class TFCaveBiomes {
    private TFCaveBiomes() {
    }

    public static void register(IEventBus modBus) {
        TerraForged.LOG.info("[TFCaveBiomes] Deferred — ModCaveBiomeFactories excluded until decor KEEP compiles on TF118 carver");
    }
}
