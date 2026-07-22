package com.terraforged.mod.worldgen.biome.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;

/**
 * Builds a starting BiomeRule from biome id tokens + MC temp/precipitation.
 * Form tokens beat material; soft/wet material downgrades mountain/peak form to hills.
 * Climate prefers explicit name tokens (warm_river, muddy_river, …).
 */
public final class BiomeRuleAutogen {
    private static final Set<String> FORM_MOUNTAIN = Set.of(
            "mountain", "mountains", "peak", "peaks", "summit", "ridge", "highland", "highlands",
            "alps", "alpine", "crag", "cliff", "cliffs", "sierra");
    private static final Set<String> FORM_HILLS = Set.of(
            "hill", "hills", "foothill", "foothills", "rolling", "height", "heights", "upland", "uplands");
    private static final Set<String> FORM_PLATEAU = Set.of("plateau", "mesa", "tableland");
    private static final Set<String> FORM_STEPPE = Set.of("steppe", "prairie", "prairies", "veld", "pampa", "pampas");
    private static final Set<String> FORM_FLAT = Set.of(
            "plains", "plain", "flat", "flats", "field", "fields", "meadow", "grassland", "land", "lands");
    private static final Set<String> FORM_BADLANDS = Set.of("badlands", "canyon", "canyons", "butte", "hoodoo", "bryce");
    private static final Set<String> FORM_BEACH = Set.of("beach", "shore", "coast", "dune", "dunes", "barrera", "barrier");
    private static final Set<String> FORM_VOLCANO = Set.of("volcano", "volcanic", "caldera", "crater");
    private static final Set<String> FORM_SWAMP = Set.of("swamp", "marsh", "bog", "fen", "mangrove", "bayou", "wetland");
    private static final Set<String> FORM_RIVER = Set.of("river", "rivers", "stream", "streams", "creek", "creeks", "brook", "brooks");

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
        Form form = detectForm(parsed.formTokens());
        boolean softOrWet = isSoftOrWet(parsed.all(), biome);
        if ((form == Form.MOUNTAIN || form == Form.PEAK) && softOrWet) {
            form = Form.HILLS;
        }

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
                if (tokensContain(parsed.all(), FORM_VOLCANO) || tokensContain(parsed.all(), Set.of("basalt", "ash", "magma"))) {
                    subterrains.put("volcanic_beach", 0.7F);
                    climateTags.add("volcanic");
                    zoneFlags.put(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(112.0F, 1.0F));
                }
            }
            case VOLCANO -> {
                terrains.put("volcano", 1.0F);
                terrains.put("volcano_pipe", 0.85F);
                terrains.put("island_volcano", 1.0F);
                terrains.put("badlands", 0.25F);
                climateTags.add("volcanic");
                zoneFlags.put(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(128.0F, 1.0F));
                canSlope = true;
            }
            case SWAMP -> {
                terrains.put("dales", 1.0F);
                terrains.put("plains", 0.5F);
                terrains.put("island_flats", 0.5F);
                subterrains.put("river_bank", 0.3F);
                climateTags.add("wet");
            }
            case RIVER -> {
                // Engine terrain is TerrainType.RIVER — not steppe/plains.
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
                terrains.put("island_hills", 0.6F);
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
                    subterrains.put("mountain_peak", 0.7F);
                    subterrains.put("bare_mountain_peak", 0.5F);
                } else {
                    subterrains.put("mountain_body", 0.6F);
                    subterrains.put("mountain_foothill", 0.5F);
                    subterrains.put("bare_mountain", 0.4F);
                }
                canSlope = true;
                climateTags.add("alpine");
            }
            case FLAT -> {
                terrains.put("plains", 1.0F);
                terrains.put("steppe", 0.5F);
                terrains.put("dales", 0.6F);
                terrains.put("island_flats", 0.5F);
            }
        }

        applyClimateFromName(climateTags, parsed.climateTokens(), form);
        // Fallback to MC climate only when name gave nothing useful.
        if (climateTags.isEmpty()) {
            applyClimateFromBiome(climateTags, biome);
        } else {
            // Still honour snow precip as additive.
            if (biome.getPrecipitation() == Precipitation.SNOW && !climateTags.contains("snowy")) {
                climateTags.add("snowy");
            }
        }

        if (tokensContain(parsed.all(), FORM_BADLANDS) || tokensContain(parsed.all(), Set.of("mesa", "outback", "terracotta"))) {
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

        if (tokensContain(parsed.all(), Set.of("volcanic", "ashen", "basalt", "magma")) && form != Form.VOLCANO) {
            climateTags.add("volcanic");
            zoneFlags.putIfAbsent(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(96.0F, 0.85F));
        }

        if (tokensContain(parsed.all(), Set.of("jungle", "rainforest", "bamboo", "tropic", "tropics"))) {
            if (!climateTags.contains("jungle")) {
                climateTags.add("jungle");
            }
            if (form == Form.FLAT || form == Form.HILLS || form == Form.SWAMP) {
                terrains.remove("mountains_1");
                terrains.remove("mountains_2");
                terrains.remove("mountains_3");
            }
        }

        if (terrains.isEmpty()) {
            terrains.put("plains", 1.0F);
            terrains.put("steppe", 0.8F);
            terrains.put("hills_1", 0.5F);
        }

        return new BiomeRule(id.toString(), canSlope, distinct(climateTags), terrains, subterrains, zoneFlags, true);
    }

    /**
     * Name-driven climate. Examples:
     * warm_river → warm; muddy_river → temperate + wet; frozen_river → cold + snowy.
     */
    private static void applyClimateFromName(List<String> climateTags, List<String> tokens, Form form) {
        boolean named = false;
        if (tokensContain(tokens, Set.of("warm", "lukewarm", "mild"))) {
            climateTags.add("warm");
            named = true;
        }
        if (tokensContain(tokens, Set.of("hot", "scorched", "burning", "tropic", "tropical"))) {
            climateTags.add("hot");
            named = true;
        }
        if (tokensContain(tokens, Set.of("cold", "cool", "chilly", "frigid"))) {
            climateTags.add("cold");
            named = true;
        }
        if (tokensContain(tokens, Set.of("frozen", "snowy", "snow", "ice", "icy", "glacial"))) {
            climateTags.add("snowy");
            climateTags.add("cold");
            named = true;
        }
        if (tokensContain(tokens, Set.of("muddy", "mud", "humid", "damp", "soggy", "lush", "wet"))) {
            climateTags.add("wet");
            // muddy_* = standard/temperate + wet
            if (tokensContain(tokens, Set.of("muddy", "mud")) && !climateTags.contains("warm") && !climateTags.contains("hot")
                    && !climateTags.contains("cold") && !climateTags.contains("snowy")) {
                climateTags.add("temperate");
            }
            named = true;
        }
        if (tokensContain(tokens, Set.of("dry", "arid", "parched"))) {
            climateTags.add("desert");
            named = true;
        }
        if (tokensContain(tokens, Set.of("desert", "dune", "dunes", "dryland"))) {
            climateTags.add("desert");
            named = true;
        }
        if (tokensContain(tokens, Set.of("savanna", "savannah", "scrub"))) {
            climateTags.add("savanna");
            named = true;
        }
        if (tokensContain(tokens, Set.of("taiga", "boreal", "coniferous"))) {
            climateTags.add("taiga");
            named = true;
        }
        if (tokensContain(tokens, Set.of("tundra"))) {
            climateTags.add("tundra");
            named = true;
        }
        if (tokensContain(tokens, Set.of("temperate", "deciduous", "grove", "forest")) && !climateTags.contains("jungle")) {
            climateTags.add("temperate");
            named = true;
        }
        if (tokensContain(tokens, Set.of("river", "rivers", "stream", "creek", "brook")) || form == Form.RIVER) {
            // Rivers are wet unless an explicit dry adjective won; warm_river stays warm+wet.
            if (!climateTags.contains("desert") && !climateTags.contains("dry")) {
                climateTags.add("wet");
            }
            named = true;
        }
        if (!named && form == Form.RIVER) {
            climateTags.add("temperate");
            climateTags.add("wet");
        }
    }

    private static void applyClimateFromBiome(List<String> climateTags, Biome biome) {
        float temp = biome.getBaseTemperature();
        if (temp > 1.0F) {
            climateTags.add("hot");
        } else if (temp > 0.8F) {
            climateTags.add("warm");
        } else if (temp < 0.2F) {
            climateTags.add("cold");
        } else {
            climateTags.add("temperate");
        }
        if (biome.getPrecipitation() == Precipitation.SNOW) {
            climateTags.add("snowy");
        }
        if (biome.getPrecipitation() == Precipitation.RAIN && temp > 0.5F && temp < 1.0F) {
            climateTags.add("wet");
        }
    }

    private static Form detectForm(List<String> tokens) {
        if (tokensContain(tokens, FORM_BEACH)) {
            return Form.BEACH;
        }
        if (tokensContain(tokens, FORM_VOLCANO)) {
            return Form.VOLCANO;
        }
        if (tokensContain(tokens, FORM_RIVER)) {
            return Form.RIVER;
        }
        if (tokensContain(tokens, FORM_SWAMP)) {
            return Form.SWAMP;
        }
        if (tokensContain(tokens, FORM_BADLANDS)) {
            return Form.BADLANDS;
        }
        if (tokensContain(tokens, Set.of("peak", "peaks", "summit"))) {
            return Form.PEAK;
        }
        if (tokensContain(tokens, FORM_MOUNTAIN)) {
            return Form.MOUNTAIN;
        }
        if (tokensContain(tokens, FORM_PLATEAU)) {
            return Form.PLATEAU;
        }
        if (tokensContain(tokens, FORM_HILLS)) {
            return Form.HILLS;
        }
        if (tokensContain(tokens, FORM_STEPPE)) {
            return Form.STEPPE;
        }
        if (tokensContain(tokens, FORM_FLAT)) {
            return Form.FLAT;
        }
        return Form.FLAT;
    }

    private static boolean isSoftOrWet(List<String> tokens, Biome biome) {
        if (tokensContain(tokens, MATERIAL_SOFT) || tokensContain(tokens, MATERIAL_WET)) {
            return true;
        }
        return biome.getPrecipitation() == Precipitation.RAIN
                && tokensContain(tokens, Set.of("forest", "grove", "woods"))
                && tokensContain(tokens, MATERIAL_SOFT);
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

    private enum Form {
        FLAT, STEPPE, HILLS, PLATEAU, MOUNTAIN, PEAK, BADLANDS, BEACH, VOLCANO, SWAMP, RIVER
    }
}
