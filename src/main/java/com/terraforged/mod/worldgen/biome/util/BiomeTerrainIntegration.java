package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import com.terraforged.mod.worldgen.biome.rules.SubterrainResolver;
import com.terraforged.mod.worldgen.biome.rules.VolcanoBiomeKits;
import com.terraforged.mod.worldgen.biome.rules.ZoneContext;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Terrain integrator: climate WeightMap is authoritative; terrain rules prefer matches
 * but soft-family / climate-fill keep at least {@link ClimateTerrainCandidates#MIN_CANDIDATES}
 * options whenever the climate pool is large enough.
 */
public final class BiomeTerrainIntegration {
    private BiomeTerrainIntegration() {
    }

    public static Holder<Biome> pick(float noise, ClimateSample sample, WeightMap<Holder<Biome>> climatePool, Holder<Biome> fallback) {
        return pick(noise, sample, climatePool, fallback, null, 0, 0);
    }

    public static Holder<Biome> pick(
            float noise,
            ClimateSample sample,
            WeightMap<Holder<Biome>> climatePool,
            Holder<Biome> fallback,
            INoiseGenerator noiseGen,
            int blockX,
            int blockZ
    ) {
        if (climatePool == null || climatePool.isEmpty()) {
            return fallback;
        }
        if (!BiomeRuleRegistry.isSynced()) {
            Holder<Biome> v = climatePool.getValue(noise);
            return v != null ? v : fallback;
        }
        Terrain terrainObj = sample != null ? sample.terrainType : null;
        String terrain = terrainObj != null ? terrainObj.getName() : null;
        String sub = SubterrainResolver.resolve(sample);
        ZoneContext zone = ZoneContext.from(sample, noiseGen, blockX, blockZ);

        ClimateTerrainCandidates.Result built = ClimateTerrainCandidates.collect(
                java.util.Arrays.asList(climatePool.getValues()),
                terrain,
                sub,
                zone,
                zone.onDormantVolcano
        );

        List<ClimateTerrainCandidates.Entry> entries = built.accepted();
        if (entries.isEmpty()) {
            // Last resort: climate distribution (never hard-plains when the pool has biomes).
            Holder<Biome> climatePick = climatePool.getValue(noise);
            return climatePick != null ? climatePick : fallback;
        }

        List<Holder<Biome>> values = new ArrayList<>(entries.size());
        List<Float> weights = new ArrayList<>(entries.size());
        List<ResourceLocation> ids = new ArrayList<>(entries.size());
        for (ClimateTerrainCandidates.Entry e : entries) {
            values.add(e.holder());
            weights.add(e.weight());
            ids.add(e.id());
        }

        // Keep mod volcano kits together on *active* cones only.
        if (zone.onActiveVolcano) {
            String preferredNs = VolcanoBiomeKits.preferredNamespace(blockX, blockZ, ids);
            VolcanoBiomeKits.Role need = ZoneContext.isPipeTerrain(terrainObj)
                    ? VolcanoBiomeKits.Role.CRATER
                    : VolcanoBiomeKits.Role.CONE;
            if (preferredNs != null) {
                for (int i = 0; i < values.size(); i++) {
                    ResourceLocation id = ids.get(i);
                    VolcanoBiomeKits.Role role = VolcanoBiomeKits.role(id);
                    float w = weights.get(i);
                    if (preferredNs.equals(id.getNamespace())) {
                        if (role == need) {
                            w *= 4.0F;
                        } else if (role != VolcanoBiomeKits.Role.OTHER) {
                            w *= 0.2F;
                        }
                    } else if (VolcanoBiomeKits.isKitNamespace(id.getNamespace()) && role != VolcanoBiomeKits.Role.OTHER) {
                        w *= 0.12F;
                    }
                    weights.set(i, w);
                }
            }
        } else if (zone.onDormantVolcano) {
            for (int i = 0; i < values.size(); i++) {
                VolcanoBiomeKits.Role role = VolcanoBiomeKits.role(ids.get(i));
                if (role != VolcanoBiomeKits.Role.OTHER) {
                    weights.set(i, weights.get(i) * 0.35F);
                }
            }
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

    /**
     * Debug-only pick trace: terrain/sub, climate-pool candidates, terrain-ok rejects + reasons.
     */
    public static List<String> explainPick(
            float noise,
            ClimateSample sample,
            WeightMap<Holder<Biome>> climatePool,
            Holder<Biome> fallback,
            INoiseGenerator noiseGen,
            int blockX,
            int blockZ
    ) {
        List<String> lines = new ArrayList<>();
        Terrain terrainObj = sample != null ? sample.terrainType : null;
        String terrain = terrainObj != null ? terrainObj.getName() : "null";
        String sub = SubterrainResolver.resolve(sample);
        ZoneContext zone = ZoneContext.from(sample, noiseGen, blockX, blockZ);
        BiomeType climate = sample != null ? sample.climateType : null;
        lines.add("terrain=" + terrain + "  subterrain=" + sub);
        lines.add(String.format(java.util.Locale.ROOT,
                "climate=%s  height=%.3f  temp=%.3f  moist=%.3f  cn=%.3f",
                climate, sample != null ? sample.heightNoise : 0, sample != null ? sample.temperature : 0,
                sample != null ? sample.moisture : 0, sample != null ? sample.continentNoise : 0));
        lines.add(String.format(java.util.Locale.ROOT,
                "zone: active=%s dormant=%s onActive=%s onDormant=%s dist=%.0f",
                zone.nearActiveVolcano, zone.nearDormantVolcano, zone.onActiveVolcano, zone.onDormantVolcano,
                zone.volcanoDistanceBlocks >= 1.0E8F ? -1 : zone.volcanoDistanceBlocks));

        if (climatePool == null || climatePool.isEmpty()) {
            lines.add("climate pool: EMPTY → fallback");
            return lines;
        }
        if (!BiomeRuleRegistry.isSynced()) {
            lines.add("rules not synced → raw climate WeightMap pick");
            return lines;
        }

        ClimateTerrainCandidates.Result built = ClimateTerrainCandidates.collect(
                java.util.Arrays.asList(climatePool.getValues()),
                terrain,
                sub,
                zone,
                zone.onDormantVolcano
        );

        lines.add("--- Candidates (min=" + ClimateTerrainCandidates.MIN_CANDIDATES
                + ", have=" + built.accepted().size() + ") ---");
        for (ClimateTerrainCandidates.Entry e : built.accepted()) {
            lines.add(String.format(java.util.Locale.ROOT, "%s  chance=%.3f  [%s]",
                    e.id(), e.weight(), e.tier()));
        }

        lines.add("--- Rejected from climate pool (strict pass, " + built.rejected().size() + ") ---");
        List<String> rejectLines = new ArrayList<>(built.rejected());
        rejectLines.sort((a, b) -> {
            boolean aOk = a.contains("TERRAIN_OK");
            boolean bOk = b.contains("TERRAIN_OK");
            if (aOk == bOk) {
                return a.compareTo(b);
            }
            return aOk ? -1 : 1;
        });
        int shown = 0;
        for (String r : rejectLines) {
            lines.add(r);
            if (++shown >= 48) {
                lines.add("… +" + (rejectLines.size() - shown) + " more");
                break;
            }
        }

        if (built.accepted().isEmpty()) {
            lines.add("picked: FALLBACK (" + biomeId(fallback) + ")");
        } else {
            @SuppressWarnings("unchecked")
            Holder<Biome>[] arr = built.accepted().stream().map(ClimateTerrainCandidates.Entry::holder).toArray(Holder[]::new);
            float[] w = new float[built.accepted().size()];
            for (int i = 0; i < w.length; i++) {
                w[i] = built.accepted().get(i).weight();
            }
            WeightMap<Holder<Biome>> candidates = new WeightMap<>(arr, w);
            Holder<Biome> picked = candidates.getValue(noise);
            lines.add("picked: " + biomeId(picked != null ? picked : fallback)
                    + String.format(java.util.Locale.ROOT, "  (biomeNoise=%.4f)", noise));
        }
        return lines;
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
