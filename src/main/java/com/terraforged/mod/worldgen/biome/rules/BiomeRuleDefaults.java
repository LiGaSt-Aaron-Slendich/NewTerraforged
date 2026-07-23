package com.terraforged.mod.worldgen.biome.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFConfigPaths;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Bundled + player-overridable default biome rules + synonym matching.
 *
 * <p>Resolution when {@link #ENABLED}:
 * <ol>
 *   <li>config Defaults/by_id (player-edited defaults)</li>
 *   <li>classpath by_id</li>
 *   <li>config / classpath by_name (path + synonyms)</li>
 *   <li>else emergency autogen</li>
 * </ol>
 *
 * <p>Saving from the EGF editor writes both the live Biomes rule and Defaults/by_id,
 * so edits become the new default for that biome id.
 */
public final class BiomeRuleDefaults {
    public static final boolean ENABLED = true;

    private static final String CLASS_ROOT = "/defaultconfigs/NewTerraForged/Terrain/Terrain_rules/Defaults/";
    private static final Map<String, String> SYNONYM_TO_CANONICAL = new HashMap<>();
    private static final Map<String, BiomeRule> BY_NAME = new HashMap<>();
    private static final Map<String, BiomeRule> BY_ID = new HashMap<>();
    private static boolean loaded;

    private BiomeRuleDefaults() {
    }

    public static Path configRoot() {
        return FMLPaths.CONFIGDIR.get().resolve(TFConfigPaths.TERRAIN_RULES_DEFAULTS);
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!ENABLED) {
            TerraForged.LOG.info("[BiomeRules] Defaults library DISABLED (ENABLED=false) - using autogen only");
            return;
        }
        loadSynonyms();
        TerraForged.LOG.info("[BiomeRules] Defaults ENABLED - {} synonym keys, config={}", SYNONYM_TO_CANONICAL.size(), configRoot());
    }

    /** Drop caches so a freshly written default is seen on next resolve. */
    public static synchronized void invalidateCaches() {
        BY_NAME.clear();
        BY_ID.clear();
    }

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

        String canonical = resolveCanonical(path);
        if (canonical != null) {
            BiomeRule named = loadByName(canonical);
            if (named != null) {
                return Optional.of(retarget(named, id));
            }
        }
        return Optional.empty();
    }

    /**
     * Prefer terrain-form synonyms (river, hills, …) over climate adjectives (frozen→tundra).
     * For {@code X_of_Y}, prefer the left side (land_of_rivers → land/plains, not rivers).
     */
    private static String resolveCanonical(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String direct = SYNONYM_TO_CANONICAL.get(path);
        if (direct != null) {
            return direct;
        }
        String[] tokens = path.split("[_/\\-]+");
        int of = -1;
        for (int i = 0; i < tokens.length; i++) {
            if ("of".equals(tokens[i])) {
                of = i;
                break;
            }
        }
        String[] primary = of > 0 ? java.util.Arrays.copyOfRange(tokens, 0, of) : tokens;
        String[] secondary = of >= 0 && of + 1 < tokens.length
                ? java.util.Arrays.copyOfRange(tokens, of + 1, tokens.length)
                : new String[0];

        String best = bestFormSynonym(primary);
        if (best != null) {
            return best;
        }
        best = bestFormSynonym(secondary);
        if (best != null) {
            return best;
        }
        // Last resort: first matching token anywhere (climate-ish synonyms).
        for (String token : tokens) {
            String c = SYNONYM_TO_CANONICAL.get(token);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static final String[] FORM_CANONICAL_PRIORITY = {
            "river", "beach", "volcanic_crater", "volcano", "swamp", "badlands", "mountains", "hills", "plateau", "steppe", "desert", "jungle",
            "taiga", "tundra", "plains"
    };

    private static String bestFormSynonym(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return null;
        }
        java.util.Set<String> hits = new java.util.HashSet<>();
        for (String token : tokens) {
            String c = SYNONYM_TO_CANONICAL.get(token);
            if (c != null) {
                hits.add(c);
            }
        }
        for (String prefer : FORM_CANONICAL_PRIORITY) {
            if (hits.contains(prefer)) {
                return prefer;
            }
        }
        return hits.isEmpty() ? null : hits.iterator().next();
    }

    /** Persist editor save into config Defaults/by_id so it becomes the default for this biome. */
    public static void savePlayerDefault(ResourceLocation id, BiomeRule rule) throws java.io.IOException {
        if (id == null || rule == null) {
            return;
        }
        ensureLoaded();
        Path file = configRoot().resolve("by_id").resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        BiomeRule asDefault = new BiomeRule(
                id.toString(),
                rule.canBeOnSlope,
                rule.climateTags,
                rule.terrains,
                rule.subterrains,
                rule.zoneFlags,
                false
        );
        BiomeRuleIO.write(file, asDefault);
        BY_ID.put(id.toString().toLowerCase(Locale.ROOT), asDefault);
        TerraForged.LOG.info("[BiomeRules] default updated {}", file);
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
                template.zoneFlags,
                false
        );
    }

    private static void loadSynonyms() {
        // Prefer config override, else classpath.
        Path configSyn = configRoot().resolve("synonyms.json");
        if (Files.isRegularFile(configSyn)) {
            try {
                String raw = Files.readString(configSyn, StandardCharsets.UTF_8);
                parseSynonyms(JsonParser.parseString(stripBom(raw)).getAsJsonObject());
                return;
            } catch (Exception e) {
                TerraForged.LOG.warn("[BiomeRules] failed config synonyms.json: {}", e.toString());
            }
        }
        String resource = CLASS_ROOT + "synonyms.json";
        try (InputStream in = BiomeRuleDefaults.class.getResourceAsStream(resource)) {
            if (in == null) {
                TerraForged.LOG.debug("[BiomeRules] no synonyms.json at {}", resource);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            parseSynonyms(root);
        } catch (Exception e) {
            TerraForged.LOG.warn("[BiomeRules] failed to load synonyms.json: {}", e.toString());
        }
    }

    private static void parseSynonyms(JsonObject root) {
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
    }

    private static BiomeRule loadByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (BY_NAME.containsKey(key)) {
            return BY_NAME.get(key);
        }
        BiomeRule rule = readConfigRule(configRoot().resolve("by_name").resolve(key + ".json"));
        if (rule == null) {
            rule = readClasspathRule(CLASS_ROOT + "by_name/" + key + ".json");
        }
        BY_NAME.put(key, rule);
        return rule;
    }

    private static BiomeRule loadById(ResourceLocation id) {
        String key = id.toString().toLowerCase(Locale.ROOT);
        if (BY_ID.containsKey(key)) {
            return BY_ID.get(key);
        }
        BiomeRule rule = readConfigRule(configRoot().resolve("by_id").resolve(id.getNamespace()).resolve(id.getPath() + ".json"));
        if (rule == null) {
            rule = readClasspathRule(CLASS_ROOT + "by_id/" + id.getNamespace() + "/" + id.getPath() + ".json");
        }
        BY_ID.put(key, rule);
        return rule;
    }

    private static BiomeRule readConfigRule(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        BiomeRuleIO.LoadResult lr = BiomeRuleIO.load(file);
        if (!lr.ok()) {
            // Defaults may be body-only JSON without ASM header.
            try {
                String raw = stripBom(Files.readString(file, StandardCharsets.UTF_8)).trim();
                if (raw.startsWith("{")) {
                    lr = BiomeRuleIO.parseBody(JsonParser.parseString(raw).getAsJsonObject());
                } else if (raw.startsWith("#")) {
                    String[] parts = raw.split("\\R", 2);
                    if (parts.length >= 2) {
                        lr = BiomeRuleIO.parseBody(JsonParser.parseString(parts[1].trim()).getAsJsonObject());
                    }
                }
            } catch (Exception e) {
                TerraForged.LOG.warn("[BiomeRules] bad config default {}: {}", file, e.toString());
                return null;
            }
        }
        if (!lr.ok()) {
            TerraForged.LOG.warn("[BiomeRules] bad config default {}: {}", file, lr.error());
            return null;
        }
        return lr.rule();
    }

    private static BiomeRule readClasspathRule(String resource) {
        try (InputStream in = BiomeRuleDefaults.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            String raw = stripBom(new String(in.readAllBytes(), StandardCharsets.UTF_8)).trim();
            String body = raw;
            if (raw.startsWith("#")) {
                String[] parts = raw.split("\\R", 2);
                if (parts.length < 2) {
                    return null;
                }
                try {
                    BiomeRuleHeader.parseOrThrow(parts[0]);
                } catch (IllegalArgumentException ignored) {
                    if (!parts[0].startsWith("#")) {
                        return null;
                    }
                }
                body = parts[1];
            }
            BiomeRuleIO.LoadResult lr = BiomeRuleIO.parseBody(JsonParser.parseString(body.trim()).getAsJsonObject());
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

    private static String stripBom(String raw) {
        if (raw != null && !raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            return raw.substring(1);
        }
        return raw == null ? "" : raw;
    }

    public static Map<String, String> synonymIndex() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(SYNONYM_TO_CANONICAL));
    }
}
