package com.terraforged.mod.client.gui.screen.nv;

import com.terraforged.mod.TerraForged;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Biome header previews at {@link #SIZE}px.
 * Only returns locations that exist in the resource pack — never points at missing PNGs.
 */
public final class BiomePreviewIcons {
    public static final int SIZE = 144;
    private static final String BASE = "textures/gui/biome_previews/";

    private BiomePreviewIcons() {}

    public static ResourceLocation forBiome(String biomeId) {
        if (biomeId == null || biomeId.isBlank()) {
            return tex("generic");
        }
        String full = biomeId.toLowerCase(Locale.ROOT);
        String path = full;
        String ns = "";
        int colon = path.indexOf(':');
        if (colon >= 0) {
            ns = path.substring(0, colon);
            path = path.substring(colon + 1);
        }
        path = path.replace('-', '_');

        // Pack-specific screenshots first (only if the file is actually present).
        if ("byg".equals(ns) || "biomeswevegone".equals(ns) || "oh_the_biomes_youll_go".equals(ns)) {
            ResourceLocation byg = namespaced("byg", path);
            if (exists(byg)) {
                return byg;
            }
        }
        if ("regions_unexplored".equals(ns) || "regionsunexplored".equals(ns)) {
            ResourceLocation ru = namespaced("regions_unexplored", path);
            if (exists(ru)) {
                return ru;
            }
        }
        if ("biomesoplenty".equals(ns) || "bop".equals(ns)) {
            ResourceLocation bop = namespaced("biomesoplenty", path);
            if (exists(bop)) {
                return bop;
            }
        }
        if ("terralith".equals(ns)) {
            ResourceLocation tl = namespaced("terralith", path);
            if (exists(tl)) {
                return tl;
            }
        }

        ResourceLocation exact = tryExact(path);
        if (exact != null && exists(exact)) {
            return exact;
        }

        ResourceLocation heur = heuristic(path);
        if (heur != null && exists(heur)) {
            return heur;
        }
        return tex("generic");
    }

    private static ResourceLocation heuristic(String path) {
        if (contains(path, "frozen_river") || (contains(path, "frozen") && contains(path, "river"))) {
            return tex("frozen_river");
        }
        if (contains(path, "river") || contains(path, "stream") || contains(path, "creek")) {
            return tex("river");
        }
        if (contains(path, "desert") || contains(path, "dune")) {
            return tex("desert");
        }
        if (contains(path, "badlands") || contains(path, "mesa")) {
            return tex("badlands");
        }
        if (contains(path, "jungle") || contains(path, "rainforest")) {
            return tex("jungle");
        }
        if (contains(path, "swamp") || contains(path, "marsh") || contains(path, "bog") || contains(path, "bayou")) {
            return tex("swamp");
        }
        if (contains(path, "savanna") || contains(path, "prairie")) {
            return tex("savanna");
        }
        if (contains(path, "snowy_taiga") || (contains(path, "snow") && contains(path, "taiga"))) {
            return tex("snowy_taiga");
        }
        if (contains(path, "taiga") || contains(path, "boreal")) {
            return tex("taiga");
        }
        if (contains(path, "ice_spike") || contains(path, "ice")) {
            return tex("ice_spikes");
        }
        if (contains(path, "snowy") || contains(path, "frozen") || contains(path, "tundra")) {
            return tex("snowy_plains");
        }
        if (contains(path, "stony_peak")) {
            return tex("stony_peaks");
        }
        if (contains(path, "jagged") || contains(path, "peak") || contains(path, "mountain") || contains(path, "alps")) {
            return tex("jagged_peaks");
        }
        if (contains(path, "beach") || contains(path, "shore")) {
            return tex("beach");
        }
        if (contains(path, "ocean") || contains(path, "sea")) {
            return contains(path, "warm") ? tex("warm_ocean") : tex("ocean");
        }
        if (contains(path, "mushroom")) {
            return tex("mushroom_fields");
        }
        if (contains(path, "meadow")) {
            return tex("meadow");
        }
        if (contains(path, "grove")) {
            return tex("grove");
        }
        if (contains(path, "dark_forest") || contains(path, "rosewood")) {
            return tex("dark_forest");
        }
        if (contains(path, "birch")) {
            return tex("birch_forest");
        }
        if (contains(path, "flower")) {
            return tex("flower_forest");
        }
        if (contains(path, "forest") || contains(path, "wood")) {
            return tex("forest");
        }
        if (contains(path, "hill") || contains(path, "height")) {
            return tex("windswept_hills");
        }
        if (contains(path, "plain") || contains(path, "grass") || contains(path, "field") || contains(path, "land")) {
            return tex("plains");
        }
        return null;
    }

    private static ResourceLocation tryExact(String path) {
        return switch (path) {
            case "plains", "sunflower_plains" -> tex("plains");
            case "forest", "wooded_hills" -> tex("forest");
            case "birch_forest", "old_growth_birch_forest" -> tex("birch_forest");
            case "dark_forest" -> tex("dark_forest");
            case "flower_forest" -> tex("flower_forest");
            case "desert", "desert_hills", "desert_lakes" -> tex("desert");
            case "badlands", "wooded_badlands", "eroded_badlands" -> tex("badlands");
            case "jungle", "sparse_jungle", "bamboo_jungle" -> tex("jungle");
            case "swamp", "mangrove_swamp" -> tex("swamp");
            case "savanna", "savanna_plateau", "windswept_savanna" -> tex("savanna");
            case "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga" -> tex("taiga");
            case "snowy_taiga" -> tex("snowy_taiga");
            case "snowy_plains", "snowy_tundra" -> tex("snowy_plains");
            case "ice_spikes" -> tex("ice_spikes");
            case "river" -> tex("river");
            case "frozen_river" -> tex("frozen_river");
            case "beach", "snowy_beach", "stony_shore" -> tex("beach");
            case "ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean", "frozen_ocean", "deep_frozen_ocean",
                 "lukewarm_ocean", "deep_lukewarm_ocean" -> tex("ocean");
            case "warm_ocean", "deep_warm_ocean" -> tex("warm_ocean");
            case "meadow" -> tex("meadow");
            case "grove" -> tex("grove");
            case "jagged_peaks" -> tex("jagged_peaks");
            case "stony_peaks" -> tex("stony_peaks");
            case "windswept_hills", "windswept_forest", "windswept_gravelly_hills" -> tex("windswept_hills");
            case "mushroom_fields", "mushroom_field_shore" -> tex("mushroom_fields");
            default -> null;
        };
    }

    private static boolean contains(String path, String token) {
        return path.contains(token);
    }

    private static boolean exists(ResourceLocation loc) {
        if (loc == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return true;
        }
        ResourceManager rm = mc.getResourceManager();
        if (rm == null) {
            return true;
        }
        try {
            return rm.hasResource(loc);
        } catch (Exception e) {
            return false;
        }
    }

    private static ResourceLocation namespaced(String folder, String path) {
        return new ResourceLocation(TerraForged.MODID, BASE + folder + "/" + path + ".png");
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TerraForged.MODID, BASE + name + ".png");
    }
}
