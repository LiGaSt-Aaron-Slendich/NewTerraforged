package com.terraforged.mod.worldgen.cave;

import net.minecraft.resources.ResourceLocation;

/**
 * Legacy −10..10 stats for generators / global / local pulses.
 * Condition targets for filtering are curated in {@link CaveCondNameDefaults} (human scales).
 */
public final class CaveBiomeStatDefaults {
    private CaveBiomeStatDefaults() {
    }

    public static boolean isGenerator(ResourceLocation id) {
        String path = id.getPath().toLowerCase();
        return containsAny(path, "mantle", "thermal_springs", "thermal_caves", "cave_thermal_springs",
                "magma", "brimstone", "volcanic", "yellowstone", "inferno", "geyser", "frostfire");
    }

    public static CaveBiomeStats infer(ResourceLocation id) {
        String path = id.getPath().toLowerCase();
        int[] cond = CaveCondNameDefaults.targetsFor(id);
        CaveStatVector conditions = condVector(cond[0], cond[2], cond[4]);

        if (path.contains("frostfire")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-1.0f, -4.0f, -2.0f))
                    .local(new CaveStatVector(-1.0f, -3.0f, -1.0f))
                    .localFalloffPerHop(1.0f)
                    .globalForClimate(CaveClimateType.FROST, new CaveStatVector(1.0f, -2.0f, 0.0f))
                    .globalForClimate(CaveClimateType.WET, new CaveStatVector(-2.0f, -3.0f, -1.0f))
                    .globalForClimate(CaveClimateType.DRY, new CaveStatVector(-2.0f, -4.0f, -2.0f))
                    .globalForClimate(CaveClimateType.NORMAL, new CaveStatVector(-1.0f, -3.0f, -1.0f))
                    .build();
        }
        if (isSpringCore(path)) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(CaveStatVector.ZERO)
                    .local(new CaveStatVector(4.0f, 3.0f, 4.0f))
                    .localFalloffPerHop(0.9f)
                    .build();
        }
        if (isVolcanicCore(path)) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-2.0f, 4.0f, -1.0f))
                    .local(new CaveStatVector(-1.0f, 2.0f, 0.0f))
                    .localFalloffPerHop(1.1f)
                    .globalForClimate(CaveClimateType.FROST, new CaveStatVector(4.0f, 2.0f, 0.0f))
                    .globalForClimate(CaveClimateType.WET, new CaveStatVector(-4.0f, 2.0f, 0.0f))
                    .globalForClimate(CaveClimateType.DRY, new CaveStatVector(-2.0f, 3.0f, -1.0f))
                    .globalForClimate(CaveClimateType.NORMAL, new CaveStatVector(-2.0f, 3.0f, 0.0f))
                    .build();
        }
        if (containsAny(path, "steaming_jungle", "steaming")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(2.0f, 2.0f, 2.0f))
                    .local(new CaveStatVector(1.0f, 1.0f, 1.0f))
                    .localFalloffPerHop(1.0f)
                    .build();
        }
        if (containsAny(path, "underground_jungle", "jungle")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(2.0f, 1.0f, 3.0f))
                    .local(new CaveStatVector(2.0f, 0.0f, 2.0f))
                    .localFalloffPerHop(0.9f)
                    .build();
        }
        if (containsAny(path, "frost", "ice", "icicle", "subzero", "glacier", "snow", "frozen")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(1.0f, -4.0f, -1.0f))
                    .local(new CaveStatVector(1.0f, -2.0f, 0.0f))
                    .localFalloffPerHop(1.0f)
                    .build();
        }
        if (containsAny(path, "fungal", "mycotoxic", "glowshroom", "mushroom", "spore", "bioshroom")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(3.0f, 0.0f, 2.0f))
                    .local(new CaveStatVector(2.0f, 0.0f, 1.0f))
                    .localFalloffPerHop(0.85f)
                    .build();
        }
        if (containsAny(path, "lush", "mossy", "grotto", "glowing", "undergarden")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(3.0f, 0.0f, 3.0f))
                    .local(new CaveStatVector(2.0f, 0.0f, 2.0f))
                    .localFalloffPerHop(0.9f)
                    .build();
        }
        if (containsAny(path, "crystal", "prisma", "prismarite", "quartz", "skyris")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(0.0f, -1.0f, -3.0f))
                    .local(new CaveStatVector(0.0f, 0.0f, -1.0f))
                    .localFalloffPerHop(1.2f)
                    .build();
        }
        if (containsAny(path, "redstone")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-1.0f, 2.0f, -3.0f))
                    .local(new CaveStatVector(0.0f, 1.0f, -1.0f))
                    .localFalloffPerHop(1.0f)
                    .build();
        }
        if (containsAny(path, "desert", "sand", "arid", "dry", "wasteland", "quartz_desert")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-3.0f, 2.0f, -2.0f))
                    .local(new CaveStatVector(-2.0f, 1.0f, -1.0f))
                    .localFalloffPerHop(1.0f)
                    .build();
        }
        if (containsAny(path, "bog", "marsh", "swamp", "wet", "embur", "delta")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(4.0f, -1.0f, 1.0f))
                    .local(new CaveStatVector(2.0f, 0.0f, 1.0f))
                    .localFalloffPerHop(0.9f)
                    .build();
        }
        if (containsAny(path, "deep")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-1.0f, -1.0f, -2.0f))
                    .local(new CaveStatVector(-1.0f, -1.0f, -1.0f))
                    .localFalloffPerHop(1.0f)
                    .build();
        }
        if (containsAny(path, "infested", "karst", "limestone", "chalk", "stone",
                "andesite", "diorite", "granite", "tuff")) {
            return CaveBiomeStats.builder()
                    .conditions(conditions)
                    .global(new CaveStatVector(-1.0f, 0.0f, -2.0f))
                    .local(new CaveStatVector(-1.0f, 0.0f, -1.0f))
                    .localFalloffPerHop(1.1f)
                    .build();
        }
        return CaveBiomeStats.builder().conditions(conditions).build();
    }

    private static CaveStatVector condVector(int tempC, int humidityPct, int fertility) {
        return new CaveStatVector(
                CaveClimateScale.humidityToInternal(humidityPct),
                CaveClimateScale.tempToInternal(tempC),
                CaveClimateScale.fertilityToInternal(fertility)
        );
    }

    private static boolean isVolcanicCore(String path) {
        return containsAny(path, "mantle", "magma", "brimstone", "volcanic", "inferno");
    }

    private static boolean isSpringCore(String path) {
        return containsAny(path, "thermal_springs", "cave_thermal_springs", "thermal_caves", "yellowstone");
    }

    public static boolean isExplicitlyConfigured(CaveBiomeStats stats) {
        return stats != null && stats != CaveBiomeStats.EMPTY && stats.hasAnyValue();
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
