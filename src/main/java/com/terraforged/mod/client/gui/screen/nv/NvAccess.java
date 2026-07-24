package com.terraforged.mod.client.gui.screen.nv;

import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import java.util.List;
import java.util.Locale;

/** Access gate for unfinished generation toggles. */
public final class NvAccess {
    public static final String BLOCKED_LABEL = "Feature Blocked";
    public static final String BLOCKED_TOOLTIP =
            "Feature Blocked due to: dev not finished implementaton yet";

    private NvAccess() {
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

    public static boolean isIslandsSetting(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String n = fieldName.toLowerCase(Locale.ROOT);
        return n.equals("coastalislands") || n.equals("coastalislandschance")
                || n.equals("volcanicislands") || n.equals("volcanicislandschance");
    }

    public static boolean isOceanLandscapeSetting(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String n = fieldName.toLowerCase(Locale.ROOT);
        return n.equals("noisescale")
                || n.equals("corridorpartners")
                || n.equals("corridorstrength")
                || n.equals("volcanodensity")
                || n.equals("oceanlandscape");
    }

    public static boolean isBlockedSetting(String fieldName) {
        if (isArchipelagoSetting(fieldName)) {
            return !TFNoiseVariantFlags.archipelagoEnabled();
        }
        if (isScatteredArchipelagoSetting(fieldName)) {
            return !TFNoiseVariantFlags.scatteredArchipelagoEnabled();
        }
        if (isIslandsSetting(fieldName)) {
            return !TFNoiseVariantFlags.islandsEnabled();
        }
        if (isOceanLandscapeSetting(fieldName)) {
            return !TFNoiseVariantFlags.oceanLandscapeEnabled();
        }
        return false;
    }

    public static boolean shipwreckedWorldTypeAllowed() {
        return TFNoiseVariantFlags.archipelagoEnabled()
                || TFNoiseVariantFlags.scatteredArchipelagoEnabled()
                || TFNoiseVariantFlags.islandsEnabled()
                || TFNoiseVariantFlags.oceanLandscapeEnabled();
    }
}
