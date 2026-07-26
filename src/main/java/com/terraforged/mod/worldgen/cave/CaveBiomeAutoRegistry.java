package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Registers mod cave biomes that exist in the biome registry but are not listed in cave-biomes.toml.
 * Biomes on the config blacklist or failing {@link CaveBiomeIds#isUndergroundBiome} are skipped.
 */
public final class CaveBiomeAutoRegistry {
    private CaveBiomeAutoRegistry() {
    }

    public static void append(Registry<Biome> biomeRegistry, TFCaveBiomeConfig config, List<CaveBiomeEntry> out, Set<ResourceLocation> configured) {
        if (config == null || !config.autoRegisterCaveBiomes) {
            return;
        }
        HashSet<ResourceLocation> seen = new HashSet<>(configured);
        int added = 0;
        for (ResourceLocation id : biomeRegistry.keySet()) {
            if (seen.contains(id) || config.isBlacklisted(id) || CaveBiomeIds.isBlockedCaveBiome(id) || CaveBiomeIds.isNetherThemedBiome(id)) {
                continue;
            }
            if (!CaveBiomeIds.isUndergroundBiome(id)) {
                continue;
            }
            if ("minecraft".equals(id.getNamespace()) && !id.getPath().contains("dripstone")) {
                continue;
            }
            CaveBiomeEntry entry = CaveBiomeAutoRegistry.buildEntry(id);
            if (entry == null || !entry.isAvailable(biomeRegistry)) {
                continue;
            }
            out.add(entry);
            seen.add(id);
            ++added;
            TerraForged.LOG.info("[CaveBiomeAutoRegistry] Auto-registered {} as {} ({})", id, entry.category(), entry.placementType());
        }
        if (added > 0) {
            TerraForged.LOG.info("[CaveBiomeAutoRegistry] Added {} biomes from registry scan", added);
        }
    }

    static CaveBiomeEntry buildEntry(ResourceLocation id) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        CaveBiomeCategory category = CaveBiomeAutoRegistry.inferCategory(path);
        CavePlacementType placement = CaveBiomeAutoRegistry.inferPlacement(path);
        float temperature = CaveBiomeAutoRegistry.inferTemperature(id);
        float vegetation = CaveBiomeAutoRegistry.inferVegetation(path);
        float weight = path.contains("transition") || category == CaveBiomeCategory.TRANSITION ? 0.85f : 1.0f;
        CaveBiomeStats stats = CaveBiomeStatDefaults.infer(id);
        boolean generator = CaveBiomeStatDefaults.isGenerator(id) || path.contains("thermal") || path.contains("mantle") || path.contains("frostfire") || path.contains("yellowstone");
        return new CaveBiomeEntry(id, category, placement, temperature, vegetation, weight, 0.2f, 0.5f, 1.5f, stats, generator);
    }

    private static CaveBiomeCategory inferCategory(String path) {
        if (CaveBiomeAutoRegistry.containsAny(path, "grotto", "coastal", "sunken", "sea_cave", "kelp", "tide", "shore")) {
            return CaveBiomeCategory.COASTAL;
        }
        if (CaveBiomeAutoRegistry.containsAny(path, "hypogeal", "island", "patch", "ceiling", "subzero", "prismachasm")) {
            return CaveBiomeCategory.SPECIAL;
        }
        if (CaveBiomeAutoRegistry.containsAny(path, "steaming", "mycotoxic", "shattered", "quartz_desert", "ancient_delta", "embur", "crimson_gardens", "nightshade", "transition", "redstone_caves")) {
            return CaveBiomeCategory.TRANSITION;
        }
        return CaveBiomeCategory.PRIMARY;
    }

    private static CavePlacementType inferPlacement(String path) {
        if (path.contains("hypogeal") || path.contains("ceiling_patch")) {
            return CavePlacementType.CEILING_PATCH;
        }
        if (path.contains("island")) {
            return CavePlacementType.ISLAND_PATCH;
        }
        return CavePlacementType.FULL_REGION;
    }

    private static float inferTemperature(ResourceLocation id) {
        CaveBiomeStats stats = CaveBiomeStatDefaults.infer(id);
        float axis = stats.conditions().temperature();
        if (axis <= -9.0f) {
            return 0.05f;
        }
        if (axis >= 2.0f) {
            return 0.9f;
        }
        return Math.max(0.05f, Math.min(0.95f, (axis + 10.0f) / 20.0f));
    }

    private static float inferVegetation(String path) {
        if (CaveBiomeAutoRegistry.containsAny(path, "fungal", "mycotoxic", "bioshroom", "glowshroom", "mushroom", "lush", "jungle", "moss", "grotto", "undergarden")) {
            return 0.65f;
        }
        if (CaveBiomeAutoRegistry.containsAny(path, "crystal", "ice", "frost", "stone", "granite", "andesite", "diorite", "tuff", "barren", "desert", "quartz")) {
            return 0.12f;
        }
        if (CaveBiomeAutoRegistry.containsAny(path, "thermal", "volcanic", "mantle", "scorch", "brimstone", "inferno")) {
            return 0.15f;
        }
        return 0.4f;
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
