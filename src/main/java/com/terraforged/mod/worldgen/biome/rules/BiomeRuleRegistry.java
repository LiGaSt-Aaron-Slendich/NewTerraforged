package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFConfigPaths;
import com.terraforged.mod.worldgen.biome.terrain.TerrainGroup;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Loads / autogens per-biome JSON rules and answers candidate queries.
 * Sync runs when a biome registry is available (world/Source setup) — not every create-world.
 */
public final class BiomeRuleRegistry {
    private static final Map<ResourceLocation, BiomeRule> RULES = new HashMap<>();
    private static volatile boolean synced;

    private BiomeRuleRegistry() {
    }

    public static Path biomesRoot() {
        return FMLPaths.CONFIGDIR.get().resolve(TFConfigPaths.TERRAIN_RULES_BIOMES);
    }

    public static synchronized void sync(Registry<Biome> biomes) {
        if (biomes == null) {
            return;
        }
        Path root = biomesRoot();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            TerraForged.LOG.error("[BiomeRules] cannot create {}", root, e);
            return;
        }
        RULES.clear();
        int created = 0;
        int loaded = 0;
        int repaired = 0;
        for (Holder<Biome> ref : BiomeUtil.getOverworldBiomes(biomes)) {
            ResourceLocation id = ref.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null) {
                continue;
            }
            String path = id.getPath();
            if (path.contains("ocean") || path.contains("river") || path.equals("beach") || path.contains("stony_shore") || path.contains("snowy_beach")) {
                continue;
            }
            Path file = root.resolve(id.getNamespace()).resolve(id.getPath() + ".json");
            BiomeRule rule = null;
            if (Files.isRegularFile(file)) {
                BiomeRuleIO.LoadResult lr = BiomeRuleIO.load(file);
                if (lr.ok()) {
                    rule = lr.rule();
                    loaded++;
                } else {
                    try {
                        BiomeRuleIO.quarantineBroken(file, lr.error());
                        TerraForged.LOG.warn("[BiomeRules] quarantined {} — {}", file.getFileName(), lr.error());
                    } catch (IOException e) {
                        TerraForged.LOG.error("[BiomeRules] failed to quarantine {}", file, e);
                    }
                    rule = BiomeRuleAutogen.generate(id, ref.value());
                    try {
                        BiomeRuleIO.write(file, rule);
                        repaired++;
                    } catch (IOException e) {
                        TerraForged.LOG.error("[BiomeRules] failed to rewrite {}", file, e);
                    }
                }
            } else {
                rule = BiomeRuleAutogen.generate(id, ref.value());
                try {
                    BiomeRuleIO.write(file, rule);
                    created++;
                } catch (IOException e) {
                    TerraForged.LOG.error("[BiomeRules] failed to write {}", file, e);
                }
            }
            if (rule != null) {
                RULES.put(id, rule);
            }
        }
        synced = true;
        TerraForged.LOG.info("[BiomeRules] sync complete: {} rules (loaded {}, created {}, repaired {})", RULES.size(), loaded, created, repaired);
    }

    public static boolean isSynced() {
        return synced;
    }

    public static BiomeRule get(ResourceLocation id) {
        return id == null ? null : RULES.get(id);
    }

    public static Map<ResourceLocation, BiomeRule> snapshot() {
        return Collections.unmodifiableMap(RULES);
    }

    /** Effective chance for this biome on the concrete terrain + optional subterrain. */
    public static float matchChance(BiomeRule rule, String terrainName, String subterrain, boolean steepSlope) {
        if (rule == null || !rule.hasTerrains()) {
            return 0.0F;
        }
        if (steepSlope && !rule.canBeOnSlope) {
            return 0.0F;
        }
        float terrainChance = chanceOnTerrain(rule, terrainName);
        if (terrainChance <= 0.0F) {
            return 0.0F;
        }
        if (rule.subterrains.isEmpty()) {
            return terrainChance;
        }
        if (subterrain == null || subterrain.isBlank() || SubterrainResolver.NONE.equals(subterrain)) {
            // Subterrain layer inactive → do not require subterrain keys.
            return terrainChance;
        }
        float sub = rule.subterrainChance(subterrain);
        if (sub <= 0.0F) {
            return 0.0F;
        }
        return terrainChance * sub;
    }

    private static float chanceOnTerrain(BiomeRule rule, String terrainName) {
        if (terrainName == null || terrainName.isBlank()) {
            return 0.0F;
        }
        String concrete = terrainName.toLowerCase(Locale.ROOT);
        float best = 0.0F;
        for (Map.Entry<String, Float> e : rule.terrains.entrySet()) {
            Set<String> expanded = new HashSet<>(TerrainGroup.expand(java.util.List.of(e.getKey())));
            expanded.add(e.getKey().toLowerCase(Locale.ROOT));
            if (expanded.contains(concrete)) {
                best = Math.max(best, e.getValue());
            }
        }
        return best;
    }

    /** Count how many of the given biomes allow terrain+subterrain (for conditional subterrains). */
    public static int countMatching(Iterable<Holder<Biome>> biomes, String terrain, String subterrain, boolean steep) {
        int n = 0;
        for (Holder<Biome> h : biomes) {
            if (h == null) {
                continue;
            }
            ResourceLocation id = h.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null) {
                continue;
            }
            if (matchChance(get(id), terrain, subterrain, steep) > 0.0F) {
                n++;
            }
        }
        return n;
    }
}
