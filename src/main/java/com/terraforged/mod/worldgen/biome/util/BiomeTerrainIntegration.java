package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.mod.platform.forge.TFBiomeTerrainIntegrationConfig;
import com.terraforged.mod.util.storage.WeightMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class BiomeTerrainIntegration {
    private BiomeTerrainIntegration() {
    }

    public static Holder<Biome> filter(Holder<Biome> candidate, String terrainName, WeightMap<Holder<Biome>> climatePool) {
        TFBiomeTerrainIntegrationConfig cfg = TFBiomeTerrainIntegrationConfig.INSTANCE;
        if (cfg == null || terrainName == null || terrainName.isBlank()) {
            return candidate;
        }
        TFBiomeTerrainIntegrationConfig.TerrainRules rules = cfg.getRules(terrainName);
        if (rules.isEmpty()) {
            return candidate;
        }
        ResourceLocation id = BiomeTerrainIntegration.biomeId(candidate);
        if (id != null && rules.isAllowed(id)) {
            return candidate;
        }
        if (climatePool == null || climatePool.isEmpty()) {
            return candidate;
        }
        Holder<Biome> fallback = climatePool.find(b -> {
            ResourceLocation biomeId = BiomeTerrainIntegration.biomeId(b);
            return biomeId != null && rules.isAllowed(biomeId);
        });
        return fallback != null ? fallback : candidate;
    }

    private static ResourceLocation biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }
}
