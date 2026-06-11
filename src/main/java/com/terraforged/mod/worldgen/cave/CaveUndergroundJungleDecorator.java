package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.cave.CaveJungleStreamDecorator;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class CaveUndergroundJungleDecorator {
    private static final int GRID = 5;
    private static final ResourceLocation[] FLOOR = new ResourceLocation[]{new ResourceLocation("terralith", "cave/fungal/coarse_dirt"), new ResourceLocation("minecraft", "patch_grass_jungle"), new ResourceLocation("terralith", "cave/fungal/patch_mushroom")};
    private static final ResourceLocation[] TREES = new ResourceLocation[]{new ResourceLocation("minecraft", "trees_jungle"), new ResourceLocation("minecraft", "trees_sparse_jungle")};
    private static final ResourceLocation[] VINES = new ResourceLocation[]{new ResourceLocation("terralith", "cave/fungal/vines"), new ResourceLocation("minecraft", "classic_vines_cave_feature"), new ResourceLocation("terralith", "cave/fungal/hanging_roots_single"), new ResourceLocation("terralith", "cave/fungal/hanging_roots_cluster")};

    private CaveUndergroundJungleDecorator() {
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        Registry registry = region.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
        int streamBudget = 1 + random.nextInt(2);
        for (int lx = 2; lx < 14; lx += 5) {
            for (int lz = 2; lz < 14; lz += 5) {
                int vineY;
                int ceilY;
                Holder<Biome> biome;
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = CaveUndergroundJungleDecorator.findFloor(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveUndergroundJungleDecorator.isTarget(biome = carver.resolveBiome(chunk, lx, floorY, lz)) || !CaveUndergroundJungleDecorator.mayPlace(chunk, carver, lx, floorY, lz, generator, wx, wz, biome) || CaveBiomeIds.isSteamingJungleBiome(biome) && CaveBiomeIds.isSteamingThermalCell(region.getSeed(), wx, wz)) continue;
                long seed = random.setDecorationSeed(region.getSeed(), wx, wz);
                BlockPos floorPos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(wx, floorY, wz), CaveFeatureRules.Anchor.FLOOR, false);
                boolean underground = CaveBiomeIds.isUndergroundJungleBiome(biome);
                if (random.nextFloat() < (underground ? 0.88f : 0.65f)) {
                    CaveUndergroundJungleDecorator.placeFirst((Registry<PlacedFeature>)registry, FLOOR, region, generator, random, floorPos, seed, 0);
                }
                if (random.nextFloat() < (underground ? 0.62f : 0.42f)) {
                    CaveUndergroundJungleDecorator.placeFirst((Registry<PlacedFeature>)registry, TREES, region, generator, random, floorPos, seed, 10);
                }
                if ((ceilY = CaveUndergroundJungleDecorator.findCeiling(chunk, lx, lz, floorY + 5, maxY)) > floorY + 6 && random.nextFloat() < 0.55f && CaveUndergroundJungleDecorator.mayPlace(chunk, carver, lx, vineY = ceilY - random.nextInt(2), lz, generator, wx, wz, biome)) {
                    BlockPos ceilPos = CaveFeaturePlacement.resolveWorldPos(new BlockPos(wx, vineY, wz), CaveFeatureRules.Anchor.CEILING, false);
                    CaveUndergroundJungleDecorator.placeFirst((Registry<PlacedFeature>)registry, VINES, region, generator, random, ceilPos, seed, 20);
                }
                if (streamBudget <= 0 || !(random.nextFloat() < 0.22f)) continue;
                --streamBudget;
                CaveJungleStreamDecorator.carveStream(chunk, carver, region, generator, random, wx, floorY, wz, seed);
            }
        }
    }

    private static boolean isTarget(Holder<Biome> biome) {
        return CaveBiomeIds.isUndergroundJungleBiome(biome) || CaveBiomeIds.isSteamingJungleBiome(biome);
    }

    private static boolean mayPlace(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, Generator generator, int wx, int wz, Holder<Biome> biome) {
        boolean mega = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, wx, y, wz);
        return CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, biome, mega, carver.isEntranceColumn(lx, lz));
    }

    private static void placeFirst(Registry<PlacedFeature> registry, ResourceLocation[] ids, WorldGenLevel region, Generator generator, WorldgenRandom random, BlockPos placePos, long seed, int seedBase) {
        for (int i = 0; i < ids.length; ++i) {
            Holder<PlacedFeature> feature = CaveUndergroundJungleDecorator.resolve(registry, ids[i]);
            if (feature == null) continue;
            random.setFeatureSeed(seed, seedBase + i, 0);
            if (!FeaturePlacement.place(feature, region, (ChunkGenerator)generator, (Random)random, placePos, true)) continue;
            return;
        }
    }

    private static Holder<PlacedFeature> resolve(Registry<PlacedFeature> registry, ResourceLocation id) {
        return registry.getHolder(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, (ResourceLocation)id)).orElse(null);
    }

    static int findFloorAt(WorldGenLevel region, int wx, int wz, int minY, int maxY) {
        for (int y = maxY; y >= minY; --y) {
            BlockPos pos = new BlockPos(wx, y, wz);
            if (!region.getBlockState(pos).isAir() || y <= minY || region.getBlockState(pos.below()).isAir()) continue;
            return y;
        }
        return -1;
    }

    static boolean canPlaceWater(WorldGenLevel region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        return state.isAir() || state.canBeReplaced((Fluid)Fluids.WATER);
    }

    static void setWater(WorldGenLevel region, BlockPos pos) {
        if (CaveUndergroundJungleDecorator.canPlaceWater(region, pos)) {
            region.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        }
    }

    static void setFallingWater(WorldGenLevel region, BlockPos pos) {
        if (CaveUndergroundJungleDecorator.canPlaceWater(region, pos)) {
            region.setBlock(pos, (BlockState)Blocks.WATER.defaultBlockState().setValue((Property)LiquidBlock.LEVEL, (Comparable)Integer.valueOf(7)), 2);
        }
    }

    static boolean isJungleBiome(CarverChunk carver, ChunkAccess chunk, int lx, int y, int lz) {
        return CaveUndergroundJungleDecorator.isTarget(carver.resolveBiome(chunk, lx, y, lz));
    }

    static boolean isThermalBiome(CarverChunk carver, ChunkAccess chunk, int lx, int y, int lz) {
        Holder<Biome> biome = carver.resolveBiome(chunk, lx, y, lz);
        return CaveBiomeIds.isModThermalPresetBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome);
    }

    private static int findFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY);
    }

    private static int findCeiling(ChunkAccess chunk, int lx, int lz, int fromY, int maxY) {
        return CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, fromY, maxY);
    }
}
