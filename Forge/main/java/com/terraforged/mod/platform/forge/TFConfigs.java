package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFBiomeTerrainIntegrationConfig;
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
        TFBiomeTerrainIntegrationConfig.load();
        TerraForged.LOG.info("[TFConfig] Loaded caves.toml (density {}% xy={} yz={}), cave-biomes.toml ({} primary), surface-biomes.toml, biome-terrain-integration.toml ({} terrains)", Float.valueOf(TFCaveSystemConfig.INSTANCE.caveDensity.cavePercent()), TFCaveSystemConfig.INSTANCE.caveDensity.xyLimit() != null ? TFCaveSystemConfig.INSTANCE.caveDensity.xyLimit() : "percent", TFCaveSystemConfig.INSTANCE.caveDensity.yzLimit() != null ? TFCaveSystemConfig.INSTANCE.caveDensity.yzLimit() : "percent", TFCaveBiomeConfig.INSTANCE.primary.size(), TFBiomeTerrainIntegrationConfig.INSTANCE.terrainRuleCount());
    }
}
