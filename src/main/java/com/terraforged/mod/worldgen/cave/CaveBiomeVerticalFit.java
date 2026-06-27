package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Ensures mega/giga chamber biomes fit the carved vertical span.
 * Narrow slits inherit a fitting cave biome from above/below or pick any compact cave biome — never surface.
 */
public final class CaveBiomeVerticalFit {
    private static final int DEFAULT_MIN = 6;
    private static final int MEDIUM_MIN = 10;
    private static final int TALL_MIN = 14;
    private static final int COLUMN_MIN = 18;
    private static final int VERTICAL_PROBE = 56;
    private static final int[][] NEIGHBOR_OFFSETS = new int[][]{
            {0, -8}, {0, 8}, {-8, 0}, {8, 0},
            {0, -16}, {0, 16}, {-16, 0}, {16, 0},
            {-8, -8}, {8, 8}, {-8, 8}, {8, -8}
    };

    private CaveBiomeVerticalFit() {
    }

    public static Holder<Biome> resolve(Holder<Biome> layoutBiome, int verticalSpan, CarverChunk carver, ChunkAccess chunk, Generator generator, NoiseCave config, int x, int z, int bottom, int top) {
        if (layoutBiome == null || verticalSpan <= 0) {
            return layoutBiome;
        }
        if (CaveBiomeVerticalFit.fits(layoutBiome, verticalSpan)) {
            return layoutBiome;
        }
        int lx = x & 0xF;
        int lz = z & 0xF;
        int midY = bottom + top >> 1;
        Holder<Biome> above = CaveBiomeVerticalFit.findPaintedCaveBiome(chunk, lx, lz, top + 1, Math.min(chunk.getMaxBuildHeight() - 1, top + VERTICAL_PROBE), 1, verticalSpan);
        if (above != null) {
            return above;
        }
        Holder<Biome> below = CaveBiomeVerticalFit.findPaintedCaveBiome(chunk, lx, lz, bottom - 1, Math.max(chunk.getMinBuildHeight(), bottom - VERTICAL_PROBE), -1, verticalSpan);
        if (below != null) {
            return below;
        }
        if (carver != null && config != null && generator != null) {
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Holder<Biome> neighbor = carver.getBiome(x + offset[0], z + offset[1], midY, config, generator);
                if (CaveBiomeVerticalFit.isUsableFallback(neighbor, layoutBiome, verticalSpan)) {
                    return neighbor;
                }
            }
        }
        CaveBiomeRegistry registry = generator != null ? generator.getBiomeSource().getCaveBiomeRegistry() : null;
        if (registry != null && !registry.isVanillaFallback()) {
            Holder<Biome> picked = CaveBiomeVerticalFit.pickFittingCaveBiome(registry, layoutBiome, verticalSpan, config != null ? config.getSeed() : 0, x, z);
            if (picked != null) {
                return picked;
            }
        }
        return layoutBiome;
    }

    private static Holder<Biome> findPaintedCaveBiome(ChunkAccess chunk, int lx, int lz, int fromY, int toY, int step, int verticalSpan) {
        int y = fromY;
        while (step > 0 ? y <= toY : y >= toY) {
            Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
            if (painted != null && CaveBiomeVerticalFit.fits(painted, verticalSpan)
                    && (CaveBiomeIds.isModCaveBiome(painted) || CaveBiomeIds.isUndergroundBiome(painted))) {
                return painted;
            }
            y += step;
        }
        return null;
    }

    private static boolean isUsableFallback(Holder<Biome> candidate, Holder<Biome> layout, int verticalSpan) {
        if (candidate == null || !CaveBiomeVerticalFit.fits(candidate, verticalSpan)) {
            return false;
        }
        if (!CaveBiomeIds.isModCaveBiome(candidate) && !CaveBiomeIds.isUndergroundBiome(candidate)) {
            return false;
        }
        return layout == null || !candidate.equals(layout);
    }

    private static Holder<Biome> pickFittingCaveBiome(CaveBiomeRegistry registry, Holder<Biome> layout, int verticalSpan, int seed, int x, int z) {
        ArrayList<CaveBiomeEntry> candidates = new ArrayList<>();
        for (CaveBiomeEntry entry : registry.getPrimary()) {
            CaveBiomeVerticalFit.collectCandidate(candidates, entry, verticalSpan);
        }
        for (CaveBiomeEntry entry : registry.getTransition()) {
            CaveBiomeVerticalFit.collectCandidate(candidates, entry, verticalSpan);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparingInt((CaveBiomeEntry e) -> CaveBiomeIds.sharesCaveTheme(registry.getHolder(e).orElse(null), layout) ? 0 : 1)
                .thenComparingInt(e -> -CaveBiomeVerticalFit.minChamberHeight(e.biome()))
                .thenComparingInt(e -> CaveBiomeIds.isSparseCaveBiome(e.biome()) ? 0 : 1));
        int pick = Math.floorMod(seed ^ x * 734287 ^ z * 912271, candidates.size());
        return registry.getHolder(candidates.get(pick)).orElse(null);
    }

    private static void collectCandidate(List<CaveBiomeEntry> out, CaveBiomeEntry entry, int verticalSpan) {
        if (entry == null || CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || CaveBiomeIds.isNetherThemedBiome(entry.biome())) {
            return;
        }
        if (CaveBiomeVerticalFit.minChamberHeight(entry.biome()) <= verticalSpan) {
            out.add(entry);
        }
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
