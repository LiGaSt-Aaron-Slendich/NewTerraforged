package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class TFBiomeTerrainIntegrationConfig {
    public static TFBiomeTerrainIntegrationConfig INSTANCE;
    private final Map<String, TerrainRules> terrains = new HashMap<>();

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open("NewTerraForged/biome-terrain-integration.toml");
        INSTANCE = new TFBiomeTerrainIntegrationConfig();
        INSTANCE.read(cfg);
        cfg.close();
    }

    private void read(CommentedFileConfig root) {
        for (String key : root.valueMap().keySet()) {
            if (key.equalsIgnoreCase("settings")) {
                continue;
            }
            Object raw = root.get(key);
            if (!(raw instanceof Config)) {
                continue;
            }
            Config section = (Config)raw;
            TerrainRules rules = TFBiomeTerrainIntegrationConfig.parseRules(section);
            if (!rules.isEmpty()) {
                this.terrains.put(key.toLowerCase(Locale.ROOT), rules);
            }
        }
    }

    public TerrainRules getRules(String terrainName) {
        if (terrainName == null || terrainName.isBlank()) {
            return TerrainRules.EMPTY;
        }
        return this.terrains.getOrDefault(terrainName.toLowerCase(Locale.ROOT), TerrainRules.EMPTY);
    }

    public int terrainRuleCount() {
        return this.terrains.size();
    }

    private static TerrainRules parseRules(Config section) {
        Set<ResourceLocation> whitelist = TFBiomeTerrainIntegrationConfig.readBiomeList(section, "whitelist");
        Set<ResourceLocation> blacklist = TFBiomeTerrainIntegrationConfig.readBiomeList(section, "blacklist");
        if (whitelist.isEmpty() && blacklist.isEmpty()) {
            return TerrainRules.EMPTY;
        }
        return new TerrainRules(whitelist, blacklist);
    }

    private static Set<ResourceLocation> readBiomeList(Config section, String key) {
        Object raw = TFConfigLoader.getIgnoreCase(section, key);
        if (!(raw instanceof List)) {
            return Set.of();
        }
        HashSet<ResourceLocation> out = new HashSet<>();
        for (Object item : (List)raw) {
            if (!(item instanceof String)) {
                continue;
            }
            String line = ((String)item).trim();
            if (!line.contains(":")) {
                continue;
            }
            try {
                out.add(new ResourceLocation(line));
            }
            catch (Exception ignored) {
            }
        }
        return out.isEmpty() ? Set.of() : Collections.unmodifiableSet(out);
    }

    public record TerrainRules(Set<ResourceLocation> whitelist, Set<ResourceLocation> blacklist) {
        public static final TerrainRules EMPTY = new TerrainRules(Set.of(), Set.of());

        public boolean isEmpty() {
            return this.whitelist.isEmpty() && this.blacklist.isEmpty();
        }

        public boolean isAllowed(ResourceLocation biomeId) {
            if (biomeId == null) {
                return true;
            }
            if (!this.whitelist.isEmpty()) {
                return this.whitelist.contains(biomeId);
            }
            return !this.blacklist.contains(biomeId);
        }
    }
}
