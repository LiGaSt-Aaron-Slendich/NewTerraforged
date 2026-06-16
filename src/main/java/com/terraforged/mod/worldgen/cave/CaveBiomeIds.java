package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class CaveBiomeIds {
    private static final Map<String, String> CONFIG_ALIASES = Map.ofEntries(Map.entry("regions_unexplored:ancient_delta_caves", "regions_unexplored:ancient_delta"), Map.entry("regions_unexplored:fungal_caves", "regions_unexplored:bioshroom_caves"), Map.entry("regions_unexplored:mycotoxic_caves", "regions_unexplored:mycotoxic_undergrowth"), Map.entry("byg:crimson_gardens_caves", "byg:crimson_gardens"), Map.entry("byg:shattered_glacier_caves", "byg:shattered_glacier"), Map.entry("byg:nightshade_redwoods_caves", "byg:nightshade_redwoods"), Map.entry("byg:quartz_desert_caves", "byg:quartz_desert"), Map.entry("biomesoplenty:undergarden", "biomesoplenty:glowing_grotto"), Map.entry("wildnature:glowshroom_caves", "wildernature:glowshroom_caves"));

    private CaveBiomeIds() {
    }

    public static ResourceLocation resolve(String id, Registry<Biome> biomeRegistry) {
        ResourceLocation loc;
        try {
            loc = new ResourceLocation(id.trim());
        }
        catch (Exception e) {
            return null;
        }
        ResourceLocation resolved = CaveBiomeIds.resolveDirect(loc, biomeRegistry);
        if (resolved != null) {
            return resolved;
        }
        String alias = CONFIG_ALIASES.get(loc.toString());
        if (alias != null) {
            try {
                resolved = CaveBiomeIds.resolveDirect(new ResourceLocation(alias), biomeRegistry);
                if (resolved != null) {
                    return resolved;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return CaveBiomeIds.resolveFuzzy(loc, biomeRegistry);
    }

    private static ResourceLocation resolveDirect(ResourceLocation loc, Registry<Biome> biomeRegistry) {
        ResourceLocation stripped;
        ResourceLocation alt;
        if (biomeRegistry.containsKey(loc)) {
            return loc;
        }
        if (!loc.getPath().startsWith("cave/") && biomeRegistry.containsKey(alt = new ResourceLocation(loc.getNamespace(), "cave/" + loc.getPath()))) {
            return alt;
        }
        String path = loc.getPath();
        if (path.endsWith("_caves") && biomeRegistry.containsKey(stripped = new ResourceLocation(loc.getNamespace(), path.substring(0, path.length() - 6)))) {
            return stripped;
        }
        if (path.endsWith("_caverns") && biomeRegistry.containsKey(stripped = new ResourceLocation(loc.getNamespace(), path.substring(0, path.length() - 8)))) {
            return stripped;
        }
        return null;
    }

    private static ResourceLocation resolveFuzzy(ResourceLocation loc, Registry<Biome> biomeRegistry) {
        String core = CaveBiomeIds.normalizeBiomeToken(loc.getPath());
        if (core.isBlank()) {
            return null;
        }
        ResourceLocation match = null;
        for (ResourceLocation key : biomeRegistry.keySet()) {
            String candidateCore;
            if (!key.getNamespace().equals(loc.getNamespace()) || !core.equals(candidateCore = CaveBiomeIds.normalizeBiomeToken(key.getPath())) && !candidateCore.contains(core) && !core.contains(candidateCore)) continue;
            if (match != null && !match.equals(key)) {
                return null;
            }
            match = key;
        }
        return match;
    }

    private static String normalizeBiomeToken(String path) {
        String core = path.toLowerCase(Locale.ROOT);
        if (core.startsWith("cave/")) {
            core = core.substring(5);
        }
        if (core.endsWith("_caves")) {
            core = core.substring(0, core.length() - 6);
        } else if (core.endsWith("_caverns")) {
            core = core.substring(0, core.length() - 8);
        } else if (core.endsWith("_hypogeal")) {
            core = core.substring(0, core.length() - 9);
        }
        if (core.endsWith("_cave")) {
            core = core.substring(0, core.length() - 5);
        }
        return core;
    }

    public static boolean isUndergroundBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isUndergroundBiome(key.location())).orElse(false);
    }

    public static boolean sameBiomeKey(Holder<Biome> a, Holder<Biome> b) {
        if (a == b) {
            return true;
        }
        Optional ka = a.unwrapKey();
        Optional kb = b.unwrapKey();
        return ka.isPresent() && kb.isPresent() && (ka.get()).equals(kb.get());
    }

    public static boolean sharesCaveTheme(Holder<Biome> a, Holder<Biome> b) {
        if (CaveBiomeIds.sameBiomeKey(a, b)) {
            return true;
        }
        String ka = a.unwrapKey().map(k -> k.location().getPath().toLowerCase()).orElse("");
        String kb = b.unwrapKey().map(k -> k.location().getPath().toLowerCase()).orElse("");
        String themeA = CaveBiomeIds.caveThemeSlug(ka);
        String themeB = CaveBiomeIds.caveThemeSlug(kb);
        return themeA != null && themeA.equals(themeB);
    }

    private static String caveThemeSlug(String path) {
        if (path.contains("fungal") || path.contains("mycotoxic") || path.contains("glowshroom") || path.contains("bioshroom")) {
            return "fungal";
        }
        if (path.contains("crystal")) {
            return "crystal";
        }
        if (path.contains("thermal")) {
            return "thermal";
        }
        if (path.contains("jungle")) {
            return "jungle";
        }
        if (path.contains("ice") || path.contains("frost")) {
            return "ice";
        }
        if (path.contains("lush") || path.contains("mossy")) {
            return "lush";
        }
        return null;
    }

    public static boolean isUndergroundBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (CaveBiomeIds.isBlockedCaveBiome(id) || CaveBiomeIds.isNetherThemedBiome(id)) {
            return false;
        }
        String ns = id.getNamespace();
        String path = id.getPath().toLowerCase();
        if ("minecraft".equals(ns)) {
            return path.contains("cave") || path.contains("dripstone");
        }
        if (CaveBiomeIds.isSurfaceOverworldBiome(path)) {
            return false;
        }
        return path.contains("cave") || path.contains("cavern") || path.contains("grotto") || path.contains("hypogeal") || path.contains("prismachasm") || path.contains("undergarden") || path.contains("mycotoxic") || path.contains("scorching") || path.contains("bioshroom") || CaveBiomeIds.isRegionalShellBiome(path);
    }

    /** Mega/giga shell biomes whose registry id has no "cave" suffix (BYG/RU surface-style ids). */
    public static boolean isRegionalShellBiome(ResourceLocation id) {
        return id != null && CaveBiomeIds.isRegionalShellBiome(id.getPath().toLowerCase());
    }

    private static boolean isRegionalShellBiome(String path) {
        return path.contains("shattered_glacier") || path.contains("nightshade") || path.contains("quartz_desert") || path.contains("ancient_delta") || path.contains("glowing_grotto") || path.contains("brimstone");
    }

    public static boolean isModCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isModCaveBiome(key.location())).orElse(false);
    }

    public static boolean isModCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if ("minecraft".equals(id.getNamespace())) {
            return CaveBiomeIds.isUndergroundBiome(id);
        }
        return CaveBiomeIds.isUndergroundBiome(id);
    }

    public static boolean isBlockedCaveBiome(ResourceLocation id) {
        if (id == null) {
            return true;
        }
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        if (cfg != null && cfg.isBlacklisted(id)) {
            return true;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("underground_jungle") || path.contains("steaming_jungle") || path.contains("cave_underground_jungle") || path.contains("cave_steaming_jungle");
    }

    public static boolean isBlockedCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isBlockedCaveBiome(key.location())).orElse(true);
    }

    /** Nether-themed biomes must not paint or decorate in overworld cave columns. */
    public static boolean isNetherThemedBiome(Holder<Biome> biome) {
        if (biome.unwrapKey().map(key -> CaveBiomeIds.isNetherThemedBiome(key.location())).orElse(false)) {
            return true;
        }
        return Biome.getBiomeCategory(biome) == Biome.BiomeCategory.NETHER;
    }

    public static boolean isNetherThemedBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("crimson") || path.contains("warped") || path.contains("nether") || path.contains("soul_sand") || path.contains("basalt_delta") || path.contains("nylium") || path.contains("weeping_vines") || path.contains("netherwart")) {
            return true;
        }
        return path.contains("soul_fire") || path.contains("warped_forest") || path.contains("crimson_forest") || path.contains("embur_bog") || path.contains("embur");
    }

    public static boolean isMegaGigaExcluded(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("subzero") || path.contains("hypogeal") || path.contains("crystal_caves") || path.contains("crystal_cave") || path.contains("underground_jungle") || path.contains("steaming_jungle");
    }

    public static boolean isMegaGigaExcluded(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isMegaGigaExcluded(key.location())).orElse(false);
    }

    public static boolean isEmptyStoneCave(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("diorite_caves") || path.contains("tuff_caves") || path.contains("andesite_caves") || path.contains("granite_caves");
    }

    public static boolean isSparseCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (CaveBiomeIds.isEmptyStoneCave(id)) {
            return true;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("frostfire") || path.contains("icicle")) {
            return false;
        }
        if (CaveBiomeIds.isCrystalCaveBiome(id) || path.contains("fungal") || path.contains("glowshroom") || path.contains("undergarden") || path.contains("jungle") || path.contains("mantle") || path.contains("brimstone") || path.contains("thermal") || path.contains("mycotoxic") || path.contains("mossy") || path.contains("prismachasm") || path.contains("skyris") || path.contains("glowing_grotto") || path.contains("ancient_delta")) {
            return false;
        }
        return path.contains("deep_caves") || path.contains("ice_caves") || path.contains("infested") || path.contains("desert_caves") || path.contains("frost_caves") || path.contains("redstone_caves") || path.contains("quartz_desert") || path.contains("subzero") || path.contains("hypogeal") || path.contains("nightshade") || path.contains("shattered_glacier");
    }

    public static boolean isSparseCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isSparseCaveBiome(key.location())).orElse(false);
    }

    public static boolean isCrystalCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("crystal_caves") || path.contains("crystal_cave") || path.contains("crystaline") || path.contains("prismachasm") || path.contains("prismarite") || path.contains("prisma");
    }

    public static boolean isSkyrisCaveBiome(ResourceLocation id) {
        return id != null && id.getPath().toLowerCase().contains("skyris");
    }

    public static boolean isSkyrisCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isSkyrisCaveBiome(key.location())).orElse(false);
    }

    public static boolean isCrystalCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isCrystalCaveBiome(key.location())).orElse(false);
    }

    public static boolean isFungalCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("fungal") || path.contains("mycotoxic") || path.contains("glowshroom") || path.contains("bioshroom");
    }

    public static boolean isFungalCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isFungalCaveBiome(key.location())).orElse(false);
    }

    public static boolean isPrismachasmBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isPrismachasmBiome(key.location())).orElse(false);
    }

    public static boolean isPrismachasmBiome(ResourceLocation id) {
        return id != null && id.getPath().toLowerCase().contains("prismachasm");
    }

    public static boolean isCoastalGrottoBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("glowing_grotto") || path.contains("grotto") || path.contains("ancient_delta") || path.contains("dripstone_caves");
    }

    public static boolean isCoastalGrottoBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isCoastalGrottoBiome(key.location())).orElse(false);
    }

    public static boolean isFeatureRichTransition(ResourceLocation id) {
        if (id == null || CaveBiomeIds.isEmptyStoneCave(id) || CaveBiomeIds.isSparseCaveBiome(id)) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("deep_caves") || path.contains("infested") || path.contains("mossy") || path.contains("crystal") || path.contains("icicle") || path.contains("karst") || path.contains("limestone") || path.contains("chalk") || path.contains("mycotoxic") || path.contains("ancient_delta") || path.contains("quartz") || path.contains("steaming_jungle") || path.contains("fungal") || path.contains("glowshroom");
    }

    private static boolean isSurfaceOverworldBiome(String path) {
        if (path.contains("fragment_forest") || path.contains("fragment")) {
            return true;
        }
        if (path.contains("shrunken")) {
            return true;
        }
        if (path.contains("redwood") || path.contains("grove") || path.contains("orchard")) {
            return true;
        }
        if (path.contains("forest") && !path.contains("cave")) {
            return true;
        }
        if (path.contains("plains") || path.contains("meadow") || path.contains("savanna")) {
            return true;
        }
        if (path.contains("jungle") && !path.contains("cave") && !path.contains("underground")) {
            return true;
        }
        if (path.contains("desert") || path.contains("beach") || path.contains("river")) {
            return true;
        }
        if (path.contains("mountain") || path.contains("peak") || path.contains("cliff")) {
            return true;
        }
        if (path.contains("taiga") && !path.contains("cave")) {
            return true;
        }
        return path.contains("swamp") || path.contains("marsh");
    }

    public static boolean isVolcanicCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isVolcanicCaveBiome(key.location())).orElse(false);
    }

    public static boolean isVolcanicCaveBiome(ResourceLocation id) {
        if (id == null || CaveBiomeIds.isThermalSpringsBiome(id)) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("thermal") || path.contains("mantle") || path.contains("brimstone") || path.contains("magma") || path.contains("scorching");
    }

    public static boolean isThermalSpringsBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isThermalSpringsBiome(key.location())).orElse(false);
    }

    public static boolean isThermalSpringsBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("cave_thermal_springs") || path.endsWith("/thermal_springs");
    }

    public static boolean isModThermalPresetBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isModThermalPresetBiome(key.location())).orElse(false);
    }

    public static boolean isModThermalPresetBiome(ResourceLocation id) {
        return id != null && "newterraforged".equals(id.getNamespace()) && id.getPath().equals("cave_thermal_springs");
    }

    public static boolean isModJunglePresetBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isModJunglePresetBiome(key.location())).orElse(false);
    }

    public static boolean isModJunglePresetBiome(ResourceLocation id) {
        return id != null && "newterraforged".equals(id.getNamespace()) && (id.getPath().equals("cave_underground_jungle") || id.getPath().equals("cave_steaming_jungle"));
    }

    public static boolean isUndergroundJungleBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isUndergroundJungleBiome(key.location())).orElse(false);
    }

    public static boolean isUndergroundJungleBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        return id.getPath().toLowerCase().contains("underground_jungle");
    }

    public static boolean isSteamingJungleBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isSteamingJungleBiome(key.location())).orElse(false);
    }

    public static boolean isSteamingJungleBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        return id.getPath().toLowerCase().contains("steaming_jungle");
    }

    public static boolean isSteamingThermalCell(long seed, int wx, int wz) {
        return (NoiseUtil.valCoord2D((int)(seed ^ 0x572EA1L), wx, wz) + 1.0f) * 0.5f > 0.5f;
    }

    public static boolean isDedicatedDecoratedCaveBiome(Holder<Biome> biome) {
        if (CaveBiomeIds.isUndergroundJungleBiome(biome)) {
            return false;
        }
        return CaveBiomeIds.isCustomDecoratedCaveBiome(biome) || CaveBiomeIds.isModThermalPresetBiome(biome) || CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome);
    }

    public static boolean isCustomDecoratedCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> "newterraforged".equals(key.location().getNamespace()) && key.location().getPath().startsWith("cave_")).orElse(false);
    }

    public static boolean isGenericRedstoneTransition(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        return id.getPath().toLowerCase().contains("redstone_caves");
    }

    public static boolean isPatchPaintedBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isPatchPaintedBiome(key.location())).orElse(false);
    }

    public static boolean isPatchPaintedBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("redstone_caves") || path.contains("skyris") || path.contains("prismachasm") || path.contains("crystal_caves") || path.contains("crystal_cave");
    }

    public static boolean isScorchingCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isScorchingCaveBiome(key.location())).orElse(false);
    }

    public static boolean isScorchingCaveBiome(ResourceLocation id) {
        return id != null && id.getPath().toLowerCase().contains("scorching");
    }

    /** Cover-heavy cave biomes need more decoration origins (mycelium, crystal clusters, vents). */
    public static boolean isCoverDenseCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isCoverDenseCaveBiome(key.location())).orElse(false);
    }

    public static boolean isCoverDenseCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("bioshroom") || path.contains("fungal") || path.contains("mycotoxic") || path.contains("crystal") || path.contains("prismachasm") || path.contains("scorching") || path.contains("glowshroom");
    }

    public static boolean isEmburBogBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isEmburBogBiome(key.location())).orElse(false);
    }

    public static boolean isEmburBogBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("embur_bog") || path.contains("embur") && path.contains("bog");
    }

    public static boolean isHeatShellCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("scorching") || path.contains("brimstone") || path.contains("mantle") || path.contains("magma");
    }

    public static boolean isHeatShellCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isHeatShellCaveBiome(key.location())).orElse(false);
    }

    public static boolean isFungalJunglePair(ResourceLocation a, ResourceLocation b) {
        return CaveBiomeIds.pathContains(a, "fungal") && CaveBiomeIds.pathContains(b, "underground_jungle") || CaveBiomeIds.pathContains(b, "fungal") && CaveBiomeIds.pathContains(a, "underground_jungle");
    }

    public static boolean isMantleThermalPair(ResourceLocation a, ResourceLocation b) {
        return CaveBiomeIds.pathContains(a, "mantle") && CaveBiomeIds.isThermalThemedBiome(b) || CaveBiomeIds.pathContains(b, "mantle") && CaveBiomeIds.isThermalThemedBiome(a);
    }

    public static boolean isFrostWarmPair(ResourceLocation a, ResourceLocation b) {
        boolean frostB;
        boolean frostA = CaveBiomeIds.isFrostPath(a);
        return frostA != (frostB = CaveBiomeIds.isFrostPath(b));
    }

    private static boolean isFrostPath(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("frost") || path.contains("frostfire") || path.contains("subzero") || path.contains("icicle") || path.contains("glacier");
    }

    private static boolean pathContains(ResourceLocation id, String token) {
        return id != null && id.getPath().toLowerCase().contains(token);
    }

    public static boolean isJungleThermalPair(ResourceLocation a, ResourceLocation b) {
        return CaveBiomeIds.isUndergroundJungleBiome(a) && CaveBiomeIds.isThermalThemedBiome(b) || CaveBiomeIds.isUndergroundJungleBiome(b) && CaveBiomeIds.isThermalThemedBiome(a);
    }

    public static boolean isThermalThemedBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("thermal_springs") || path.contains("thermal_caves") || path.contains("cave_thermal");
    }

    public static boolean isThermalThemedBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isThermalThemedBiome(key.location())).orElse(false);
    }

    /** Mantle / scorching / brimstone heat-shell biomes that should not border vegetation directly. */
    public static boolean isAggressiveCaveBiome(ResourceLocation id) {
        return CaveBiomeIds.isHeatShellCaveBiome(id);
    }

    public static boolean isAggressiveCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isAggressiveCaveBiome(key.location())).orElse(false);
    }

    /** Dense vegetation cave biomes (fungal, jungle, mossy, etc.). */
    public static boolean isVegetationDenseCaveBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("fungal") || path.contains("mycotoxic") || path.contains("bioshroom") || path.contains("glowshroom") || path.contains("mossy") || path.contains("underground_jungle") || path.contains("steaming_jungle") || path.contains("jungle") && path.contains("cave") || path.contains("nightshade") || path.contains("crimson_gardens");
    }

    public static boolean isVegetationDenseCaveBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isVegetationDenseCaveBiome(key.location())).orElse(false);
    }

    public static boolean isSulfurRiverBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("sulfur") && (path.contains("river") || path.contains("cave_sulfur"));
    }

    public static boolean isSulfurRiverBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeIds.isSulfurRiverBiome(key.location())).orElse(false);
    }

    public static boolean isTransitionTaggedCaveBiome(CaveBiomeEntry entry) {
        return entry != null && entry.category() == CaveBiomeCategory.TRANSITION || entry != null && CaveBiomeIds.isSulfurRiverBiome(entry.biome());
    }

    public static boolean isAggressiveVegetationPair(ResourceLocation a, ResourceLocation b) {
        return CaveBiomeIds.isAggressiveCaveBiome(a) && CaveBiomeIds.isVegetationDenseCaveBiome(b) || CaveBiomeIds.isAggressiveCaveBiome(b) && CaveBiomeIds.isVegetationDenseCaveBiome(a);
    }
}
