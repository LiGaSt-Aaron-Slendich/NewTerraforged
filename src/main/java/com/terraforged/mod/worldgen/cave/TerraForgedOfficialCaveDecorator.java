/*
 * MIT License
 *
 * Copyright (c) 2021 TerraForged
 */
package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Unified official TF cave decorator: one backend per painted biome, multi-origin per chunk.
 * Floor pass matches original 0.3.x TF (full feature stages, no anchor splitting).
 * Ceiling pass adds stalactites/icicles at stable ceiling anchors only.
 */
public final class TerraForgedOfficialCaveDecorator {
    private static final int FIRST_STAGE = GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal();

    private TerraForgedOfficialCaveDecorator() {
    }

    public static void decorateVolume(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        Map<Holder<Biome>, BlockPos> painted = TerraForgedOfficialCaveDecorator.collectPaintedCaveBiomes(chunk, carver, generator);
        if (painted.isEmpty()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        boolean megaGiga = columns.anyMegaGiga();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        HashSet<Holder<Biome>> decorated = new HashSet<>();
        for (Map.Entry<Holder<Biome>, BlockPos> entry : painted.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            if (!decorated.add(biome)) {
                continue;
            }
            int grid = megaGiga ? 4 : 3;
            List<BlockPos> floorOrigins = TerraForgedOfficialCaveDecorator.collectFloorOrigins(chunk, carver, generator, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, grid);
            BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
            for (BlockPos origin : floorOrigins) {
                BlockPos pos = TerraForgedOfficialCaveDecorator.resolveFloorOrigin(chunk, origin);
                if (pos == null) {
                    continue;
                }
                random.setDecorationSeed(region.getSeed(), pos.getX(), pos.getZ());
                int lx = pos.getX() & 0xF;
                int lz = pos.getZ() & 0xF;
                int chamberSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, pos.getY(), lz, minY, maxY);
                TerraForgedOfficialCaveDecorator.decorateFloor(pos, guarded, generator, carver, settings, random, chunk, biome, chamberSpan);
            }
            List<BlockPos> ceilingOrigins = TerraForgedOfficialCaveDecorator.collectCeilingOrigins(chunk, carver, generator, biome, entry.getValue(), chunkX, chunkZ, minY, maxY, grid);
            for (BlockPos origin : ceilingOrigins) {
                random.setDecorationSeed(region.getSeed(), origin.getX(), origin.getZ());
                TerraForgedOfficialCaveDecorator.decorateCeiling(origin, guarded, generator, settings, random, chunk, biome);
            }
        }
        if (megaGiga) {
            CaveMegaAccentDecorator.decorate(chunk, carver, region, generator);
            if (carver.hasTunnelRiver()) {
                CaveTunnelRiverDecorator.decorate(chunk, carver, region, generator);
            }
        }
    }

