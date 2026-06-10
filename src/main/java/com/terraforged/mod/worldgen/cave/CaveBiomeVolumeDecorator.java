package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.compat.DynamicTreesCompat;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeFeatureRunner;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class CaveBiomeVolumeDecorator {
    private static final int ANCHOR_GRID = 2;
    private static final int MEGA_GIGA_ANCHOR_GRID = 4;
    private static final int FUNGAL_MEGA_GIGA_GRID = 4;

    private CaveBiomeVolumeDecorator() {
    }

    public static void decorateChunk(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
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
        for (Map.Entry<Holder<Biome>, BlockPos> entry : painted.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(biome) || !decoratedBiomes.add(biome)) continue;
            boolean trees = CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome);
            boolean megaGigaChunk = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, chunkX + 8, minY + maxY >> 1, chunkZ + 8);
            List<BlockPos> floorAnchors = CaveBiomeVolumeDecorator.collectFloorAnchors(chunk, carver, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, generator, megaGigaChunk);
            for (BlockPos anchor : floorAnchors) {
                if (carver.isEntranceColumn(anchor.getX() & 0xF, anchor.getZ() & 0xF)) continue;
                random.setDecorationSeed(region.getSeed(), anchor.getX(), anchor.getZ());
                if (DynamicTreesCompat.isLoaded() && CaveBiomeIds.isFungalCaveBiome(biome)) {
                    DynamicTreesCompat.decorateFungalCave(chunk, carver, region, generator, biome, anchor, random);
                }
                if (CaveBiomeIds.isFungalCaveBiome(biome)) {
                    CaveBiomeFeatureRunner.decorateFungalCover(chunk, carver, region, generator, biome, anchor, random);
                }
                CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, anchor, random, trees);
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
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx += 2) {
            for (int lz = 0; lz < 16; lz += 2) {
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
        return CaveColumnScan.findTopValidFloor(chunk, lx, lz, minY, maxY, y -> {
            boolean megaGiga = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, wx, y, wz);
            boolean entrance = carver.isEntranceColumn(lx, lz);
            if (target != null) {
                return CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, target, megaGiga, entrance);
            }
            Holder<Biome> resolved = carver.resolveBiome(chunk, lx, y, lz);
            if (!CaveBiomeIds.isModCaveBiome(resolved) || CaveBiomeIds.isBlockedCaveBiome(resolved)) {
                return false;
            }
            return CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, resolved, megaGiga, entrance);
        });
    }

    private static int anchorGridFor(Holder<Biome> biome, boolean megaGigaChunk) {
        if (CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome)) {
            return 6;
        }
        if (CaveBiomeIds.isFungalCaveBiome(biome) && megaGigaChunk) {
            return 4;
        }
        if (CaveBiomeIds.isModJunglePresetBiome(biome) || CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isModThermalPresetBiome(biome)) {
            return 3;
        }
        if (megaGigaChunk) {
            return 4;
        }
        return 2;
    }
}
