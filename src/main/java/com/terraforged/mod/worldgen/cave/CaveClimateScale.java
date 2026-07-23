package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * Human-facing cave climate scales and regional barriers.
 * Internal engine vectors stay −10..10; convert at the UI / match boundary.
 *
 * <pre>
 * Temp °C: −50..150
 *   100..150 → only thermal / volcanic biomes
 *   70..100 and −50..15 → no vegetation biomes
 * Humidity %: 0..100
 *   0..30 → no vegetation biomes
 * Fertility: 0..200
 *   &lt;30 → no vegetation biomes
 *   &gt;100 → plant-growth bonus (decor / future)
 * </pre>
 */
public final class CaveClimateScale {
    public static final int TEMP_MIN = -50;
    public static final int TEMP_MAX = 150;
    public static final int HUM_MIN = 0;
    public static final int HUM_MAX = 100;
    public static final int FERT_MIN = 0;
    public static final int FERT_MAX = 200;
    public static final int UNSET = Integer.MIN_VALUE;
    public static final int DEFAULT_DELTA_TEMP = 15;
    public static final int DEFAULT_DELTA_HUM = 15;
    public static final int DEFAULT_DELTA_FERT = 20;

    private CaveClimateScale() {
    }

    public static int tempFromInternal(float v) {
        return Math.round(-50.0F + (NoiseUtil.clamp(v, -10.0F, 10.0F) + 10.0F) * 10.0F);
    }

    public static float tempToInternal(int t) {
        int c = clamp(t, TEMP_MIN, TEMP_MAX);
        return (c + 50) / 10.0F - 10.0F;
    }

    public static int humidityFromInternal(float v) {
        return Math.round((NoiseUtil.clamp(v, -10.0F, 10.0F) + 10.0F) * 5.0F);
    }

    public static float humidityToInternal(int h) {
        int c = clamp(h, HUM_MIN, HUM_MAX);
        return c / 5.0F - 10.0F;
    }

    public static int fertilityFromInternal(float v) {
        return Math.round((NoiseUtil.clamp(v, -10.0F, 10.0F) + 10.0F) * 10.0F);
    }

    public static float fertilityToInternal(int f) {
        int c = clamp(f, FERT_MIN, FERT_MAX);
        return c / 10.0F - 10.0F;
    }

    public static boolean inRange(int value, int target, int delta) {
        if (target == UNSET) {
            return true;
        }
        int d = Math.max(0, delta);
        return Math.abs(value - target) <= d;
    }

    /** Regional hard barriers from the live pool climate. */
    public static boolean regionAllowsBiome(ResourceLocation id, int tempC, int humidityPct, int fertility) {
        if (id == null) {
            return false;
        }
        boolean thermal = isThermalOrVolcanic(id);
        boolean plant = isVegetationBiome(id);

        if (tempC >= 100 && tempC <= TEMP_MAX) {
            if (!thermal) {
                return false;
            }
        } else if ((tempC >= 70 && tempC < 100) || (tempC >= TEMP_MIN && tempC <= 15)) {
            if (plant) {
                return false;
            }
        }

        if (humidityPct <= 30 && plant) {
            return false;
        }
        if (fertility < 30 && plant) {
            return false;
        }
        return true;
    }

    public static boolean matchesRuleTargets(
            ResourceLocation id,
            CaveStatVector pool,
            int condTemp, int deltaTemp,
            int condHum, int deltaHum,
            int condFert, int deltaFert
    ) {
        if (pool == null) {
            return false;
        }
        int t = tempFromInternal(pool.temperature());
        int h = humidityFromInternal(pool.moisture());
        int f = fertilityFromInternal(pool.fertility());
        if (!regionAllowsBiome(id, t, h, f)) {
            return false;
        }
        return inRange(t, condTemp, deltaTemp)
                && inRange(h, condHum, deltaHum)
                && inRange(f, condFert, deltaFert);
    }

    public static boolean isThermalOrVolcanic(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (CaveBiomeClimateAffinity.isHeatGenerator(id) || CaveBiomeClimateAffinity.isSpringGenerator(id)) {
            return true;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("thermal")
                || path.contains("volcan")
                || path.contains("magma")
                || path.contains("mantle")
                || path.contains("brimstone")
                || path.contains("scorch")
                || path.contains("lava")
                || path.contains("ash");
    }

    public static boolean isVegetationBiome(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (isThermalOrVolcanic(id)) {
            return false;
        }
        return path.contains("lush")
                || path.contains("moss")
                || path.contains("jungle")
                || path.contains("fungal")
                || path.contains("mushroom")
                || path.contains("glowshroom")
                || path.contains("bioshroom")
                || path.contains("grotto")
                || path.contains("garden")
                || path.contains("undergarden")
                || path.contains("root")
                || path.contains("vine")
                || path.contains("forest")
                || path.contains("bloom");
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
