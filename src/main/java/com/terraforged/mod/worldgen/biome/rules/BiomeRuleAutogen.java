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
 */
public final class BiomeRuleAutogen {
    private static final Set<String> FORM_MOUNTAIN = Set.of(
            "mountain", "mountains", "peak", "peaks", "summit", "ridge", "highland", "highlands",
            "alps", "alpine", "crag", "cliff", "cliffs", "sierra");
    private static final Set<String> FORM_HILLS = Set.of("hill", "hills", "foothill", "foothills", "rolling");
    private static final Set<String> FORM_PLATEAU = Set.of("plateau", "mesa", "tableland");
    private static final Set<String> FORM_FLAT = Set.of("plains", "plain", "flat", "flats", "steppe", "field", "fields", "meadow");
    private static final Set<String> FORM_BADLANDS = Set.of("badlands", "canyon", "canyons", "butte", "hoodoo", "bryce");
    private static final Set<String> FORM_BEACH = Set.of("beach", "shore", "coast", "dune", "dunes", "barrera", "barrier");
    private static final Set<String> FORM_VOLCANO = Set.of("volcano", "volcanic", "caldera", "crater");
    private static final Set<String> FORM_SWAMP = Set.of("swamp", "marsh", "bog", "fen", "mangrove", "bayou", "wetland");

    private static final Set<String> MATERIAL_SOFT = Set.of(
            "sand", "sandy", "dirt", "mud", "muddy", "clay", "silt", "soil", "loam", "peat", "moss", "gravel", "ash", "dust");
    private static final Set<String> MATERIAL_WET = Set.of(
            "swamp", "marsh", "bog", "fen", "mangrove", "wetland", "muddy", "soggy", "lush", "river", "lake",
            "aquatic", "coral", "kelp", "flooded", "rain", "rainforest");
    private static final Set<String> MATERIAL_SKIP = Set.of(
            "snowy", "snow", "frozen", "ice", "icy", "cold", "warm", "hot", "dry", "lush", "old", "growth",
            "deep", "shallow", "giant", "sparse", "wooded", "temperate", "tropical", "modified");

    private BiomeRuleAutogen() {
    }

    public static BiomeRule generate(ResourceLocation id, Biome biome) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        List<String> tokens = tokenize(path);
        Form form = detectForm(tokens);
        boolean softOrWet = isSoftOrWet(tokens, biome);
        if ((form == Form.MOUNTAIN || form == Form.PEAK) && softOrWet) {
            form = Form.HILLS; // soft/wet material downgrades mountain form → hills
        }

        Map<String, Float> terrains = new LinkedHashMap<>();
        Map<String, Float> subterrains = new LinkedHashMap<>();
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
                if (tokensContain(tokens, FORM_VOLCANO) || tokensContain(tokens, Set.of("basalt", "ash", "magma"))) {
                    subterrains.put("volcanic_beach", 0.7F);
                    climateTags.add("volcanic");
                }
                canSlope = false;
            }
            case VOLCANO -> {
                terrains.put("island_volcano", 1.0F);
                terrains.put("badlands", 0.4F);
                climateTags.add("volcanic");
                canSlope = true;
            }
            case SWAMP -> {
                terrains.put("dales", 1.0F);
                terrains.put("steppe", 0.7F);
                terrains.put("plains", 0.7F);
                terrains.put("island_flats", 0.5F);
                terrains.put("island_hills", 0.4F);
                climateTags.add("wet");
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
                terrains.put("steppe", 1.0F);
                terrains.put("dales", 0.6F);
                terrains.put("island_flats", 0.5F);
            }
        }

        // Climate tags from temp / name
        float temp = biome.getBaseTemperature();
        if (temp > 1.0F) {
            climateTags.add("hot");
        } else if (temp < 0.2F) {
            climateTags.add("cold");
        }
        if (biome.getPrecipitation() == Precipitation.SNOW) {
            climateTags.add("snowy");
        }
        if (tokensContain(tokens, Set.of("jungle", "rainforest", "bamboo", "tropic", "tropics"))) {
            climateTags.add("jungle");
            if (!terrains.containsKey("plains")) {
                terrains.put("plains", 1.0F);
                terrains.put("steppe", 0.8F);
                terrains.put("dales", 0.8F);
            }
            // jungle not on high mountains unless form stayed mountain
            if (form == Form.FLAT || form == Form.HILLS || form == Form.SWAMP) {
                terrains.remove("mountains_1");
                terrains.remove("mountains_2");
                terrains.remove("mountains_3");
            }
        }
        if (tokensContain(tokens, FORM_BADLANDS) || tokensContain(tokens, Set.of("mesa", "outback", "terracotta"))) {
            terrains.clear();
            subterrains.clear();
            terrains.put("badlands", 1.0F);
            subterrains.put("canyon", 0.5F);
            subterrains.put("desert_canyon", 0.5F);
            canSlope = false;
            climateTags.add("mesa");
        }

        if (terrains.isEmpty()) {
            terrains.put("plains", 1.0F);
            terrains.put("steppe", 0.8F);
            terrains.put("hills_1", 0.5F);
        }

        return new BiomeRule(id.toString(), canSlope, distinct(climateTags), terrains, subterrains, true);
    }

    private static List<String> tokenize(String path) {
        String[] parts = path.split("[_/\\-]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            out.add(p.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static Form detectForm(List<String> tokens) {
        // Form wins: scan all tokens; more specific forms first.
        if (tokensContain(tokens, FORM_BEACH)) {
            return Form.BEACH;
        }
        if (tokensContain(tokens, FORM_VOLCANO)) {
            return Form.VOLCANO;
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
        if (tokensContain(tokens, FORM_FLAT)) {
            return Form.FLAT;
        }
        return Form.FLAT;
    }

    private static boolean isSoftOrWet(List<String> tokens, Biome biome) {
        if (tokensContain(tokens, MATERIAL_SOFT) || tokensContain(tokens, MATERIAL_WET)) {
            return true;
        }
        // Name-driven primarily; rain + wetland-ish tokens already covered above.
        return biome.getPrecipitation() == Precipitation.RAIN
                && tokensContain(tokens, Set.of("forest", "grove", "woods"))
                && tokensContain(tokens, MATERIAL_SOFT);
    }

    private static boolean tokensContain(List<String> tokens, Set<String> set) {
        for (String t : tokens) {
            if (set.contains(t)) {
                return true;
            }
            // skip pure climate adjectives when matching form sets that include them? handled by MATERIAL_SKIP elsewhere
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
        FLAT, HILLS, PLATEAU, MOUNTAIN, PEAK, BADLANDS, BEACH, VOLCANO, SWAMP
    }
}
