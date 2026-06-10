package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CaveClimateType;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import net.minecraft.resources.ResourceLocation;

public final class CaveBiomeStatDefaults {
    private CaveBiomeStatDefaults() {
    }

    public static boolean isGenerator(ResourceLocation id) {
        String path = id.getPath().toLowerCase();
        return CaveBiomeStatDefaults.containsAny(path, "mantle", "thermal_springs", "thermal_caves", "cave_thermal_springs", "magma", "brimstone", "volcanic", "yellowstone", "inferno", "geyser", "frostfire");
    }

    public static CaveBiomeStats infer(ResourceLocation id) {
        String path = id.getPath().toLowerCase();
        if (path.contains("frostfire")) {
            return CaveBiomeStatDefaults.frostfireCold();
        }
        if (CaveBiomeStatDefaults.isSpringCore(path)) {
            return CaveBiomeStatDefaults.springCore();
        }
        if (CaveBiomeStatDefaults.isVolcanicCore(path)) {
            return CaveBiomeStatDefaults.volcanicCore();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "steaming_jungle", "steaming")) {
            return CaveBiomeStatDefaults.steamingTransition();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "underground_jungle", "jungle")) {
            return CaveBiomeStatDefaults.jungle();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "frost", "ice", "icicle", "subzero", "glacier", "snow", "frozen")) {
            return CaveBiomeStatDefaults.frost();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "fungal", "mycotoxic", "glowshroom", "mushroom", "spore")) {
            return CaveBiomeStatDefaults.fungal();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "lush", "mossy", "grotto", "glowing", "undergarden")) {
            return CaveBiomeStatDefaults.lush();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "crystal", "prisma", "prismarite", "quartz", "skyris")) {
            return CaveBiomeStatDefaults.crystal();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "redstone")) {
            return CaveBiomeStatDefaults.redstone();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "deep")) {
            return CaveBiomeStatDefaults.deep();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "desert", "sand", "arid", "dry", "wasteland")) {
            return CaveBiomeStatDefaults.dry();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "bog", "marsh", "swamp", "wet", "embur")) {
            return CaveBiomeStatDefaults.wet();
        }
        if (CaveBiomeStatDefaults.containsAny(path, "infested", "karst", "limestone", "chalk", "stone")) {
            return CaveBiomeStatDefaults.barren();
        }
        return CaveBiomeStatDefaults.neutral();
    }

    private static boolean isVolcanicCore(String path) {
        return CaveBiomeStatDefaults.containsAny(path, "mantle", "magma", "brimstone", "volcanic", "inferno");
    }

    private static boolean isSpringCore(String path) {
        return CaveBiomeStatDefaults.containsAny(path, "thermal_springs", "cave_thermal_springs", "thermal_caves", "yellowstone");
    }

    private static CaveBiomeStats springCore() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(1.0f, 2.0f, 2.0f)).global(CaveStatVector.ZERO).local(new CaveStatVector(4.0f, 3.0f, 4.0f)).localFalloffPerHop(0.9f).build();
    }

    private static CaveBiomeStats frostfireCold() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-10.0f, -2.0f, -10.0f)).global(new CaveStatVector(-1.0f, -4.0f, -2.0f)).local(new CaveStatVector(-1.0f, -3.0f, -1.0f)).localFalloffPerHop(1.0f).globalForClimate(CaveClimateType.FROST, new CaveStatVector(1.0f, -2.0f, 0.0f)).globalForClimate(CaveClimateType.WET, new CaveStatVector(-2.0f, -3.0f, -1.0f)).globalForClimate(CaveClimateType.DRY, new CaveStatVector(-2.0f, -4.0f, -2.0f)).globalForClimate(CaveClimateType.NORMAL, new CaveStatVector(-1.0f, -3.0f, -1.0f)).build();
    }

    private static CaveBiomeStats volcanicCore() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-10.0f, 3.0f, -10.0f)).global(new CaveStatVector(-2.0f, 4.0f, -1.0f)).local(new CaveStatVector(-1.0f, 2.0f, 0.0f)).localFalloffPerHop(1.1f).globalForClimate(CaveClimateType.FROST, new CaveStatVector(4.0f, 2.0f, 0.0f)).globalForClimate(CaveClimateType.WET, new CaveStatVector(-4.0f, 2.0f, 0.0f)).globalForClimate(CaveClimateType.DRY, new CaveStatVector(-2.0f, 3.0f, -1.0f)).globalForClimate(CaveClimateType.NORMAL, new CaveStatVector(-2.0f, 3.0f, 0.0f)).build();
    }

    private static CaveBiomeStats jungle() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(2.0f, -10.0f, 4.0f)).global(new CaveStatVector(2.0f, 1.0f, 3.0f)).local(new CaveStatVector(2.0f, 0.0f, 2.0f)).localFalloffPerHop(0.9f).build();
    }

    private static CaveBiomeStats steamingTransition() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(1.0f, 2.0f, 2.0f)).global(new CaveStatVector(2.0f, 2.0f, 2.0f)).local(new CaveStatVector(1.0f, 1.0f, 1.0f)).localFalloffPerHop(1.0f).globalForClimate(CaveClimateType.FROST, new CaveStatVector(3.0f, 2.0f, 0.0f)).globalForClimate(CaveClimateType.WET, new CaveStatVector(-2.0f, 2.0f, 1.0f)).build();
    }

    private static CaveBiomeStats frost() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-10.0f, -2.0f, -10.0f)).global(new CaveStatVector(1.0f, -4.0f, -1.0f)).local(new CaveStatVector(1.0f, -2.0f, 0.0f)).localFalloffPerHop(1.0f).build();
    }

    private static CaveBiomeStats fungal() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(1.0f, -10.0f, 2.0f)).global(new CaveStatVector(3.0f, 0.0f, 2.0f)).local(new CaveStatVector(2.0f, 0.0f, 1.0f)).localFalloffPerHop(0.85f).build();
    }

    private static CaveBiomeStats lush() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(1.0f, -10.0f, 2.0f)).global(new CaveStatVector(3.0f, 0.0f, 3.0f)).local(new CaveStatVector(2.0f, 0.0f, 2.0f)).localFalloffPerHop(0.9f).build();
    }

    private static CaveBiomeStats crystal() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-10.0f, -10.0f, -2.0f)).global(new CaveStatVector(0.0f, -1.0f, -3.0f)).local(new CaveStatVector(0.0f, 0.0f, -1.0f)).localFalloffPerHop(1.2f).build();
    }

    private static CaveBiomeStats redstone() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-10.0f, 0.0f, -2.0f)).global(new CaveStatVector(-1.0f, 2.0f, -3.0f)).local(new CaveStatVector(0.0f, 1.0f, -1.0f)).localFalloffPerHop(1.0f).build();
    }

    private static CaveBiomeStats deep() {
        return CaveBiomeStats.builder().global(new CaveStatVector(-1.0f, -1.0f, -2.0f)).local(new CaveStatVector(-1.0f, -1.0f, -1.0f)).localFalloffPerHop(1.0f).build();
    }

    private static CaveBiomeStats dry() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(-2.0f, 1.0f, -10.0f)).global(new CaveStatVector(-3.0f, 2.0f, -2.0f)).local(new CaveStatVector(-2.0f, 1.0f, -1.0f)).localFalloffPerHop(1.0f).build();
    }

    private static CaveBiomeStats wet() {
        return CaveBiomeStats.builder().conditions(new CaveStatVector(2.0f, -10.0f, 0.0f)).global(new CaveStatVector(4.0f, -1.0f, 1.0f)).local(new CaveStatVector(2.0f, 0.0f, 1.0f)).localFalloffPerHop(0.9f).build();
    }

    private static CaveBiomeStats barren() {
        return CaveBiomeStats.builder().global(new CaveStatVector(-1.0f, 0.0f, -2.0f)).local(new CaveStatVector(-1.0f, 0.0f, -1.0f)).localFalloffPerHop(1.1f).build();
    }

    private static CaveBiomeStats neutral() {
        return CaveBiomeStats.EMPTY;
    }

    public static boolean isExplicitlyConfigured(CaveBiomeStats stats) {
        return stats != null && stats != CaveBiomeStats.EMPTY && stats.hasAnyValue();
    }

    private static boolean containsAny(String path, String ... tokens) {
        for (String token : tokens) {
            if (!path.contains(token)) continue;
            return true;
        }
        return false;
    }
}
