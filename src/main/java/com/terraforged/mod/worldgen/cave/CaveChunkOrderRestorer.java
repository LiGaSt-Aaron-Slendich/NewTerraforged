package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.SurfaceDecorator;
import com.terraforged.mod.worldgen.biome.surface.Surface;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class CaveChunkOrderRestorer {
    private static final ResourceLocation CORRUPTED_BIOME_ID = new ResourceLocation("newtd", "corrupted_chunks");

    private CaveChunkOrderRestorer() {
    }

    public static boolean restore(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, StructureFeatureManager structures, FeatureDecorator featureDecorator, SurfaceDecorator surfaceDecorator, CompletableFuture<TerrainData> terrainFuture, CaveChunkCorruptionReport initialReport) {
        if (carver == null) {
            return !initialReport.corrupted();
        }
        CompletableFuture<TerrainData> terrainRef = terrainFuture;
        if (terrainRef == null) {
            TerrainData ready = generator.getChunkDataIfReady(chunk.getPos());
            if (ready != null) {
                terrainRef = CompletableFuture.completedFuture(ready);
            }
        }
        List<CutFeature> extracted = new ArrayList<>();
        CaveChunkOrderRestorer.phase1Surface(chunk, carver, generator, region, terrainRef, extracted, initialReport);
        CaveChunkOrderRestorer.phase2Underground(chunk, carver, generator, region, extracted);
        return CaveChunkOrderRestorer.phase3Verify(chunk, carver, generator, region, structures, featureDecorator, surfaceDecorator, terrainRef, initialReport);
    }

    /** Phase 1: strip every surface feature first, then repair terrain and cover — no decoration yet. */
    private static void phase1Surface(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, CompletableFuture<TerrainData> terrainFuture, List<CutFeature> extracted, CaveChunkCorruptionReport report) {
        CaveChunkOrderRestorer.stripAllSurfaceFeatures(chunk, carver, extracted);
        ChunkUtil.refreshHeightmaps(chunk);
        CaveChunkSurfaceRepair.repairNoiseSurface(chunk, carver, generator, region, false);
        CaveSurfaceBiomeRestorer.forceRestoreSurfaceColumns(chunk, generator, carver);
        TerrainData terrain = terrainFuture != null ? terrainFuture.getNow(generator.getChunkDataIfReady(chunk.getPos())) : generator.getChunkDataIfReady(chunk.getPos());
        if (terrain != null && region != null) {
            Surface.repairExposedCover(chunk, region, generator, terrain, carver);
            Surface.applyPost(chunk, terrain, generator);
        }
        CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
        ChunkUtil.refreshHeightmaps(chunk);
    }

    private static void stripAllSurfaceFeatures(ChunkAccess chunk, CarverChunk carver, List<CutFeature> extracted) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int shell = CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
                int yMin = CaveChunkSurfaceBounds.bandMinY(chunk, lx, lz);
                int yMax = CaveChunkSurfaceBounds.bandMaxY(chunk, lx, lz);
                for (int y = yMax; y >= yMin; --y) {
                    if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (y == shell && CaveChunkOrderRestorer.isNaturalGroundBlock(state)) {
                        continue;
                    }
                    CaveChunkOrderRestorer.extractAndClear(chunk, lx, y, lz, state, extracted);
                }
            }
        }
    }

    private static void extractAndClear(ChunkAccess chunk, int lx, int y, int lz, BlockState state, List<CutFeature> extracted) {
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted != null && (CaveBiomeIds.isUndergroundBiome(painted) || CaveBiomeIds.isModCaveBiome(painted))) {
            extracted.add(new CutFeature(lx, y, lz, state, painted));
        }
        chunk.setBlockState(new BlockPos(lx, y, lz), Blocks.AIR.defaultBlockState(), false);
    }

    private static boolean isNaturalGroundBlock(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.SNOW_BLOCK)) {
            return true;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.SNOW);
    }

    private static void phase2Underground(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, List<CutFeature> extracted) {
        if (extracted.isEmpty() || region == null) {
            return;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        IdentityHashMap<Holder<Biome>, BlockPos> replantAnchors = new IdentityHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (CutFeature cut : extracted) {
            if (cut.biome() == null) {
                continue;
            }
            int floorY = CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, cut.biome(), cut.lx(), cut.lz(), minY, maxY, generator, chunkX + cut.lx(), chunkZ + cut.lz());
            if (floorY < 0) {
                continue;
            }
            pos.set(cut.lx(), floorY + 1, cut.lz());
            if (!chunk.getBlockState(pos).isAir()) {
                continue;
            }
            chunk.setBlockState(pos, cut.state(), false);
            replantAnchors.putIfAbsent(cut.biome(), new BlockPos(chunkX + cut.lx(), floorY, chunkZ + cut.lz()));
        }
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        boolean megaGiga = carver.columnCache().anyMegaGiga();
        for (Map.Entry<Holder<Biome>, BlockPos> entry : replantAnchors.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            BlockPos anchor = entry.getValue();
            BlockPos prepared = CaveFloorCover.prepare(chunk, carver, biome, anchor.above());
            random.setDecorationSeed(region.getSeed(), prepared.getX(), prepared.getZ());
            CaveBiomeFeatureRunner.decorateBiomeCover(chunk, carver, region, generator, biome, prepared, random);
            if (megaGiga || CaveBiomeIds.isCoverDenseCaveBiome(biome)) {
                CaveBiomeFeatureRunner.decorateFloorAndCeiling(chunk, carver, region, generator, biome, prepared, random, CaveBiomeIds.isFungalCaveBiome(biome));
            } else {
                CaveBiomeFeatureRunner.decorateLightFloorAndCeiling(chunk, carver, region, generator, biome, prepared, random);
            }
        }
    }

    private static boolean phase3Verify(ChunkAccess chunk, CarverChunk carver, Generator generator, WorldGenLevel region, StructureFeatureManager structures, FeatureDecorator featureDecorator, SurfaceDecorator surfaceDecorator, CompletableFuture<TerrainData> terrainFuture, CaveChunkCorruptionReport initialReport) {
        CaveChunkCorruptionReport after = CaveChunkCorruptionChecker.verify(chunk, carver, generator);
        if (!after.corrupted()) {
            CaveChunkOrderRestorer.finishSurfaceDecoration(chunk, region, structures, featureDecorator, surfaceDecorator, terrainFuture, generator, carver);
            return true;
        }
        if (after.issues().contains(CaveChunkCorruptionReport.Issue.UNDERGROUND_FEATURES)) {
            List<CutFeature> retryExtracted = new ArrayList<>();
            CaveChunkOrderRestorer.phase1Surface(chunk, carver, generator, region, terrainFuture, retryExtracted, initialReport);
            CaveChunkOrderRestorer.phase2Underground(chunk, carver, generator, region, retryExtracted);
            ChunkUtil.refreshHeightmaps(chunk);
            after = CaveChunkCorruptionChecker.verify(chunk, carver, generator);
            if (!after.corrupted()) {
                CaveChunkOrderRestorer.finishSurfaceDecoration(chunk, region, structures, featureDecorator, surfaceDecorator, terrainFuture, generator, carver);
                return true;
            }
        }
        if (after.issues().contains(CaveChunkCorruptionReport.Issue.NOISE)) {
            CaveChunkSurfaceRepair.repairNoiseSurface(chunk, carver, generator, region, true);
            CaveChunkSurfaceRepair.stripSurfacePillars(chunk, carver);
            ChunkUtil.refreshHeightmaps(chunk);
            after = CaveChunkCorruptionChecker.verify(chunk, carver, generator);
            if (!after.corrupted()) {
                CaveChunkOrderRestorer.finishSurfaceDecoration(chunk, region, structures, featureDecorator, surfaceDecorator, terrainFuture, generator, carver);
                return true;
            }
        }
        boolean undergroundClean = !after.issues().contains(CaveChunkCorruptionReport.Issue.UNDERGROUND_FEATURES);
        if (undergroundClean) {
            CaveChunkOrderRestorer.finishSurfaceDecoration(chunk, region, structures, featureDecorator, surfaceDecorator, terrainFuture, generator, carver);
            return false;
        }
        CaveChunkOrderRestorer.markCorruptedChunkBiome(chunk, generator, carver);
        CaveChunkSurfaceRepair.paintCorruptedMarkerSurface(chunk, carver, generator);
        return false;
    }

    /** After successful verify: full surface pipeline + features — geometry untouched. */
    private static void finishSurfaceDecoration(ChunkAccess chunk, WorldGenLevel region, StructureFeatureManager structures, FeatureDecorator featureDecorator, SurfaceDecorator surfaceDecorator, CompletableFuture<TerrainData> terrainFuture, Generator generator, CarverChunk carver) {
        TerrainData terrain = terrainFuture != null ? terrainFuture.getNow(generator.getChunkDataIfReady(chunk.getPos())) : generator.getChunkDataIfReady(chunk.getPos());
        if (surfaceDecorator != null && region != null && terrain != null) {
            surfaceDecorator.refreshAfterIntegrity(chunk, region, generator, terrain, carver);
        }
        if (featureDecorator != null && region != null && structures != null && terrainFuture != null) {
            featureDecorator.decorate(chunk, region, structures, terrainFuture, generator);
        }
        ChunkUtil.refreshHeightmaps(chunk);
    }

    static void markCorruptedChunkBiome(ChunkAccess chunk, Generator generator, CarverChunk carver) {
        Holder<Biome> corrupted = CaveChunkOrderRestorer.resolveCorruptedBiome(generator);
        if (corrupted == null) {
            return;
        }
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int yStart = Math.max(chunk.getMinBuildHeight(), surface - 3);
                int yEnd = Math.min(chunk.getMaxBuildHeight() - 1, surface + 4);
                for (int y = yStart; y <= yEnd; ++y) {
                    CaveSurfaceBiomeRestorer.setBiomeQuart(chunk, lx, y, lz, corrupted);
                }
            }
        }
    }

    private static Holder<Biome> resolveCorruptedBiome(Generator generator) {
        ResourceKey<Biome> key = ResourceKey.create(net.minecraft.core.Registry.BIOME_REGISTRY, CORRUPTED_BIOME_ID);
        Holder<Biome> holder = generator.getBiomeSource().getRegistry().getHolder(key).orElse(null);
        if (holder != null) {
            return holder;
        }
        key = ResourceKey.create(net.minecraft.core.Registry.BIOME_REGISTRY, new ResourceLocation("newterraforged", "corrupted_chunks"));
        return generator.getBiomeSource().getRegistry().getHolder(key).orElse(null);
    }

    record CutFeature(int lx, int srcY, int lz, BlockState state, Holder<Biome> biome) {
    }
}
