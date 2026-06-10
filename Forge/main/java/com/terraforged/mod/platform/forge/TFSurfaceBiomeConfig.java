package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.platform.forge.TFConfigLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TFSurfaceBiomeConfig {
    public static TFSurfaceBiomeConfig INSTANCE;
    public float modBiomeWeight = 5.0f;
    public boolean autoDetectModBiomes = true;
    private final Map<BiomeType, List<Entry>> climates = new EnumMap<BiomeType, List<Entry>>(BiomeType.class);

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open("NewTerraForged/surface-biomes.toml");
        INSTANCE = new TFSurfaceBiomeConfig();
        INSTANCE.read(cfg);
        cfg.close();
    }

    private void read(CommentedFileConfig root) {
        Config settings = TFConfigLoader.section((Config)root, "settings");
        this.modBiomeWeight = TFConfigLoader.getFloat(settings, "mod_biome_weight", this.modBiomeWeight);
        this.autoDetectModBiomes = TFConfigLoader.getBool(settings, "auto_detect_mod_biomes", this.autoDetectModBiomes);
        for (BiomeType type : BiomeType.values()) {
            String key = type.name().toLowerCase(Locale.ROOT);
            this.climates.put(type, TFSurfaceBiomeConfig.readClimateTable((Config)root, key));
        }
    }

    public List<Entry> getClimateList(BiomeType type) {
        return this.climates.getOrDefault(type, List.of());
    }

    private static List<Entry> readClimateTable(Config root, String key) {
        Object raw = root.get(key);
        if (!(raw instanceof List)) {
            return List.of();
        }
        List list = (List)raw;
        ArrayList<Entry> out = new ArrayList<Entry>(list.size());
        for (Object item : list) {
            Entry entry = TFSurfaceBiomeConfig.parseEntry(item);
            if (entry == null) continue;
            out.add(entry);
        }
        return Collections.unmodifiableList(out);
    }

    private static Entry parseEntry(Object item) {
        if (item instanceof Config) {
            Config cfg = (Config)item;
            String id = TFConfigLoader.getString(cfg, "id", "");
            if (!id.contains(":")) {
                return null;
            }
            return new Entry(id.trim(), TFConfigLoader.getFloat(cfg, "weight", 1.0f));
        }
        if (item instanceof String) {
            String line = (String)item;
            return TFSurfaceBiomeConfig.parseLegacyLine(line);
        }
        return null;
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
                    params.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
                }
            }
        }
        if (!biomeId.contains(":")) {
            return null;
        }
        float weight = 1.0f;
        try {
            String w = (String)params.get("weight");
            if (w != null) {
                weight = Float.parseFloat(w);
            }
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        return new Entry(biomeId, weight);
    }

    public record Entry(String biomeId, float weight) {
    }
}
