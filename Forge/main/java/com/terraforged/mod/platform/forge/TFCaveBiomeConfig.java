package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
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
import net.minecraft.resources.ResourceLocation;

public final class TFCaveBiomeConfig {
    public static TFCaveBiomeConfig INSTANCE;
    public final List<Entry> primary = new ArrayList<Entry>();
    public final List<Entry> transition = new ArrayList<Entry>();
    public final List<Entry> special = new ArrayList<Entry>();
    public final List<Entry> coastal = new ArrayList<Entry>();
    /** Original TerraForged 0.3.x NoiseCaveDecorator (github.com/TerraForged/TerraForged). */
    public boolean useOfficialTfCaveDecorator = false;
    /** Route each painted cave biome to the best decorator backend (official / vanilla / legacy / compromise). */
    public boolean usePerBiomeDecorators = true;
    /** Lower minimum stat thresholds when picking shell biomes (0 = strict, 3 = much more variety). */
    public float conditionRelax = 3.0f;
    /** Compromise decorator: themed cover + bounded scatter + mega accents (default on). */
    public boolean useCompromiseCaveDecorator = true;
    /** TerraLith-style {@code placeWithBiomeCheck} per chunk. */
    public boolean useVanillaCavePass = false;
    /** Legacy anchor/scatter decorators (volume, mega accent, noise cave decorator). */
    public boolean useLegacyCaveDecorators = false;
    /** Scan biome registry for unlisted cave biomes and auto-register them. */
    public boolean autoRegisterCaveBiomes = true;
    /** Chunk grid step for vanilla decoration origins (2–8). Lower = denser cover/features. */
    public int vanillaOriginGrid = 2;
    /** Max floor origins per biome per chunk. */
    public int vanillaOriginsPerBiome = 6;
    /** Also run vanilla pass at ceiling anchor (stalactites, crystal down). */
    public boolean vanillaCeilingPass = true;
    public final List<String> blacklist = new ArrayList<String>();

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open("NewTerraForged/cave-biomes.toml");
        INSTANCE = new TFCaveBiomeConfig();
        INSTANCE.read(cfg);
        cfg.close();
    }

    private void read(CommentedFileConfig root) {
        Config decoration = TFConfigLoader.section((Config)root, "decoration");
        this.useOfficialTfCaveDecorator = TFConfigLoader.getBool(decoration, "use_official_tf_decorator", this.useOfficialTfCaveDecorator);
        this.usePerBiomeDecorators = TFConfigLoader.getBool(decoration, "use_per_biome_decorators", this.usePerBiomeDecorators);
        this.conditionRelax = Math.max(0.0f, Math.min(6.0f, TFConfigLoader.getFloat(decoration, "condition_relax", this.conditionRelax)));
        this.useCompromiseCaveDecorator = TFConfigLoader.getBool(decoration, "use_compromise_decorator", this.useCompromiseCaveDecorator);
        this.useVanillaCavePass = TFConfigLoader.getBool(decoration, "use_vanilla_pass", this.useVanillaCavePass);
        this.useLegacyCaveDecorators = TFConfigLoader.getBool(decoration, "use_legacy_decorators", this.useLegacyCaveDecorators);
        this.autoRegisterCaveBiomes = TFConfigLoader.getBool(decoration, "auto_register_biomes", this.autoRegisterCaveBiomes);
        this.vanillaOriginGrid = Math.max(2, Math.min(8, TFConfigLoader.getInt(decoration, "vanilla_origin_grid", this.vanillaOriginGrid)));
        this.vanillaOriginsPerBiome = Math.max(1, Math.min(8, TFConfigLoader.getInt(decoration, "vanilla_origins_per_biome", this.vanillaOriginsPerBiome)));
        this.vanillaCeilingPass = TFConfigLoader.getBool(decoration, "vanilla_ceiling_pass", this.vanillaCeilingPass);
        this.enforceExclusiveDecorationMode();
        Object rawBlacklist = root.get("blacklist");
        this.blacklist.clear();
        if (rawBlacklist instanceof List) {
            for (Object item : (List)rawBlacklist) {
                if (item instanceof String s && !s.isBlank()) {
                    this.blacklist.add(s.trim());
                }
            }
        }
        this.primary.addAll(TFCaveBiomeConfig.readTable((Config)root, "primary"));
        this.transition.addAll(TFCaveBiomeConfig.readTable((Config)root, "transition"));
        this.special.addAll(TFCaveBiomeConfig.readTable((Config)root, "special"));
        this.coastal.addAll(TFCaveBiomeConfig.readTable((Config)root, "coastal"));
    }

    private void enforceExclusiveDecorationMode() {
        int enabled = (this.usePerBiomeDecorators ? 1 : 0) + (this.useOfficialTfCaveDecorator ? 1 : 0) + (this.useCompromiseCaveDecorator ? 1 : 0) + (this.useVanillaCavePass ? 1 : 0) + (this.useLegacyCaveDecorators ? 1 : 0);
        if (enabled <= 1) {
            return;
        }
        if (this.usePerBiomeDecorators) {
            if (this.useOfficialTfCaveDecorator) {
                TerraForged.LOG.warn("[cave-biomes] use_official_tf_decorator ignored — use_per_biome_decorators takes priority");
                this.useOfficialTfCaveDecorator = false;
            }
            if (this.useCompromiseCaveDecorator) {
                TerraForged.LOG.warn("[cave-biomes] use_compromise_decorator ignored — use_per_biome_decorators takes priority");
                this.useCompromiseCaveDecorator = false;
            }
            if (this.useVanillaCavePass) {
                TerraForged.LOG.warn("[cave-biomes] use_vanilla_pass ignored — use_per_biome_decorators takes priority");
                this.useVanillaCavePass = false;
            }
            if (this.useLegacyCaveDecorators) {
                TerraForged.LOG.warn("[cave-biomes] use_legacy_decorators ignored — use_per_biome_decorators takes priority");
                this.useLegacyCaveDecorators = false;
            }
            return;
        }
        if (this.useOfficialTfCaveDecorator) {
            if (this.useCompromiseCaveDecorator) {
                TerraForged.LOG.warn("[cave-biomes] use_compromise_decorator ignored — use_official_tf_decorator takes priority");
                this.useCompromiseCaveDecorator = false;
            }
            if (this.useVanillaCavePass) {
                TerraForged.LOG.warn("[cave-biomes] use_vanilla_pass ignored — use_official_tf_decorator takes priority");
                this.useVanillaCavePass = false;
            }
            if (this.useLegacyCaveDecorators) {
                TerraForged.LOG.warn("[cave-biomes] use_legacy_decorators ignored — use_official_tf_decorator takes priority");
                this.useLegacyCaveDecorators = false;
            }
            return;
        }
        if (this.useCompromiseCaveDecorator) {
            if (this.useVanillaCavePass) {
                TerraForged.LOG.warn("[cave-biomes] use_vanilla_pass ignored — use_compromise_decorator takes priority");
                this.useVanillaCavePass = false;
            }
            if (this.useLegacyCaveDecorators) {
                TerraForged.LOG.warn("[cave-biomes] use_legacy_decorators ignored — use_compromise_decorator takes priority");
                this.useLegacyCaveDecorators = false;
            }
            return;
        }
        if (this.useVanillaCavePass && this.useLegacyCaveDecorators) {
            TerraForged.LOG.warn("[cave-biomes] use_legacy_decorators ignored — use_vanilla_pass takes priority");
            this.useLegacyCaveDecorators = false;
        }
    }

    public boolean isBlacklisted(ResourceLocation id) {
        if (id == null) {
            return true;
        }
        String full = id.toString();
        String path = id.getPath();
        for (String entry : this.blacklist) {
            if (entry.equalsIgnoreCase(full) || entry.equalsIgnoreCase(path)) {
                return true;
            }
        }
        return false;
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
