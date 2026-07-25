package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import com.terraforged.mod.worldgen.biome.rules.ZoneContext;
import com.terraforged.mod.worldgen.biome.terrain.TerrainGroup;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Climate-first candidate builder: terrain rules prefer, but never leave a climate
 * cell with an empty (or tiny) pick set. Guarantees up to {@link #MIN_CANDIDATES}
 * biomes from the climate WeightMap whenever the pool is large enough.
 */
public final class ClimateTerrainCandidates {
    /** Minimum biome options for a climate ∩ terrain cell when the climate pool allows it. */
    public static final int MIN_CANDIDATES = 3;

    private static final float SOFT_WEIGHT = 0.40F;
    private static final float CLIMATE_FILL_WEIGHT = 0.18F;

    private ClimateTerrainCandidates() {
    }

    public record Entry(Holder<Biome> holder, ResourceLocation id, float weight, String tier) {
    }

    public record Result(List<Entry> accepted, List<String> rejected) {
    }

    public static Result collect(
            Iterable<Holder<Biome>> climatePool,
            String terrain,
            String subterrain,
            ZoneContext zone,
            boolean onDormantVolcano
    ) {
        LinkedHashMap<ResourceLocation, Entry> accepted = new LinkedHashMap<>();
        List<String> rejected = new ArrayList<>();

        // Pass 0 — strict terrain/sub/zone match.
        scan(climatePool, terrain, subterrain, zone, onDormantVolcano, accepted, rejected, 0);
        // Pass 1 — soft landform family (climate keeps priority; terrain is advisory).
        if (accepted.size() < MIN_CANDIDATES) {
            scan(climatePool, terrain, subterrain, zone, onDormantVolcano, accepted, rejected, 1);
        }
        // Pass 2 — climate fill: any zone-safe biome from this climate pool.
        if (accepted.size() < MIN_CANDIDATES) {
            scan(climatePool, terrain, subterrain, zone, onDormantVolcano, accepted, rejected, 2);
        }

        return new Result(new ArrayList<>(accepted.values()), rejected);
    }

    private static void scan(
            Iterable<Holder<Biome>> climatePool,
            String terrain,
            String subterrain,
            ZoneContext zone,
            boolean onDormantVolcano,
            LinkedHashMap<ResourceLocation, Entry> accepted,
            List<String> rejected,
            int pass
    ) {
        if (climatePool == null) {
            return;
        }
        for (Holder<Biome> holder : climatePool) {
            if (holder == null) {
                continue;
            }
            ResourceLocation id = biomeId(holder);
            if (id == null || accepted.containsKey(id)) {
                continue;
            }
            if (CaveBiomeIds.isUndergroundBiome(id)) {
                if (pass == 0) {
                    rejected.add(id + " → underground biome");
                }
                continue;
            }
            BiomeRule rule = BiomeRuleRegistry.get(id);
            if (rule == null) {
                if (pass == 0) {
                    accepted.put(id, new Entry(holder, id, 1.0F, "no-rule"));
                }
                continue;
            }

            float chance;
            String tier;
            if (pass == 0) {
                chance = BiomeRuleRegistry.matchChance(rule, terrain, subterrain, false, zone);
                if (chance <= 0.0F && onDormantVolcano) {
                    chance = dormantConeChance(rule, subterrain, zone);
                }
                tier = "strict";
            } else if (pass == 1) {
                chance = softLandformChance(rule, terrain, zone);
                tier = "soft-family";
            } else {
                chance = climateFillChance(rule, zone);
                tier = "climate-fill";
            }

            if (chance <= 0.0F) {
                if (pass == 0) {
                    String why = BiomeRuleRegistry.rejectReason(rule, terrain, subterrain, zone);
                    if (why != null && why.startsWith("subterrain")) {
                        rejected.add(id + " → TERRAIN_OK but " + why);
                    } else {
                        rejected.add(id + " → " + (why != null ? why : "chance=0"));
                    }
                }
                continue;
            }

            float weight = chance;
            if (pass == 1) {
                weight = chance * SOFT_WEIGHT;
            } else if (pass == 2) {
                weight = CLIMATE_FILL_WEIGHT;
            }
            accepted.put(id, new Entry(holder, id, Math.max(0.01F, weight), tier));
        }
    }

    /**
     * Soft landform match: ignore subterrains; accept related hills/plains/mountain rules
     * so temperate forests can still paint mountain cells from their climate pool.
     */
    public static float softLandformChance(BiomeRule rule, String terrain, ZoneContext zone) {
        if (rule == null || !rule.hasTerrains()) {
            return 0.0F;
        }
        if (zoneBlocked(rule, zone)) {
            return 0.0F;
        }
        Set<String> family = softFamily(terrain);
        if (family.isEmpty()) {
            return 0.0F;
        }
        float best = 0.0F;
        for (Map.Entry<String, Float> e : rule.terrains.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0.0F) {
                continue;
            }
            Set<String> expanded = new HashSet<>(TerrainGroup.expand(List.of(e.getKey())));
            expanded.add(e.getKey().toLowerCase(Locale.ROOT));
            for (String key : expanded) {
                if (family.contains(key)) {
                    best = Math.max(best, e.getValue());
                    break;
                }
            }
        }
        return best;
    }

    /** Zone-safe only — climate wins when terrain rules would empty the pool. */
    public static float climateFillChance(BiomeRule rule, ZoneContext zone) {
        if (rule == null) {
            return 1.0F;
        }
        if (zoneBlocked(rule, zone)) {
            return 0.0F;
        }
        return 1.0F;
    }

    private static boolean zoneBlocked(BiomeRule rule, ZoneContext zone) {
        boolean needsActive = rule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
        boolean needsDormant = rule.requiresZone(BiomeRule.ZONE_NEAR_DORMANT_VOLCANO);
        if (needsActive && (zone == null || !zone.nearAnyVolcano())) {
            return true;
        }
        if (needsDormant && (zone == null || !(zone.nearDormantVolcano || zone.onDormantVolcano))) {
            return true;
        }
        if (zone != null && zone.onActiveVolcano && !isActiveVolcanoBiome(rule)) {
            return true;
        }
        if (zone != null && zone.nearActiveVolcano && !zone.onActiveVolcano && !needsActive) {
            return true;
        }
        return false;
    }

    private static boolean isActiveVolcanoBiome(BiomeRule rule) {
        if (rule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO)) {
            return true;
        }
        if (rule.climateTags.contains("volcanic")) {
            return true;
        }
        return rule.terrains.containsKey("volcano")
                || rule.terrains.containsKey("volcano_pipe")
                || rule.terrains.containsKey("island_volcano");
    }

    /**
     * Related landforms that may soft-accept biomes for a painted engine terrain.
     * Mountain cells intentionally include plains/hills so climate pools like
     * TEMPERATE_FOREST still yield candidates.
     */
    public static Set<String> softFamily(String terrain) {
        if (terrain == null || terrain.isBlank() || "none".equalsIgnoreCase(terrain)) {
            return Set.of();
        }
        String key = terrain.trim().toLowerCase(Locale.ROOT);
        LinkedHashSet<String> out = new LinkedHashSet<>(TerrainGroup.aliasesForEngineName(key));

        if (isMountainish(key)) {
            addAll(out, "mountains", "mountains_1", "mountains_2", "mountains_3",
                    "mountains_ridge_1", "mountains_ridge_2", "dolomites", "torridonian",
                    "plateau", "hills", "hills_1", "hills_2", "plains", "steppe", "dales",
                    "island_mountains", "island_hills", "island_plateau");
        } else if (isHillish(key)) {
            addAll(out, "hills", "hills_1", "hills_2", "torridonian", "plateau",
                    "plains", "steppe", "dales", "mountains_1", "mountains_2",
                    "island_hills", "island_plateau");
        } else if (isFlatish(key)) {
            addAll(out, "plains", "steppe", "dales", "hills_1", "hills_2", "island_flats");
        } else if ("dales".equals(key)) {
            addAll(out, "dales", "plains", "steppe", "hills_1");
        } else if ("badlands".equals(key)) {
            addAll(out, "badlands", "plateau", "hills_1", "steppe", "plains");
        } else if (key.contains("volcano")) {
            addAll(out, "volcano", "volcano_pipe", "island_volcano", "hills_1", "mountains_1", "plains");
        } else if ("beach".equals(key) || "river".equals(key) || "laguna".equals(key)) {
            // Keep soft family narrow for linear/coast features.
            out.add(key);
            if ("river".equals(key)) {
                addAll(out, "plains", "dales");
            }
        } else {
            // Unknown terrain — allow broad land soft-match so climate still paints.
            addAll(out, "plains", "steppe", "dales", "hills_1", "hills_2", "plateau",
                    "mountains_1", "mountains_2", "torridonian");
        }
        return out;
    }

    private static boolean isMountainish(String key) {
        return key.startsWith("mountain")
                || "dolomites".equals(key)
                || "torridonian".equals(key)
                || "island_mountains".equals(key);
    }

    private static boolean isHillish(String key) {
        return key.startsWith("hills") || "plateau".equals(key) || "island_hills".equals(key)
                || "island_plateau".equals(key);
    }

    private static boolean isFlatish(String key) {
        return "plains".equals(key) || "steppe".equals(key) || "flats".equals(key)
                || "island_flats".equals(key);
    }

    private static void addAll(Set<String> out, String... keys) {
        for (String k : keys) {
            out.add(k);
            out.addAll(TerrainGroup.expand(List.of(k)));
        }
    }

    private static float dormantConeChance(BiomeRule rule, String sub, ZoneContext zone) {
        String[] alts = {"hills_1", "hills_2", "mountains_1", "mountains_2", "plateau", "plains", "steppe"};
        float best = 0.0F;
        for (String alt : alts) {
            best = Math.max(best, BiomeRuleRegistry.matchChance(rule, alt, sub, false, zone));
        }
        return best;
    }

    private static ResourceLocation biomeId(Holder<Biome> biome) {
        if (biome == null) {
            return null;
        }
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }
}
