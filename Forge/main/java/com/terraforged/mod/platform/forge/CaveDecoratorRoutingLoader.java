package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.cave.CaveDecoratorKind;
import com.terraforged.mod.worldgen.cave.CaveDecoratorRoutingTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

final class CaveDecoratorRoutingLoader {
    static final String ROUTING_CONFIG = "NewTerraForged/Critical Options/Hybrid options/decorator-routing.toml";

    private CaveDecoratorRoutingLoader() {
    }

    static CaveDecoratorRoutingTable load() {
        CommentedFileConfig cfg = TFConfigLoader.open(ROUTING_CONFIG);
        try {
            return CaveDecoratorRoutingLoader.load((Config) cfg);
        } finally {
            cfg.close();
        }
    }

    static CaveDecoratorRoutingTable load(Config root) {
        Object raw = root.get("decorator_routing");
        if (!(raw instanceof Config section) || section.isEmpty()) {
            return CaveDecoratorRoutingTable.defaults();
        }
        CaveDecoratorRoutingTable defaults = CaveDecoratorRoutingTable.defaults();
        CaveDecoratorRoutingTable.Builder builder = new CaveDecoratorRoutingTable.Builder();
        builder.defaultKind(CaveDecoratorRoutingTable.parseKind(TFConfigLoader.getString(section, "default", defaults.defaultKind().name()), defaults.defaultKind()));
        int ruleCount = CaveDecoratorRoutingLoader.countRules(section);
        if (ruleCount == 0) {
            for (CaveDecoratorRoutingTable.PatternRule rule : defaults.rules()) {
                builder.rule(rule.decorator(), rule.patterns());
            }
        } else {
            CaveDecoratorRoutingLoader.readRules(section, builder);
        }
        CaveDecoratorRoutingLoader.readOverrides(section, builder);
        CaveDecoratorRoutingTable table = builder.build();
        TerraForged.LOG.info("[cave-biomes] Decorator routing ({}): {} overrides, {} pattern rules, default={}", ROUTING_CONFIG, CaveDecoratorRoutingLoader.countOverrides(section), ruleCount == 0 ? defaults.rules().size() : ruleCount, table.defaultKind().name().toLowerCase(Locale.ROOT));
        return table;
    }

    static int countOverrides(Config section) {
        Object raw = section.get("override");
        return raw instanceof List list ? list.size() : 0;
    }

    static int countRules(Config section) {
        Object raw = section.get("rule");
        return raw instanceof List list ? list.size() : 0;
    }

    private static void readOverrides(Config section, CaveDecoratorRoutingTable.Builder builder) {
        Object raw = section.get("override");
        if (!(raw instanceof List list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Config cfg)) {
                continue;
            }
            String id = TFConfigLoader.getString(cfg, "id", "");
            if (!id.contains(":")) {
                TerraForged.LOG.warn("[cave-biomes] decorator_routing.override skipped — invalid id: {}", id);
                continue;
            }
            try {
                ResourceLocation loc = new ResourceLocation(id.trim());
                CaveDecoratorKind kind = CaveDecoratorRoutingTable.parseKind(TFConfigLoader.getString(cfg, "decorator", ""), null);
                if (kind == null) {
                    TerraForged.LOG.warn("[cave-biomes] decorator_routing.override skipped — unknown decorator for {}", id);
                    continue;
                }
                builder.override(loc, kind);
            }
            catch (Exception e) {
                TerraForged.LOG.warn("[cave-biomes] decorator_routing.override skipped — bad id: {}", id);
            }
        }
    }

    private static void readRules(Config section, CaveDecoratorRoutingTable.Builder builder) {
        Object raw = section.get("rule");
        if (!(raw instanceof List list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Config cfg)) {
                continue;
            }
            CaveDecoratorKind kind = CaveDecoratorRoutingTable.parseKind(TFConfigLoader.getString(cfg, "decorator", ""), null);
            if (kind == null) {
                TerraForged.LOG.warn("[cave-biomes] decorator_routing.rule skipped — unknown decorator");
                continue;
            }
            ArrayList<String> patterns = new ArrayList<>();
            Object patternRaw = cfg.get("patterns");
            if (patternRaw instanceof List patternList) {
                for (Object p : patternList) {
                    if (p instanceof String s && !s.isBlank()) {
                        patterns.add(s.trim());
                    }
                }
            }
            String single = TFConfigLoader.getString(cfg, "pattern", "");
            if (!single.isBlank()) {
                patterns.add(single.trim());
            }
            if (patterns.isEmpty()) {
                TerraForged.LOG.warn("[cave-biomes] decorator_routing.rule skipped — no patterns for {}", kind.name().toLowerCase(Locale.ROOT));
                continue;
            }
            builder.rule(kind, patterns);
        }
    }
}
