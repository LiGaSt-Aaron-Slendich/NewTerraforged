package com.terraforged.mod.worldgen.biome.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Detects per-mod volcano "kits" (cone/peaks + crater/vent) so a single volcano
 * prefers biomes from one namespace instead of mixing Terralith crater with BYG cone.
 */
public final class VolcanoBiomeKits {
    public enum Role {
        CONE,
        CRATER,
        OTHER
    }

    public record Kit(String namespace, Set<ResourceLocation> cones, Set<ResourceLocation> craters) {
        public boolean complete() {
            return !cones.isEmpty() && !craters.isEmpty();
        }
    }

    private static final Map<String, Kit> BY_NS = new HashMap<>();
    private static final Map<ResourceLocation, Role> ROLES = new HashMap<>();

    private VolcanoBiomeKits() {
    }

    public static synchronized void rebuild(Map<ResourceLocation, BiomeRule> rules) {
        BY_NS.clear();
        ROLES.clear();
        if (rules == null || rules.isEmpty()) {
            return;
        }
        Map<String, Set<ResourceLocation>> cones = new HashMap<>();
        Map<String, Set<ResourceLocation>> craters = new HashMap<>();
        for (Map.Entry<ResourceLocation, BiomeRule> e : rules.entrySet()) {
            ResourceLocation id = e.getKey();
            BiomeRule rule = e.getValue();
            Role role = classify(id, rule);
            ROLES.put(id, role);
            if (role == Role.OTHER) {
                continue;
            }
            String ns = id.getNamespace();
            if (role == Role.CONE) {
                cones.computeIfAbsent(ns, k -> new HashSet<>()).add(id);
            } else if (role == Role.CRATER) {
                craters.computeIfAbsent(ns, k -> new HashSet<>()).add(id);
            }
        }
        Set<String> namespaces = new HashSet<>();
        namespaces.addAll(cones.keySet());
        namespaces.addAll(craters.keySet());
        for (String ns : namespaces) {
            BY_NS.put(ns, new Kit(
                    ns,
                    Set.copyOf(cones.getOrDefault(ns, Set.of())),
                    Set.copyOf(craters.getOrDefault(ns, Set.of()))
            ));
        }
    }

    public static Role role(ResourceLocation id) {
        if (id == null) {
            return Role.OTHER;
        }
        Role r = ROLES.get(id);
        return r == null ? Role.OTHER : r;
    }

    public static boolean isKitNamespace(String namespace) {
        return namespace != null && BY_NS.containsKey(namespace);
    }

    public static Kit kit(String namespace) {
        return namespace == null ? null : BY_NS.get(namespace);
    }

    /**
     * Prefer a complete kit (cone+crater) present in the candidate pool; stable per ~512-block cell.
     */
    public static String preferredNamespace(int blockX, int blockZ, Iterable<ResourceLocation> candidateIds) {
        Set<String> inPool = new HashSet<>();
        Set<String> coneNs = new HashSet<>();
        Set<String> craterNs = new HashSet<>();
        for (ResourceLocation id : candidateIds) {
            if (id == null) {
                continue;
            }
            Role role = role(id);
            if (role == Role.OTHER) {
                continue;
            }
            inPool.add(id.getNamespace());
            if (role == Role.CONE) {
                coneNs.add(id.getNamespace());
            } else if (role == Role.CRATER) {
                craterNs.add(id.getNamespace());
            }
        }
        if (inPool.isEmpty()) {
            return null;
        }
        List<String> complete = new ArrayList<>();
        for (String ns : inPool) {
            Kit kit = BY_NS.get(ns);
            // Complete if registry kit has both roles, and this pool has the needed side(s).
            if (kit != null && kit.complete() && coneNs.contains(ns) && craterNs.contains(ns)) {
                complete.add(ns);
            } else if (kit != null && kit.complete() && (coneNs.contains(ns) || craterNs.contains(ns))) {
                // Pool only has one side right now (climate filter) — still prefer complete kits.
                complete.add(ns);
            }
        }
        List<String> pickFrom = complete.isEmpty() ? new ArrayList<>(inPool) : complete;
        Collections.sort(pickFrom);
        int cellX = blockX >> 9; // 512-block cells
        int cellZ = blockZ >> 9;
        int h = cellX * 734287 + cellZ * 19349663;
        if (h < 0) {
            h = -h;
        }
        return pickFrom.get(h % pickFrom.size());
    }

    public static Role classify(ResourceLocation id, BiomeRule rule) {
        if (id == null) {
            return Role.OTHER;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("crater") || path.contains("caldera") || path.contains("vent")
                || path.contains("fumarole") || path.contains("magma_chamber")) {
            return Role.CRATER;
        }
        // volcanic_plains / ashen fields = near-ring biomes, not cone/crater kit halves.
        if ((path.contains("volcan") || path.contains("ashen"))
                && (path.contains("plain") || path.contains("field") || path.contains("flat")
                || path.contains("steppe") || path.contains("meadow"))) {
            return Role.OTHER;
        }
        boolean pipe = rule != null && rule.terrains.containsKey("volcano_pipe");
        boolean cone = rule != null && (rule.terrains.containsKey("volcano") || rule.terrains.containsKey("island_volcano"));
        if (pipe && !cone) {
            return Role.CRATER;
        }
        if (path.contains("volcano") || path.contains("volcanic") || cone) {
            return Role.CONE;
        }
        return Role.OTHER;
    }
}
