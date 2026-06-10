package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDensityBudget;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlan;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class NoiseCaveDecorator {
    private static final int GRID_STEP = 2;
    private static final int SPREAD = 3;
    private static final int CEILING_GRID_STEP = 2;

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, NoiseCave config, Set<Long> decoratedColumns, FeatureDensityBudget budget, CaveFeaturePlan.Cache planCache) {
        int maxY;
        int minY;
        int chunkZ;
        int seed = Seeds.get(region);
        int centerX = chunk.getPos().getMinBlockX() + 8;
        int centerZ = chunk.getPos().getMinBlockZ() + 8;
        if (config.getType().isMegaOrGiga() && CaveOceanFilter.isBlockedForMegaGiga(generator, config.getType(), centerX, centerZ)) {
            return;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        List<CaveColumn> columns = NoiseCaveDecorator.collectCaveColumns(chunk, carver, chunkX, chunkZ = chunk.getPos().getMinBlockZ(), minY = config.getMinY(), maxY = Math.min(config.getMaxY(), chunk.getHighestSectionPosition() + 15), seed, config);
        if (columns.isEmpty()) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        boolean megaGiga = config.getType().isMegaOrGiga();
        FeatureDensityBudget ceilingBudget = megaGiga ? FeatureDensityBudget.forCaveCeilingMegaGiga() : FeatureDensityBudget.forCaveCeiling();
        for (CaveColumn column : columns) {
            if (CaveBiomeIds.isModCaveBiome(column.biome()) || !CaveBiomeIds.isUndergroundBiome(column.biome())) continue;
            int lx = column.pos().getX() & 0xF;
            int lz = column.pos().getZ() & 0xF;
            if (!NoiseCaveDecorator.isValidCaveSite(chunk, carver, lx, column.pos().getY(), lz, column.kind(), megaGiga, column.biome()) || decoratedColumns != null && !decoratedColumns.add(column.pos().asLong())) continue;
            int ox = random.nextInt(7) - 3;
            int oz = random.nextInt(7) - 3;
            BlockPos pos = column.pos().offset(ox, 0, oz);
            int plx = pos.getX() & 0xF;
            int plz = pos.getZ() & 0xF;
            if (!chunk.getBlockState(NoiseCaveDecorator.worldToLocal(chunk, pos)).isAir() || !NoiseCaveDecorator.isValidCaveSite(chunk, carver, plx, pos.getY(), plz, column.kind(), megaGiga, column.biome())) continue;
            FeatureDensityBudget activeBudget = column.kind() == ColumnKind.CEILING ? ceilingBudget : budget;
            NoiseCaveDecorator.decorateAt(pos, plx, plz, column.kind(), chunk, carver, region, generator, planCache.get(column.biome()), column.biome(), random, activeBudget, megaGiga);
        }
    }

    private static BlockPos worldToLocal(ChunkAccess chunk, BlockPos world) {
        return new BlockPos(world.getX() - chunk.getPos().getMinBlockX(), world.getY(), world.getZ() - chunk.getPos().getMinBlockZ());
    }

    private static List<CaveColumn> collectCaveColumns(ChunkAccess chunk, CarverChunk carver, int chunkX, int chunkZ, int minY, int maxY, int seed, NoiseCave config) {
        int floorY;
        int wz;
        int wx;
        int lz;
        int lx;
        ArrayList<CaveColumn> columns = new ArrayList<CaveColumn>();
        int floorStep = 2;
        int ceilStep = config.getType().isMegaOrGiga() ? 2 : 2;
        for (lx = 0; lx < 16; lx += floorStep) {
            for (lz = 0; lz < 16; lz += floorStep) {
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = NoiseCaveDecorator.findCaveAirY(chunk, lx, lz, minY, maxY, seed, wx, wz, config);
                if (floorY < 0) continue;
                NoiseCaveDecorator.addColumn(columns, carver, chunk, wx, floorY, wz, ColumnKind.FLOOR);
            }
        }
        for (lx = 0; lx < 16; lx += ceilStep) {
            for (lz = 0; lz < 16; lz += ceilStep) {
                wx = chunkX + lx;
                wz = chunkZ + lz;
                floorY = NoiseCaveDecorator.findCaveAirY(chunk, lx, lz, minY, maxY, seed, wx, wz, config);
                if (floorY < 0) continue;
                int ceilY = NoiseCaveDecorator.findCeilingAir(chunk, lx, lz, floorY + 3, maxY);
                if (ceilY < 0) {
                    ceilY = NoiseCaveDecorator.findCeilingFromTop(chunk, lx, lz, floorY, maxY);
                }
                if (ceilY <= floorY) continue;
                NoiseCaveDecorator.addColumn(columns, carver, chunk, wx, ceilY, wz, ColumnKind.CEILING);
            }
        }
        return columns;
    }

    private static void addColumn(List<CaveColumn> columns, CarverChunk carver, ChunkAccess chunk, int wx, int y, int wz, ColumnKind kind) {
        int lx = wx & 0xF;
        int lz = wz & 0xF;
        Holder<Biome> biome = carver.resolveBiome(chunk, lx, y, lz);
        columns.add(new CaveColumn(new BlockPos(wx, y, wz), biome, kind));
    }

    private static boolean isValidCaveSite(ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, ColumnKind kind, boolean megaGiga, Holder<Biome> expected) {
        if (!CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, expected, megaGiga, carver.isEntranceColumn(lx, lz))) {
            return false;
        }
        BlockPos pos = new BlockPos(lx, y, lz);
        if (kind == ColumnKind.FLOOR) {
            return y > chunk.getMinBuildHeight() && !chunk.getBlockState(pos.below()).isAir();
        }
        if (kind == ColumnKind.CEILING) {
            return y < chunk.getMaxBuildHeight() - 1 && !chunk.getBlockState(pos.above()).isAir();
        }
        return false;
    }

    private static int findCaveAirY(ChunkAccess chunk, int lx, int lz, int minY, int maxY, int seed, int wx, int wz, NoiseCave config) {
        int targetY = config.getHeight(seed, wx, wz);
        int lo = Math.max(minY, targetY - 32);
        int hi = Math.min(maxY, targetY + 24);
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, lo, hi);
    }

    private static int findCeilingAir(ChunkAccess chunk, int lx, int lz, int fromY, int maxY) {
        for (int y = Math.min(maxY, fromY + 24); y >= fromY; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir() || y + 1 > maxY || chunk.getBlockState(new BlockPos(lx, y + 1, lz)).isAir()) continue;
            return y;
        }
        return -1;
    }

    private static int findCeilingFromTop(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        for (int y = Math.min(maxY - 1, floorY + 48); y > floorY + 2; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir() || chunk.getBlockState(new BlockPos(lx, y + 1, lz)).isAir()) continue;
            return y;
        }
        return -1;
    }

    private static void decorateAt(BlockPos airPos, int localX, int localZ, ColumnKind kind, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, CaveFeaturePlan plan, Holder<Biome> biome, WorldgenRandom random, FeatureDensityBudget budget, boolean megaGiga) {
        CaveFeatureRules.Anchor anchor = kind == ColumnKind.FLOOR ? CaveFeatureRules.Anchor.FLOOR : CaveFeatureRules.Anchor.CEILING;
        CaveFeaturePlan.StageFeature[] candidates = plan.forAnchor(anchor);
        if (candidates.length == 0) {
            return;
        }
        if (anchor == CaveFeatureRules.Anchor.FLOOR && !FeaturePlacement.hasStableGround((BlockGetter)chunk, localX, airPos.getY(), localZ, 1)) {
            return;
        }
        if (anchor == CaveFeatureRules.Anchor.CEILING && !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, localX, airPos.getY(), localZ, 1)) {
            return;
        }
        WorldGenLevel placement = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        long baseSeed = random.setDecorationSeed(region.getSeed(), airPos.getX(), airPos.getZ());
        boolean ceiling = anchor == CaveFeatureRules.Anchor.CEILING;
        int placedOnCeiling = 0;
        for (CaveFeaturePlan.StageFeature entry : candidates) {
            if (FeatureMassClassifier.spawnsSurfaceVegetation(entry.feature()) || !CaveFeaturePlacement.mayPlace(entry.feature(), anchor, airPos, chunk) || !budget.canPlace(entry.mass(), localX, localZ)) continue;
            BlockPos placePos = CaveFeaturePlacement.resolveWorldPos(airPos, anchor, entry.topLayer());
            random.setFeatureSeed(baseSeed, entry.featureIndex(), entry.stageIndex());
            if (!NoiseCaveDecorator.placeFeature(entry.feature(), placement, generator, random, placePos)) continue;
            budget.record(entry.mass(), localX, localZ);
            if (ceiling && megaGiga && ++placedOnCeiling >= 3) break;
        }
    }

    private static boolean placeFeature(Holder<PlacedFeature> placedHolder, WorldGenLevel region, Generator generator, WorldgenRandom random, BlockPos pos) {
        return FeaturePlacement.place(placedHolder, region, (ChunkGenerator)generator, (Random)random, pos, true);
    }

    private record CaveColumn(BlockPos pos, Holder<Biome> biome, ColumnKind kind) {
    }

    private static enum ColumnKind {
        FLOOR,
        CEILING;

    }
}
