package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeFeatureRunner;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class CaveThermalSpringsDecorator {
    private static final int GRID = 4;
    private static final float ANCHOR_ATTEMPT = 0.72f;

    private CaveThermalSpringsDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        for (int lx = 1; lx < 16; lx += 4) {
            for (int lz = 1; lz < 16; lz += 4) {
                Holder<Biome> biome;
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || carver.isEntranceColumn(lx, lz) || !MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, wx, floorY, wz) || !CaveThermalSpringsDecorator.isTarget(biome = carver.resolveBiome(chunk, lx, floorY, lz)) || CaveBiomeIds.isSteamingJungleBiome(biome) && !CaveBiomeIds.isSteamingThermalCell(region.getSeed(), wx, wz)) continue;
                random.setDecorationSeed(region.getSeed(), wx, wz);
                if (random.nextFloat() > 0.72f) continue;
                CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, new BlockPos(wx, floorY, wz), random, false);
            }
        }
    }

    private static boolean isTarget(Holder<Biome> biome) {
        return CaveBiomeIds.isModThermalPresetBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome);
    }
}
