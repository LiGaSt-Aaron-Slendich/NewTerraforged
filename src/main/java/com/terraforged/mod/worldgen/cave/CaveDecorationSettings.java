package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;

/**
 * Toggle between compromise, vanilla, and legacy cave decoration passes.
 */
public final class CaveDecorationSettings {
    private CaveDecorationSettings() {
    }

    public static boolean usePerBiomeDecorators() {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        return cfg == null || cfg.usePerBiomeDecorators;
    }

    public static boolean useOfficialTfDecorator() {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        if (CaveDecorationSettings.usePerBiomeDecorators()) {
            return false;
        }
        return cfg != null && cfg.useOfficialTfCaveDecorator;
    }

    public static boolean useCompromiseDecorator() {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        if (CaveDecorationSettings.usePerBiomeDecorators() || CaveDecorationSettings.useOfficialTfDecorator()) {
            return false;
        }
        return cfg == null || cfg.useCompromiseCaveDecorator;
    }

    public static boolean useVanillaPass() {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        return cfg != null && cfg.useVanillaCavePass && !CaveDecorationSettings.useOfficialTfDecorator() && !CaveDecorationSettings.useCompromiseDecorator();
    }

    public static boolean useLegacyDecorators() {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        if (cfg == null || !cfg.useLegacyCaveDecorators || CaveDecorationSettings.useOfficialTfDecorator()) {
            return false;
        }
        return !CaveDecorationSettings.useCompromiseDecorator() && !CaveDecorationSettings.useVanillaPass();
    }

    public static String activeModeLabel() {
        if (CaveDecorationSettings.usePerBiomeDecorators()) {
            return "hybrid";
        }
        if (CaveDecorationSettings.useOfficialTfDecorator()) {
            return "official";
        }
        if (CaveDecorationSettings.useLegacyDecorators()) {
            return "legacy";
        }
        if (CaveDecorationSettings.useVanillaPass()) {
            return "vanilla";
        }
        if (CaveDecorationSettings.useCompromiseDecorator()) {
            return "compromise";
        }
        return "none";
    }
}
