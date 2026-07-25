package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import com.terraforged.mod.worldgen.biome.rules.ClimateTagMatch;
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
 * Terrain integrator: builds a <b>candidate pool</b> from climate weights ∩ biome JSON rules
 * (biomes that cannot spawn on this terrain/subterrain never enter the pick set).
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
        boolean steep = false; // can_be_on_slope retired
        ZoneContext zone = ZoneContext.from(sample, noiseGen, blockX, blockZ);

        List<Holder<Biome>> values = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        List<ResourceLocation> ids = new ArrayList<>();
        for (Holder<Biome> holder : climatePool.getValues()) {
            if (holder == null) {
                continue;
            }
            ResourceLocation id = biomeId(holder);
            if (id == null) {
                continue;
            }
            // Cave biomes never paint the surface, even if still listed in a climate WeightMap.
            if (com.terraforged.mod.worldgen.cave.CaveBiomeIds.isUndergroundBiome(id)) {
                continue;
            }
            BiomeRule rule = BiomeRuleRegistry.get(id);
            if (rule == null) {
                // No rule → climate pool membership only (no terrain soft-pass for unknown biomes).
                continue;
            }
            // Hard climate_tags gate — pool leftovers must not paint hot biomes onto tundra.
            if (!ClimateTagMatch.matches(sample != null ? sample.climateType : null, rule.climateTags)) {
                continue;
            }
            float chance = BiomeRuleRegistry.matchChance(rule, terrain, sub, steep, zone);
            // Dormant cones: allow normal land biomes (hills/mountains/plains), not only volcanic kits.
            if (chance <= 0.0F && zone.onDormantVolcano) {
                chance = dormantConeChance(rule, sub, steep, zone);
            }
            if (chance <= 0.0F) {
                continue;
            }
            values.add(holder);
            weights.add(chance);
            ids.add(id);
        }

        if (values.isEmpty()) {
            // If landform was never resolved (none/blank), keep climate distribution instead of
            // hard plains. Concrete terrains with zero matches still use the plains fallback
            // so rule exclusions (e.g. highlands on badlands) stay authoritative.
            if (terrain == null || terrain.isBlank() || "none".equalsIgnoreCase(terrain)) {
                Holder<Biome> climatePick = climatePool.getValue(noise);
                return climatePick != null ? climatePick : fallback;
            }
            return fallback;
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
                            w *= 0.2F; // wrong half of the preferred kit
                        }
                    } else if (VolcanoBiomeKits.isKitNamespace(id.getNamespace()) && role != VolcanoBiomeKits.Role.OTHER) {
                        w *= 0.12F; // other mods' volcanic kits
                    }
                    weights.set(i, w);
                }
            }
        } else if (zone.onDormantVolcano) {
            // Soft-prefer land biomes over volcanic kits on dormant cones.
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
        boolean steep = false;
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

        lines.add("--- Candidates (chance>0) ---");
        List<Holder<Biome>> values = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        int rejected = 0;
        List<String> rejectLines = new ArrayList<>();
        for (Holder<Biome> holder : climatePool.getValues()) {
            if (holder == null) {
                continue;
            }
            ResourceLocation id = biomeId(holder);
            if (id == null) {
                continue;
            }
            if (com.terraforged.mod.worldgen.cave.CaveBiomeIds.isUndergroundBiome(id)) {
                rejectLines.add(id + " → underground biome");
                rejected++;
                continue;
            }
            BiomeRule rule = BiomeRuleRegistry.get(id);
            if (rule == null) {
                rejectLines.add(id + " → no rule (excluded)");
                rejected++;
                continue;
            }
            if (!ClimateTagMatch.matches(climate, rule.climateTags)) {
                rejectLines.add(id + " → climate_tags mismatch (" + rule.climateTags + ")");
                rejected++;
                continue;
            }
            float chance = BiomeRuleRegistry.matchChance(rule, terrain, sub, steep, zone);
            if (chance <= 0.0F && zone.onDormantVolcano) {
                chance = dormantConeChance(rule, sub, steep, zone);
            }
            if (chance <= 0.0F) {
                String why = BiomeRuleRegistry.rejectReason(rule, terrain, sub, zone);
                if (why != null && why.startsWith("subterrain")) {
                    rejectLines.add(id + " → TERRAIN_OK but " + why);
                } else {
                    rejectLines.add(id + " → " + (why != null ? why : "chance=0"));
                }
                rejected++;
                continue;
            }
            values.add(holder);
            weights.add(chance);
            lines.add(String.format(java.util.Locale.ROOT, "%s  chance=%.3f", id, chance));
        }
        lines.add("--- Rejected from climate pool (" + rejected + ") ---");
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
        if (values.isEmpty()) {
            lines.add("picked: FALLBACK (" + biomeId(fallback) + ")");
        } else {
            @SuppressWarnings("unchecked")
            Holder<Biome>[] arr = values.toArray(Holder[]::new);
            float[] w = new float[weights.size()];
            for (int i = 0; i < w.length; i++) {
                w[i] = weights.get(i);
            }
            WeightMap<Holder<Biome>> candidates = new WeightMap<>(arr, w);
            Holder<Biome> picked = candidates.getValue(noise);
            lines.add("picked: " + biomeId(picked != null ? picked : fallback)
                    + String.format(java.util.Locale.ROOT, "  (biomeNoise=%.4f)", noise));
        }
        return lines;
    }

    /** Try common land terrains so non-volcanic biomes can sit on dormant volcano cells. */
    private static float dormantConeChance(BiomeRule rule, String sub, boolean steep, ZoneContext zone) {
        String[] alts = {"hills_1", "hills_2", "mountains_1", "mountains_2", "plateau", "plains", "steppe"};
        float best = 0.0F;
        for (String alt : alts) {
            best = Math.max(best, BiomeRuleRegistry.matchChance(rule, alt, sub, steep, zone));
        }
        return best;
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
