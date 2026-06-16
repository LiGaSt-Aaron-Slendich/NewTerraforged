package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveUndergroundGuard {
    public static final int MIN_ANCHOR_DEPTH = 10;
    public static final int MEGA_GIGA_ANCHOR_DEPTH = 12;
    public static final int ENTRANCE_ANCHOR_DEPTH = 3;
    public static final int ENTRANCE_BIOME_DEPTH = 6;
    private static final int[] NEIGHBOR_Y = new int[]{0, 1, -1, 2, -2};

    private CaveUndergroundGuard() {
    }

    public static boolean mayPlaceAnchor(ChunkAccess chunk, int lx, int y, int lz, boolean megaGiga) {
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return false;
        }
        return CaveUndergroundGuard.isBelowAnchorDepth(chunk, lx, y, lz, megaGiga);
    }

    public static boolean mayPlaceAnchorForBiome(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> expected, boolean megaGiga, boolean entranceColumn) {
        if (entranceColumn && !megaGiga) {
            return false;
        }
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return false;
        }
        if (!CaveUndergroundGuard.biomeMatchesAnchor(chunk, carver, lx, y, lz, expected)) {
            if (megaGiga && CaveBiomeIds.isModCaveBiome(expected)) {
                Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
                if (!CaveBiomeIds.isModCaveBiome(resolved) || !CaveBiomeIds.matchesDecorAnchor(expected, resolved)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return CaveUndergroundGuard.isBelowAnchorDepth(chunk, lx, y, lz, megaGiga);
    }

    /** Ceiling scatter — only needs to stay below open sky, not full floor anchor depth. */
    public static boolean mayPlaceCeilingAnchorForBiome(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> expected, boolean megaGiga, boolean entranceColumn) {
        if (entranceColumn && !megaGiga) {
            return false;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y >= surface - 4) {
            return false;
        }
        if (CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz)) {
            return false;
        }
        if (!CaveUndergroundGuard.biomeMatchesAnchor(chunk, carver, lx, y, lz, expected)) {
            if (megaGiga && CaveBiomeIds.isModCaveBiome(expected)) {
                Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
                if (!CaveBiomeIds.isModCaveBiome(resolved) || !CaveBiomeIds.matchesDecorAnchor(expected, resolved)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean biomeMatchesAnchor(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> expected) {
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted != null && CaveBiomeIds.isUndergroundBiome(painted) && CaveBiomeIds.matchesDecorAnchor(expected, painted)) {
            return true;
        }
        for (int dy : NEIGHBOR_Y) {
            int checkY = y + dy;
            if (checkY < chunk.getMinBuildHeight() || checkY > chunk.getMaxBuildHeight() || (painted = CarverChunk.readPaintedBiomeAt(chunk, lx, checkY, lz)) == null || !CaveBiomeIds.isUndergroundBiome(painted) || !CaveBiomeIds.matchesDecorAnchor(expected, painted)) continue;
            return true;
        }
        Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
        return CaveBiomeIds.matchesDecorAnchor(expected, resolved);
    }

    public static boolean mayPlaceEntranceAccent(ChunkAccess chunk, int lx, int y, int lz) {
        if (CaveOpenAirCheck.isSunFloor(chunk, lx, y, lz)) {
            return false;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y >= surface || y < surface - 20) {
            return false;
        }
        return chunk.getBlockState(new BlockPos(lx, y, lz)).isAir();
    }

    public static boolean mayWriteBlockForBiome(WorldGenLevel level, ChunkAccess primaryChunk, BlockPos worldPos, Holder<Biome> boundBiome, CarverChunk carver) {
        ChunkAccess chunk = CaveUndergroundGuard.resolveChunk(level, primaryChunk, worldPos);
        int lx = worldPos.getX() & 0xF;
        int lz = worldPos.getZ() & 0xF;
        int y = worldPos.getY();
        boolean megaGiga = carver != null && carver.isColumnCacheReady() && carver.columnCache().isMegaGigaZone(lx, lz);
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return false;
        }
        for (int dy : NEIGHBOR_Y) {
            int checkY = y + dy;
            if (checkY < chunk.getMinBuildHeight() || checkY > chunk.getMaxBuildHeight() || !CaveUndergroundGuard.biomeMatches(chunk, carver, lx, checkY, lz, boundBiome)) continue;
            return true;
        }
        return false;
    }

    private static boolean biomeMatches(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Holder<Biome> bound) {
        Holder<Biome> biome = carver != null ? carver.resolveBiome(chunk, lx, y, lz) : CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        return biome != null && CaveBiomeIds.matchesDecorAnchor(bound, biome);
    }

    public static boolean mayWriteBlock(WorldGenLevel level, ChunkAccess primaryChunk, BlockPos worldPos) {
        return CaveUndergroundGuard.mayWriteBlock(level, primaryChunk, worldPos, null);
    }

    public static boolean mayWriteBlock(WorldGenLevel level, ChunkAccess primaryChunk, BlockPos worldPos, CarverChunk carver) {
        ChunkAccess chunk = CaveUndergroundGuard.resolveChunk(level, primaryChunk, worldPos);
        int lx = worldPos.getX() & 0xF;
        int lz = worldPos.getZ() & 0xF;
        int y = worldPos.getY();
        boolean megaGiga = carver != null && carver.isColumnCacheReady() && carver.columnCache().isMegaGigaZone(lx, lz);
        if (carver != null && carver.isColumnCacheReady()) {
            return !carver.columnCache().forbidsUndergroundWrite(lx, y, lz, chunk, carver.isEntranceColumn(lx, lz));
        }
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return false;
        }
        return !CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz);
    }

    private static boolean isBelowAnchorDepth(ChunkAccess chunk, int lx, int y, int lz, boolean megaGiga) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minDepth = megaGiga ? MEGA_GIGA_ANCHOR_DEPTH : MIN_ANCHOR_DEPTH;
        return y < surface - minDepth;
    }

    private static ChunkAccess resolveChunk(WorldGenLevel level, ChunkAccess primaryChunk, BlockPos worldPos) {
        int cx = worldPos.getX() >> 4;
        int cz = worldPos.getZ() >> 4;
        if (cx == primaryChunk.getPos().x && cz == primaryChunk.getPos().z) {
            return primaryChunk;
        }
        if (level.hasChunk(cx, cz)) {
            return level.getChunk(cx, cz);
        }
        return primaryChunk;
    }
}
