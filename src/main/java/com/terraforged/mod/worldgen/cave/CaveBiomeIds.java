package com.terraforged.mod.worldgen.cave;

import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** Minimal stub for 1.19 green build. */
public final class CaveBiomeIds {
    private CaveBiomeIds() {}

    public static boolean isUndergroundBiome(ResourceLocation id) {
        if (id == null) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("cave") || path.contains("cavern") || path.contains("dripstone")
                || path.contains("lush_caves") || path.contains("deep_dark") || path.contains("grotto");
    }

    public static boolean isUndergroundBiome(Holder<Biome> biome) {
        return biome != null && biome.unwrapKey().map(k -> isUndergroundBiome(k.location())).orElse(false);
    }

    public static boolean isModCaveBiome(ResourceLocation id) {
        return isUndergroundBiome(id);
    }

    public static boolean isModCaveBiome(Holder<Biome> biome) {
        return isUndergroundBiome(biome);
    }

    public static boolean isNetherThemedBiome(Holder<Biome> biome) {
        return biome != null && biome.unwrapKey().map(k -> k.location().getPath().contains("nether")).orElse(false);
    }

    public static boolean isNetherThemedBiome(ResourceLocation id) {
        return id != null && id.getPath().toLowerCase(Locale.ROOT).contains("nether");
    }

    public static boolean isBlockedCaveBiome(ResourceLocation id) {
        return false;
    }

    public static boolean isBlockedCaveBiome(Holder<Biome> biome) {
        return false;
    }
}
