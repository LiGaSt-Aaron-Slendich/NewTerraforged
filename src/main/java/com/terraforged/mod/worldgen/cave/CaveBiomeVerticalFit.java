package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Ensures mega/giga chamber biomes fit the carved vertical span.
 * Narrow slits use the surface biome above instead of a layout biome that cannot place its features.
 */
public final class CaveBiomeVerticalFit {
    private static final int DEFAULT_MIN = 6;
    private static final int MEDIUM_MIN = 10;
    private static final int TALL_MIN = 14;
    private static final int COLUMN_MIN = 18;

    private CaveBiomeVerticalFit() {
    }

    public static Holder<Biome> resolve(Holder<Biome> layoutBiome, Holder<Biome> surfaceBiome, int verticalSpan) {
        if (layoutBiome == null) {
            return surfaceBiome;
        }
        if (verticalSpan >= CaveBiomeVerticalFit.minChamberHeight(layoutBiome)) {
            return layoutBiome;
        }
        if (surfaceBiome != null && !CaveBiomeIds.isModCaveBiome(surfaceBiome) && !CaveBiomeIds.isUndergroundBiome(surfaceBiome)) {
            return surfaceBiome;
        }
        return layoutBiome;
    }

    public static int minChamberHeight(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveBiomeVerticalFit.minChamberHeight(key.location())).orElse(DEFAULT_MIN);
    }

    public static int minChamberHeight(ResourceLocation id) {
        if (id == null) {
            return DEFAULT_MIN;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("frostfire") || path.contains("yellowstone") || path.contains("thermal") || path.contains("mantle")) {
            return TALL_MIN;
        }
        if (path.contains("column") || path.contains("crystal") || path.contains("prismachasm") || path.contains("icicle")) {
            return COLUMN_MIN;
        }
        if (path.contains("fungal") || path.contains("mycotoxic") || path.contains("bioshroom") || path.contains("scorching") || path.contains("brimstone")) {
            return MEDIUM_MIN;
        }
        if (CaveBiomeIds.isSparseCaveBiome(id)) {
            return 4;
        }
        return DEFAULT_MIN;
    }

    public static boolean fits(Holder<Biome> biome, int verticalSpan) {
        return verticalSpan >= CaveBiomeVerticalFit.minChamberHeight(biome);
    }
}
