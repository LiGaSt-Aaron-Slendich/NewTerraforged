package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.compat.DynamicTreesCompat;
import com.terraforged.mod.worldgen.Generator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class CaveBiomeVolumeDecorator {
    private static final int MEGA_GIGA_ANCHOR_GRID = 6;

    private CaveBiomeVolumeDecorator() {
    }

    public static void decorateChunk(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        CarverColumnCache columns = carver.columnCache();
        if (!columns.anyMegaGiga() && !carver.hasDecorateAnchors() && !CaveBiomeVolumeDecorator.hasUndergroundCaveVolume(chunk)) {
            return;
        }
        Map<Holder<Biome>, BlockPos> painted = CaveBiomeVolumeDecorator.collectPaintedModBiomes(chunk, carver, generator);
        if (painted.isEmpty()) {
            return;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        HashSet<Holder<Biome>> decoratedBiomes = new HashSet<Holder<Biome>>();
        boolean megaGigaChunk = columns.anyMegaGiga() || CaveBiomeVolumeDecorator.hasUndergroundCaveVolume(chunk);
        for (Map.Entry<Holder<Biome>, BlockPos> entry : painted.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(biome) || !decoratedBiomes.add(biome)) continue;
            List<BlockPos> coverAnchors = megaGigaChunk ? CaveBiomeVolumeDecorator.collectFloorAnchors(chunk, carver, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, generator, true) : CaveBiomeVolumeDecorator.collectLightCoverAnchors(chunk, carver, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, generator);
            for (BlockPos anchor : coverAnchors) {
                if (carver.isEntranceColumn(anchor.getX() & 0xF, anchor.getZ() & 0xF)) continue;
                random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
                CaveBiomeFeatureRunner.decorateBiomeCover(chunk, carver, region, generator, biome, anchor, random);
            }
            if (megaGigaChunk) {
                for (BlockPos anchor : coverAnchors) {
                    random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
                    if (DynamicTreesCompat.isLoaded() && CaveBiomeIds.isFungalCaveBiome(biome)) {
                        DynamicTreesCompat.decorateFungalCave(chunk, carver, region, generator, biome, anchor, random);
                    }
                    if (CaveBiomeIds.isCoverDenseCaveBiome(biome)) {
                        CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, CaveBiomeIds.isFungalCaveBiome(biome));
                    } else {
                        CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, false);
                    }
                }
            } else {
                for (BlockPos anchor : coverAnchors) {
                    random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
                    if (CaveBiomeIds.isCoverDenseCaveBiome(biome)) {
                        CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, false);
                    } else {
                        CaveBiomeFeatureRunner.decorateLightFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random);
                    }
                }
            }
        }
    }

    public static void decorateSingleBiome(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos seedAnchor, boolean megaGigaChunk, WorldgenRandom random) {
        CaveBiomeVolumeDecorator.decorateSingleBiome(chunk, carver, region, generator, biome, seedAnchor, megaGigaChunk, random, CaveBiomeIds.isFungalCaveBiome(biome));
    }

    public static void decorateSingleBiome(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos seedAnchor, boolean megaGigaChunk, WorldgenRandom random, boolean denseLegacy) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        List<BlockPos> coverAnchors = megaGigaChunk || denseLegacy ? CaveBiomeVolumeDecorator.collectFloorAnchors(chunk, carver, biome, seedAnchor, chunkX, chunkZ, minY, maxY, generator, true) : CaveBiomeVolumeDecorator.collectLightCoverAnchors(chunk, carver, biome, seedAnchor, chunkX, chunkZ, minY, maxY, generator);
        for (BlockPos anchor : coverAnchors) {
            if (carver.isEntranceColumn(anchor.getX() & 0xF, anchor.getZ() & 0xF)) continue;
            random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
            CaveBiomeFeatureRunner.decorateBiomeCover(chunk, carver, region, generator, biome, anchor, random);
        }
        boolean fullScatter = megaGigaChunk || denseLegacy;
        for (BlockPos anchor : coverAnchors) {
            random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
            if (DynamicTreesCompat.isLoaded() && CaveBiomeIds.isFungalCaveBiome(biome)) {
                DynamicTreesCompat.decorateFungalCave(chunk, carver, region, generator, biome, anchor, random);
            }
            if (fullScatter) {
                if (CaveBiomeIds.isCoverDenseCaveBiome(biome)) {
                    CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, CaveBiomeIds.isFungalCaveBiome(biome));
                } else {
                    CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, denseLegacy && CaveBiomeIds.isFungalCaveBiome(biome));
                }
            } else if (CaveBiomeIds.isCoverDenseCaveBiome(biome)) {
                CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, false);
            } else {
                CaveBiomeFeatureRunner.decorateLightFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random);
            }
        }
    }

    static Map<Holder<Biome>, BlockPos> collectPaintedModBiomesPublic(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        return CaveBiomeVolumeDecorator.collectPaintedModBiomes(chunk, carver, generator);
    }

    static int findFloorAirPublic(ChunkAccess chunk, CarverChunk carver, Holder<Biome> target, int lx, int lz, int minY, int maxY, Generator generator, int wx, int wz) {
        return CaveBiomeVolumeDecorator.findFloorAir(chunk, carver, target, lx, lz, minY, maxY, generator, wx, wz);
    }

    static boolean hasUndergroundCaveVolumePublic(ChunkAccess chunk) {
        return CaveBiomeVolumeDecorator.hasUndergroundCaveVolume(chunk);
    }

    private static List<BlockPos> collectLightCoverAnchors(ChunkAccess chunk, CarverChunk carver, Holder<Biome> target, BlockPos seedAnchor, int chunkX, int chunkZ, int minY, int maxY, Generator generator) {
        ArrayList<BlockPos> anchors = new ArrayList<BlockPos>();
        HashSet<Long> seen = new HashSet<Long>();
        seen.add(seedAnchor.asLong());
        anchors.add(seedAnchor);
        boolean dense = CaveBiomeIds.isCoverDenseCaveBiome(target);
        int grid = dense ? 3 : 4;
        int maxAnchors = dense ? 16 : 10;
        CaveBiomeVolumeDecorator.collectAnchorsOnGrid(anchors, seen, chunk, carver, target, chunkX, chunkZ, minY, maxY, generator, grid, maxAnchors, 0, 0);
        if (dense && grid > 2) {
            CaveBiomeVolumeDecorator.collectAnchorsOnGrid(anchors, seen, chunk, carver, target, chunkX, chunkZ, minY, maxY, generator, grid, maxAnchors, grid / 2, grid / 2);
        }
        return anchors;
    }

    private static void collectAnchorsOnGrid(ArrayList<BlockPos> anchors, HashSet<Long> seen, ChunkAccess chunk, CarverChunk carver, Holder<Biome> target, int chunkX, int chunkZ, int minY, int maxY, Generator generator, int grid, int maxAnchors, int offsetX, int offsetZ) {
        for (int lx = offsetX; lx < 16 && anchors.size() < maxAnchors; lx += grid) {
            for (int lz = offsetZ; lz < 16 && anchors.size() < maxAnchors; lz += grid) {
                BlockPos pos;
                Holder<Biome> resolved;
                int floorY = CaveBiomeVolumeDecorator.findFloorAir(chunk, carver, target, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0 || !CaveBiomeIds.sharesCaveTheme(resolved = carver.resolveBiome(chunk, lx, floorY, lz), target) || !seen.add((pos = new BlockPos(chunkX + lx, floorY, chunkZ + lz)).asLong())) continue;
                anchors.add(pos);
            }
        }
    }

    private static Map<Holder<Biome>, BlockPos> collectPaintedModBiomes(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        IdentityHashMap<Holder<Biome>, BlockPos> result = new IdentityHashMap<Holder<Biome>, BlockPos>();
        carver.forEachDecorateAnchor((biome, pos) -> {
            if (CaveBiomeIds.isModCaveBiome((Holder<Biome>)biome) && !CaveBiomeIds.isBlockedCaveBiome((Holder<Biome>)biome)) {
                result.putIfAbsent((Holder<Biome>)biome, (BlockPos)pos);
            }
        });
        if (!carver.columnCache().anyMegaGiga() && !CaveBiomeVolumeDecorator.hasUndergroundCaveVolume(chunk)) {
            return result;
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx += 4) {
            for (int lz = 0; lz < 16; lz += 4) {
                Holder<Biome> resolved;
                int floorY = CaveBiomeVolumeDecorator.findFloorAir(chunk, carver, null, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0 || !CaveBiomeIds.isModCaveBiome(resolved = carver.resolveBiome(chunk, lx, floorY, lz)) || CaveBiomeIds.isBlockedCaveBiome(resolved)) continue;
                result.putIfAbsent(resolved, new BlockPos(chunkX + lx, floorY, chunkZ + lz));
            }
        }
        return result;
    }

    private static List<BlockPos> collectFloorAnchors(ChunkAccess chunk, CarverChunk carver, Holder<Biome> target, BlockPos seedAnchor, int chunkX, int chunkZ, int minY, int maxY, Generator generator, boolean megaGigaChunk) {
        ArrayList<BlockPos> anchors = new ArrayList<BlockPos>();
        HashSet<Long> seen = new HashSet<Long>();
        seen.add(seedAnchor.asLong());
        anchors.add(seedAnchor);
        int grid = CaveBiomeVolumeDecorator.anchorGridFor(target, megaGigaChunk);
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                BlockPos pos;
                Holder<Biome> resolved;
                int floorY = CaveBiomeVolumeDecorator.findFloorAir(chunk, carver, target, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0 || !CaveBiomeIds.sharesCaveTheme(resolved = carver.resolveBiome(chunk, lx, floorY, lz), target) || !seen.add((pos = new BlockPos(chunkX + lx, floorY, chunkZ + lz)).asLong())) continue;
                anchors.add(pos);
            }
        }
        return anchors;
    }

    private static int findFloorAir(ChunkAccess chunk, CarverChunk carver, Holder<Biome> target, int lx, int lz, int minY, int maxY, Generator generator, int wx, int wz) {
        boolean columnMegaGiga = carver.isColumnCacheReady() && carver.columnCache().zone(lx, lz) != CarverColumnCache.ZONE_NONE;
        if (!columnMegaGiga) {
            columnMegaGiga = MegaCaveStructureFilter.isInMegaOrGigaCave(generator, wx, wz);
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minDepth = columnMegaGiga ? CaveUndergroundGuard.MEGA_GIGA_ANCHOR_DEPTH : CaveUndergroundGuard.MIN_ANCHOR_DEPTH;
        int scanTop = Math.min(maxY, surface - minDepth);
        int scanBottom = Math.max(minY, surface - 72);
        boolean entrance = carver.isEntranceColumn(lx, lz);
        for (int y = scanTop; y >= scanBottom; --y) {
            if (!CaveBiomeVolumeDecorator.isAirFloor(chunk, lx, lz, y, minY)) continue;
            boolean megaGiga = columnMegaGiga;
            if (target != null) {
                if (CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, target, megaGiga, entrance)) {
                    return y;
                }
                continue;
            }
            Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
            if (!CaveBiomeIds.isModCaveBiome(resolved) || CaveBiomeIds.isBlockedCaveBiome(resolved)) continue;
            if (!CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, resolved, megaGiga, entrance)) continue;
            return y;
        }
        return -1;
    }

    private static boolean isAirFloor(ChunkAccess chunk, int lx, int lz, int y, int minY) {
        BlockPos pos = new BlockPos(lx, y, lz);
        return chunk.getBlockState(pos).isAir() && y > minY && !chunk.getBlockState(pos.below()).isAir();
    }

    private static int anchorGridFor(Holder<Biome> biome, boolean megaGigaChunk) {
        if (CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 2;
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome) && megaGigaChunk) {
            return MEGA_GIGA_ANCHOR_GRID;
        }
        if (CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isModThermalPresetBiome(biome)) {
            return 3;
        }
        if (CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome)) {
            return 2;
        }
        if (megaGigaChunk) {
            return MEGA_GIGA_ANCHOR_GRID;
        }
        return 2;
    }

    private static boolean hasUndergroundCaveVolume(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        for (int lx = 0; lx < 16; lx += 2) {
            for (int lz = 0; lz < 16; lz += 2) {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int scanBottom = Math.max(minY, surface - 72);
                for (int y = surface - 4; y >= scanBottom; --y) {
                    BlockPos pos = new BlockPos(lx, y, lz);
                    if (chunk.getBlockState(pos).isAir() && y > minY && !chunk.getBlockState(pos.below()).isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
