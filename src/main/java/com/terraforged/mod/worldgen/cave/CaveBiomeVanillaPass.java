package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * TerraLith-style cave decoration: {@code placeWithBiomeCheck} per feature per origin.
 * Multiple origins per chunk improve cover density; ceiling pass handles stalactites/crystal down.
 */
public final class CaveBiomeVanillaPass {
    private static final int[] DECORATION_STAGES = new int[]{
            GenerationStep.Decoration.RAW_GENERATION.ordinal(),
            GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal(),
            GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal(),
            GenerationStep.Decoration.VEGETAL_DECORATION.ordinal(),
            GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal()
    };

    private CaveBiomeVanillaPass() {
    }

    public static void decorateBiome(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos seedOrigin) {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        int grid = cfg != null ? cfg.vanillaOriginGrid : 2;
        int maxPerBiome = cfg != null ? cfg.vanillaOriginsPerBiome + 4 : 8;
        boolean ceilingPass = cfg == null || cfg.vanillaCeilingPass;
        IdentityHashMap<Holder<Biome>, List<BlockPos>> map = new IdentityHashMap<>();
        IdentityHashMap<Holder<Biome>, HashSet<Long>> seen = new IdentityHashMap<>();
        CaveBiomeVanillaPass.tryAddOrigin(map, seen, chunk, carver, biome, seedOrigin, maxPerBiome);
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                CaveBiomeVanillaPass.scanColumn(map, seen, chunk, carver, chunkX, chunkZ, lx, lz, minY, maxY, maxPerBiome, biome);
                if (grid > 2) {
                    CaveBiomeVanillaPass.scanColumn(map, seen, chunk, carver, chunkX, chunkZ, lx + grid / 2, lz + grid / 2, minY, maxY, maxPerBiome, biome);
                }
            }
        }
        List<BlockPos> origins = map.get(biome);
        if (origins == null || origins.isEmpty()) {
            return;
        }
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        for (BlockPos origin : origins) {
            random.setDecorationSeed(region.getSeed(), origin.getX(), origin.getZ());
            CaveBiomeVanillaPass.decorateBiomeAt(chunk, carver, region, generator, biome, origin, random);
            if (!ceilingPass) {
                continue;
            }
            int lx = origin.getX() & 0xF;
            int lz = origin.getZ() & 0xF;
            int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, origin.getY() + 4, maxY);
            if (ceilY > origin.getY() + 5) {
                BlockPos ceilOrigin = new BlockPos(origin.getX(), ceilY, origin.getZ());
                random.setDecorationSeed(region.getSeed(), ceilOrigin.getX(), ceilOrigin.getZ());
                CaveBiomeVanillaPass.decorateBiomeAt(chunk, carver, region, generator, biome, ceilOrigin, random);
            }
        }
    }

    public static void decorateChunk(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        Map<Holder<Biome>, List<BlockPos>> biomes = CaveBiomeVanillaPass.collectOrigins(chunk, carver);
        if (biomes.isEmpty()) {
            return;
        }
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        boolean ceilingPass = cfg == null || cfg.vanillaCeilingPass;
        int maxY = chunk.getHighestSectionPosition() + 15;
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        for (Map.Entry<Holder<Biome>, List<BlockPos>> entry : biomes.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            for (BlockPos origin : entry.getValue()) {
                random.setDecorationSeed(region.getSeed(), origin.getX(), origin.getZ());
                CaveBiomeVanillaPass.decorateBiomeAt(chunk, carver, region, generator, biome, origin, random);
                if (!ceilingPass) {
                    continue;
                }
                int lx = origin.getX() & 0xF;
                int lz = origin.getZ() & 0xF;
                int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, origin.getY() + 4, maxY);
                if (ceilY > origin.getY() + 5) {
                    BlockPos ceilOrigin = new BlockPos(origin.getX(), ceilY, origin.getZ());
                    random.setDecorationSeed(region.getSeed(), ceilOrigin.getX(), ceilOrigin.getZ());
                    CaveBiomeVanillaPass.decorateBiomeAt(chunk, carver, region, generator, biome, ceilOrigin, random);
                }
            }
        }
    }

    private static void decorateBiomeAt(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, Holder<Biome> biome, BlockPos origin, WorldgenRandom random) {
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithBiomeGuard(region, chunk, biome, carver);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        long seed = random.setDecorationSeed(region.getSeed(), origin.getX(), origin.getZ());
        for (int stageIndex : DECORATION_STAGES) {
            HolderSet<PlacedFeature> stage;
            if (stageIndex >= settings.features().size() || (stage = settings.features().get(stageIndex)) == null || stage.size() == 0) {
                continue;
            }
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                Holder<PlacedFeature> feature = stage.get(featureIndex);
                if (FeatureMassClassifier.isTree(feature) || FeatureMassClassifier.spawnsSurfaceVegetation(feature)) {
                    continue;
                }
                random.setFeatureSeed(seed, featureIndex, stageIndex);
                FeaturePlacement.place(feature, guarded, (ChunkGenerator)generator, (Random)random, origin, true);
            }
        }
    }

    private static Map<Holder<Biome>, List<BlockPos>> collectOrigins(ChunkAccess chunk, CarverChunk carver) {
        TFCaveBiomeConfig cfg = TFCaveBiomeConfig.INSTANCE;
        int grid = cfg != null ? cfg.vanillaOriginGrid : 4;
        int maxPerBiome = cfg != null ? cfg.vanillaOriginsPerBiome : 4;
        IdentityHashMap<Holder<Biome>, List<BlockPos>> result = new IdentityHashMap<>();
        IdentityHashMap<Holder<Biome>, HashSet<Long>> seen = new IdentityHashMap<>();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        carver.forEachDecorateAnchor((biome, pos) -> CaveBiomeVanillaPass.tryAddOrigin(result, seen, chunk, carver, biome, pos, maxPerBiome));
        for (int lx = 0; lx < 16; lx += grid) {
            for (int lz = 0; lz < 16; lz += grid) {
                CaveBiomeVanillaPass.scanColumn(result, seen, chunk, carver, chunkX, chunkZ, lx, lz, minY, maxY, maxPerBiome);
                if (grid > 2) {
                    CaveBiomeVanillaPass.scanColumn(result, seen, chunk, carver, chunkX, chunkZ, lx + grid / 2, lz + grid / 2, minY, maxY, maxPerBiome);
                }
            }
        }
        return result;
    }

    private static void scanColumn(Map<Holder<Biome>, List<BlockPos>> result, Map<Holder<Biome>, HashSet<Long>> seen, ChunkAccess chunk, CarverChunk carver, int chunkX, int chunkZ, int lx, int lz, int minY, int maxY, int maxPerBiome) {
        CaveBiomeVanillaPass.scanColumn(result, seen, chunk, carver, chunkX, chunkZ, lx, lz, minY, maxY, maxPerBiome, null);
    }

    private static void scanColumn(Map<Holder<Biome>, List<BlockPos>> result, Map<Holder<Biome>, HashSet<Long>> seen, ChunkAccess chunk, CarverChunk carver, int chunkX, int chunkZ, int lx, int lz, int minY, int maxY, int maxPerBiome, Holder<Biome> onlyBiome) {
        if (lx < 0 || lz < 0 || lx >= 16 || lz >= 16) {
            return;
        }
        int floorY = CaveBiomeVanillaPass.findCarvedFloor(chunk, lx, lz, minY, maxY);
        if (floorY < 0) {
            return;
        }
        Holder<Biome> biome = carver.resolveBiome(chunk, lx, floorY, lz);
        if (onlyBiome != null && !CaveBiomeIds.sharesCaveTheme(biome, onlyBiome)) {
            return;
        }
        int cap = CaveBiomeIds.isCoverDenseCaveBiome(biome) ? maxPerBiome + 4 : maxPerBiome;
        CaveBiomeVanillaPass.tryAddOrigin(result, seen, chunk, carver, biome, new BlockPos(chunkX + lx, floorY, chunkZ + lz), cap);
    }

    private static void tryAddOrigin(Map<Holder<Biome>, List<BlockPos>> result, Map<Holder<Biome>, HashSet<Long>> seen, ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, BlockPos pos, int maxPerBiome) {
        if (biome == null || CaveBiomeIds.isBlockedCaveBiome(biome) || CaveBiomeIds.isNetherThemedBiome(biome) || !CaveBiomeIds.isUndergroundBiome(biome)) {
            return;
        }
        int lx = pos.getX() & 0xF;
        int lz = pos.getZ() & 0xF;
        if (carver.isEntranceColumn(lx, lz)) {
            return;
        }
        List<BlockPos> list = result.computeIfAbsent(biome, k -> new ArrayList<>());
        HashSet<Long> biomeSeen = seen.computeIfAbsent(biome, k -> new HashSet<>());
        if (list.size() >= maxPerBiome || !biomeSeen.add(pos.asLong())) {
            return;
        }
        list.add(pos);
    }

    private static int findCarvedFloor(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        for (int y = maxY; y >= minY; --y) {
            BlockPos pos = new BlockPos(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir()) {
                continue;
            }
            if (chunk.getBlockState(pos.below()).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }
}
