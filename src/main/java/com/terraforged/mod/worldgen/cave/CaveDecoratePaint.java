package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Shared decorate ↔ paint bridge for synapse, mega, and giga caves.
 * Fixes the same root failure mode everywhere: carve leaves air but skips quart paint near
 * the surface band, so grid anchors fail {@link CaveBiomeIds#matchesDecoratePaint} and
 * cover/features only hit a few clustered columns.
 */
public final class CaveDecoratePaint {
    private static final int PAINT_SEARCH_VERTICAL = 20;
    private static final int SURFACE_PAINT_GAP = 12;

    private CaveDecoratePaint() {
    }

    public static boolean mayDecorateAt(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> target, boolean megaGiga) {
        if (target == null) {
            return false;
        }
        Holder<Biome> resolved = CaveDecoratePaint.resolveForDecoration(chunk, carver, lx, y, lz, target);
        if (CaveBiomeIds.matchesDecoratePaint(resolved, target)) {
            return true;
        }
        if (!CaveDecoratePaint.isOpenCaveAir(chunk, lx, y, lz)) {
            return false;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y >= surface - 4) {
            return false;
        }
        if (megaGiga) {
            return carver.isColumnCacheReady() && carver.columnCache().isMegaGigaZone(lx, lz);
        }
        return y < surface - SURFACE_PAINT_GAP;
    }

    public static Holder<Biome> resolveForDecoration(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> target) {
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted != null && CaveBiomeIds.isUndergroundBiome(painted)) {
            return painted;
        }
        Holder<Biome> columnPaint = CaveDecoratePaint.findColumnUndergroundPaint(chunk, lx, y, lz, target);
        if (columnPaint != null) {
            return columnPaint;
        }
        Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
        if (CaveBiomeIds.matchesDecoratePaint(resolved, target)) {
            return resolved;
        }
        if (CaveDecoratePaint.isOpenCaveAir(chunk, lx, y, lz) && CaveBiomeIds.isUndergroundBiome(target)) {
            return target;
        }
        return resolved;
    }

    /** Paint floor quart + neighbours when carve skipped the surface band. */
    public static void ensureFloorPaint(ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, int lx, int floorY, int lz) {
        if (biome == null || floorY < chunk.getMinBuildHeight()) {
            return;
        }
        Holder<Biome> existing = CarverChunk.readPaintedBiomeAt(chunk, lx, floorY, lz);
        if (existing != null && CaveBiomeIds.matchesDecoratePaint(existing, biome)) {
            return;
        }
        CarverChunk.writeBiomeAt(chunk, lx, floorY, lz, biome);
        carver.markBiomeRestoreColumn(lx, lz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int px = lx + ox;
                int pz = lz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                    continue;
                }
                pos.set(px, floorY, pz);
                if (!chunk.getBlockState(pos).isAir()) {
                    CarverChunk.writeBiomeAt(chunk, px, floorY, pz, biome);
                }
            }
        }
    }

    private static Holder<Biome> findColumnUndergroundPaint(ChunkAccess chunk, int lx, int centerY, int lz, Holder<Biome> target) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int yStart = Math.min(maxY, centerY + PAINT_SEARCH_VERTICAL);
        int yEnd = Math.max(minY, centerY - PAINT_SEARCH_VERTICAL);
        Holder<Biome> themed = null;
        for (int y = yStart; y >= yEnd; y -= 4) {
            Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
            if (painted == null || !CaveBiomeIds.isUndergroundBiome(painted)) {
                continue;
            }
            if (CaveBiomeIds.matchesDecoratePaint(painted, target)) {
                return painted;
            }
            if (themed == null && CaveBiomeIds.sharesCaveTheme(painted, target)) {
                themed = painted;
            }
        }
        return themed;
    }

    private static boolean isOpenCaveAir(ChunkAccess chunk, int lx, int y, int lz) {
        BlockPos pos = new BlockPos(lx, y, lz);
        if (!chunk.getBlockState(pos).isAir()) {
            return false;
        }
        return y > chunk.getMinBuildHeight() && !chunk.getBlockState(pos.below()).isAir();
    }
}
