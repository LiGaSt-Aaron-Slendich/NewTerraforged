package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.fml.ModList;

public final class TerraBlenderCompat {
    private static final String TB_ID = "terrablender";
    private static final String TERRALITH_ID = "terralith";
    private static boolean terraBlenderLoaded;
    private static boolean terralithLoaded;
    private static boolean initialized;

    private TerraBlenderCompat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        terraBlenderLoaded = ModList.get().isLoaded(TB_ID);
        terralithLoaded = ModList.get().isLoaded(TERRALITH_ID);
        if (terraBlenderLoaded) {
            TerraForged.LOG.info("TerraBlender detected \u0432\u0402\u201d NewTerraForged keeps custom terrain; mod biomes use newterraforged:climate source");
        }
        if (terralithLoaded) {
            TerraForged.LOG.info("Terralith detected \u0432\u0402\u201d surface/cave biomes integrated via BiomeMapManager and TF configs");
        }
    }

    public static boolean isTerraBlenderLoaded() {
        return terraBlenderLoaded;
    }

    public static boolean isTerralithLoaded() {
        return terralithLoaded;
    }

    public static void onGeneratorActive() {
        if (!terraBlenderLoaded && !terralithLoaded) {
            return;
        }
        TerraForged.LOG.info("NewTerraForged generator active with{}{}", (terraBlenderLoaded ? " TerraBlender" : ""), (terralithLoaded ? " Terralith" : ""));
    }

    public static void warnVanillaOverworld(String generatorType) {
        if (!terraBlenderLoaded) {
            return;
        }
        TerraForged.LOG.warn("TerraBlender is loaded but overworld uses {} instead of NewTerraForged \u0432\u0402\u201d create a new world with the NewTerraForged world type", generatorType);
    }
}
