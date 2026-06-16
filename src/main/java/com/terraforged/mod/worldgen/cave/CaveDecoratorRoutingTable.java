package com.terraforged.mod.worldgen.cave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Hybrid decorator routing: exact biome overrides, then ordered path-pattern rules, then default.
 */
public final class CaveDecoratorRoutingTable {
    public static final String MINECRAFT_CAVE_TOKEN = "@minecraft_cave";

    private final Map<ResourceLocation, CaveDecoratorKind> overrides;
    private final List<PatternRule> rules;
    private final CaveDecoratorKind defaultKind;

    public CaveDecoratorRoutingTable(Map<ResourceLocation, CaveDecoratorKind> overrides, List<PatternRule> rules, CaveDecoratorKind defaultKind) {
        this.overrides = Map.copyOf(overrides);
        this.rules = List.copyOf(rules);
        this.defaultKind = defaultKind;
    }

    public CaveDecoratorKind resolve(ResourceLocation id) {
        if (id == null) {
            return this.defaultKind;
        }
        CaveDecoratorKind exact = this.overrides.get(id);
        if (exact != null) {
            return exact;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String full = id.toString().toLowerCase(Locale.ROOT);
        for (PatternRule rule : this.rules) {
            if (rule.matches(id, path, full)) {
                return rule.decorator();
            }
        }
        return this.defaultKind;
    }

    public CaveDecoratorKind defaultKind() {
        return this.defaultKind;
    }

    public List<PatternRule> rules() {
        return this.rules;
    }

    public static CaveDecoratorRoutingTable defaults() {
        LinkedHashMap<ResourceLocation, CaveDecoratorKind> overrides = new LinkedHashMap<>();
        ArrayList<PatternRule> rules = new ArrayList<>();
        rules.add(new PatternRule(CaveDecoratorKind.OFFICIAL, List.of(
                "scorching", "mantle", "magma",
                "karst", "limestone", "tuff_cave", "tuff_caves",
                "icicle", "stalactite",
                "empty_stone", "stone_cave", CaveDecoratorRoutingTable.MINECRAFT_CAVE_TOKEN
        )));
        rules.add(new PatternRule(CaveDecoratorKind.COMPROMISE, List.of(
                "scorching", "mantle", "magma", "brimstone",
                "dripstone",
                "fungal", "mycotoxic", "frostfire"
        )));
        rules.add(new PatternRule(CaveDecoratorKind.VANILLA, List.of("glowing_grotto", "undergarden")));
        rules.add(new PatternRule(CaveDecoratorKind.LEGACY, List.of("bioshroom", "glowshroom")));
        return new CaveDecoratorRoutingTable(overrides, rules, CaveDecoratorKind.COMPROMISE);
    }

    public record PatternRule(CaveDecoratorKind decorator, List<String> patterns) {
        boolean matches(ResourceLocation id, String path, String full) {
            for (String raw : this.patterns) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String pattern = raw.trim().toLowerCase(Locale.ROOT);
                if (CaveDecoratorRoutingTable.MINECRAFT_CAVE_TOKEN.equals(pattern)) {
                    if ("minecraft".equals(id.getNamespace()) && path.contains("cave")) {
                        return true;
                    }
                    continue;
                }
                if (pattern.contains(":")) {
                    try {
                        ResourceLocation loc = new ResourceLocation(pattern);
                        if (id.equals(loc)) {
                            return true;
                        }
                    }
                    catch (Exception ignored) {
                        if (full.equals(pattern) || full.endsWith("/" + pattern.substring(pattern.indexOf(':') + 1))) {
                            return true;
                        }
                    }
                    continue;
                }
                if (path.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static CaveDecoratorKind parseKind(String raw, CaveDecoratorKind fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return CaveDecoratorKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static final class Builder {
        private final LinkedHashMap<ResourceLocation, CaveDecoratorKind> overrides = new LinkedHashMap<>();
        private final ArrayList<PatternRule> rules = new ArrayList<>();
        private CaveDecoratorKind defaultKind = CaveDecoratorKind.COMPROMISE;

        public Builder defaultKind(CaveDecoratorKind kind) {
            if (kind != null) {
                this.defaultKind = kind;
            }
            return this;
        }

        public Builder override(ResourceLocation id, CaveDecoratorKind kind) {
            if (id != null && kind != null) {
                this.overrides.put(id, kind);
            }
            return this;
        }

        public Builder rule(CaveDecoratorKind kind, List<String> patterns) {
            if (kind != null && patterns != null && !patterns.isEmpty()) {
                this.rules.add(new PatternRule(kind, List.copyOf(patterns)));
            }
            return this;
        }

        public CaveDecoratorRoutingTable build() {
            if (this.rules.isEmpty() && this.overrides.isEmpty()) {
                return CaveDecoratorRoutingTable.defaults();
            }
            return new CaveDecoratorRoutingTable(this.overrides, this.rules, this.defaultKind);
        }
    }
}
