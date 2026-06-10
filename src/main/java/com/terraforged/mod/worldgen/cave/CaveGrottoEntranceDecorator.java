package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveGrottoEntranceDecorator {
    private static final int GRID = 2;
    private static final ResourceLocation LARGE_DRIPSTONE = new ResourceLocation("minecraft", "large_dripstone");
    private static final ResourceLocation POINTED_DRIPSTONE = new ResourceLocation("minecraft", "pointed_dripstone");
    private static final ResourceLocation DRIPSTONE_CLUSTER = new ResourceLocation("minecraft", "dripstone_cluster");

    private CaveGrottoEntranceDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        CaveBiomeRegistry registry = generator.getBiomeSource().getCaveBiomeRegistry();
        if (registry == null || registry.isVanillaFallback() || registry.getCoastal().isEmpty()) {
            return;
        }
        int sea = generator.getSeaLevel();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int grottoBottom = sea + 3;
        Registry featureRegistry = region.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
        Holder<PlacedFeature> pointed = CaveGrottoEntranceDecorator.resolve((Registry<PlacedFeature>)featureRegistry, POINTED_DRIPSTONE);
        if (pointed == null) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        for (int lx = 0; lx < 16; lx += 2) {
            for (int lz = 0; lz < 16; lz += 2) {
                int midY;
                Holder<Biome> biome;
                int surface;
                int floorY;
                int wz;
                int wx;
                if (!carver.isCoastalEntranceColumn(lx, lz) || !CaveOceanFilter.isNearSea(generator, wx = chunkX + lx, wz = chunkZ + lz) || (floorY = CaveGrottoEntranceDecorator.findGrottoFloor(chunk, lx, lz, grottoBottom, Math.min(maxY, (surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz)) - 2))) < 0 || !CaveGrottoEntranceDecorator.isGrottoBiome(biome = carver.resolveBiome(chunk, lx, floorY, lz), registry) || !CaveUndergroundGuard.mayPlaceEntranceAccent(chunk, lx, floorY, lz)) continue;
                int ceilY = CaveGrottoEntranceDecorator.findGrottoCeiling(chunk, lx, lz, floorY + 3, Math.min(maxY, surface - 1));
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                float humidity = 0.55f + (float)(surface - sea) * 0.015f;
                if (pointed != null && ceilY > floorY + 2 && random.nextFloat() < humidity * 0.88f) {
                    random.setFeatureSeed(seed, 2, 1);
                    FeaturePlacement.place(pointed, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, ceilY, wz), true);
                }
                if (pointed == null || !(random.nextFloat() < humidity * 0.72f) || !CaveUndergroundGuard.mayPlaceEntranceAccent(chunk, lx, midY = floorY + Math.max(floorY + 2, ceilY) >> 1, lz)) continue;
                random.setFeatureSeed(seed, 3, 2);
                FeaturePlacement.place(pointed, region, (ChunkGenerator)generator, (Random)random, new BlockPos(wx, midY, wz), true);
            }
        }
    }

    private static boolean isGrottoBiome(Holder<Biome> biome, CaveBiomeRegistry registry) {
        if (registry.isCoastalBiome(biome)) {
            return true;
        }
        return CaveBiomeIds.isCoastalGrottoBiome(biome);
    }

    private static int findGrottoFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY, y -> {
            BlockPos pos = new BlockPos(lx, y, lz);
            return chunk.getBlockState(pos).isAir() && !chunk.getBlockState(pos.below()).isAir();
        });
    }

    private static int findGrottoCeiling(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        return CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY, maxY);
    }

    private static Holder<PlacedFeature> resolve(Registry<PlacedFeature> registry, ResourceLocation id) {
        return registry.getHolder(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, (ResourceLocation)id)).orElse(null);
    }
}
