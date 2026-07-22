package com.terraforged.mod.client.gui.screen.egf;

import com.terraforged.mod.platform.forge.TFExperimentalGenerationConfig;
import java.util.List;
import java.util.Locale;

/** Gates Customize widgets / world types that depend on Experimental Generation Features. */
public final class EgfFeatureGate {
    public static final String BLOCKED_LABEL = "Feature Blocked";
    public static final String BLOCKED_TOOLTIP =
            "Feature Blocked due to: dev not finished implementaton yet";

    private EgfFeatureGate() {
    }

    public static List<String> blockedTooltip() {
        return List.of(BLOCKED_TOOLTIP);
    }

    public static boolean isArchipelagoSetting(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String n = fieldName.toLowerCase(Locale.ROOT);
        return n.equals("archipelago") || n.equals("archipelagochance");
    }

    public static boolean isScatteredArchipelagoSetting(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String n = fieldName.toLowerCase(Locale.ROOT);
        return n.equals("scatteredarchipelago") || n.equals("scatteredarchipelagochance");
    }

    public static boolean isBlockedSetting(String fieldName) {
        if (isArchipelagoSetting(fieldName)) {
            return !TFExperimentalGenerationConfig.archipelagoEnabled();
        }
        if (isScatteredArchipelagoSetting(fieldName)) {
            return !TFExperimentalGenerationConfig.scatteredArchipelagoEnabled();
        }
        return false;
    }

    /** Shipwrecked world type is experimental until EGF archipelago features are enabled. */
    public static boolean shipwreckedWorldTypeAllowed() {
        return TFExperimentalGenerationConfig.archipelagoEnabled()
                || TFExperimentalGenerationConfig.scatteredArchipelagoEnabled();
    }
}
