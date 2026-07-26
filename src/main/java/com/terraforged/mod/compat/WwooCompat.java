package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.fml.ModList;

/**
 * WWOO (William Wythers' Overhauled Overworld) is a datapack mod ({@code wwoo_forge}) that
 * replaces {@code minecraft:overworld} noise settings and vanilla biomes. With a NewTF/TF
 * chunk generator that still consumes those registries for surface rules + VanillaDecorator,
 * WWOO features carve under-river shafts.
 *
 * Gate (when WWOO is loaded and TF generator is active):
 * <ul>
 *   <li>Pin {@link NoiseGeneratorSettings} to builtin vanilla overworld (not WWOO datapack)</li>
 *   <li>Skip subsurface carve-like placed features in {@code VanillaDecorator}</li>
 * </ul>
 * Soft optional dependency — never hard-dep or void-fill around WWOO.
 */
public final class WwooCompat {
    public static final String WWOO_MOD_ID = "wwoo_forge";

    /** Paths that punch air/lava columns under river beds when WWOO rewrites biome feature lists. */
    private static final String[] BLOCKED_SURFACE_FEATURE_FRAGMENTS = {
        "lake_lava",
        "lake_lava_underground",
        "lake_lava_surface",
        "monster_room",
        "underwater_magma",
        "spring_lava",
        "spring_water",
        "amethyst_geode",
        "geode"
    };

    private static boolean initialized;
    private static boolean wwooLoaded;
    private static boolean warned;
    private static boolean gateLogged;

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
        TectonicCompat.onGeneratorActive();
        if (wwooLoaded) {
            warnConflict("generator active");
            if (!gateLogged) {
                gateLogged = true;
                TerraForged.LOG.info(
                    "[WWOO] Gate ON — pinning builtin vanilla NoiseGeneratorSettings and filtering subsurface VanillaDecorator features");
            }
        }
    }

    public static boolean isWwooLoaded() {
        init();
        return wwooLoaded;
    }

    /** True when WWOO is present and TF/NewTF generator path should suppress WWOO injection. */
    public static boolean shouldSuppressWwooInjection() {
        return isWwooLoaded();
    }

    /** True when any known Overworld terrain-overhaul datapack mod should be pinned off TF gen. */
    public static boolean shouldPinBuiltinNoiseSettings() {
        return shouldSuppressWwooInjection() || TectonicCompat.shouldSuppressTerrainInjection();
    }

    /**
     * Noise settings for {@link com.terraforged.mod.worldgen.VanillaGen}.
     * When WWOO/Tectonic is loaded, use builtin vanilla overworld instead of the datapack-polluted registry entry.
     */
    public static Holder<NoiseGeneratorSettings> resolveOverworldNoiseSettings(RegistryAccess access) {
        init();
        TectonicCompat.init();
        Registry<NoiseGeneratorSettings> dynamic = access.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        if (!shouldPinBuiltinNoiseSettings()) {
            return dynamic.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        }
        Holder<NoiseGeneratorSettings> builtin = BuiltinRegistries.NOISE_GENERATOR_SETTINGS.getHolder(NoiseGeneratorSettings.OVERWORLD).orElse(null);
        if (builtin != null) {
            return builtin;
        }
        return Holder.direct(NoiseGeneratorSettings.overworld(false, false));
    }

    /** Skip WWOO-injected features that carve under rivers / punch subsurface voids. */
    public static boolean shouldSkipSurfaceFeature(Holder<PlacedFeature> holder) {
        if (!shouldSuppressWwooInjection() || holder == null) {
            return false;
        }
        ResourceLocation id = holder.unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        for (String fragment : BLOCKED_SURFACE_FEATURE_FRAGMENTS) {
            if (path.contains(fragment)) {
                return true;
            }
        }
        if ("wythers".equals(id.getNamespace())
            && (path.contains("lake") || path.contains("magma") || path.contains("dungeon") || path.contains("spring"))) {
            return true;
        }
        return false;
    }

    private static void warnConflict(String when) {
        if (warned) {
            return;
        }
        warned = true;
        TerraForged.LOG.warn(
            "[WWOO] {} is loaded ({}) — suppressing WWOO noise-settings + subsurface feature injection for TerraForged/NewTF generator. Prefer disabling WWOO if you still see river shafts on new chunks.",
            WWOO_MOD_ID,
            when);
    }
}
