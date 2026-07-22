package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import com.terraforged.mod.worldgen.biome.rules.SubterrainResolver;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Terrain integrator: builds a <b>candidate pool</b> from climate weights ∩ biome JSON rules
 * (biomes that cannot spawn on this terrain/subterrain never enter the pick set).
 */
public final class BiomeTerrainIntegration {
    private BiomeTerrainIntegration() {
    }

    public static Holder<Biome> pick(float noise, ClimateSample sample, WeightMap<Holder<Biome>> climatePool, Holder<Biome> fallback) {
        if (climatePool == null || climatePool.isEmpty()) {
            return fallback;
        }
        if (!BiomeRuleRegistry.isSynced()) {
            // Rules not ready yet — keep climate pick.
            Holder<Biome> v = climatePool.getValue(noise);
            return v != null ? v : fallback;
        }
        String terrain = sample != null && sample.terrainType != null ? sample.terrainType.getName() : null;
        String sub = SubterrainResolver.resolve(sample);
        boolean steep = SubterrainResolver.isSteepSlope(sample, sub);

        // Conditional subterrains: if nobody matches this subterrain, deactivate it.
        if (!SubterrainResolver.NONE.equals(sub)) {
            int withSub = BiomeRuleRegistry.countMatching(java.util.Arrays.asList(climatePool.getValues()), terrain, sub, steep);
            if (withSub == 0) {
                sub = SubterrainResolver.NONE;
            }
        }

        List<Holder<Biome>> values = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (Holder<Biome> holder : climatePool.getValues()) {
            if (holder == null) {
                continue;
            }
            ResourceLocation id = biomeId(holder);
            if (id == null) {
                continue;
            }
            BiomeRule rule = BiomeRuleRegistry.get(id);
            if (rule == null) {
                // No rule file indexed (ocean etc.) — allow as climate-only candidate.
                values.add(holder);
                weights.add(1.0F);
                continue;
            }
            float chance = BiomeRuleRegistry.matchChance(rule, terrain, sub, steep);
            if (chance <= 0.0F) {
                continue;
            }
            values.add(holder);
            weights.add(chance);
        }

        if (values.isEmpty()) {
            // Absolute empty — fall back to climate pool without terrain gate (avoid void biomes).
            Holder<Biome> v = climatePool.getValue(noise);
            return v != null ? v : fallback;
        }

        @SuppressWarnings("unchecked")
        Holder<Biome>[] arr = values.toArray(Holder[]::new);
        float[] w = new float[weights.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = weights.get(i);
        }
        WeightMap<Holder<Biome>> candidates = new WeightMap<>(arr, w);
        Holder<Biome> picked = candidates.getValue(noise);
        return picked != null ? picked : fallback;
    }

    /** @deprecated Use {@link #pick}; kept for any leftover call sites. */
    @Deprecated
    public static Holder<Biome> filter(Holder<Biome> candidate, String terrainName, WeightMap<Holder<Biome>> climatePool) {
        return candidate;
    }

    private static ResourceLocation biomeId(Holder<Biome> biome) {
        if (biome == null) {
            return null;
        }
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }
}
