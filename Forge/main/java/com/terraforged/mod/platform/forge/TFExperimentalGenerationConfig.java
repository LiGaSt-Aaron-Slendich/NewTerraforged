package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.mod.TerraForged;

/**
 * Instance-wide Experimental Generation Features (EGF).
 * Default off — enable via title-screen console {@code /EGF open}.
 */
public final class TFExperimentalGenerationConfig {
    public static TFExperimentalGenerationConfig INSTANCE = new TFExperimentalGenerationConfig();

    public boolean archipelago = false;
    public boolean scatteredArchipelago = false;

    private TFExperimentalGenerationConfig() {
    }

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open(TFConfigPaths.EXPERIMENTAL_GENERATION);
        INSTANCE = new TFExperimentalGenerationConfig();
        INSTANCE.read(cfg);
        cfg.close();
        TerraForged.LOG.info(
                "[TFConfig] EGF loaded (archipelago={}, scattered={})",
                INSTANCE.archipelago,
                INSTANCE.scatteredArchipelago
        );
    }

    public static boolean archipelagoEnabled() {
        return INSTANCE != null && INSTANCE.archipelago;
    }

    public static boolean scatteredArchipelagoEnabled() {
        return INSTANCE != null && INSTANCE.scatteredArchipelago;
    }

    public static void setArchipelago(boolean value) {
        ensure();
        INSTANCE.archipelago = value;
        INSTANCE.save();
    }

    public static void setScatteredArchipelago(boolean value) {
        ensure();
        INSTANCE.scatteredArchipelago = value;
        INSTANCE.save();
    }

    private static void ensure() {
        if (INSTANCE == null) {
            INSTANCE = new TFExperimentalGenerationConfig();
        }
    }

    private void read(CommentedFileConfig root) {
        Config features = TFConfigLoader.section(root, "features");
        this.archipelago = TFConfigLoader.getBool(features, "archipelago", false);
        this.scatteredArchipelago = TFConfigLoader.getBool(features, "scattered_archipelago", false);
    }

    private void save() {
        try {
            CommentedFileConfig cfg = TFConfigLoader.open(TFConfigPaths.EXPERIMENTAL_GENERATION);
            cfg.set("features.archipelago", this.archipelago);
            cfg.set("features.scattered_archipelago", this.scatteredArchipelago);
            cfg.setComment("features", " Experimental Generation Features — OFF by default for releases.");
            cfg.setComment("features.archipelago", " Compact Archipelago clusters (2–5 islands).");
            cfg.setComment("features.scattered_archipelago", " Scattered Archipelago clusters (many islands).");
            cfg.save();
            cfg.close();
        } catch (Exception e) {
            TerraForged.LOG.error("[TFConfig] Failed to save EGF config", e);
        }
    }
}
