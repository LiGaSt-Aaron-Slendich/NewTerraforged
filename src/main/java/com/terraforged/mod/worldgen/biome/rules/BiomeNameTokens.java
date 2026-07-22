package com.terraforged.mod.worldgen.biome.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tokenizes biome paths and applies {@code X_of_Y} splitting:
 * first side = form base, second side = climate / tag source.
 * If the left side is vague ({@code land}, {@code realm}…) and the right carries a real form
 * word ({@code rivers}), form also reads the right side.
 */
public final class BiomeNameTokens {
    private static final Set<String> VAGUE_BASE = Set.of(
            "land", "lands", "realm", "realms", "region", "regions", "place", "places",
            "world", "domain", "area", "zone", "biome", "biomes", "vale", "valley"
    );

    private BiomeNameTokens() {
    }

    public record Parsed(List<String> all, List<String> formTokens, List<String> climateTokens) {}

    public static Parsed parsePath(String path) {
        List<String> raw = tokenizeKeepOf(path == null ? "" : path);
        int of = raw.indexOf("of");
        if (of < 0) {
            List<String> tokens = stripOf(raw);
            return new Parsed(tokens, tokens, tokens);
        }
        List<String> left = new ArrayList<>();
        for (int i = 0; i < of; i++) {
            String t = raw.get(i);
            if (!"of".equals(t)) {
                left.add(t);
            }
        }
        List<String> right = new ArrayList<>();
        for (int i = of + 1; i < raw.size(); i++) {
            String t = raw.get(i);
            if (!"of".equals(t)) {
                right.add(t);
            }
        }

        // First word(s) = form base; second side = climate/tag.
        List<String> form = new ArrayList<>(left);
        List<String> climate = new ArrayList<>(right);

        // land_of_rivers: vague left → also take form from right (rivers → river terrain).
        boolean vague = left.isEmpty() || left.stream().allMatch(VAGUE_BASE::contains);
        if (vague) {
            form.addAll(right);
        }
        // Left-side climate adjectives still apply (warm_land_of_…).
        climate.addAll(left);

        return new Parsed(distinct(stripOf(raw)), distinct(form), distinct(climate));
    }

    public static List<String> tokenizeKeepOf(String path) {
        String[] parts = path.toLowerCase(Locale.ROOT).split("[_/\\-.]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                out.add(p);
            }
        }
        return out;
    }

    private static List<String> stripOf(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String t : in) {
            if (!"of".equals(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<String> distinct(List<String> in) {
        return List.copyOf(new LinkedHashSet<>(in));
    }
}
