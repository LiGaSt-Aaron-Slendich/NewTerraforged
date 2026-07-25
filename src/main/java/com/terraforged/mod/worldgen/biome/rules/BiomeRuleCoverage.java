package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFSurfaceBiomeConfig;
import com.terraforged.mod.worldgen.biome.terrain.TerrainGroup;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * After rules sync: ensure each climate pool has at least {@link #MIN_CANDIDATES}
 * biomes that list each common land terrain. Patches biome JSON rules (does not
 * change pick-time filtering).
 */
public final class BiomeRuleCoverage {
    public static final int MIN_CANDIDATES = 3;
    private static final float PATCH_CHANCE = 0.55F;

    /** Landforms that appear under every climate and must not starve for candidates. */
    private static final String[] COVER_TERRAINS = {
            "plains", "steppe", "dales",
            "hills_1", "hills_2", "plateau", "torridonian",
            "mountains_1", "mountains_2", "mountains_3",
            "mountains_ridge_1", "mountains_ridge_2",
            "dolomites", "badlands"
    };

    private BiomeRuleCoverage() {
    }

    /**
     * @param rules live registry map (mutated in place)
     * @return number of biome rule files patched
     */
    public static int ensureMinCandidates(Map<ResourceLocation, BiomeRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return 0;
        }
        TFSurfaceBiomeConfig cfg = TFSurfaceBiomeConfig.INSTANCE;
        if (cfg == null) {
            TerraForged.LOG.warn("[BiomeRules] coverage skipped — surface-biomes config not loaded");
            return 0;
        }

        int patchedFiles = 0;
        Set<ResourceLocation> written = new HashSet<>();

        for (BiomeType climate : BiomeType.values()) {
            List<ResourceLocation> pool = resolvePool(cfg, climate, rules);
            if (pool.size() < MIN_CANDIDATES) {
                continue;
            }
            for (String terrain : COVER_TERRAINS) {
                if ("badlands".equals(terrain) && !allowsBadlands(climate)) {
                    continue;
                }
                patchedFiles += ensureTerrain(rules, pool, terrain, written, climate);
            }
        }

        if (patchedFiles > 0) {
            TerraForged.LOG.info(
                    "[BiomeRules] coverage: patched {} biome rules so each climate×terrain has ≥{} candidates",
                    patchedFiles, MIN_CANDIDATES);
        }
        return patchedFiles;
    }

    private static List<ResourceLocation> resolvePool(
            TFSurfaceBiomeConfig cfg, BiomeType climate, Map<ResourceLocation, BiomeRule> rules
    ) {
        List<ResourceLocation> out = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (TFSurfaceBiomeConfig.Entry e : cfg.getClimateList(climate)) {
            if (e == null || e.biomeId() == null || e.biomeId().isBlank()) {
                continue;
            }
            ResourceLocation id;
            try {
                id = new ResourceLocation(e.biomeId().trim());
            } catch (RuntimeException ex) {
                continue;
            }
            if (!rules.containsKey(id) || !seen.add(id)) {
                continue;
            }
            BiomeRule rule = rules.get(id);
            // Zone-locked / volcano-only / beach-only stay specialized — don't use as mountain fillers.
            if (isSpecializedOnly(rule)) {
                continue;
            }
            out.add(id);
        }
        return out;
    }

    private static int ensureTerrain(
            Map<ResourceLocation, BiomeRule> rules,
            List<ResourceLocation> pool,
            String terrain,
            Set<ResourceLocation> written,
            BiomeType climate
    ) {
        List<ResourceLocation> matching = new ArrayList<>();
        List<ResourceLocation> candidates = new ArrayList<>();
        for (ResourceLocation id : pool) {
            BiomeRule rule = rules.get(id);
            if (rule == null || isSpecializedOnly(rule)) {
                continue;
            }
            if (chanceOnTerrain(rule, terrain) > 0.0F) {
                matching.add(id);
            } else {
                candidates.add(id);
            }
        }
        int need = MIN_CANDIDATES - matching.size();
        if (need <= 0) {
            return 0;
        }

        // Prefer biomes already on related landforms (hills ↔ mountains ↔ plains).
        candidates.sort(Comparator
                .comparingInt((ResourceLocation id) -> -relatedScore(rules.get(id), terrain))
                .thenComparing(ResourceLocation::toString));

        int patched = 0;
        for (ResourceLocation id : candidates) {
            if (need <= 0) {
                break;
            }
            BiomeRule rule = rules.get(id);
            if (rule == null) {
                continue;
            }
            BiomeRule next = withTerrain(rule, terrain, PATCH_CHANCE, climate);
            if (next == rule) {
                continue;
            }
            rules.put(id, next);
            if (written.add(id)) {
                try {
                    Path file = BiomeRuleRegistry.biomesRoot()
                            .resolve(id.getNamespace())
                            .resolve(id.getPath() + ".json");
                    BiomeRuleIO.write(file, next);
                    patched++;
                } catch (IOException e) {
                    TerraForged.LOG.error("[BiomeRules] coverage write failed for {}", id, e);
                }
            } else {
                // Already written once this pass — rewrite with accumulated terrains.
                try {
                    Path file = BiomeRuleRegistry.biomesRoot()
                            .resolve(id.getNamespace())
                            .resolve(id.getPath() + ".json");
                    BiomeRuleIO.write(file, next);
                } catch (IOException e) {
                    TerraForged.LOG.error("[BiomeRules] coverage rewrite failed for {}", id, e);
                }
            }
            need--;
        }
        return patched;
    }

    private static BiomeRule withTerrain(BiomeRule rule, String terrain, float chance, BiomeType climate) {
        String key = terrain.toLowerCase(Locale.ROOT);
        LinkedHashMap<String, Float> terrains = new LinkedHashMap<>(rule.terrains);
        LinkedHashMap<String, Float> subterrains = new LinkedHashMap<>(rule.subterrains);
        boolean changed = false;
        if (!terrains.containsKey(key)) {
            terrains.put(key, chance);
            changed = true;
        }
        if (isMountainTerrainKey(key)) {
            boolean alpine = climate == BiomeType.ALPINE || rule.climateTags.contains("alpine");
            if (alpine) {
                if (!subterrains.containsKey("mountain_peak") && !subterrains.containsKey("bare_mountain_peak")) {
                    subterrains.keySet().removeIf(k -> k.contains("mountain") && !k.contains("peak"));
                    subterrains.put("mountain_peak", 1.0F);
                    subterrains.put("bare_mountain_peak", 0.75F);
                    changed = true;
                }
            } else if (!subterrains.containsKey("mountain_foothill") && !subterrains.containsKey("mountain_body")) {
                // Temperate/etc. forests on mountains: never unrestricted peaks.
                subterrains.put("mountain_foothill", 0.55F);
                subterrains.put("mountain_body", 0.50F);
                subterrains.putIfAbsent("bare_mountain", 0.35F);
                subterrains.remove("mountain_peak");
                subterrains.remove("bare_mountain_peak");
                changed = true;
            }
        }
        if (!changed) {
            return rule;
        }
        return new BiomeRule(
                rule.biome,
                rule.canBeOnSlope,
                rule.climateTags,
                terrains,
                subterrains,
                rule.zoneFlags,
                rule.autoGenerated
        );
    }

    private static boolean isMountainTerrainKey(String key) {
        return key.startsWith("mountains") || "dolomites".equals(key) || "torridonian".equals(key)
                || "island_mountains".equals(key);
    }

    private static float chanceOnTerrain(BiomeRule rule, String terrainName) {
        String concrete = terrainName.toLowerCase(Locale.ROOT);
        Set<String> aliases = TerrainGroup.aliasesForEngineName(concrete);
        float best = 0.0F;
        for (Map.Entry<String, Float> e : rule.terrains.entrySet()) {
            Set<String> expanded = new HashSet<>(TerrainGroup.expand(List.of(e.getKey())));
            expanded.add(e.getKey().toLowerCase(Locale.ROOT));
            for (String alias : aliases) {
                if (expanded.contains(alias)) {
                    best = Math.max(best, e.getValue());
                    break;
                }
            }
        }
        return best;
    }

    private static int relatedScore(BiomeRule rule, String terrain) {
        if (rule == null) {
            return 0;
        }
        int score = 0;
        String t = terrain.toLowerCase(Locale.ROOT);
        if (t.startsWith("mountain") || "torridonian".equals(t) || "dolomites".equals(t)) {
            if (rule.terrains.containsKey("hills_1") || rule.terrains.containsKey("hills_2")) {
                score += 3;
            }
            if (rule.terrains.containsKey("plateau")) {
                score += 2;
            }
            if (rule.terrains.containsKey("plains")) {
                score += 1;
            }
        } else if (t.startsWith("hills") || "plateau".equals(t)) {
            if (rule.terrains.containsKey("plains") || rule.terrains.containsKey("dales")) {
                score += 2;
            }
            if (rule.terrains.containsKey("mountains_1")) {
                score += 1;
            }
        } else if ("plains".equals(t) || "steppe".equals(t) || "dales".equals(t)) {
            if (rule.terrains.containsKey("hills_1")) {
                score += 1;
            }
        } else if ("badlands".equals(t)) {
            if (rule.terrains.containsKey("steppe") || rule.terrains.containsKey("plateau")) {
                score += 2;
            }
        }
        return score;
    }

    /** Volcano/beach/river-only rules should not be force-expanded onto every landform. */
    private static boolean isSpecializedOnly(BiomeRule rule) {
        if (rule == null || !rule.hasTerrains()) {
            return true;
        }
        if (rule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO)
                || rule.requiresZone(BiomeRule.ZONE_NEAR_DORMANT_VOLCANO)) {
            return true;
        }
        Set<String> keys = rule.terrains.keySet();
        boolean onlyVolcano = keys.stream().allMatch(k ->
                k.equals("volcano") || k.equals("volcano_pipe") || k.equals("island_volcano"));
        boolean onlyBeach = keys.size() == 1 && keys.contains("beach");
        boolean onlyRiver = keys.size() == 1 && keys.contains("river");
        return onlyVolcano || onlyBeach || onlyRiver;
    }

    private static boolean allowsBadlands(BiomeType climate) {
        return climate == BiomeType.DESERT
                || climate == BiomeType.SAVANNA
                || climate == BiomeType.STEPPE;
    }
}
