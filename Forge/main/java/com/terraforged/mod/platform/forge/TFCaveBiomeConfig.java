package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.mod.platform.forge.TFConfigLoader;
import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CaveClimateType;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TFCaveBiomeConfig {
    public static TFCaveBiomeConfig INSTANCE;
    public final List<Entry> primary = new ArrayList<Entry>();
    public final List<Entry> transition = new ArrayList<Entry>();
    public final List<Entry> special = new ArrayList<Entry>();
    public final List<Entry> coastal = new ArrayList<Entry>();

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open("NewTerraForged/cave-biomes.toml");
        INSTANCE = new TFCaveBiomeConfig();
        INSTANCE.read(cfg);
        cfg.close();
    }

    private void read(CommentedFileConfig root) {
        this.primary.addAll(TFCaveBiomeConfig.readTable((Config)root, "primary"));
        this.transition.addAll(TFCaveBiomeConfig.readTable((Config)root, "transition"));
        this.special.addAll(TFCaveBiomeConfig.readTable((Config)root, "special"));
        this.coastal.addAll(TFCaveBiomeConfig.readTable((Config)root, "coastal"));
    }

    private static List<Entry> readTable(Config root, String key) {
        Object raw = root.get(key);
        if (!(raw instanceof List)) {
            return List.of();
        }
        List list = (List)raw;
        ArrayList<Entry> out = new ArrayList<Entry>(list.size());
        for (Object item : list) {
            Entry entry = TFCaveBiomeConfig.parseEntry(item);
            if (entry == null) continue;
            out.add(entry);
        }
        return Collections.unmodifiableList(out);
    }

    private static Entry parseEntry(Object item) {
        if (item instanceof Config) {
            Config cfg = (Config)item;
            return TFCaveBiomeConfig.parseConfigEntry(cfg);
        }
        if (item instanceof String) {
            String line = (String)item;
            return TFCaveBiomeConfig.parseLegacyLine(line);
        }
        return null;
    }

    private static Entry parseConfigEntry(Config cfg) {
        String id = TFConfigLoader.getString(cfg, "id", "");
        if (!id.contains(":")) {
            return null;
        }
        return new Entry(id.trim(), TFConfigLoader.getFloat(cfg, "temperature", 0.5f), TFConfigLoader.getFloat(cfg, "vegetation_density", 0.5f), TFConfigLoader.getFloat(cfg, "weight", 1.0f), TFConfigLoader.getString(cfg, "placement_type", "full_region"), TFConfigLoader.getFloat(cfg, "ceiling_patch_min", 0.2f), TFConfigLoader.getFloat(cfg, "ceiling_patch_max", 0.5f), TFConfigLoader.getFloat(cfg, "island_max_radius", 1.5f), TFCaveBiomeConfig.parseStats(cfg), TFCaveBiomeConfig.isGeneratorRole(TFConfigLoader.getString(cfg, "stat_role", "")));
    }

    private static boolean isGeneratorRole(String role) {
        return "generator".equalsIgnoreCase(role.trim());
    }

    static CaveBiomeStats parseStats(Config cfg) {
        Config stats = TFConfigLoader.section(cfg, "stats");
        if (stats.isEmpty()) {
            return CaveBiomeStats.EMPTY;
        }
        CaveStatVector conditions = TFCaveBiomeConfig.parseVector(TFConfigLoader.section(stats, "conditions"), true);
        CaveStatVector global = TFCaveBiomeConfig.parseVector(TFConfigLoader.section(stats, "global"), false);
        Config localSection = TFConfigLoader.section(stats, "local");
        CaveStatVector local = TFCaveBiomeConfig.parseVector(localSection, false);
        float falloff = TFConfigLoader.getFloat(localSection, "falloff_per_hop", 1.0f);
        CaveBiomeStats.Builder builder = CaveBiomeStats.builder().conditions(conditions).global(global).local(local).localFalloffPerHop(falloff);
        for (CaveClimateType climate : CaveClimateType.values()) {
            String key = "global_" + climate.name().toLowerCase(Locale.ROOT);
            Config climateSection = TFConfigLoader.section(stats, key);
            if (climateSection.isEmpty()) continue;
            builder.globalForClimate(climate, TFCaveBiomeConfig.parseVector(climateSection, false));
        }
        return builder.build();
    }

    private static CaveStatVector parseVector(Config cfg, boolean conditions) {
        return new CaveStatVector(TFCaveBiomeConfig.axis(cfg, "moisture", conditions), TFCaveBiomeConfig.axis(cfg, "temperature", conditions), TFCaveBiomeConfig.axis(cfg, "fertility", conditions));
    }

    private static float axis(Config cfg, String key, boolean conditions) {
        Object raw = cfg.get(key);
        if (raw == null) {
            return conditions ? -10.0f : 0.0f;
        }
        return TFConfigLoader.getFloat(cfg, key, conditions ? -10.0f : 0.0f);
    }

    private static Entry parseLegacyLine(String raw) {
        String biomeId;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        raw = raw.trim();
        int braceStart = raw.indexOf(123);
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        if (braceStart < 0) {
            biomeId = raw.trim();
        } else {
            biomeId = raw.substring(0, braceStart).trim();
            int braceEnd = raw.lastIndexOf(125);
            if (braceEnd > braceStart) {
                for (String pair : raw.substring(braceStart + 1, braceEnd).split(",")) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length != 2) continue;
                    params.put(kv[0].trim().toLowerCase(), kv[1].trim());
                }
            }
        }
        if (!biomeId.contains(":")) {
            return null;
        }
        return new Entry(biomeId, TFCaveBiomeConfig.parseFloat(params, "temperature", 0.5f), TFCaveBiomeConfig.parseFloat(params, "vegetationdensity", TFCaveBiomeConfig.parseFloat(params, "vegetation_density", 0.5f)), TFCaveBiomeConfig.parseFloat(params, "weight", 1.0f), params.getOrDefault("placementtype", params.getOrDefault("placement_type", "full_region")), TFCaveBiomeConfig.parseFloat(params, "ceilingpatchmin", TFCaveBiomeConfig.parseFloat(params, "ceiling_patch_min", 0.2f)), TFCaveBiomeConfig.parseFloat(params, "ceilingpatchmax", TFCaveBiomeConfig.parseFloat(params, "ceiling_patch_max", 0.5f)), TFCaveBiomeConfig.parseFloat(params, "islandmaxradius", TFCaveBiomeConfig.parseFloat(params, "island_max_radius", 1.5f)), CaveBiomeStats.EMPTY, false);
    }

    private static float parseFloat(Map<String, String> map, String key, float def) {
        try {
            String v = map.get(key);
            return v != null ? Float.parseFloat(v) : def;
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    public record Entry(String biomeId, float temperature, float vegetationDensity, float weight, String placementType, float ceilingPatchMin, float ceilingPatchMax, float islandMaxRadius, CaveBiomeStats stats, boolean statGenerator) {
    }
}
