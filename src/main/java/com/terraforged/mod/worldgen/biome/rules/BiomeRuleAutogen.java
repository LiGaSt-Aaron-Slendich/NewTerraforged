package com.terraforged.mod.worldgen.biome.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Builds a starting {@link BiomeRule} from <b>mod biome parameters first</b>
 * (temperature, downfall, precipitation, category, BiomeDictionary), with id tokens
 * as a secondary hint. Name alone must not force alpine mountains on warm/wet biomes
 * (e.g. {@code byg:crag_gardens}).
 */
public final class BiomeRuleAutogen {
    private static final Set<String> FORM_MOUNTAIN = Set.of(
            "mountain", "mountains", "peak", "peaks", "summit", "ridge",
            "alps", "alpine", "crag", "cliff", "cliffs", "sierra");
    /** Highland = hills or plateau — never mountains. */
    private static final Set<String> FORM_HILLS = Set.of(
            "hill", "hills", "foothill", "foothills", "rolling", "height", "heights", "upland", "uplands",
            "highland", "highlands");
    private static final Set<String> FORM_PLATEAU = Set.of("plateau", "mesa", "tableland");
    private static final Set<String> FORM_STEPPE = Set.of("steppe", "prairie", "prairies", "veld", "pampa", "pampas");
    private static final Set<String> FORM_FLAT = Set.of(
            "plains", "plain", "flat", "flats", "field", "fields", "meadow", "grassland", "land", "lands");
    private static final Set<String> FORM_BADLANDS = Set.of("badlands", "canyon", "canyons", "butte", "hoodoo", "bryce");
    private static final Set<String> FORM_BEACH = Set.of("beach", "shore", "coast", "dune", "dunes", "barrera", "barrier");
    private static final Set<String> FORM_VOLCANO = Set.of("volcano");
    private static final Set<String> FORM_VOLCANIC_ADJ = Set.of("volcanic");
    private static final Set<String> FORM_CRATER = Set.of("crater", "caldera", "vent", "fumarole");
    /** Default radius for biomes that only appear near an active volcano (not on the cone itself). */
    public static final float NEAR_VOLCANO_RADIUS = 640.0F;
    private static final Set<String> FORM_SWAMP = Set.of("swamp", "marsh", "bog", "fen", "mangrove", "bayou", "wetland");
    private static final Set<String> FORM_RIVER = Set.of("river", "rivers", "stream", "streams", "creek", "creeks", "brook", "brooks");
    private static final Set<String> SOFT_LUSH_NAME = Set.of(
            "garden", "gardens", "jungle", "rainforest", "bamboo", "lush", "orchid", "eucalyptus");

    private static final Set<String> MATERIAL_SOFT = Set.of(
            "sand", "sandy", "dirt", "mud", "muddy", "clay", "silt", "soil", "loam", "peat", "moss", "gravel", "ash", "dust");
    private static final Set<String> MATERIAL_WET = Set.of(
            "swamp", "marsh", "bog", "fen", "mangrove", "wetland", "muddy", "soggy", "lush", "river", "rivers", "lake",
            "aquatic", "coral", "kelp", "flooded", "rain", "rainforest");

    private BiomeRuleAutogen() {
    }

    public static BiomeRule generate(ResourceLocation id, Biome biome) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        BiomeNameTokens.Parsed parsed = BiomeNameTokens.parsePath(path);
        BiomeParams params = BiomeParams.of(id, biome);

        Form nameForm = detectFormFromName(parsed.formTokens(), parsed.all());
        Form paramForm = detectFormFromParams(params);
        Form form = reconcileForm(nameForm, paramForm, params, parsed.all());

        Map<String, Float> terrains = new LinkedHashMap<>();
        Map<String, Float> subterrains = new LinkedHashMap<>();
        Map<String, BiomeRule.ZoneFlag> zoneFlags = new LinkedHashMap<>();
        List<String> climateTags = new ArrayList<>();
        boolean canSlope = false;

