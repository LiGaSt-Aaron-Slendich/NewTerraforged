package com.terraforged.mod.client.gui.screen.nv;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Maps terrain / climate / zone ids to native 16×16 GUI textures (not downscaled HD). */
public final class BiomeRuleIcons {
    public static final int SIZE = 16;
    private static final String BASE = "textures/gui/biome_rules/";

    private BiomeRuleIcons() {}

    public static ResourceLocation terrain(String terrainId) {
        String id = norm(terrainId);
        if (id.isEmpty()) {
            return tex("terrain_generic");
        }
        // Subterrains first (more specific names)
        ResourceLocation sub = subterrain(id);
        if (sub != null) {
            return sub;
        }
        if (id.equals("plains")) {
            return tex("terrain_plains");
        }
        if (id.equals("steppe")) {
            return tex("terrain_steppe");
        }
        if (id.equals("dales")) {
            return tex("terrain_dales");
        }
        if (id.equals("river")) {
            return tex("terrain_river");
        }
        if (id.equals("plateau") || id.equals("island_plateau")) {
            return tex("terrain_plateau");
        }
        if (id.equals("badlands")) {
            return tex("terrain_badlands");
        }
        if (id.contains("beach")) {
            return tex("terrain_beach");
        }
        if (id.equals("volcano_pipe")) {
            return tex("terrain_volcano_pipe");
        }
        if (id.contains("volcano")) {
            return tex("terrain_volcano");
        }
        if (id.contains("island")) {
            return tex("terrain_island");
        }
        if (id.contains("canyon")) {
            return tex("terrain_canyon");
        }
        if (id.contains("peak")) {
            return tex("terrain_peak");
        }
        if (id.contains("foothill") || id.contains("body") || id.contains("bare_mountain")) {
            return tex("terrain_foothill");
        }
        if (id.contains("mountain") || id.contains("dolomite") || id.contains("torridonian") || id.contains("ridge")) {
            return tex("terrain_mountains");
        }
        if (id.contains("hill")) {
            return tex("terrain_hills");
        }
        return tex("terrain_generic");
    }

    /** Dedicated subterrain icons; null if not a known subterrain id. */
    public static ResourceLocation subterrain(String id) {
        return switch (norm(id)) {
            case "river_bank" -> tex("sub_river_bank");
            case "canyon" -> tex("sub_canyon");
            case "desert_canyon" -> tex("sub_desert_canyon");
            case "ocean_beach" -> tex("sub_ocean_beach");
            case "sea_beach" -> tex("sub_sea_beach");
            case "volcanic_beach" -> tex("sub_volcanic_beach");
            case "mountain_peak" -> tex("sub_mountain_peak");
            case "bare_mountain_peak" -> tex("sub_bare_mountain_peak");
            case "mountain_body" -> tex("sub_mountain_body");
            case "mountain_foothill" -> tex("sub_mountain_foothill");
            case "bare_mountain" -> tex("sub_bare_mountain");
            default -> null;
        };
    }

    public static ResourceLocation climate(String tag) {
        String id = norm(tag);
        return switch (id) {
            case "wet" -> tex("climate_wet");
            case "warm" -> tex("climate_warm");
            case "hot" -> tex("climate_hot");
            case "cold" -> tex("climate_cold");
            case "snowy" -> tex("climate_snowy");
            case "desert" -> tex("climate_desert");
            case "savanna" -> tex("climate_savanna");
            case "taiga" -> tex("climate_taiga");
            case "tundra" -> tex("climate_tundra");
            case "temperate" -> tex("climate_temperate");
            case "jungle" -> tex("climate_jungle");
            case "alpine" -> tex("climate_alpine");
            case "volcanic" -> tex("climate_volcanic");
            case "mesa" -> tex("climate_mesa");
            default -> tex("terrain_generic");
        };
    }

    public static ResourceLocation zone(String flagId) {
        String id = norm(flagId);
        if (BiomeRule.ZONE_NEAR_DORMANT_VOLCANO.equals(id) || id.contains("dormant")) {
            // Reuse active icon asset until a dedicated dormant texture ships.
            return tex("zone_near_active_volcano");
        }
        if (BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO.equals(id) || id.contains("volcano")) {
            return tex("zone_near_active_volcano");
        }
        return tex("terrain_generic");
    }

    public static ResourceLocation slope() {
        return tex("flag_slope");
    }

    public static ResourceLocation ui(String name) {
        return tex(name);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TerraForged.MODID, BASE + name + ".png");
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
