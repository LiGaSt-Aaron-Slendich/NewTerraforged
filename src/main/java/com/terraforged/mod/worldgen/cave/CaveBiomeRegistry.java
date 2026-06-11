package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.noise.util.Noise;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class CaveBiomeRegistry {
    private static final ResourceLocation FALLBACK_MC = new ResourceLocation("minecraft", "dripstone_caves");
    private final boolean vanillaFallback;
    private final List<CaveBiomeEntry> primary;
    private final List<CaveBiomeEntry> transition;
    private final List<CaveBiomeEntry> special;
    private final List<CaveBiomeEntry> coastal;
    private final Set<ResourceLocation> coastalBiomeIds;
    private final TreeMap<Float, List<CaveBiomeEntry>> temperatureIndex;
    private final Registry<Biome> biomeRegistry;

    public CaveBiomeRegistry(List<CaveBiomeEntry> allEntries, Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
        List<CaveBiomeEntry> available = allEntries.stream().filter(e -> e.isAvailable(biomeRegistry)).collect(Collectors.toList());
        this.primary = CaveBiomeRegistry.filter(available, CaveBiomeCategory.PRIMARY);
        this.transition = CaveBiomeRegistry.filter(available, CaveBiomeCategory.TRANSITION);
        this.special = CaveBiomeRegistry.filter(available, CaveBiomeCategory.SPECIAL);
        this.coastal = CaveBiomeRegistry.filter(available, CaveBiomeCategory.COASTAL);
        this.coastalBiomeIds = this.coastal.stream().map(CaveBiomeEntry::biome).collect(Collectors.toSet());
        this.vanillaFallback = this.primary.isEmpty();
        this.temperatureIndex = CaveBiomeRegistry.buildTemperatureIndex(available);
        if (this.vanillaFallback) {
            TerraForged.LOG.info("[CaveBiomeRegistry] No PRIMARY cave biomes found, using vanilla fallback");
        } else {
            TerraForged.LOG.info("[CaveBiomeRegistry] Loaded: {} primary, {} transition, {} special, {} coastal", this.primary.size(), this.transition.size(), this.special.size(), this.coastal.size());
        }
    }

    public boolean isVanillaFallback() {
        return this.vanillaFallback;
    }

    public List<CaveBiomeEntry> getPrimary() {
        return this.primary;
    }

    public List<CaveBiomeEntry> getTransition() {
        return this.transition;
    }

    public List<CaveBiomeEntry> getSpecial() {
        return this.special;
    }

    public List<CaveBiomeEntry> getSpecial(CavePlacementType placementType) {
        ArrayList<CaveBiomeEntry> out = new ArrayList<CaveBiomeEntry>();
        for (CaveBiomeEntry entry : this.special) {
            if (entry.placementType() != placementType) continue;
            out.add(entry);
        }
        return out;
    }

    public List<CaveBiomeEntry> getCoastal() {
        return this.coastal;
    }

    public boolean isCoastalBiome(ResourceLocation id) {
        return id != null && this.coastalBiomeIds.contains(id);
    }

    public boolean isCoastalBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> this.isCoastalBiome(key.location())).orElse(false);
    }

    public List<CaveBiomeEntry> getMegaGigaShellPool() {
        HashSet<ResourceLocation> seen = new HashSet<ResourceLocation>();
        ArrayList<CaveBiomeEntry> out = new ArrayList<CaveBiomeEntry>();
        for (CaveBiomeEntry entry : this.primary) {
            if (entry.statGenerator() || CaveBiomeIds.isMegaGigaExcluded(entry.biome()) || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || CaveBiomeIds.isSparseCaveBiome(entry.biome()) || !seen.add(entry.biome())) continue;
            out.add(entry);
        }
        for (CaveBiomeEntry entry : this.transition) {
            if (entry.statGenerator() || CaveBiomeIds.isMegaGigaExcluded(entry.biome()) || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || CaveBiomeIds.isSparseCaveBiome(entry.biome()) || !seen.add(entry.biome())) continue;
            out.add(entry);
        }
        return List.copyOf(out);
    }

    public List<CaveBiomeEntry> getMegaGigaGeneratorPool() {
        return this.primary.stream().filter(CaveBiomeEntry::statGenerator).filter(e -> !CaveBiomeIds.isMegaGigaExcluded(e.biome())).toList();
    }

    public CaveBiomeEntry findTransitionBetween(float tempA, float tempB) {
        return this.findTransitionBetween(tempA, tempB, null, null);
    }

    public CaveBiomeEntry findTransitionBetween(float tempA, float tempB, ResourceLocation biomeA, ResourceLocation biomeB) {
        float tMax;
        CaveBiomeEntry paired;
        if (biomeA != null && biomeB != null && (paired = this.findPairedTransition(biomeA, biomeB)) != null) {
            return paired;
        }
        float tMin = Math.min(tempA, tempB);
        float mid = (tMin + (tMax = Math.max(tempA, tempB))) * 0.5f;
        CaveBiomeEntry best = this.pickTransition(mid, tMin, tMax, true);
        if (best != null) {
            return best;
        }
        best = this.pickTransition(mid, tMin, tMax, false);
        if (best != null) {
            return best;
        }
        return this.makeFallbackEntry(mid);
    }

    private CaveBiomeEntry findPairedTransition(ResourceLocation a, ResourceLocation b) {
        if (CaveBiomeIds.isJungleThermalPair(a, b)) {
            return this.findEntryByPath("steaming_jungle");
        }
        if (CaveBiomeIds.isFungalJunglePair(a, b)) {
            return this.findEntryByPath("mycotoxic");
        }
        if (CaveBiomeIds.isMantleThermalPair(a, b)) {
            return this.findEntryByPath("quartz_desert");
        }
        if (CaveBiomeIds.isFrostWarmPair(a, b)) {
            return this.findEntryByPath("shattered_glacier");
        }
        return null;
    }

    private CaveBiomeEntry findEntryByPath(String pathToken) {
        for (CaveBiomeEntry entry : this.transition) {
            if (!entry.biome().getPath().toLowerCase().contains(pathToken)) continue;
            return entry;
        }
        for (CaveBiomeEntry entry : this.primary) {
            if (!entry.biome().getPath().toLowerCase().contains(pathToken)) continue;
            return entry;
        }
        return null;
    }

    private CaveBiomeEntry pickTransition(float mid, float tMin, float tMax, boolean featureRichOnly) {
        CaveBiomeEntry best = null;
        float bestDist = Float.MAX_VALUE;
        CaveBiomeEntry fallback = null;
        float fallbackDist = Float.MAX_VALUE;
        float span = tMax - tMin;
        for (CaveBiomeEntry entry : this.transition) {
            if (CaveBiomeIds.isEmptyStoneCave(entry.biome()) || featureRichOnly && !CaveBiomeIds.isFeatureRichTransition(entry.biome()) || CaveBiomeIds.isGenericRedstoneTransition(entry.biome()) && span > 0.22f) continue;
            float temp = entry.caveTemperature();
            float d = Math.abs(temp - mid);
            if (d < fallbackDist) {
                fallbackDist = d;
                fallback = entry;
            }
            if (temp < tMin - 0.02f || temp > tMax + 0.02f || !(d < bestDist)) continue;
            bestDist = d;
            best = entry;
        }
        return best != null ? best : fallback;
    }

    public boolean needsTransitionBuffer(CaveBiomeEntry a, CaveBiomeEntry b) {
        return Math.abs(a.caveTemperature() - b.caveTemperature()) > 0.4f;
    }

    private static List<CaveBiomeEntry> filter(List<CaveBiomeEntry> src, CaveBiomeCategory cat) {
        return src.stream().filter(e -> e.category() == cat).sorted(Comparator.comparingDouble(CaveBiomeEntry::caveTemperature)).collect(Collectors.toList());
    }

    private static TreeMap<Float, List<CaveBiomeEntry>> buildTemperatureIndex(List<CaveBiomeEntry> entries) {
        TreeMap<Float, List<CaveBiomeEntry>> map = new TreeMap<Float, List<CaveBiomeEntry>>();
        for (CaveBiomeEntry e : entries) {
            map.computeIfAbsent(Float.valueOf(e.caveTemperature()), k -> new ArrayList()).add(e);
        }
        return map;
    }

    private CaveBiomeEntry makeFallbackEntry(float temperature) {
        ResourceLocation loc = this.resolveFallbackBiome();
        return new CaveBiomeEntry(loc, CaveBiomeCategory.TRANSITION, CavePlacementType.FULL_REGION, temperature, 0.3f, 1.0f, 0.2f, 0.5f, 1.5f, CaveBiomeStats.EMPTY, false);
    }

    private ResourceLocation resolveFallbackBiome() {
        ResourceLocation current = TerraForged.location("cave");
        if (this.biomeRegistry.containsKey(current)) {
            return current;
        }
        ResourceLocation legacy = new ResourceLocation("terraforged", "cave");
        if (this.biomeRegistry.containsKey(legacy)) {
            return legacy;
        }
        return FALLBACK_MC;
    }

    public Optional<Holder<Biome>> getHolder(CaveBiomeEntry entry) {
        return this.biomeRegistry.getHolder(ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)entry.biome()));
    }

    public CaveBiomeEntry pickCoastalEntrance(float caveTemperature, int seed, int x, int z) {
        if (this.coastal.isEmpty()) {
            return null;
        }
        CaveBiomeEntry best = null;
        float bestScore = Float.MAX_VALUE;
        float tie = (Noise.singleSimplex((float)x * 0.002f, (float)z * 0.002f, seed ^ 0xC04A1) + 1.0f) * 0.5f * 0.08f;
        for (CaveBiomeEntry entry : this.coastal) {
            float tempDist;
            float score;
            if (CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || !((score = (tempDist = Math.abs(entry.caveTemperature() - caveTemperature)) / Math.max(0.05f, entry.weight()) + tie) < bestScore)) continue;
            bestScore = score;
            best = entry;
        }
        return best != null ? best : this.coastal.get(0);
    }
}
