package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.worldgen.cave.CaveBiomeAutoRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeStatDefaults;
import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class CaveBiomeRegistryLoader {
    private static CaveBiomeRegistry cached;
    private static int cachedBiomeCount = -1;

    public static CaveBiomeRegistry build(Registry<Biome> biomeRegistry, TFCaveBiomeConfig config) {
        int biomeCount = biomeRegistry.size();
        if (cached != null && cachedBiomeCount == biomeCount) {
            return cached;
        }
        ArrayList<CaveBiomeEntry> entries = new ArrayList<CaveBiomeEntry>();
        HashSet<ResourceLocation> configured = new HashSet<ResourceLocation>();
        CaveBiomeRegistryLoader.addAll(config.primary, CaveBiomeCategory.PRIMARY, biomeRegistry, entries, configured);
        CaveBiomeRegistryLoader.addAll(config.transition, CaveBiomeCategory.TRANSITION, biomeRegistry, entries, configured);
        CaveBiomeRegistryLoader.addAll(config.special, CaveBiomeCategory.SPECIAL, biomeRegistry, entries, configured);
        CaveBiomeRegistryLoader.addAll(config.coastal, CaveBiomeCategory.COASTAL, biomeRegistry, entries, configured);
        CaveBiomeAutoRegistry.append(biomeRegistry, config, entries, configured);
        cached = new CaveBiomeRegistry(entries, biomeRegistry);
        cachedBiomeCount = biomeCount;
        return cached;
    }

    private static void addAll(List<TFCaveBiomeConfig.Entry> list, CaveBiomeCategory category, Registry<Biome> biomeRegistry, List<CaveBiomeEntry> out, Set<ResourceLocation> configured) {
        for (TFCaveBiomeConfig.Entry parsed : list) {
            CaveBiomeEntry entry = CaveBiomeRegistryLoader.buildEntry(parsed, category, biomeRegistry);
            if (entry == null) continue;
            out.add(entry);
            configured.add(entry.biome());
        }
    }

    private static CaveBiomeEntry buildEntry(TFCaveBiomeConfig.Entry parsed, CaveBiomeCategory category, Registry<Biome> biomeRegistry) {
        CavePlacementType placementType;
        ResourceLocation loc = CaveBiomeIds.resolve(parsed.biomeId(), biomeRegistry);
        if (loc == null) {
            TerraForged.LOG.warn("[CaveBiomeRegistry] Unknown biome '{}', skipped", parsed.biomeId());
            return null;
        }
        if (CaveBiomeIds.isBlockedCaveBiome(loc) || CaveBiomeIds.isNetherThemedBiome(loc)) {
            TerraForged.LOG.info("[CaveBiomeRegistry] Blocked cave biome '{}', skipped", loc);
            return null;
        }
        try {
            placementType = CavePlacementType.fromString(parsed.placementType());
        }
        catch (Exception e) {
            placementType = CavePlacementType.FULL_REGION;
        }
        CaveBiomeStats stats = CaveBiomeStatDefaults.isExplicitlyConfigured(parsed.stats()) ? parsed.stats() : CaveBiomeStatDefaults.infer(loc);
        boolean generator = parsed.statGenerator() || CaveBiomeStatDefaults.isGenerator(loc);
        return new CaveBiomeEntry(loc, category, placementType, CaveBiomeRegistryLoader.clamp(parsed.temperature(), 0.0f, 1.0f), CaveBiomeRegistryLoader.clamp(parsed.vegetationDensity(), 0.0f, 1.0f), CaveBiomeRegistryLoader.clamp(parsed.weight(), 0.1f, 10.0f), CaveBiomeRegistryLoader.clamp(parsed.ceilingPatchMin(), 0.0f, 1.0f), CaveBiomeRegistryLoader.clamp(parsed.ceilingPatchMax(), 0.0f, 1.0f), CaveBiomeRegistryLoader.clamp(parsed.islandMaxRadius(), 0.5f, 10.0f), stats, generator);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
