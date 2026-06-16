package com.terraforged.mod.worldgen.cave;

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

/**
 * Per-biome decorator routing: official TF for scorching/dripstone, vanilla for glowing grotto,
 * legacy scatter for fungal/bioshroom, compromise for everything else.
 */
public final class CaveHybridBiomeDecorator {
    private CaveHybridBiomeDecorator() {
    }

    public static void decorateVolume(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        Map<Holder<Biome>, BlockPos> painted = CaveBiomeVolumeDecorator.collectPaintedModBiomesPublic(chunk, carver, generator);
        if (painted.isEmpty()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        boolean megaGigaChunk = columns.anyMegaGiga() || CaveBiomeVolumeDecorator.hasUndergroundCaveVolumePublic(chunk);
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        HashSet<Holder<Biome>> decorated = new HashSet<>();
        ArrayList<Map.Entry<Holder<Biome>, BlockPos>> officialDeferred = new ArrayList<>();
        for (Map.Entry<Holder<Biome>, BlockPos> entry : painted.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(biome) || !decorated.add(biome)) {
                continue;
            }
            CaveDecoratorKind kind = CaveBiomeDecoratorRouter.resolve(biome);
            if (kind == CaveDecoratorKind.OFFICIAL) {
                officialDeferred.add(entry);
                continue;
            }
            CaveHybridBiomeDecorator.decorateBiomeEntry(kind, chunk, carver, region, generator, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, megaGigaChunk, random);
        }
        for (Map.Entry<Holder<Biome>, BlockPos> entry : officialDeferred) {
            Holder<Biome> biome = entry.getKey();
            List<BlockPos> origins = CaveHybridBiomeDecorator.collectOriginsForBiome(chunk, carver, generator, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, 6);
            TerraForgedOfficialCaveDecorator.decorateBiome(origins, chunk, carver, region, generator, biome);
        }
        if (columns.anyMegaGiga()) {
            CaveMegaAccentDecorator.decorate(chunk, carver, region, generator);
            if (carver.hasTunnelRiver()) {
                CaveTunnelRiverDecorator.decorate(chunk, carver, region, generator);
            }
        }
    }

    private static void decorateBiomeEntry(CaveDecoratorKind kind, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos seed, int chunkX, int chunkZ, int minY, int maxY, boolean megaGigaChunk, WorldgenRandom random) {
        switch (kind) {
            case VANILLA -> CaveBiomeVanillaPass.decorateBiome(chunk, carver, region, generator, biome, seed);
            case LEGACY -> CaveBiomeVolumeDecorator.decorateSingleBiome(chunk, carver, region, generator, biome, seed, true, random, true);
            default -> CaveBiomeVolumeDecorator.decorateSingleBiome(chunk, carver, region, generator, biome, seed, megaGigaChunk, random, CaveBiomeIds.isCoverDenseCaveBiome(biome));
        }
    }

    public static void decorateEntrances(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        if (carver.hasAnyEntranceColumn()) {
            CaveEntranceSurfaceDecorator.decorate(chunk, carver, region, generator);
            CaveEntranceVanillaDecorator.decorate(chunk, carver, region, generator);
        }
    }

    static List<BlockPos> collectOriginsForBiome(ChunkAccess chunk, CarverChunk carver, Generator generator, Holder<Biome> target, BlockPos seed, int chunkX, int chunkZ, int minY, int maxY, int grid) {
        ArrayList<BlockPos> origins = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();
        seen.add(seed.asLong());
        origins.add(seed);
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                BlockPos pos;
                Holder<Biome> resolved;
                int floorY = CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, target, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0 || !CaveBiomeIds.sameBiomeKey(resolved = carver.resolveBiome(chunk, lx, floorY, lz), target) || !seen.add((pos = new BlockPos(chunkX + lx, floorY, chunkZ + lz)).asLong())) {
                    continue;
                }
                origins.add(pos);
            }
        }
        return origins;
    }
}
