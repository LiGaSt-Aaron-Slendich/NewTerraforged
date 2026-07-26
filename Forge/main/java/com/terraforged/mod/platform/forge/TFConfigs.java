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
        TFConfigLoader.ensureNestedLayout();
        TFCaveSystemConfig.load();
        TFCaveBiomeConfig.load();
        TFSurfaceBiomeConfig.load();
        // Legacy TOML integrator kept loadable for migration; runtime uses Terrain_rules/Biomes JSON.
        try {
            TFBiomeTerrainIntegrationConfig.load();
        } catch (Exception e) {
            TerraForged.LOG.warn("[TFConfig] legacy biome-terrain-integration.toml skipped: {}", e.toString());
        }
        TFNoiseVariantFlags.load();
        try {
            java.nio.file.Files.createDirectories(com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry.biomesRoot());
        } catch (Exception e) {
            TerraForged.LOG.warn("[TFConfig] could not create Terrain_rules/Biomes: {}", e.toString());
        }
        TerraForged.LOG.info("[TFConfig] Loaded caves.toml (density {}% xy={} yz={}), cave-biomes.toml ({} primary), surface-biomes.toml; biome rules dir {}", Float.valueOf(TFCaveSystemConfig.INSTANCE.caveDensity.cavePercent()), TFCaveSystemConfig.INSTANCE.caveDensity.xyLimit() != null ? TFCaveSystemConfig.INSTANCE.caveDensity.xyLimit() : "percent", TFCaveSystemConfig.INSTANCE.caveDensity.yzLimit() != null ? TFCaveSystemConfig.INSTANCE.caveDensity.yzLimit() : "percent", TFCaveBiomeConfig.INSTANCE.primary.size(), com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry.biomesRoot());
    }
}
