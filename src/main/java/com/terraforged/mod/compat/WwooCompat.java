package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.fml.ModList;

/**
 * WWOO (William Wythers' Overhauled Overworld) replaces {@code minecraft:overworld}
 * with a noise + multi_noise setup that conflicts with TerraForged / NewTF generators
 * (river shafts, carver fights). Soft warn only — never hard-dep or void-fill around it.
 */
public final class WwooCompat {
    public static final String WWOO_MOD_ID = "wwoo_forge";
    private static boolean initialized;
    private static boolean wwooLoaded;
    private static boolean warned;

    private WwooCompat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        wwooLoaded = ModList.get().isLoaded(WWOO_MOD_ID);
        if (wwooLoaded) {
            warnConflict("common setup");
        }
    }

    /** Call when NewTF/TF generator is known active (world load / preset). */
    public static void onGeneratorActive() {
        init();
        if (wwooLoaded) {
            warnConflict("generator active");
        }
    }

    public static boolean isWwooLoaded() {
        init();
        return wwooLoaded;
    }

    private static void warnConflict(String when) {
        if (warned) {
            return;
        }
        warned = true;
        TerraForged.LOG.warn(
                "[WWOO] {} is loaded ({}) — WWOO replaces minecraft:overworld with noise+multi_noise and conflicts with TerraForged/NewTF (river shafts, carver fights). Disable WWOO when using the NewTerraForged/TerraForged world type.",
                WWOO_MOD_ID,
                when);
    }
}
