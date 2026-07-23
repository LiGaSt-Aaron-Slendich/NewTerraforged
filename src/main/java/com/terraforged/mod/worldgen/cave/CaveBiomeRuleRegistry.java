package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFConfigPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Per-biome cave JSON under {@code Cave_configs/Biomes/{ns}/{path}.json}.
 * Migrates legacy {@code cave-biomes.toml} biome tables on first empty sync.
 */
public final class CaveBiomeRuleRegistry {
    private static final Map<ResourceLocation, CaveBiomeRule> RULES = new HashMap<>();
    private static volatile boolean synced;

    private CaveBiomeRuleRegistry() {
    }

    public static Path biomesRoot() {
        return FMLPaths.CONFIGDIR.get().resolve(TFConfigPaths.CAVE_RULES_BIOMES);
    }

    public static synchronized void syncFromConfig(TFCaveBiomeConfig config) {
        Path root = biomesRoot();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            TerraForged.LOG.error("[CaveBiomeRules] cannot create {}", root, e);
            return;
        }
        int onDisk = countJson(root);
        if (onDisk == 0 && config != null) {
            int written = migrateToml(config, root);
            TerraForged.LOG.info("[CaveBiomeRules] migrated {} biome entries from toml → {}", written, root);
        }
        RULES.clear();
        int loaded = loadTree(root);
        synced = !RULES.isEmpty();
        TerraForged.LOG.info("[CaveBiomeRules] sync: {} rules from {}", loaded, root);
    }

    public static synchronized void reload() {
        syncFromConfig(TFCaveBiomeConfig.INSTANCE);
        CaveBiomeRegistryLoader.invalidateCache();
    }

    public static boolean isSynced() {
        return synced;
    }

    public static CaveBiomeRule get(ResourceLocation id) {
        return id == null ? null : RULES.get(id);
    }

    public static Map<ResourceLocation, CaveBiomeRule> snapshot() {
        return Collections.unmodifiableMap(RULES);
    }

    public static List<ResourceLocation> idsSorted() {
        ArrayList<ResourceLocation> ids = new ArrayList<>(RULES.keySet());
        ids.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        return ids;
    }

    public static synchronized void putAndSave(ResourceLocation id, CaveBiomeRule rule) {
        if (id == null || rule == null) {
            return;
        }
        RULES.put(id, rule);
        Path file = biomesRoot().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        try {
            CaveBiomeRuleIO.write(file, rule);
        } catch (IOException e) {
            TerraForged.LOG.warn("[CaveBiomeRules] save failed {}: {}", id, e.toString());
        }
        CaveBiomeRegistryLoader.invalidateCache();
    }

    /** Entries for registry loader, grouped like legacy toml tables. */
    public static List<TFCaveBiomeConfig.Entry> entriesFor(CaveBiomeCategory category) {
        ArrayList<TFCaveBiomeConfig.Entry> out = new ArrayList<>();
        for (CaveBiomeRule rule : RULES.values()) {
            if (rule.category != category) {
                continue;
            }
            out.add(new TFCaveBiomeConfig.Entry(
                    rule.biome,
                    rule.temperature,
                    rule.vegetationDensity,
                    rule.weight,
                    rule.placementType.name().toLowerCase(Locale.ROOT),
                    rule.ceilingPatchMin,
                    rule.ceilingPatchMax,
                    rule.islandMaxRadius,
                    rule.stats,
                    rule.statGenerator
            ));
        }
        return out;
    }

    private static int migrateToml(TFCaveBiomeConfig config, Path root) {
        int n = 0;
        n += writeList(config.primary, CaveBiomeCategory.PRIMARY, root);
        n += writeList(config.transition, CaveBiomeCategory.TRANSITION, root);
        n += writeList(config.special, CaveBiomeCategory.SPECIAL, root);
        n += writeList(config.coastal, CaveBiomeCategory.COASTAL, root);
        return n;
    }

    private static int writeList(List<TFCaveBiomeConfig.Entry> list, CaveBiomeCategory cat, Path root) {
        int n = 0;
        if (list == null) {
            return 0;
        }
        for (TFCaveBiomeConfig.Entry e : list) {
            if (e == null || e.biomeId() == null || e.biomeId().isBlank()) {
                continue;
            }
            ResourceLocation id;
            try {
                id = new ResourceLocation(e.biomeId().trim());
            } catch (Exception ex) {
                continue;
            }
            CavePlacementType placement;
            try {
                placement = CavePlacementType.fromString(e.placementType());
            } catch (Exception ex) {
                placement = CavePlacementType.FULL_REGION;
            }
            CaveBiomeRule rule = new CaveBiomeRule(
                    id.toString(),
                    cat,
                    placement,
                    Set.of(),
                    Set.of(),
                    e.temperature(),
                    e.vegetationDensity(),
                    e.weight(),
                    e.ceilingPatchMin(),
                    e.ceilingPatchMax(),
                    e.islandMaxRadius(),
                    e.stats(),
                    e.statGenerator(),
                    false
            );
            Path file = root.resolve(id.getNamespace()).resolve(id.getPath() + ".json");
            try {
                if (!Files.isRegularFile(file)) {
                    CaveBiomeRuleIO.write(file, rule);
                    n++;
                }
            } catch (IOException ex) {
                TerraForged.LOG.warn("[CaveBiomeRules] migrate write failed {}: {}", id, ex.toString());
            }
        }
        return n;
    }

    private static int loadTree(Path root) {
        int n = 0;
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".json"))::iterator) {
                CaveBiomeRuleIO.LoadResult r = CaveBiomeRuleIO.load(file);
                if (!r.ok()) {
                    TerraForged.LOG.warn("[CaveBiomeRules] skip {}: {}", file, r.error());
                    continue;
                }
                ResourceLocation id;
                try {
                    id = new ResourceLocation(r.rule().biome);
                } catch (Exception e) {
                    continue;
                }
                RULES.put(id, r.rule());
                n++;
            }
        } catch (IOException e) {
            TerraForged.LOG.error("[CaveBiomeRules] walk failed", e);
        }
        return n;
    }

    private static int countJson(Path root) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return (int) walk.filter(p -> p.toString().endsWith(".json")).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
