package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveClimateType;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class CaveBiomeClimateAffinity {
    private CaveBiomeClimateAffinity() {
    }

    public static boolean matches(ResourceLocation id, CaveClimateType climate) {
        if (id == null || climate == null) {
            return false;
        }
        return CaveBiomeClimateAffinity.affinityFor(id).contains(climate);
    }

    public static boolean matches(ResourceLocation id, CaveClimateType climate, float localTemperature, boolean thermalOasis) {
        if (id == null || climate == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (thermalOasis && localTemperature >= 1.5f) {
            if (CaveBiomeClimateAffinity.isPureColdBiome(path) && localTemperature >= 2.0f) {
                return false;
            }
            if (CaveBiomeClimateAffinity.isWarmOasisBiome(path) && localTemperature >= 2.0f) {
                return true;
            }
            if (localTemperature >= 4.0f && CaveBiomeClimateAffinity.isWarmOasisBiome(path)) {
                return true;
            }
        }
        return CaveBiomeClimateAffinity.matches(id, climate);
    }

    public static boolean isWarmOasisBiome(String path) {
        return CaveBiomeClimateAffinity.containsAny(path, "underground_jungle", "steaming_jungle", "jungle", "thermal_caves", "thermal_springs", "undergarden", "mossy_caves", "fungal", "mycotoxic", "glowshroom", "bioshroom", "embur_bog", "ancient_delta", "glowing_grotto");
    }

    private static boolean isPureColdBiome(String path) {
        return CaveBiomeClimateAffinity.containsAny(path, "ice_caves", "icicle", "frost_caves", "subzero", "hypogeal", "shattered_glacier");
    }

    private static Set<CaveClimateType> affinityFor(ResourceLocation id) {
        String path = id.getPath().toLowerCase();
        if (CaveBiomeClimateAffinity.containsAny(path, "ice_caves", "icicle", "subzero", "hypogeal", "shattered_glacier")) {
            return CaveBiomeClimateAffinity.only(CaveClimateType.FROST);
        }
        if (path.contains("frost_caves") && !path.contains("frostfire")) {
            return CaveBiomeClimateAffinity.only(CaveClimateType.FROST);
        }
        if (path.contains("frostfire")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.FROST, CaveClimateType.DRY, CaveClimateType.NORMAL);
        }
        if (CaveBiomeClimateAffinity.containsAny(path, "desert_caves", "quartz_desert", "arid", "sand_caves")) {
            return CaveBiomeClimateAffinity.only(CaveClimateType.DRY);
        }
        if (CaveBiomeClimateAffinity.containsAny(path, "brimstone", "mantle")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.DRY, CaveClimateType.NORMAL);
        }
        if (CaveBiomeClimateAffinity.containsAny(path, "underground_jungle", "steaming_jungle", "undergarden", "glowing_grotto", "embur_bog", "ancient_delta", "mossy_caves")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.WET, CaveClimateType.NORMAL);
        }
        if (CaveBiomeClimateAffinity.containsAny(path, "fungal", "mycotoxic", "glowshroom", "bioshroom", "crimson_gardens")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.WET, CaveClimateType.NORMAL);
        }
        if (path.contains("thermal_springs") || path.contains("thermal_caves")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.WET, CaveClimateType.DRY, CaveClimateType.NORMAL);
        }
        if (CaveBiomeClimateAffinity.containsAny(path, "crystal_caves", "crystal", "prismachasm", "prisma", "skyris")) {
            return CaveBiomeClimateAffinity.of(CaveClimateType.NORMAL, CaveClimateType.FROST, CaveClimateType.WET);
        }
        return CaveBiomeClimateAffinity.all();
    }

    public static boolean isThermalGenerator(ResourceLocation id) {
        return CaveBiomeClimateAffinity.isHeatGenerator(id);
    }

    public static boolean isHeatGenerator(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("mantle") || path.contains("brimstone") || path.contains("magma");
    }

    public static boolean isSpringGenerator(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("thermal_springs") || path.contains("cave_thermal_springs") || path.contains("thermal_caves");
    }

    public static boolean isColdGenerator(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        return id.getPath().toLowerCase().contains("frostfire");
    }

    public static boolean isWarmOasisGenerator(ResourceLocation id) {
        return CaveBiomeClimateAffinity.isHeatGenerator(id) || CaveBiomeClimateAffinity.isSpringGenerator(id);
    }

    private static Set<CaveClimateType> only(CaveClimateType type) {
        return EnumSet.of(type);
    }

    private static Set<CaveClimateType> of(CaveClimateType ... types) {
        return EnumSet.copyOf(Set.of(types));
    }

    private static Set<CaveClimateType> all() {
        return EnumSet.allOf(CaveClimateType.class);
    }

    private static boolean containsAny(String path, String ... tokens) {
        for (String token : tokens) {
            if (!path.contains(token)) continue;
            return true;
        }
        return false;
    }
}
