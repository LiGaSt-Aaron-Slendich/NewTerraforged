package com.terraforged.mod.worldgen.noise.climate;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;

/**
 * Climate temperature/moisture {@code scale} is a <b>percent of continent-linked base</b> (50–300),
 * not an absolute period. Larger continent scale → larger climate features at the same %.
 */
public final class ClimateScaleResolver {
    /** Historic absolute scale at continent={@link WorldSettings#DEFAULT_CONTINENT_SCALE} and 100%. */
    public static final int BASE_ABSOLUTE_AT_DEFAULT_CONTINENT = 6;
    public static final int PERCENT_MIN = 50;
    public static final int PERCENT_MAX = 300;
    public static final int PERCENT_DEFAULT = 100;

    private ClimateScaleResolver() {
    }

    public static int clampPercent(int percent) {
        return Math.max(PERCENT_MIN, Math.min(PERCENT_MAX, percent));
    }

    /**
     * Absolute horizontal scale fed into {@link ClimateNoise} / engine ClimateModule.
     */
    public static int absoluteScale(int continentScale, int percent) {
        int cont = Math.max(100, continentScale);
        float base = (cont / (float) WorldSettings.DEFAULT_CONTINENT_SCALE) * BASE_ABSOLUTE_AT_DEFAULT_CONTINENT;
        return Math.max(1, Math.round(base * (clampPercent(percent) / 100.0F)));
    }

    /** Legacy saves stored absolute 1–20-ish; treat &lt;50 as legacy and snap to 100%. */
    public static int migratePercent(int stored) {
        if (stored < PERCENT_MIN) {
            return PERCENT_DEFAULT;
        }
        return clampPercent(stored);
    }

    /**
     * Mutate a settings clone so engine preview ClimateModule sees absolute periods.
     * Caller must pass a copy — live UI settings keep percent values.
     */
    public static void bakeAbsoluteScales(Settings settings) {
        if (settings == null || settings.climate == null || settings.world == null) {
            return;
        }
        int cont = settings.world.continent != null ? settings.world.continent.continentScale : WorldSettings.DEFAULT_CONTINENT_SCALE;
        settings.climate.temperature.scale = absoluteScale(cont, migratePercent(settings.climate.temperature.scale));
        settings.climate.moisture.scale = absoluteScale(cont, migratePercent(settings.climate.moisture.scale));
    }
}