        switch (form) {
            case BADLANDS -> {
                terrains.put("badlands", 1.0F);
                subterrains.put("canyon", 0.6F);
                subterrains.put("desert_canyon", 0.4F);
                climateTags.add("desert");
            }
            case BEACH -> {
                terrains.put("beach", 1.0F);
                subterrains.put("ocean_beach", 0.7F);
                subterrains.put("sea_beach", 0.3F);
                if (tokensContain(parsed.all(), FORM_VOLCANO) || tokensContain(parsed.all(), FORM_VOLCANIC_ADJ)
                        || tokensContain(parsed.all(), FORM_CRATER)
                        || tokensContain(parsed.all(), Set.of("basalt", "ash", "magma"))
                        || params.hasDict(Type.WASTELAND) && params.temp >= 0.8F) {
                    subterrains.put("volcanic_beach", 0.7F);
                    climateTags.add("volcanic");
                    zoneFlags.put(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(NEAR_VOLCANO_RADIUS, 1.0F));
                }
            }
            case CRATER -> {
                terrains.put("volcano_pipe", 1.0F);
                climateTags.add("volcanic");
                canSlope = true;
            }
            case VOLCANO -> {
                terrains.put("volcano", 1.0F);
                terrains.put("island_volcano", 1.0F);
                climateTags.add("volcanic");
                canSlope = true;
            }
            case SWAMP -> {
                terrains.put("dales", 1.0F);
                terrains.put("plains", 0.5F);
                terrains.put("island_flats", 0.5F);
                climateTags.add("wet");
            }
            case RIVER -> {
                terrains.put("river", 1.0F);
                subterrains.put("river_bank", 1.0F);
            }
            case STEPPE -> {
                terrains.put("steppe", 1.0F);
                terrains.put("island_flats", 0.4F);
            }
            case PLATEAU -> {
                terrains.put("plateau", 1.0F);
                terrains.put("island_plateau", 0.5F);
                canSlope = true;
            }
            case HILLS -> {
                terrains.put("hills_1", 1.0F);
                terrains.put("hills_2", 1.0F);
                terrains.put("torridonian", 0.75F);
                terrains.put("island_hills", 0.6F);
                terrains.put("plains", 0.40F);
                terrains.put("mountains_1", 0.45F);
                terrains.put("mountains_2", 0.45F);
                terrains.put("mountains_3", 0.35F);
                subterrains.put("mountain_foothill", 0.55F);
                subterrains.put("mountain_body", 0.50F);
                subterrains.put("bare_mountain", 0.35F);
                if (tokensContain(parsed.all(), Set.of("highland", "highlands"))
                        || params.category == BiomeCategory.MOUNTAIN && params.isWarmWet()) {
                    terrains.put("plateau", 0.9F);
                    terrains.put("island_plateau", 0.5F);
                }
                canSlope = true;
            }
            case MOUNTAIN, PEAK -> {
                terrains.put("mountains_1", 1.0F);
                terrains.put("mountains_2", 1.0F);
                terrains.put("mountains_3", 1.0F);
                terrains.put("mountains_ridge_1", 0.8F);
                terrains.put("mountains_ridge_2", 0.8F);
                terrains.put("dolomites", 0.6F);
                terrains.put("torridonian", 0.5F);
                terrains.put("island_mountains", 0.6F);
                if (form == Form.PEAK) {
                    // Peak biomes only on summit band — keeps forests off 7km-style crests.
                    subterrains.put("mountain_peak", 1.0F);
                    subterrains.put("bare_mountain_peak", 0.75F);
                } else {
                    subterrains.put("mountain_body", 0.6F);
                    subterrains.put("mountain_foothill", 0.5F);
                    subterrains.put("bare_mountain", 0.4F);
                }
                canSlope = true;
                // Alpine tag only when climate params say cold — not every *mountain* name.
                if (params.isColdAlpine()) {
                    climateTags.add("alpine");
                }
            }
            case FLAT -> {
                terrains.put("plains", 1.0F);
                terrains.put("steppe", 0.5F);
                terrains.put("dales", 0.6F);
                terrains.put("torridonian", 0.35F);
                terrains.put("hills_1", 0.45F);
                terrains.put("hills_2", 0.45F);
                // Foothill/body only — never peak (peak subterrains block summit forests).
                terrains.put("mountains_1", 0.30F);
                terrains.put("mountains_2", 0.30F);
                terrains.put("mountains_3", 0.25F);
                subterrains.put("mountain_foothill", 0.55F);
                subterrains.put("mountain_body", 0.50F);
                subterrains.put("bare_mountain", 0.35F);
                terrains.put("island_flats", 0.5F);
            }
        }

        // Climate: parameters are authoritative; name adjectives refine.
        applyClimateFromBiome(climateTags, params);
        applyClimateFromName(climateTags, parsed.climateTokens(), form);
        applyClimateFromDictionary(climateTags, params);

        mergeSecondaryForm(terrains, subterrains, parsed.secondaryFormTokens());

        if (tokensContain(parsed.all(), FORM_BADLANDS) || tokensContain(parsed.all(), Set.of("mesa", "outback", "terracotta"))
                || params.category == BiomeCategory.MESA) {
            if (form != Form.BADLANDS) {
                terrains.clear();
                subterrains.clear();
                terrains.put("badlands", 1.0F);
                subterrains.put("canyon", 0.5F);
                subterrains.put("desert_canyon", 0.5F);
                canSlope = false;
                climateTags.add("mesa");
            }
        }

