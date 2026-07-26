package com.terraforged.mod.compat.legacy;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * 1.18 BiomeCategory / BiomeDictionary stand-ins for 1.19 (category API removed).
 * Path heuristics only — good enough for rule autogen / cave pool filters at compile time.
 */
public enum LegacyBiomeCategory {
    NONE,
    TAIGA,
    EXTREME_HILLS,
    JUNGLE,
    MESA,
    PLAINS,
    SAVANNA,
    ICY,
    THEEND,
    BEACH,
    FOREST,
    OCEAN,
    DESERT,
    RIVER,
    SWAMP,
    MUSHROOM,
    NETHER,
    UNDERGROUND,
    MOUNTAIN;

    public static LegacyBiomeCategory of(Holder<Biome> biome) {
        if (biome == null) {
            return NONE;
        }
        return biome.unwrapKey().map(k -> ofPath(k.location())).orElse(NONE);
    }

    public static LegacyBiomeCategory ofPath(ResourceLocation id) {
        if (id == null) {
            return NONE;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String ns = id.getNamespace().toLowerCase(Locale.ROOT);
        if (path.contains("nether") || "the_nether".equals(path) || ns.contains("nether")) {
            return NETHER;
        }
        if (path.contains("the_end") || path.startsWith("end_") || path.contains("end_")) {
            return THEEND;
        }
        if (path.contains("cave") || path.contains("cavern") || path.contains("dripstone")
                || path.contains("lush_caves") || path.contains("deep_dark") || path.contains("grotto")) {
            return UNDERGROUND;
        }
        if (path.contains("beach") || path.contains("shore")) {
            return BEACH;
        }
        if (path.contains("river")) {
            return RIVER;
        }
        if (path.contains("ocean") || path.contains("warm_ocean") || path.contains("lukewarm")) {
            return OCEAN;
        }
        if (path.contains("desert")) {
            return DESERT;
        }
        if (path.contains("badlands") || path.contains("mesa")) {
            return MESA;
        }
        if (path.contains("jungle")) {
            return JUNGLE;
        }
        if (path.contains("savanna")) {
            return SAVANNA;
        }
        if (path.contains("swamp") || path.contains("mangrove")) {
            return SWAMP;
        }
        if (path.contains("mushroom")) {
            return MUSHROOM;
        }
        if (path.contains("taiga") || path.contains("grove")) {
            return TAIGA;
        }
        if (path.contains("frozen") || path.contains("snowy") || path.contains("ice") || path.contains("icy")) {
            return ICY;
        }
        if (path.contains("peak") || path.contains("mountain") || path.contains("windswept") || path.contains("stony_peaks")) {
            return MOUNTAIN;
        }
        if (path.contains("hill")) {
            return EXTREME_HILLS;
        }
        if (path.contains("forest") || path.contains("birch") || path.contains("dark_forest") || path.contains("wooded")) {
            return FOREST;
        }
        if (path.contains("plains") || path.contains("meadow") || path.contains("sunflower")) {
            return PLAINS;
        }
        return NONE;
    }

    public static final class Dict {
        public enum Type {
            OVERWORLD, NETHER, END, VOID, UNDERGROUND, BEACH, RIVER, SWAMP, MESA, SANDY,
            MOUNTAIN, HILLS, PLAINS, FOREST, CONIFEROUS, SAVANNA, JUNGLE, WET, SNOWY,
            HOT, COLD, DRY, WASTELAND, PLATEAU
        }

        private Dict() {}

        public static boolean hasType(ResourceKey<Biome> key, Type type) {
            if (key == null || type == null) {
                return false;
            }
            LegacyBiomeCategory cat = ofPath(key.location());
            return switch (type) {
                case NETHER -> cat == NETHER;
                case END -> cat == THEEND;
                case VOID -> false;
                case UNDERGROUND -> cat == UNDERGROUND;
                case BEACH -> cat == BEACH;
                case RIVER -> cat == RIVER;
                case SWAMP -> cat == SWAMP;
                case MESA -> cat == MESA;
                case SANDY -> cat == DESERT || cat == BEACH || cat == MESA;
                case MOUNTAIN -> cat == MOUNTAIN;
                case HILLS -> cat == EXTREME_HILLS || cat == MOUNTAIN;
                case PLAINS -> cat == PLAINS;
                case FOREST -> cat == FOREST;
                case CONIFEROUS -> cat == TAIGA;
                case SAVANNA -> cat == SAVANNA;
                case JUNGLE -> cat == JUNGLE;
                case WET -> cat == SWAMP || cat == JUNGLE || cat == RIVER;
                case SNOWY -> cat == ICY;
                case HOT -> cat == DESERT || cat == SAVANNA || cat == MESA || cat == JUNGLE;
                case COLD -> cat == ICY || cat == TAIGA;
                case DRY -> cat == DESERT || cat == SAVANNA || cat == MESA;
                case WASTELAND -> cat == DESERT || cat == MESA;
                case PLATEAU -> cat == MESA || cat == SAVANNA || pathHint(key, "plateau");
                case OVERWORLD -> cat != NETHER && cat != THEEND;
            };
        }

        public static Set<Type> getTypes(ResourceKey<Biome> key) {
            if (key == null) {
                return Collections.emptySet();
            }
            // Cheap: expose matching types only.
            java.util.EnumSet<Type> out = java.util.EnumSet.noneOf(Type.class);
            for (Type t : Type.values()) {
                if (hasType(key, t)) {
                    out.add(t);
                }
            }
            return out;
        }

        private static boolean pathHint(ResourceKey<Biome> key, String token) {
            return key.location().getPath().toLowerCase(Locale.ROOT).contains(token);
        }
    }
}