    public static void decorateBiome(List<BlockPos> origins, ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome) {
        if (origins == null || origins.isEmpty()) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        int maxY = chunk.getHighestSectionPosition() + 15;
        for (BlockPos origin : origins) {
            BlockPos floor = TerraForgedOfficialCaveDecorator.resolveFloorOrigin(chunk, origin);
            if (floor != null) {
                random.setDecorationSeed(region.getSeed(), floor.getX(), floor.getZ());
                int lx = floor.getX() & 0xF;
                int lz = floor.getZ() & 0xF;
                int minY = chunk.getMinBuildHeight();
                int chamberSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, floor.getY(), lz, minY, maxY);
                TerraForgedOfficialCaveDecorator.decorateFloor(floor, guarded, generator, carver, settings, random, chunk, biome, chamberSpan);
            }
            int ceilY = TerraForgedOfficialCaveDecorator.resolveCeilingAir(chunk, origin.getX() & 0xF, origin.getZ() & 0xF, origin.getY(), maxY);
            if (ceilY >= 0) {
                BlockPos ceil = new BlockPos(origin.getX(), ceilY, origin.getZ());
                random.setDecorationSeed(region.getSeed(), ceil.getX(), ceil.getZ());
                TerraForgedOfficialCaveDecorator.decorateCeiling(ceil, guarded, generator, settings, random, chunk, biome);
            }
        }
    }

    public static void decorate(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, NoiseCave config) {
        BiomeList biomes = carver.getBiomes(config);
        if (biomes == null || biomes.size() == 0) {
            return;
        }
        int seed = Seeds.get(region);
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int startY = config.getHeight(seed, startX, startZ);
        BlockPos pos = TerraForgedOfficialCaveDecorator.resolveFloorOrigin(chunk, new BlockPos(startX, startY, startZ));
        if (pos == null) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        for (int i = 0; i < biomes.size(); ++i) {
            Holder<Biome> biome = biomes.get(i);
            int lx = pos.getX() & 0xF;
            int lz = pos.getZ() & 0xF;
            int minY = chunk.getMinBuildHeight();
            int maxY = chunk.getHighestSectionPosition() + 15;
            int chamberSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, pos.getY(), lz, minY, maxY);
            TerraForgedOfficialCaveDecorator.decorateFloor(pos, guarded, generator, carver, ((Biome)biome.value()).getGenerationSettings(), random, chunk, biome, chamberSpan);
        }
    }

    private static Map<Holder<Biome>, BlockPos> collectPaintedCaveBiomes(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        IdentityHashMap<Holder<Biome>, BlockPos> result = new IdentityHashMap<>(CaveBiomeVolumeDecorator.collectPaintedModBiomesPublic(chunk, carver, generator));
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        carver.forEachDecorateAnchor((biome, pos) -> {
            if (CaveBiomeIds.isUndergroundBiome((Holder<Biome>)biome) && !CaveBiomeIds.isBlockedCaveBiome((Holder<Biome>)biome)) {
                result.putIfAbsent((Holder<Biome>)biome, (BlockPos)pos);
            }
        });
        for (int lx = 0; lx < 16; lx += 2) {
            for (int lz = 0; lz < 16; lz += 2) {
                int floorY = TerraForgedOfficialCaveDecorator.findSimpleFloorAir(chunk, lx, lz, minY, maxY);
                if (floorY < 0) {
                    continue;
                }
                Holder<Biome> resolved = carver.resolveBiome(chunk, lx, floorY, lz);
                if (resolved == null || !CaveBiomeIds.isUndergroundBiome(resolved) || CaveBiomeIds.isBlockedCaveBiome(resolved)) {
                    continue;
                }
                result.putIfAbsent(resolved, new BlockPos(chunkX + lx, floorY, chunkZ + lz));
            }
        }
        return TerraForgedOfficialCaveDecorator.filterByPaintedVolume(chunk, result);
    }

    /** Drop patch-painted sliver biomes that only occupy a few quarts — they break feature passes. */
    private static Map<Holder<Biome>, BlockPos> filterByPaintedVolume(ChunkAccess chunk, Map<Holder<Biome>, BlockPos> painted) {
        IdentityHashMap<Holder<Biome>, Integer> quartCounts = new IdentityHashMap<>();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                for (int y = minY; y <= maxY; y += 4) {
                    Holder<Biome> biome = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
                    if (biome == null || !CaveBiomeIds.isModCaveBiome(biome)) {
                        continue;
                    }
                    quartCounts.merge(biome, 1, Integer::sum);
                }
            }
        }
        IdentityHashMap<Holder<Biome>, BlockPos> filtered = new IdentityHashMap<>();
        for (Map.Entry<Holder<Biome>, BlockPos> entry : painted.entrySet()) {
            int count = quartCounts.getOrDefault(entry.getKey(), 0);
            if (count >= 3) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered.isEmpty() ? painted : filtered;
    }

    private static List<BlockPos> collectFloorOrigins(ChunkAccess chunk, CarverChunk carver, Generator generator, Holder<Biome> target, BlockPos seed, int chunkX, int chunkZ, int minY, int maxY, int grid) {
        ArrayList<BlockPos> origins = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();
        seen.add(seed.asLong());
        origins.add(seed);
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                int floorY = CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, target, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0) {
                    floorY = TerraForgedOfficialCaveDecorator.findSimpleFloorAir(chunk, lx, lz, minY, maxY);
                }
                if (floorY < 0) {
                    continue;
                }
                Holder<Biome> resolved = carver.resolveBiome(chunk, lx, floorY, lz);
                if (!CaveBiomeIds.sharesCaveTheme(resolved, target)) {
                    continue;
                }
                BlockPos pos = new BlockPos(chunkX + lx, floorY, chunkZ + lz);
                if (seen.add(pos.asLong())) {
                    origins.add(pos);
                }
            }
        }
        return origins;
    }

    private static List<BlockPos> collectCeilingOrigins(ChunkAccess chunk, CarverChunk carver, Generator generator, Holder<Biome> target, BlockPos seed, int chunkX, int chunkZ, int minY, int maxY, int grid) {
        ArrayList<BlockPos> origins = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();
        int seedLx = seed.getX() & 0xF;
        int seedLz = seed.getZ() & 0xF;
        int seedCeil = TerraForgedOfficialCaveDecorator.resolveCeilingAir(chunk, seedLx, seedLz, seed.getY(), maxY);
        if (seedCeil >= 0) {
            BlockPos seedPos = new BlockPos(chunkX + seedLx, seedCeil, chunkZ + seedLz);
            seen.add(seedPos.asLong());
            origins.add(seedPos);
        }
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                int floorY = CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, target, lx, lz, minY, maxY, generator, chunkX + lx, chunkZ + lz);
                if (floorY < 0) {
                    floorY = TerraForgedOfficialCaveDecorator.findSimpleFloorAir(chunk, lx, lz, minY, maxY);
                }
                if (floorY < 0 || !CaveBiomeIds.sharesCaveTheme(carver.resolveBiome(chunk, lx, floorY, lz), target)) {
                    continue;
                }
                int ceilY = TerraForgedOfficialCaveDecorator.resolveCeilingAir(chunk, lx, lz, floorY, maxY);
                if (ceilY < 0) {
                    continue;
                }
                BlockPos pos = new BlockPos(chunkX + lx, ceilY, chunkZ + lz);
                if (seen.add(pos.asLong())) {
                    origins.add(pos);
                }
            }
        }
        return origins;
    }

    private static int findSimpleFloorAir(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int scanTop = Math.min(maxY, surface - 4);
        int scanBottom = Math.max(minY, surface - 96);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = scanTop; y >= scanBottom; --y) {
            pos.set(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir() || y <= minY || chunk.getBlockState(pos.setY(y - 1)).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    private static int resolveCeilingAir(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY + 4, maxY);
        if (ceilY < 0 || !FeaturePlacement.hasStableCeiling((BlockGetter)chunk, lx, ceilY, lz, 1)) {
            return -1;
        }
        return ceilY;
    }

    /** Accept floor anchors already found in open cave air — do not clamp them up to the surface band. */
    private static BlockPos resolveFloorOrigin(ChunkAccess chunk, BlockPos origin) {
        int lx = origin.getX() & 0xF;
        int lz = origin.getZ() & 0xF;
        int y = origin.getY();
        if (y < chunk.getMinBuildHeight() + 4) {
            return null;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y >= surface - 1) {
            return null;
        }
        BlockPos pos = new BlockPos(lx, y, lz);
        if (!chunk.getBlockState(pos).isAir()) {
            return null;
        }
        return origin;
    }

    /** Original TF 0.3.x floor decorate — all cave stages except surface hazards. */
    private static void decorateFloor(BlockPos pos, WorldGenLevel region, Generator generator, CarverChunk carver, BiomeGenerationSettings settings, WorldgenRandom random, ChunkAccess chunk, Holder<Biome> biome, int chamberSpan) {
        var features = settings.features();
        if (features.isEmpty()) {
            return;
        }
        pos = CaveFloorCover.prepare(chunk, carver, biome, pos);
        int lx = pos.getX() & 0xF;
        int lz = pos.getZ() & 0xF;
        boolean megaGigaSurface = CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, pos.getY(), lz, true);
        long baseSeed = random.setDecorationSeed(region.getSeed(), pos.getX(), pos.getZ());
        int lastStage = Math.min(features.size() - 1, GenerationStep.Decoration.FLUID_SPRINGS.ordinal());
        for (int stageIndex = FIRST_STAGE; stageIndex <= lastStage; ++stageIndex) {
            HolderSet<PlacedFeature> stage = features.get(stageIndex);
            if (stage == null || stage.size() == 0) {
                continue;
            }
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder<PlacedFeature> placed = stage.get(featureIndex);
                if (TerraForgedOfficialCaveDecorator.shouldSkipFeature(placed)) {
                    continue;
                }
                if (TerraForgedOfficialCaveDecorator.shouldSkipForeignFeature(placed, biome)) {
                    continue;
                }
                if (TerraForgedOfficialCaveDecorator.shouldSkipChamberFeature(placed, biome, chamberSpan, megaGigaSurface)) {
                    continue;
                }
                random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
                FeaturePlacement.place(placed, region, (ChunkGenerator)generator, (Random)random, pos, true);
            }
        }
    }

    /** Ceiling-only features at stable ceiling air — avoids stalactites in open sky but keeps floor decor dense. */
    private static void decorateCeiling(BlockPos airPos, WorldGenLevel region, Generator generator, BiomeGenerationSettings settings, WorldgenRandom random, ChunkAccess chunk, Holder<Biome> biome) {
        var features = settings.features();
        if (features.isEmpty()) {
            return;
        }
        int lx = airPos.getX() & 0xF;
        int lz = airPos.getZ() & 0xF;
        if (!FeaturePlacement.hasStableCeiling((BlockGetter)chunk, lx, airPos.getY(), lz, 1)) {
            return;
        }
        BlockPos placePos = airPos.above();
        long baseSeed = random.setDecorationSeed(region.getSeed(), placePos.getX(), placePos.getZ());
        int lastStage = Math.min(features.size() - 1, GenerationStep.Decoration.FLUID_SPRINGS.ordinal());
        for (int stageIndex = FIRST_STAGE; stageIndex <= lastStage; ++stageIndex) {
            HolderSet<PlacedFeature> stage = features.get(stageIndex);
            if (stage == null || stage.size() == 0) {
                continue;
            }
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder<PlacedFeature> placed = stage.get(featureIndex);
                if (TerraForgedOfficialCaveDecorator.shouldSkipFeature(placed)) {
                    continue;
                }
                if (TerraForgedOfficialCaveDecorator.shouldSkipForeignFeature(placed, biome)) {
                    continue;
                }
                if (!TerraForgedOfficialCaveDecorator.isCeilingFeature(placed)) {
                    continue;
                }
                random.setFeatureSeed(baseSeed, featureIndex, stageIndex);
                FeaturePlacement.place(placed, region, (ChunkGenerator)generator, (Random)random, placePos, true);
            }
        }
    }

    private static boolean isCeilingFeature(Holder<PlacedFeature> placed) {
        if (FeatureMassClassifier.isCeilingScatter(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed)) {
            return true;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("ceiling") || path.contains("hanging") || path.contains("icicle") || path.contains("stalactite");
    }

    private static boolean shouldSkipFeature(Holder<PlacedFeature> placed) {
        return placed.unwrapKey().map(key -> TerraForgedOfficialCaveDecorator.isBlockedFeaturePath(key.location().getPath())).orElse(false);
    }

    /** Verdict for cave debug — mirrors the active decor backend at the probe feet. */
    public static String decorFeatureVerdict(Holder<PlacedFeature> placed, Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust) {
        if (TerraForgedOfficialCaveDecorator.shouldSkipFeature(placed)) {
            return "blocked hazard/tree/ore (official skip list)";
        }
        if (TerraForgedOfficialCaveDecorator.shouldSkipForeignFeature(placed, biome)) {
            return "foreign/deferred feature (not for this cave biome)";
        }
        if (TerraForgedOfficialCaveDecorator.shouldSkipChamberFeature(placed, biome, chamberSpan, nearSurfaceCrust)) {
            return "skipped - shallow chamber or surface crust (official chamber guard)";
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id != null && TerraForgedOfficialCaveDecorator.isCeilingFeature(placed)) {
            return "ceiling-only - floor anchor will not place this";
        }
        return "allowed - official TF floor pass will attempt placement";
    }

    /** Drop injected globals and features that belong to another mod cave theme. */
    private static boolean shouldSkipForeignFeature(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isDeferredOrGlobalFeature(placed)) {
            return true;
        }
        if (CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return true;
        }
        if (!CaveBiomeIds.isModCaveBiome(biome) && !CaveBiomeIds.isUndergroundBiome(biome)) {
            return false;
        }
        return !CaveFeatureFilters.belongsToModCaveBiome(placed, biome);
    }

    private static boolean isBlockedFeaturePath(String path) {
        String lower = path.toLowerCase();
        return lower.contains("geode") || lower.contains("mega_geode") || lower.contains("crystal_geode")
                || lower.contains("monster_room") || lower.contains("fossil")
                || lower.startsWith("ore_") || lower.contains("/ore_")
                || lower.contains("lake_lava") || lower.contains("lake_water")
                || lower.contains("spring_lava") || lower.contains("spring_water")
                || FeatureMassClassifier.isTree(lower);
    }

    /** Skip tall scatter in shallow chambers; allow low cover/replacer when span >= 4. */
    private static boolean shouldSkipChamberFeature(Holder<PlacedFeature> placed, Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust) {
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        boolean cover = CaveFeatureFilters.isCoverFeaturePath(path) || path.contains("replacer") || path.contains("frostfire_patch");
        boolean tall = path.contains("fuck_art") || path.contains("tiles")
                || path.contains("/columns") || path.contains("/column/")
                || path.contains("yellowstone") && !cover;
        if (!cover && !tall) {
            return false;
        }
        if (nearSurfaceCrust) {
            return true;
        }
        if (tall && chamberSpan > 0 && chamberSpan < 8) {
            return true;
        }
        if (tall && chamberSpan > 0 && !CaveBiomeVerticalFit.fits(biome, chamberSpan)) {
            return true;
        }
        return false;
    }
}