        if ((tokensContain(parsed.all(), FORM_VOLCANIC_ADJ) || tokensContain(parsed.all(), Set.of("ashen", "basalt", "magma")))
                && form != Form.VOLCANO && form != Form.CRATER) {
            climateTags.add("volcanic");
            zoneFlags.putIfAbsent(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(NEAR_VOLCANO_RADIUS, 0.85F));
        }

        if (params.isJungleLike() || tokensContain(parsed.all(), Set.of("jungle", "rainforest", "bamboo", "tropic", "tropics"))) {
            if (!climateTags.contains("jungle")) {
                climateTags.add("jungle");
            }
            // Warm lush biomes never keep mountain slots from a misleading "crag" token.
            if (form == Form.FLAT || form == Form.HILLS || form == Form.SWAMP || form == Form.PLATEAU
                    || params.isWarmWet()) {
                terrains.keySet().removeIf(k -> k.startsWith("mountains") || "dolomites".equals(k)
                        || "torridonian".equals(k) || "island_mountains".equals(k));
                subterrains.keySet().removeIf(k -> k.contains("mountain"));
            }
        }

        // Strip alpine from warm/rainy biomes even if name suggested peaks.
        if (params.isWarmWet() || params.temp >= 0.85F && params.precipitation == Precipitation.RAIN) {
            climateTags.remove("alpine");
            climateTags.remove("tundra");
            climateTags.remove("snowy");
        }

        if (terrains.isEmpty()) {
            terrains.put("plains", 1.0F);
            terrains.put("steppe", 0.8F);
            terrains.put("hills_1", 0.5F);
        }

        // Land climates need foothill/mountain/dale slots so climate pools aren't empty on those terrains.
        ensureCommonLandTerrains(form, terrains, subterrains, params);

        // Alpine / peak biomes: summit band only — forests never use this path.
        if (form == Form.PEAK || climateTags.contains("alpine")) {
            applyPeakOnlySubterrains(terrains, subterrains);
        }

