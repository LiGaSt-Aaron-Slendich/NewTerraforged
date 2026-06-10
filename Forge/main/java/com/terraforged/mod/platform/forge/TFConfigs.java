package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.platform.forge.TFSurfaceBiomeConfig;

public final class TFConfigs {
    private TFConfigs() {
    }

    public static void register() {
        TFCaveSystemConfig.load();
        TFCaveBiomeConfig.load();
        TFSurfaceBiomeConfig.load();
        TerraForged.LOG.info("[TFConfig] Loaded caves.toml, cave-biomes.toml ({} primary), surface-biomes.toml", TFCaveBiomeConfig.INSTANCE.primary.size());
    }
}
