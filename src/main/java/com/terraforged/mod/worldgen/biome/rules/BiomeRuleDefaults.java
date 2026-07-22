package com.terraforged.mod.worldgen.biome.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.terraforged.mod.TerraForged;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Bundled default biome rules + synonym matching.
 *
 * <p><b>DISABLED by default</b> ({@link #ENABLED} = false) until curated defaults are filled in.
 * When enabled, missing player rules are copied from classpath defaults before falling back to autogen.
 *
 * <p>Classpath layout:
 * <pre>
 * /defaultconfigs/NewTerraForged/Terrain/Terrain_rules/Defaults/
 *   synonyms.json          — canonical name → [synonym paths…]
 *   by_name/{name}.json    — rule body (ASM header optional) keyed by canonical / path name
 *   by_id/{ns}/{path}.json — exact mod:biome override (optional)
 * </pre>
 */
public final class BiomeRuleDefaults {
    /**
     * Master switch. Keep false until we author Defaults/by_name together.
     * Autogen remains the emergency path for unknown mods.
     */
    public static final boolean ENABLED = false;

    private static final String ROOT = "/defaultconfigs/NewTerraForged/Terrain/Terrain_rules/Defaults/";
    private static final Map<String, String> SYNONYM_TO_CANONICAL = new HashMap<>();
    private static final Map<String, BiomeRule> BY_NAME = new HashMap<>();
    private static final Map<String, BiomeRule> BY_ID = new HashMap<>();
    private static boolean loaded;

    private BiomeRuleDefaults() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!ENABLED) {
            TerraForged.LOG.info("[BiomeRules] Defaults library present but DISABLED (ENABLED=false) — using autogen only");
            return;
        }
        loadSynonyms();
        // by_name / by_id are loaded lazily on resolve to keep startup light; index synonym keys only.
        TerraForged.LOG.info("[BiomeRules] Defaults ENABLED — {} synonym keys", SYNONYM_TO_CANONICAL.size());
    }

    /**
     * Try to resolve a default template for this biome id.
     * Returns empty when disabled or no match.
     */
    public static Optional<BiomeRule> tryCopyFor(ResourceLocation biomeId) {
        ensureLoaded();
        if (!ENABLED || biomeId == null) {
            return Optional.empty();
        }
        String id = biomeId.toString().toLowerCase(Locale.ROOT);
        String path = biomeId.getPath().toLowerCase(Locale.ROOT);

        BiomeRule exact = loadById(biomeId);
        if (exact != null) {
            return Optional.of(retarget(exact, id));
        }

        BiomeRule byPath = loadByName(path);
        if (byPath != null) {
            return Optional.of(retarget(byPath, id));
        }

        String canonical = SYNONYM_TO_CANONICAL.get(path);
        if (canonical == null) {
            // token-level synonym: any path token maps to a canonical template
            for (String token : path.split("[_/\\-]+")) {
                canonical = SYNONYM_TO_CANONICAL.get(token);
                if (canonical != null) {
                    break;
                }
            }
        }
        if (canonical != null) {
            BiomeRule named = loadByName(canonical);
            if (named != null) {
                return Optional.of(retarget(named, id));
            }
        }
        return Optional.empty();
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    private static BiomeRule retarget(BiomeRule template, String biomeId) {
        return new BiomeRule(
                biomeId,
                template.canBeOnSlope,
                template.climateTags,
                template.terrains,
                template.subterrains,
                false // copied from curated default — not emergency autogen
        );
    }

    private static void loadSynonyms() {
        String resource = ROOT + "synonyms.json";
        try (InputStream in = BiomeRuleDefaults.class.getResourceAsStream(resource)) {
            if (in == null) {
                TerraForged.LOG.debug("[BiomeRules] no synonyms.json at {}", resource);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String canonical = e.getKey().toLowerCase(Locale.ROOT);
                if (canonical.startsWith("_")) {
                    continue;
                }
                SYNONYM_TO_CANONICAL.put(canonical, canonical);
                if (!e.getValue().isJsonArray()) {
                    continue;
                }
                JsonArray arr = e.getValue().getAsJsonArray();
                for (JsonElement el : arr) {
                    if (el.isJsonPrimitive()) {
                        String syn = el.getAsString().toLowerCase(Locale.ROOT).trim();
                        if (!syn.isEmpty()) {
                            SYNONYM_TO_CANONICAL.put(syn, canonical);
                        }
                    }
                }
            }
        } catch (Exception e) {
            TerraForged.LOG.warn("[BiomeRules] failed to load synonyms.json: {}", e.toString());
        }
    }

    private static BiomeRule loadByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (BY_NAME.containsKey(key)) {
            return BY_NAME.get(key);
        }
        BiomeRule rule = readClasspathRule(ROOT + "by_name/" + key + ".json");
        BY_NAME.put(key, rule); // may store null to avoid re-hit
        return rule;
    }

    private static BiomeRule loadById(ResourceLocation id) {
        String key = id.toString().toLowerCase(Locale.ROOT);
        if (BY_ID.containsKey(key)) {
            return BY_ID.get(key);
        }
        String resource = ROOT + "by_id/" + id.getNamespace() + "/" + id.getPath() + ".json";
        BiomeRule rule = readClasspathRule(resource);
        BY_ID.put(key, rule);
        return rule;
    }

    private static BiomeRule readClasspathRule(String resource) {
        try (InputStream in = BiomeRuleDefaults.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
                raw = raw.substring(1);
            }
            String body = raw;
            if (raw.startsWith("#")) {
                String[] parts = raw.split("\\R", 2);
                if (parts.length < 2) {
                    return null;
                }
                // Defaults may omit ASM header; if present, validate lightly.
                try {
                    BiomeRuleHeader.parseOrThrow(parts[0]);
                } catch (IllegalArgumentException ignored) {
                    // bundled templates can use a comment line instead of live token
                    if (!parts[0].startsWith("#")) {
                        return null;
                    }
                }
                body = parts[1];
            }
            BiomeRuleIO.LoadResult lr = BiomeRuleIO.parseBody(
                    JsonParser.parseString(body.trim()).getAsJsonObject());
            if (!lr.ok()) {
                TerraForged.LOG.warn("[BiomeRules] bad default {}: {}", resource, lr.error());
                return null;
            }
            return lr.rule();
        } catch (Exception e) {
            TerraForged.LOG.warn("[BiomeRules] failed reading default {}: {}", resource, e.toString());
            return null;
        }
    }

    /** Snapshot of synonym map (canonical ← synonyms) for debugging / tooling. */
    public static Map<String, String> synonymIndex() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(SYNONYM_TO_CANONICAL));
    }
}