        return new BiomeRule(id.toString(), canSlope, distinct(climateTags), terrains, subterrains, zoneFlags, true);
    }

    /** Peak-only height gate for alpine / summit biomes. */
    private static void applyPeakOnlySubterrains(Map<String, Float> terrains, Map<String, Float> subterrains) {
        if (!hasMountainTerrain(terrains)) {
            terrains.put("mountains_1", 1.0F);
            terrains.put("mountains_2", 1.0F);
            terrains.put("mountains_3", 0.9F);
            terrains.putIfAbsent("island_mountains", 0.6F);
        }
        subterrains.keySet().removeIf(k -> k.contains("mountain") && !k.contains("peak"));
        subterrains.put("mountain_peak", 1.0F);
        subterrains.putIfAbsent("bare_mountain_peak", 0.75F);
    }

    private static boolean hasMountainTerrain(Map<String, Float> terrains) {
        for (String k : terrains.keySet()) {
            if (k.startsWith("mountains") || "dolomites".equals(k) || "island_mountains".equals(k)
                    || "torridonian".equals(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Expand non-specialized forms so temperate/cold/etc. climate pools keep ≥3 candidates
     * on hills, torridonian, mountains, dales, …
     */
    private static void ensureCommonLandTerrains(
            Form form, Map<String, Float> terrains, Map<String, Float> subterrains, BiomeParams params
    ) {
        if (form == Form.BEACH || form == Form.RIVER || form == Form.VOLCANO || form == Form.CRATER
                || form == Form.BADLANDS || form == Form.PEAK) {
            return;
        }
        terrains.putIfAbsent("plains", 0.45F);
        terrains.putIfAbsent("steppe", 0.35F);
        terrains.putIfAbsent("dales", 0.40F);
        terrains.putIfAbsent("hills_1", 0.50F);
        terrains.putIfAbsent("hills_2", 0.50F);
        terrains.putIfAbsent("plateau", 0.35F);
        // Warm jungle-like biomes stay off alpine mountain slots (stripped earlier for a reason).
        boolean allowHigh = !(params != null && (params.isJungleLike() || params.isWarmWet() && params.temp >= 0.85F));
        if (allowHigh) {
            terrains.putIfAbsent("torridonian", 0.40F);
            terrains.putIfAbsent("mountains_1", 0.35F);
            terrains.putIfAbsent("mountains_2", 0.35F);
            terrains.putIfAbsent("mountains_3", 0.30F);
            // Forests/hills on mountains: body + foothill only — never summit.
            if (form != Form.MOUNTAIN) {
                subterrains.putIfAbsent("mountain_foothill", 0.55F);
                subterrains.putIfAbsent("mountain_body", 0.50F);
                subterrains.putIfAbsent("bare_mountain", 0.35F);
                subterrains.remove("mountain_peak");
                subterrains.remove("bare_mountain_peak");
            }
            if (form == Form.MOUNTAIN || form == Form.HILLS || form == Form.PLATEAU) {
                terrains.putIfAbsent("mountains_ridge_1", 0.30F);
                terrains.putIfAbsent("mountains_ridge_2", 0.30F);
                terrains.putIfAbsent("dolomites", 0.25F);
            }
        }
        if (form == Form.STEPPE || form == Form.FLAT) {
            terrains.putIfAbsent("badlands", 0.20F);
        }
    }

    /**
     * True when an existing auto rule contradicts live biome climate parameters
     * (forces regenerate on next sync).
     */
    public static boolean isClimateStale(ResourceLocation id, BiomeRule rule, Biome biome) {
        if (rule == null || !rule.autoGenerated || biome == null) {
            return false;
        }
        BiomeParams params = BiomeParams.of(id, biome);
        boolean alpine = rule.climateTags.contains("alpine");
        boolean mountainTerrain = rule.terrains.containsKey("mountains_1")
                || rule.terrains.containsKey("mountains_2")
                || rule.terrains.containsKey("mountains_3");
        // Warm wet / jungle-like biomes must not sit in alpine-tagged mountain rules.
        if (alpine && (params.isWarmWet() || params.isJungleLike())) {
            return true;
        }
        // Pure mountain-only auto rules on warm/jungle biomes (no soft foothill plains/hills).
        if (mountainTerrain && (params.isWarmWet() || params.isJungleLike())
                && !rule.terrains.containsKey("plains")
                && !rule.terrains.containsKey("hills_1")
                && !rule.terrains.containsKey("hills_2")) {
            return true;
        }
        if (alpine && params.temp >= 0.75F && params.precipitation == Precipitation.RAIN) {
            return true;
        }
        // Snowy biomes without cold/snowy tags.
        if (params.precipitation == Precipitation.SNOW
                && !rule.climateTags.contains("snowy")
                && !rule.climateTags.contains("cold")
                && !rule.climateTags.contains("tundra")) {
            return true;
        }
        // Alpine / peak biomes must gate on summit subterrains.
        if ((alpine || params.isColdAlpine())
                && mountainTerrain
                && !rule.subterrains.containsKey("mountain_peak")
                && !rule.subterrains.containsKey("bare_mountain_peak")) {
            return true;
        }
        // Land forests on mountains must use body/foothill — not unrestricted peaks.
        if (mountainTerrain && !alpine && !params.isColdAlpine()
                && rule.terrains.containsKey("plains")
                && (rule.subterrains.isEmpty()
                || (rule.subterrains.containsKey("mountain_peak")
                && !rule.subterrains.containsKey("mountain_body")
                && !rule.subterrains.containsKey("mountain_foothill")))) {
            return true;
        }
        // Land auto rules missing mountain foothill slots → climate pools starve on mountains.
        if (!params.isJungleLike() && !(params.isWarmWet() && params.temp >= 0.85F)
                && !rule.terrains.containsKey("beach")
                && !rule.terrains.containsKey("volcano")
                && !rule.terrains.containsKey("volcano_pipe")
                && !rule.terrains.containsKey("river")
                && rule.terrains.containsKey("plains")
                && !rule.terrains.containsKey("mountains_1")
                && !rule.terrains.containsKey("torridonian")) {
            return true;
        }
        return false;
    }

    /** Merge secondary of-pattern forms at slightly lower weight (does not replace primary). */
    private static void mergeSecondaryForm(
            Map<String, Float> terrains, Map<String, Float> subterrains, List<String> secondaryTokens
    ) {
        if (secondaryTokens == null || secondaryTokens.isEmpty()) {
            return;
        }
        Form secondary = detectFormFromName(secondaryTokens, secondaryTokens);
        switch (secondary) {
            case RIVER -> {
                terrains.putIfAbsent("river", 0.85F);
                subterrains.putIfAbsent("river_bank", 0.85F);
            }
            case STEPPE -> terrains.putIfAbsent("steppe", 0.7F);
            case HILLS -> {
                terrains.putIfAbsent("hills_1", 0.7F);
                terrains.putIfAbsent("hills_2", 0.7F);
            }
            case FLAT -> {
                terrains.putIfAbsent("plains", 0.7F);
                terrains.putIfAbsent("dales", 0.5F);
            }
            case SWAMP -> terrains.putIfAbsent("dales", 0.7F);
            case BEACH -> terrains.putIfAbsent("beach", 0.7F);
            case BADLANDS -> terrains.putIfAbsent("badlands", 0.7F);
            case PLATEAU -> terrains.putIfAbsent("plateau", 0.7F);
            case MOUNTAIN, PEAK -> {
                terrains.putIfAbsent("mountains_1", 0.7F);
                terrains.putIfAbsent("mountains_2", 0.7F);
            }
            case VOLCANO -> {
                terrains.putIfAbsent("volcano", 0.7F);
                terrains.putIfAbsent("island_volcano", 0.7F);
                terrains.remove("badlands");
                terrains.remove("volcano_pipe");
            }
            case CRATER -> {
                terrains.clear();
                subterrains.clear();
                terrains.put("volcano_pipe", 0.85F);
            }
        }
    }

    /**
     * Overlay name-driven climate / river fixes onto a curated default copy.
     * Keeps cold_desert = cold+desert even when synonym template is desert-only.
     */
    public static BiomeRule enrichFromName(ResourceLocation id, BiomeRule base) {
        if (id == null || base == null) {
            return base;
        }
        BiomeNameTokens.Parsed parsed = BiomeNameTokens.parsePath(id.getPath());
        List<String> climate = new ArrayList<>(base.climateTags);
        Form form = detectFormFromName(parsed.formTokens(), parsed.all());
        applyClimateFromName(climate, parsed.climateTokens(), form);
        Map<String, Float> terrains = new LinkedHashMap<>(base.terrains);
        Map<String, Float> subterrains = new LinkedHashMap<>(base.subterrains);
        Map<String, BiomeRule.ZoneFlag> zones = new LinkedHashMap<>(base.zoneFlags);
        // Pure *river* biomes (not land_of_rivers) must be river-primary (not plains/tundra leftovers).
        boolean ofPattern = id.getPath().toLowerCase(Locale.ROOT).contains("_of_");
        if (!ofPattern && tokensContain(parsed.all(), FORM_RIVER)) {
            float riverW = terrains.getOrDefault("river", 0.0F);
            float bestOther = 0.0F;
            for (Map.Entry<String, Float> e : terrains.entrySet()) {
                if (!"river".equals(e.getKey())) {
                    bestOther = Math.max(bestOther, e.getValue());
                }
            }
            if (riverW <= 0.0F || riverW + 0.001F < bestOther) {
                terrains.clear();
                subterrains.clear();
                terrains.put("river", 1.0F);
                subterrains.put("river_bank", 1.0F);
            }
        }
        if (form == Form.CRATER || tokensContain(parsed.all(), FORM_CRATER)) {
            terrains.clear();
            subterrains.clear();
            terrains.put("volcano_pipe", 1.0F);
            zones.remove(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
        } else if (form == Form.VOLCANO || tokensContain(parsed.all(), FORM_VOLCANO)
                || (tokensContain(parsed.all(), FORM_VOLCANIC_ADJ)
                && (tokensContain(parsed.all(), FORM_MOUNTAIN) || tokensContain(parsed.all(), FORM_HILLS)
                || tokensContain(parsed.all(), Set.of("peak", "peaks"))))) {
            terrains.keySet().removeIf(k -> k.equals("badlands") || k.equals("volcano_pipe"));
            terrains.putIfAbsent("volcano", 1.0F);
            terrains.putIfAbsent("island_volcano", 1.0F);
            zones.remove(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
        }
        BiomeRule.ZoneFlag near = zones.get(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
        if (near != null && near.enabled() && near.radiusBlocks() < 256.0F) {
            zones.put(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(NEAR_VOLCANO_RADIUS, near.chance()));
        }
        mergeSecondaryForm(terrains, subterrains, parsed.secondaryFormTokens());
        if (ofPattern && tokensContain(parsed.formTokens(), FORM_FLAT) && tokensContain(parsed.secondaryFormTokens(), FORM_RIVER)) {
            terrains.putIfAbsent("plains", 1.0F);
            terrains.putIfAbsent("dales", 0.6F);
            terrains.putIfAbsent("steppe", 0.5F);
            terrains.putIfAbsent("river", 0.85F);
            subterrains.putIfAbsent("river_bank", 0.85F);
        }
        return new BiomeRule(
                base.biome,
                base.canBeOnSlope,
                distinct(climate),
                terrains,
                subterrains,
                zones,
                base.autoGenerated
        );
    }

    private static void applyClimateFromName(List<String> climateTags, List<String> tokens, Form form) {
        if (tokensContain(tokens, Set.of("warm", "lukewarm", "mild"))) {
            climateTags.add("warm");
        }
        if (tokensContain(tokens, Set.of("hot", "scorched", "burning", "tropic", "tropical"))) {
            climateTags.add("hot");
        }
        if (tokensContain(tokens, Set.of("cold", "cool", "chilly", "frigid"))) {
            climateTags.add("cold");
        }
        if (tokensContain(tokens, Set.of("frozen", "snowy", "snow", "ice", "icy", "glacial"))) {
            climateTags.add("snowy");
            climateTags.add("cold");
        }
        if (tokensContain(tokens, Set.of("muddy", "mud", "humid", "damp", "soggy", "lush", "wet"))) {
            climateTags.add("wet");
            if (tokensContain(tokens, Set.of("muddy", "mud")) && !climateTags.contains("warm") && !climateTags.contains("hot")
                    && !climateTags.contains("cold") && !climateTags.contains("snowy")) {
                climateTags.add("temperate");
            }
        }
        if (tokensContain(tokens, Set.of("dry", "arid", "parched", "desert", "dune", "dunes", "dryland"))) {
            climateTags.add("desert");
        }
        if (tokensContain(tokens, Set.of("savanna", "savannah", "scrub"))) {
            climateTags.add("savanna");
        }
        if (tokensContain(tokens, Set.of("taiga", "boreal", "coniferous"))) {
            climateTags.add("taiga");
        }
        if (tokensContain(tokens, Set.of("tundra"))) {
            climateTags.add("tundra");
        }
        if (tokensContain(tokens, Set.of("temperate", "deciduous", "grove", "forest")) && !climateTags.contains("jungle")) {
            climateTags.add("temperate");
        }
        if (tokensContain(tokens, Set.of("river", "rivers", "stream", "creek", "brook")) || form == Form.RIVER) {
            if (!climateTags.contains("desert")) {
                climateTags.add("wet");
            }
        }
        if (form == Form.RIVER && climateTags.stream().noneMatch(t ->
                t.equals("warm") || t.equals("hot") || t.equals("cold") || t.equals("temperate") || t.equals("snowy"))) {
            climateTags.add("temperate");
            climateTags.add("wet");
        }
    }

    private static void applyClimateFromBiome(List<String> climateTags, BiomeParams params) {
        float temp = params.temp;
        float downfall = params.downfall;
        if (temp > 1.0F) {
            climateTags.add("hot");
        } else if (temp > 0.8F) {
            climateTags.add("warm");
        } else if (temp < 0.15F) {
            climateTags.add("cold");
            if (params.precipitation == Precipitation.SNOW) {
                climateTags.add("tundra");
            }
        } else if (temp < 0.3F) {
            climateTags.add("cold");
        } else {
            climateTags.add("temperate");
        }
        if (params.precipitation == Precipitation.SNOW) {
            climateTags.add("snowy");
            climateTags.add("cold");
        }
        if (downfall >= 0.85F || (params.precipitation == Precipitation.RAIN && downfall >= 0.55F && temp >= 0.5F)) {
            climateTags.add("wet");
        }
        if (downfall <= 0.15F && temp >= 0.8F) {
            climateTags.add("desert");
        } else if (downfall <= 0.25F && temp >= 0.7F && temp < 1.05F) {
            climateTags.add("savanna");
        }
        if (params.isJungleLike()) {
            climateTags.add("jungle");
            climateTags.add("wet");
        }
        if (params.isColdAlpine()) {
            climateTags.add("alpine");
        }
    }

    private static void applyClimateFromDictionary(List<String> climateTags, BiomeParams params) {
        if (params.hasDict(Type.JUNGLE)) {
            climateTags.add("jungle");
            climateTags.add("wet");
        }
        if (params.hasDict(Type.HOT)) {
            climateTags.add("hot");
        }
        if (params.hasDict(Type.COLD) || params.hasDict(Type.SNOWY)) {
            climateTags.add("cold");
        }
        if (params.hasDict(Type.SNOWY)) {
            climateTags.add("snowy");
        }
        if (params.hasDict(Type.DRY)) {
            if (params.temp >= 0.8F) {
                climateTags.add("desert");
            } else {
                climateTags.add("savanna");
            }
        }
        if (params.hasDict(Type.WET) || params.hasDict(Type.SWAMP)) {
            climateTags.add("wet");
        }
        if (params.hasDict(Type.CONIFEROUS)) {
            climateTags.add("taiga");
        }
        if (params.hasDict(Type.SAVANNA)) {
            climateTags.add("savanna");
        }
        if (params.hasDict(Type.MESA)) {
            climateTags.add("mesa");
            climateTags.add("desert");
        }
    }

    private static Form detectFormFromName(List<String> formTokens, List<String> allTokens) {
        if (tokensContain(formTokens, FORM_BEACH) || tokensContain(allTokens, FORM_BEACH)) {
            return Form.BEACH;
        }
        if (tokensContain(formTokens, FORM_CRATER) || tokensContain(allTokens, FORM_CRATER)) {
            return Form.CRATER;
        }
        if (tokensContain(formTokens, FORM_VOLCANO)
                || (tokensContain(allTokens, FORM_VOLCANIC_ADJ)
                && (tokensContain(allTokens, FORM_MOUNTAIN) || tokensContain(allTokens, FORM_HILLS)
                || tokensContain(allTokens, Set.of("peak", "peaks", "summit"))))) {
            return Form.VOLCANO;
        }
        if (tokensContain(formTokens, FORM_RIVER) || tokensContain(allTokens, FORM_RIVER)) {
            return Form.RIVER;
        }
        if (tokensContain(formTokens, FORM_SWAMP) || tokensContain(allTokens, FORM_SWAMP)) {
            return Form.SWAMP;
        }
        if (tokensContain(formTokens, FORM_BADLANDS) || tokensContain(allTokens, FORM_BADLANDS)) {
            return Form.BADLANDS;
        }
        if (tokensContain(formTokens, Set.of("peak", "peaks", "summit"))
                || tokensContain(allTokens, Set.of("peak", "peaks", "summit"))) {
            return Form.PEAK;
        }
        if (tokensContain(allTokens, SOFT_LUSH_NAME)) {
            if (tokensContain(allTokens, FORM_PLATEAU)) {
                return Form.PLATEAU;
            }
            return Form.HILLS;
        }
        if (tokensContain(formTokens, FORM_MOUNTAIN) || tokensContain(allTokens, FORM_MOUNTAIN)) {
            return Form.MOUNTAIN;
        }
        if (tokensContain(formTokens, FORM_PLATEAU) || tokensContain(allTokens, FORM_PLATEAU)) {
            return Form.PLATEAU;
        }
        if (tokensContain(formTokens, FORM_HILLS) || tokensContain(allTokens, FORM_HILLS)) {
            return Form.HILLS;
        }
        if (tokensContain(formTokens, FORM_STEPPE) || tokensContain(allTokens, FORM_STEPPE)) {
            return Form.STEPPE;
        }
        if (tokensContain(formTokens, FORM_FLAT) || tokensContain(allTokens, FORM_FLAT)) {
            return Form.FLAT;
        }
        return Form.FLAT;
    }

    private static Form detectFormFromParams(BiomeParams params) {
        if (params.category == BiomeCategory.BEACH || params.hasDict(Type.BEACH)) {
            return Form.BEACH;
        }
        if (params.category == BiomeCategory.RIVER || params.hasDict(Type.RIVER)) {
            return Form.RIVER;
        }
        if (params.category == BiomeCategory.SWAMP || params.hasDict(Type.SWAMP)) {
            return Form.SWAMP;
        }
        if (params.category == BiomeCategory.MESA || params.hasDict(Type.MESA)) {
            return Form.BADLANDS;
        }
        if (params.category == BiomeCategory.DESERT || (params.hasDict(Type.SANDY) && params.temp >= 0.9F)) {
            return Form.STEPPE;
        }
        if (params.isJungleLike()) {
            // Elevated tropical "crags" → hills/plateau, never alpine mountain.
            return params.hasDict(Type.PLATEAU) ? Form.PLATEAU : Form.HILLS;
        }
        if (params.category == BiomeCategory.MOUNTAIN || params.hasDict(Type.MOUNTAIN)) {
            if (params.isColdAlpine() && params.precipitation == Precipitation.SNOW) {
                return Form.PEAK;
            }
            if (params.isWarmWet()) {
                return Form.HILLS;
            }
            return Form.MOUNTAIN;
        }
        if (params.category == BiomeCategory.EXTREME_HILLS || params.hasDict(Type.HILLS)) {
            return Form.HILLS;
        }
        if (params.category == BiomeCategory.PLAINS || params.hasDict(Type.PLAINS)) {
            return Form.FLAT;
        }
        if (params.category == BiomeCategory.FOREST || params.hasDict(Type.FOREST)) {
            return Form.HILLS;
        }
        if (params.category == BiomeCategory.TAIGA || params.hasDict(Type.CONIFEROUS)) {
            return Form.HILLS;
        }
        if (params.category == BiomeCategory.SAVANNA || params.hasDict(Type.SAVANNA)) {
            return Form.STEPPE;
        }
        return Form.FLAT;
    }

    /**
     * Parameters veto misleading name forms (warm "crag" ≠ alpine mountain).
     * Strong name forms (volcano/crater/beach/river) still win.
     */
    private static Form reconcileForm(Form nameForm, Form paramForm, BiomeParams params, List<String> allTokens) {
        if (nameForm == Form.VOLCANO || nameForm == Form.CRATER || nameForm == Form.BEACH || nameForm == Form.RIVER) {
            return nameForm;
        }
        if (nameForm == Form.MOUNTAIN || nameForm == Form.PEAK) {
            if (params.isWarmWet() || params.isJungleLike() || tokensContain(allTokens, SOFT_LUSH_NAME)) {
                return paramForm == Form.PLATEAU ? Form.PLATEAU : Form.HILLS;
            }
            if (params.temp >= 0.8F && params.precipitation == Precipitation.RAIN && params.downfall >= 0.4F) {
                return Form.HILLS;
            }
            // Name peak + cold snowy params → keep peak.
            if (nameForm == Form.PEAK && params.isColdAlpine()) {
                return Form.PEAK;
            }
            if (paramForm == Form.MOUNTAIN || paramForm == Form.PEAK) {
                return nameForm;
            }
            // Name says mountain but category is plains/forest/jungle → prefer params.
            if (paramForm != Form.FLAT || params.category == BiomeCategory.MOUNTAIN) {
                return paramForm != Form.FLAT ? paramForm : nameForm;
            }
            return nameForm;
        }
        // Name vague (flat) but params say mountain → use params.
        if ((nameForm == Form.FLAT || nameForm == Form.HILLS)
                && (paramForm == Form.MOUNTAIN || paramForm == Form.PEAK)
                && params.isColdAlpine()) {
            return paramForm;
        }
        if (nameForm == Form.FLAT && paramForm != Form.FLAT) {
            return paramForm;
        }
        return nameForm;
    }

    private static boolean tokensContain(List<String> tokens, Set<String> set) {
        for (String t : tokens) {
            if (set.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> distinct(List<String> in) {
        LinkedHashMap<String, Boolean> m = new LinkedHashMap<>();
        for (String s : in) {
            m.put(s, Boolean.TRUE);
        }
        return List.copyOf(m.keySet());
    }

    /** Snapshot of registry / dictionary climate signals for one biome. */
    private static final class BiomeParams {
        final float temp;
        final float downfall;
        final Precipitation precipitation;
        final BiomeCategory category;
        final Set<Type> dict;

        private BiomeParams(float temp, float downfall, Precipitation precipitation, BiomeCategory category, Set<Type> dict) {
            this.temp = temp;
            this.downfall = downfall;
            this.precipitation = precipitation;
            this.category = category != null ? category : BiomeCategory.NONE;
            this.dict = dict != null ? dict : Set.of();
        }

        static BiomeParams of(ResourceLocation id, Biome biome) {
            float temp = biome.getBaseTemperature();
            float downfall = 0.5F;
            try {
                downfall = biome.getDownfall();
            } catch (Throwable ignored) {
            }
            Precipitation precip = biome.getPrecipitation();
            BiomeCategory cat = BiomeCategory.NONE;
            try {
                cat = Biome.getBiomeCategory(Holder.direct(biome));
            } catch (Throwable ignored) {
            }
            Set<Type> types = Set.of();
            try {
                ResourceKey<Biome> key = ResourceKey.create(ForgeRegistries.BIOMES.getRegistryKey(), id);
                types = BiomeDictionary.getTypes(key);
            } catch (Throwable ignored) {
            }
            return new BiomeParams(temp, downfall, precip, cat, types);
        }

        boolean hasDict(Type type) {
            return dict.contains(type);
        }

        boolean isWarmWet() {
            return temp >= 0.75F
                    && precipitation == Precipitation.RAIN
                    && (downfall >= 0.45F || hasDict(Type.WET) || hasDict(Type.JUNGLE));
        }

        boolean isJungleLike() {
            return category == BiomeCategory.JUNGLE
                    || hasDict(Type.JUNGLE)
                    || (temp >= 0.85F && downfall >= 0.7F && precipitation == Precipitation.RAIN);
        }

        boolean isColdAlpine() {
            if (precipitation == Precipitation.SNOW && temp <= 0.35F) {
                return true;
            }
            if (hasDict(Type.SNOWY) && (category == BiomeCategory.MOUNTAIN || hasDict(Type.MOUNTAIN))) {
                return true;
            }
            return temp <= 0.25F && (category == BiomeCategory.MOUNTAIN || hasDict(Type.MOUNTAIN));
        }
    }

    private enum Form {
        FLAT, STEPPE, HILLS, PLATEAU, MOUNTAIN, PEAK, BADLANDS, BEACH, VOLCANO, CRATER, SWAMP, RIVER
    }
}
