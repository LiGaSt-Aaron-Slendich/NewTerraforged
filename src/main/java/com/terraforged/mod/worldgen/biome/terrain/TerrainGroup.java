package com.terraforged.mod.worldgen.biome.terrain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Player-facing terrain groups that expand to concrete TerraForged terrain names.
 * Used by biome-terrain-integration.toml ({@code [group.mountains]} / known group keys).
 */
public final class TerrainGroup {
    private static final Map<String, Set<String>> GROUPS = Map.ofEntries(
            Map.entry("mountains", Set.of("mountains_1", "mountains_2", "mountains_3")),
            Map.entry("mountains_ridge", Set.of("mountains_ridge_1", "mountains_ridge_2")),
            // hills = rolling hills only; dales stay separate (swamps allowed there).
            Map.entry("hills", Set.of("hills_1", "hills_2")),
            Map.entry("plains", Set.of("steppe", "plains")),
            Map.entry("plateau", Set.of("plateau")),
            Map.entry("badlands", Set.of("badlands")),
            Map.entry("dolomites", Set.of("dolomites")),
            Map.entry("steppe", Set.of("steppe")),
            Map.entry("dales", Set.of("dales")),
            Map.entry("torridonian", Set.of("torridonian")),
            // Island sub-terrains (painted by IslandFeatureOverlay for integrator filtering).
            Map.entry("island", Set.of("island_hills", "island_plateau", "island_mountains", "island_flats")),
            Map.entry("island_hills", Set.of("island_hills")),
            Map.entry("island_plateau", Set.of("island_plateau")),
            Map.entry("island_mountains", Set.of("island_mountains")),
            Map.entry("island_flats", Set.of("island_flats")),
            Map.entry("island_volcano", Set.of("island_volcano")),
            Map.entry("volcano", Set.of("volcano", "volcano_pipe", "island_volcano")),
            Map.entry("volcano_pipe", Set.of("volcano_pipe")),
            Map.entry("laguna", Set.of("laguna")),
            Map.entry("river", Set.of("river")),
            Map.entry("beach", Set.of("beach"))
    );

    private TerrainGroup() {
    }

    /** Expand group ids and/or raw terrain names into concrete terrain name set. */
    public static Set<String> expand(Iterable<String> names) {
        if (names == null) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("group.")) {
                key = key.substring("group.".length());
            }
            Set<String> group = GROUPS.get(key);
            if (group != null) {
                out.addAll(group);
            } else {
                out.add(key);
            }
        }
        return out.isEmpty() ? Set.of() : Collections.unmodifiableSet(out);
    }

    public static boolean isKnownGroup(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("group.")) {
            key = key.substring("group.".length());
        }
        return GROUPS.containsKey(key);
    }

    public static Set<String> knownGroupIds() {
        return Collections.unmodifiableSet(GROUPS.keySet());
    }
}
