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
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Loads / autogens per-biome JSON rules and answers candidate queries.
 * Primary sync: game launch ({@link #syncAtGameLaunch}). World registry sync fills any late biomes.
 */
public final class BiomeRuleRegistry {
    private static final Map<ResourceLocation, BiomeRule> RULES = new HashMap<>();
    private static volatile boolean synced;

    private BiomeRuleRegistry() {
    }

    public static Path biomesRoot() {
        return FMLPaths.CONFIGDIR.get().resolve(TFConfigPaths.TERRAIN_RULES_BIOMES);
    }

    /** After registries freeze — generates missing JSON under Terrain_rules/Biomes. */
    public static synchronized void syncAtGameLaunch() {
        Path root = biomesRoot();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            TerraForged.LOG.error("[BiomeRules] cannot create {}", root, e);
            return;
        }
        int created = 0;
        int loaded = 0;
        int repaired = 0;
        int skipped = 0;
        try {
            for (Map.Entry<ResourceKey<Biome>, Biome> entry : ForgeRegistries.BIOMES.getEntries()) {
                ResourceLocation id = entry.getKey().location();
                Biome biome = entry.getValue();
                if (shouldSkipSurface(id, biome)) {
                    removeIfPresent(id);
                    skipped++;
                    continue;
                }
                int r = ensureRule(id, biome);
                if (r == 1) {
                    loaded++;
                } else if (r == 2) {
                    created++;
                } else if (r == 3) {
                    repaired++;
                }
            }
        } catch (Throwable t) {
            TerraForged.LOG.error("[BiomeRules] syncAtGameLaunch failed", t);
            return;
        }
        synced = !RULES.isEmpty();
        TerraForged.LOG.info(
                "[BiomeRules] game-launch sync: {} rules (loaded {}, created {}, repaired {}, skipped {}) → {}",
                RULES.size(), loaded, created, repaired, skipped, root);
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
        int created = 0;
        int loaded = 0;
        int repaired = 0;
        try {
            for (Holder<Biome> ref : BiomeUtil.getOverworldBiomes(biomes)) {
                ResourceLocation id = ref.unwrapKey().map(ResourceKey::location).orElse(null);
                if (id == null || shouldSkipSurface(id, ref.value())) {
                    if (id != null) {
                        removeIfPresent(id);
                    }
                    continue;
                }
                int r = ensureRule(id, ref.value());
                if (r == 1) {
                    loaded++;
                } else if (r == 2) {
                    created++;
                } else if (r == 3) {
                    repaired++;
                }
            }
        } catch (Throwable t) {
            TerraForged.LOG.error("[BiomeRules] world registry sync failed", t);
            // Keep whatever game-launch already produced.
            synced = !RULES.isEmpty() || synced;
            return;
        }
        synced = true;
        TerraForged.LOG.info(
                "[BiomeRules] world sync: {} rules total (pass loaded {}, created {}, repaired {})",
                RULES.size(), loaded, created, repaired);
    }

    /**
     * @return 1 loaded existing, 2 created new, 3 repaired broken/stale, 0 failed
     */
    private static int ensureRule(ResourceLocation id, Biome biome) {
        Path file = biomesRoot().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        BiomeRule rule = null;
        int kind = 0;
        if (Files.isRegularFile(file)) {
            BiomeRuleIO.LoadResult lr = BiomeRuleIO.load(file);
            if (lr.ok() && !isStaleAutogen(id, lr.rule())) {
                rule = lr.rule();
                kind = 1;
            } else {
                String reason = lr.ok() ? "stale auto_generated rule (classification/volcano fix)" : lr.error();
                try {
                    BiomeRuleIO.quarantineBroken(file, reason);
                    TerraForged.LOG.warn("[BiomeRules] quarantined {} — {}", file.getFileName(), reason);
                } catch (IOException e) {
                    TerraForged.LOG.error("[BiomeRules] failed to quarantine {}", file, e);
                }
                rule = createNewRule(id, biome);
                try {
                    BiomeRuleIO.write(file, rule);
                    kind = 3;
                } catch (IOException e) {
                    TerraForged.LOG.error("[BiomeRules] failed to rewrite {}", file, e);
                    return 0;
                }
            }
        } else {
            rule = createNewRule(id, biome);
            try {
                BiomeRuleIO.write(file, rule);
                kind = 2;
            } catch (IOException e) {
                TerraForged.LOG.error("[BiomeRules] failed to write {}", file, e);
                return 0;
            }
        }
        if (rule != null) {
            RULES.put(id, rule);
        }
        return kind;
    }

    /**
     * New file body: curated defaults (when ENABLED) → else emergency autogen.
     */
    private static BiomeRule createNewRule(ResourceLocation id, Biome biome) {
        BiomeRuleDefaults.ensureLoaded();
        return BiomeRuleDefaults.tryCopyFor(id).orElseGet(() -> BiomeRuleAutogen.generate(id, biome));
    }

    /** One-shot rewrite for known bad auto templates (volcano wiring, heights→plains, prairie→plains). */
    private static boolean isStaleAutogen(ResourceLocation id, BiomeRule rule) {
        if (rule == null || !rule.autoGenerated) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        boolean volcanicName = path.contains("volcan") || path.contains("caldera") || path.contains("magma_wastes")
                || path.contains("basalt_deltas") || path.contains("ashen");
        if (volcanicName && (!rule.terrains.containsKey("volcano") || !rule.terrains.containsKey("volcano_pipe"))) {
            return true;
        }
        // heights / uplands were falling into FLAT (plains+steppe) — should be hills.
        if (tokenIn(path, "height", "heights", "upland", "uplands")
                && rule.terrains.containsKey("plains")
                && !rule.terrains.containsKey("hills_1")) {
            return true;
        }
        // prairie must be steppe-only (no plains).
        if (tokenIn(path, "prairie", "prairies") && rule.terrains.containsKey("plains")) {
            return true;
        }
        return false;
    }

    private static boolean tokenIn(String path, String... tokens) {
        for (String t : path.split("[_/\\-]+")) {
            for (String want : tokens) {
                if (t.equals(want)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void removeIfPresent(ResourceLocation id) {
        RULES.remove(id);
        Path file = biomesRoot().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            BiomeRuleIO.quarantineBroken(file, "not a surface biome (cave/nether/end/ocean) — removed from surface rules");
            TerraForged.LOG.info("[BiomeRules] removed non-surface rule {}", id);
        } catch (IOException e) {
            TerraForged.LOG.warn("[BiomeRules] could not remove {}: {}", id, e.toString());
        }
    }

    private static boolean shouldSkipSurface(ResourceLocation id, Biome biome) {
        if (id == null) {
            return true;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        // Nested datapack cave ids: terralith:cave/...
        if (path.startsWith("cave/") || path.contains("/cave/") || path.contains("/caves/")) {
            return true;
        }
        // Rivers are included (editable surface-adjacent rules). Oceans / caves / dimensions stay skipped.
        if (path.contains("ocean")
                || path.equals("beach")
                || path.contains("stony_shore")
                || path.contains("snowy_beach")
                || path.endsWith("_caves")
                || path.endsWith("_caverns")
                || path.contains("_cave_")
                || path.contains("cave")
                || path.contains("cavern")
                || path.contains("grotto")
                || path.contains("deep_dark")
                || path.contains("nether")
                || path.contains("the_end")
                || path.startsWith("end_")) {
            return true;
        }
        try {
            BiomeCategory cat = Biome.getBiomeCategory(Holder.direct(biome));
            if (cat == BiomeCategory.UNDERGROUND
                    || cat == BiomeCategory.NETHER
                    || cat == BiomeCategory.THEEND) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, id);
        try {
            if (BiomeDictionary.hasType(key, Type.NETHER)
                    || BiomeDictionary.hasType(key, Type.END)
                    || BiomeDictionary.hasType(key, Type.VOID)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
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

    public static float matchChance(BiomeRule rule, String terrainName, String subterrain, boolean steepSlope) {
        return matchChance(rule, terrainName, subterrain, steepSlope, new ZoneContext(false));
    }

    public static float matchChance(
            BiomeRule rule, String terrainName, String subterrain, boolean steepSlope, ZoneContext zone
    ) {
        if (rule == null || !rule.hasTerrains()) {
            return 0.0F;
        }
        if (steepSlope && !rule.canBeOnSlope) {
            return 0.0F;
        }
        // Zone flags bind to a local footprint, not an entire terrain type.
        if (rule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO)) {
            if (zone == null || !zone.nearActiveVolcano) {
                return 0.0F;
            }
            BiomeRule.ZoneFlag z = rule.zone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
            if (z != null && z.chance() <= 0.0F) {
                return 0.0F;
            }
        }
        float terrainChance = chanceOnTerrain(rule, terrainName);
        if (terrainChance <= 0.0F) {
            return 0.0F;
        }
        if (rule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO)) {
            BiomeRule.ZoneFlag z = rule.zone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
            if (z != null) {
                terrainChance *= Math.max(0.0F, z.chance());
            }
        }
        if (rule.subterrains.isEmpty()) {
            return terrainChance;
        }
        if (subterrain == null || subterrain.isBlank() || SubterrainResolver.NONE.equals(subterrain)) {
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

    public static int countMatching(Iterable<Holder<Biome>> biomes, String terrain, String subterrain, boolean steep) {
        return countMatching(biomes, terrain, subterrain, steep, new ZoneContext(false));
    }

    public static int countMatching(
            Iterable<Holder<Biome>> biomes, String terrain, String subterrain, boolean steep, ZoneContext zone
    ) {
        int n = 0;
        for (Holder<Biome> h : biomes) {
            if (h == null) {
                continue;
            }
            ResourceLocation id = h.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null) {
                continue;
            }
            if (matchChance(get(id), terrain, subterrain, steep, zone) > 0.0F) {
                n++;
            }
        }
        return n;
    }
}
