package com.terraforged.mod.worldgen.biome.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tokenizes biome paths and applies {@code X_of_Y} splitting:
 * <ul>
 *   <li>left = primary form base (land_of_rivers → land/plains first)</li>
 *   <li>right = climate/tags + secondary form (rivers → also add river terrain)</li>
 * </ul>
 */
public final class BiomeNameTokens {
    private BiomeNameTokens() {
    }

    public record Parsed(
            List<String> all,
            List<String> formTokens,
            List<String> secondaryFormTokens,
            List<String> climateTokens
    ) {}

    public static Parsed parsePath(String path) {
        List<String> raw = tokenizeKeepOf(path == null ? "" : path);
        int of = raw.indexOf("of");
        if (of < 0) {
            List<String> tokens = stripOf(raw);
            return new Parsed(tokens, tokens, List.of(), tokens);
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
        // Primary form from left only (land stays land — do NOT promote rivers to primary).
        List<String> form = new ArrayList<>(left);
        List<String> secondary = new ArrayList<>(right);
        List<String> climate = new ArrayList<>(right);
        climate.addAll(left);
        return new Parsed(distinct(stripOf(raw)), distinct(form), distinct(secondary), distinct(climate));
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
