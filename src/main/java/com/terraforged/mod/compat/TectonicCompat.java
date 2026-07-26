package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tectonic rewrites vanilla {@code noise_settings} / density functions for the Overworld.
 * NewTerraforged owns terrain via its chunk generator — keep the mod JAR loaded (other biome
 * mods stay usable) but suppress Tectonic generative datapacks so create-world validation
 * and TF terrain stay intact.
 */
public final class TectonicCompat {
    public static final String MOD_ID = "tectonic";

    private static boolean initialized;
    private static boolean loaded;
    private static boolean warned;

    private TectonicCompat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        loaded = ModList.get().isLoaded(MOD_ID);
        if (loaded) {
            warn("common setup");
        }
    }

    public static boolean isLoaded() {
        init();
        return loaded;
    }

    /** Pin vanilla noise settings / drop Tectonic terrain packs when TF generator is used. */
    public static boolean shouldSuppressTerrainInjection() {
        return isLoaded();
    }

    public static boolean isTerrainPackId(String packId) {
        if (packId == null || packId.isEmpty()) {
            return false;
        }
        String id = packId.toLowerCase(Locale.ROOT);
        // Terratonic is a biome bridge — leave it alone.
        if (id.contains("terratonic")) {
            return false;
        }
        // mod:tectonic, file/tectonic*, tectonic.zip, etc.
        return id.equals(MOD_ID)
                || id.equals("mod:" + MOD_ID)
                || id.contains("tectonic");
    }

    /** Remove Tectonic generative packs from the selected set (biome mods stay). */
    public static List<String> filterSelectedPackIds(Collection<String> selected) {
        init();
        if (!loaded || selected == null || selected.isEmpty()) {
            return selected instanceof List ? (List<String>) selected : new ArrayList<>(selected);
        }
        List<String> out = new ArrayList<>(selected.size());
        boolean removed = false;
        for (String id : selected) {
            if (isTerrainPackId(id)) {
                removed = true;
                TerraForged.LOG.info("[Tectonic] Deselected generative datapack {}", id);
                continue;
            }
            out.add(id);
        }
        if (removed) {
            warn("datapack selection");
        }
        return out;
    }

    /** Merge tectonic pack ids into the disabled list (create-world DataPackConfig). */
    public static List<String> withDisabledTerrainPacks(Collection<String> enabled, Collection<String> disabled) {
        init();
        Set<String> out = new LinkedHashSet<>();
        if (disabled != null) {
            out.addAll(disabled);
        }
        if (!loaded || enabled == null) {
            return new ArrayList<>(out);
        }
        for (String id : enabled) {
            if (isTerrainPackId(id)) {
                out.add(id);
            }
        }
        return new ArrayList<>(out);
    }

    public static void onGeneratorActive() {
        init();
        if (loaded) {
            warn("generator active");
        }
    }

    private static void warn(String when) {
        if (warned) {
            return;
        }
        warned = true;
        TerraForged.LOG.warn(
            "[Tectonic] {} is loaded ({}) — suppressing Tectonic Overworld terrain datapacks; NewTerraforged owns generation. Biome mods remain unaffected.",
            MOD_ID,
            when);
    }
}
