package com.terraforged.mod.worldgen.cave;

import net.minecraft.resources.ResourceLocation;

/**
 * Emergency Cond fill when a cave biome has no curated JSON and legacy stats left axes UNSET.
 * Prefer hand-authored {@code Cave_configs/Biomes/*.json} from {@link CaveBiomeCuratedDefaults}.
 */
public final class CaveCondNameDefaults {
    private CaveCondNameDefaults() {
    }

    /** @return {temp, deltaTemp, humidity, deltaHum, fertility, deltaFert} — never null */
    public static int[] targetsFor(ResourceLocation id) {
        if (id == null) {
            return temperateStone();
        }
        String full = id.toString().toLowerCase();
        String path = id.getPath().toLowerCase();

        // Exact known generators / themes first
        if (full.contains("frostfire") || path.contains("frostfire")) {
            return of(-20, 20, 40, 15, 20, 20);
        }
        if (path.contains("thermal_springs") || path.contains("cave_thermal_springs") || path.contains("yellowstone")) {
            return of(95, 25, 70, 20, 80, 25);
        }
        if (path.contains("mantle") || path.contains("brimstone") || path.contains("inferno")
                || path.contains("magma") || path.contains("volcanic")) {
            return of(120, 25, 15, 15, 20, 20);
        }
        if (path.contains("thermal_caves") || path.contains("thermal") || path.contains("geyser")) {
            return of(85, 20, 65, 20, 70, 25);
        }
        if (path.contains("steaming")) {
            return of(75, 20, 80, 15, 110, 25);
        }
        if (path.contains("underground_jungle") || (path.contains("jungle") && path.contains("cave"))) {
            return of(55, 20, 85, 15, 150, 30);
        }
        if (path.contains("subzero") || path.contains("shattered_glacier")) {
            return of(-40, 15, 30, 15, 10, 15);
        }
        if (path.contains("icicle") || path.contains("ice_caves") || path.contains("frost")
                || path.contains("glacier") || path.contains("frozen") || path.contains("snow")) {
            return of(-25, 20, 45, 15, 15, 15);
        }
        if (path.contains("fungal") || path.contains("mycotoxic") || path.contains("glowshroom")
                || path.contains("bioshroom") || path.contains("mushroom") || path.contains("spore")) {
            return of(45, 20, 75, 15, 140, 30);
        }
        if (path.contains("glowing_grotto") || path.contains("mossy") || path.contains("lush")
                || path.contains("grotto") || path.contains("undergarden")) {
            return of(40, 20, 70, 15, 130, 30);
        }
        if (path.contains("desert") || path.contains("quartz_desert") || path.contains("arid")
                || path.contains("sand") || path.contains("wasteland")) {
            return of(75, 20, 15, 15, 20, 20);
        }
        if (path.contains("ancient_delta") || path.contains("embur") || path.contains("bog")
                || path.contains("marsh") || path.contains("swamp") || path.contains("delta")) {
            return of(30, 20, 85, 15, 100, 25);
        }
        if (path.contains("dripstone")) {
            return of(35, 20, 60, 15, 50, 20);
        }
        if (path.contains("crystal") || path.contains("prisma") || path.contains("prismarite")
                || path.contains("quartz") || path.contains("skyris")) {
            return of(35, 20, 40, 15, 40, 20);
        }
        if (path.contains("redstone")) {
            return of(55, 20, 30, 15, 40, 20);
        }
        if (path.contains("deep_caves") || path.contains("deep")) {
            return of(20, 20, 35, 15, 30, 20);
        }
        if (path.contains("infested") || path.contains("andesite") || path.contains("diorite")
                || path.contains("granite") || path.contains("tuff") || path.contains("limestone")
                || path.contains("chalk") || path.contains("karst") || path.contains("stone")) {
            return of(30, 20, 40, 15, 35, 20);
        }
        if (path.contains("crimson") || path.contains("warped") || path.contains("nether")) {
            return of(110, 25, 25, 15, 40, 20);
        }
        if (path.contains("nightshade")) {
            return of(15, 20, 55, 15, 110, 25);
        }
        return temperateStone();
    }

    /** Fill unset Cond axes from curated name defaults (keeps explicit player/toml values). */
    public static int[] mergeWithExisting(
            ResourceLocation id,
            int condTemp, int deltaTemp,
            int condHum, int deltaHum,
            int condFert, int deltaFert
    ) {
        int[] named = targetsFor(id);
        int t = condTemp == CaveClimateScale.UNSET ? named[0] : condTemp;
        int dT = deltaTemp > 0 ? deltaTemp : named[1];
        int h = condHum == CaveClimateScale.UNSET ? named[2] : condHum;
        int dH = deltaHum > 0 ? deltaHum : named[3];
        int f = condFert == CaveClimateScale.UNSET ? named[4] : condFert;
        int dF = deltaFert > 0 ? deltaFert : named[5];
        return new int[]{t, dT, h, dH, f, dF};
    }

    private static int[] temperateStone() {
        return of(30, 20, 45, 15, 50, 20);
    }

    private static int[] of(int t, int dT, int h, int dH, int f, int dF) {
        return new int[]{t, dT, h, dH, f, dF};
    }
}
