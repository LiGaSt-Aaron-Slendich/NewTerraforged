package com.terraforged.mod.worldgen.cave;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Load / save per-biome cave rules (ASM header + JSON body). */
public final class CaveBiomeRuleIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CaveBiomeRuleIO() {
    }

    public record LoadResult(CaveBiomeRule rule, String error) {
        public boolean ok() {
            return this.rule != null && this.error == null;
        }
    }

    public static LoadResult load(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
                raw = raw.substring(1);
            }
            String[] lines = raw.split("\\R", 2);
            String body;
            if (lines.length >= 2 && lines[0].startsWith("#")) {
                body = lines[1].trim();
            } else {
                body = raw.trim();
            }
            if (body.isEmpty()) {
                return new LoadResult(null, "empty JSON body");
            }
            try {
                return parseBody(JsonParser.parseString(body).getAsJsonObject());
            } catch (JsonSyntaxException | IllegalStateException e) {
                return new LoadResult(null, "invalid JSON: " + e.getMessage());
            }
        } catch (IOException e) {
            return new LoadResult(null, "io error: " + e.getMessage());
        }
    }

    public static LoadResult parseBody(JsonObject obj) {
        if (!obj.has("biome") || !obj.get("biome").isJsonPrimitive()) {
            return new LoadResult(null, "missing biome");
        }
        String biome = obj.get("biome").getAsString();
        CaveBiomeCategory cat = CaveBiomeRule.categoryFromGeneration(
                obj.has("generation_type") ? obj.get("generation_type").getAsString()
                        : (obj.has("category") ? obj.get("category").getAsString() : "primary"));
        CavePlacementType placement = CavePlacementType.FULL_REGION;
        if (obj.has("placement_type")) {
            try {
                placement = CavePlacementType.fromString(obj.get("placement_type").getAsString());
            } catch (Exception ignored) {
            }
        } else if (cat == CaveBiomeCategory.SPECIAL) {
            placement = CavePlacementType.CEILING_PATCH;
        }
        Set<CaveClimateType> climates = readClimates(obj);
        Set<String> systems = readSystems(obj);
        CaveBiomeRule.Dimension dimension = CaveBiomeRule.Dimension.fromAlias(
                obj.has("dimension") ? obj.get("dimension").getAsString() : "overworld");
        float temperature = getFloat(obj, "temperature", 0.5F);
        float veg = getFloat(obj, "vegetation_density", 0.5F);
        float weight = getFloat(obj, "weight", 1.0F);
        float cmin = getFloat(obj, "ceiling_patch_min", 0.2F);
        float cmax = getFloat(obj, "ceiling_patch_max", 0.5F);
        float island = getFloat(obj, "island_max_radius", 1.5F);
        boolean generator = obj.has("stat_generator") && obj.get("stat_generator").getAsBoolean();
        boolean auto = obj.has("auto_generated") && obj.get("auto_generated").getAsBoolean();
        CaveBiomeStats stats = CaveBiomeStats.EMPTY;
        if (obj.has("stats") && obj.get("stats").isJsonObject()) {
            stats = parseStats(obj.getAsJsonObject("stats"));
        }
        boolean hasNew = obj.has("cond_temp") || obj.has("cond_humidity") || obj.has("cond_fertility");
        if (auto) {
            // Auto rules always pick up curated name Cond defaults (player Save clears auto_generated).
            int[] named;
            try {
                named = CaveCondNameDefaults.targetsFor(new net.minecraft.resources.ResourceLocation(biome));
            } catch (Exception e) {
                named = CaveCondNameDefaults.targetsFor(null);
            }
            return new LoadResult(new CaveBiomeRule(
                    biome, cat, placement, climates, systems, dimension,
                    temperature, veg, weight, cmin, cmax, island, stats, generator, auto,
                    named[0], named[1], named[2], named[3], named[4], named[5]
            ), null);
        }
        if (hasNew) {
            int ct = obj.has("cond_temp") ? obj.get("cond_temp").getAsInt() : CaveClimateScale.UNSET;
            int dt = obj.has("delta_temp") ? obj.get("delta_temp").getAsInt() : CaveClimateScale.DEFAULT_DELTA_TEMP;
            int ch = obj.has("cond_humidity") ? obj.get("cond_humidity").getAsInt() : CaveClimateScale.UNSET;
            int dh = obj.has("delta_humidity") ? obj.get("delta_humidity").getAsInt() : CaveClimateScale.DEFAULT_DELTA_HUM;
            int cf = obj.has("cond_fertility") ? obj.get("cond_fertility").getAsInt() : CaveClimateScale.UNSET;
            int df = obj.has("delta_fertility") ? obj.get("delta_fertility").getAsInt() : CaveClimateScale.DEFAULT_DELTA_FERT;
            try {
                int[] merged = CaveCondNameDefaults.mergeWithExisting(
                        new net.minecraft.resources.ResourceLocation(biome), ct, dt, ch, dh, cf, df);
                ct = merged[0];
                dt = merged[1];
                ch = merged[2];
                dh = merged[3];
                cf = merged[4];
                df = merged[5];
            } catch (Exception ignored) {
            }
            return new LoadResult(new CaveBiomeRule(
                    biome, cat, placement, climates, systems, dimension,
                    temperature, veg, weight, cmin, cmax, island, stats, generator, auto,
                    ct, dt, ch, dh, cf, df
            ), null);
        }
        return new LoadResult(new CaveBiomeRule(
                biome, cat, placement, climates, systems, dimension,
                temperature, veg, weight, cmin, cmax, island, stats, generator, auto
        ), null);
    }

    public static void write(Path file, CaveBiomeRule rule) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject obj = toJson(rule);
        String header = "# ASM: cave_biome_rule v1 — " + rule.biome + "\n";
        Files.writeString(file, header + GSON.toJson(obj) + "\n", StandardCharsets.UTF_8);
    }

    public static JsonObject toJson(CaveBiomeRule rule) {
        JsonObject obj = new JsonObject();
        obj.addProperty("biome", rule.biome);
        obj.addProperty("generation_type", CaveBiomeRule.generationAlias(rule.category));
        obj.addProperty("placement_type", rule.placementType.name().toLowerCase(Locale.ROOT));
        JsonArray climates = new JsonArray();
        for (CaveClimateType c : rule.climates) {
            climates.add(c.alias());
        }
        obj.add("climates", climates);
        JsonArray systems = new JsonArray();
        for (String s : rule.systems) {
            systems.add(s);
        }
        obj.add("systems", systems);
        obj.addProperty("dimension", rule.dimension.alias());
        obj.addProperty("temperature", rule.temperature);
        obj.addProperty("vegetation_density", rule.vegetationDensity);
        obj.addProperty("weight", rule.weight);
        if (rule.condTemp != CaveClimateScale.UNSET) {
            obj.addProperty("cond_temp", rule.condTemp);
            obj.addProperty("delta_temp", rule.deltaTemp);
        }
        if (rule.condHumidity != CaveClimateScale.UNSET) {
            obj.addProperty("cond_humidity", rule.condHumidity);
            obj.addProperty("delta_humidity", rule.deltaHumidity);
        }
        if (rule.condFertility != CaveClimateScale.UNSET) {
            obj.addProperty("cond_fertility", rule.condFertility);
            obj.addProperty("delta_fertility", rule.deltaFertility);
        }
        obj.addProperty("vegetation_density", rule.vegetationDensity);
        obj.addProperty("weight", rule.weight);
        obj.addProperty("ceiling_patch_min", rule.ceilingPatchMin);
        obj.addProperty("ceiling_patch_max", rule.ceilingPatchMax);
        obj.addProperty("island_max_radius", rule.islandMaxRadius);
        obj.addProperty("stat_generator", rule.statGenerator);
        obj.addProperty("auto_generated", rule.autoGenerated);
        if (rule.stats != null && rule.stats != CaveBiomeStats.EMPTY && rule.stats.hasAnyValue()) {
            obj.add("stats", statsToJson(rule.stats));
        }
        return obj;
    }

    private static Set<CaveClimateType> readClimates(JsonObject obj) {
        EnumSet<CaveClimateType> out = EnumSet.noneOf(CaveClimateType.class);
        if (!obj.has("climates") || !obj.get("climates").isJsonArray()) {
            return out;
        }
        for (JsonElement e : obj.getAsJsonArray("climates")) {
            if (e.isJsonPrimitive()) {
                out.add(CaveClimateType.fromAlias(e.getAsString()));
            }
        }
        return out;
    }

    private static Set<String> readSystems(JsonObject obj) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (!obj.has("systems") || !obj.get("systems").isJsonArray()) {
            return out;
        }
        for (JsonElement e : obj.getAsJsonArray("systems")) {
            if (e.isJsonPrimitive()) {
                String s = e.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception e) {
            return def;
        }
    }

    private static CaveBiomeStats parseStats(JsonObject stats) {
        CaveBiomeStats.Builder b = CaveBiomeStats.builder();
        b.conditions(readVec(stats, "conditions", true));
        b.global(readVec(stats, "global", false));
        JsonObject local = stats.has("local") && stats.get("local").isJsonObject()
                ? stats.getAsJsonObject("local") : new JsonObject();
        b.local(readVecObj(local, false));
        if (local.has("falloff_per_hop")) {
            b.localFalloffPerHop(local.get("falloff_per_hop").getAsFloat());
        }
        for (CaveClimateType climate : CaveClimateType.values()) {
            String key = "global_" + climate.name().toLowerCase(Locale.ROOT);
            String aliasKey = "global_" + climate.alias();
            JsonObject section = null;
            if (stats.has(key) && stats.get(key).isJsonObject()) {
                section = stats.getAsJsonObject(key);
            } else if (stats.has(aliasKey) && stats.get(aliasKey).isJsonObject()) {
                section = stats.getAsJsonObject(aliasKey);
            }
            if (section != null) {
                b.globalForClimate(climate, readVecObj(section, false));
            }
        }
        return b.build();
    }

    private static JsonObject statsToJson(CaveBiomeStats stats) {
        JsonObject o = new JsonObject();
        o.add("conditions", vecToJson(stats.conditions()));
        o.add("global", vecToJson(stats.global()));
        JsonObject local = vecToJson(stats.local());
        local.addProperty("falloff_per_hop", stats.localFalloffPerHop());
        o.add("local", local);
        return o;
    }

    private static CaveStatVector readVec(JsonObject parent, String key, boolean conditions) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            return conditions ? new CaveStatVector(-10.0f, -10.0f, -10.0f) : CaveStatVector.ZERO;
        }
        return readVecObj(parent.getAsJsonObject(key), conditions);
    }

    private static CaveStatVector readVecObj(JsonObject o, boolean conditions) {
        float def = conditions ? -10.0f : 0.0f;
        float m = o.has("moisture") ? o.get("moisture").getAsFloat() : def;
        float t = o.has("temperature") ? o.get("temperature").getAsFloat() : def;
        float f = o.has("fertility") ? o.get("fertility").getAsFloat() : def;
        return new CaveStatVector(m, t, f);
    }

    private static JsonObject vecToJson(CaveStatVector v) {
        JsonObject o = new JsonObject();
        o.addProperty("moisture", v.moisture());
        o.addProperty("temperature", v.temperature());
        o.addProperty("fertility", v.fertility());
        return o;
    }
}
