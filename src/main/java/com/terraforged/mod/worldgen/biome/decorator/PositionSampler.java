package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDecorator;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDensity;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDensityBudget;
import com.terraforged.mod.worldgen.biome.decorator.FeaturePlacement;
import com.terraforged.mod.worldgen.biome.decorator.SamplerContext;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetation;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.mod.worldgen.cave.CavePlacementFilter;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class PositionSampler {
    protected static final float BORDER = 6.0f;
    public static final float SQUASH_FACTOR = 2.0f / NoiseUtil.sqrt(3.0f);
    private static final float MOD_BIOME_FREQ = 0.16f;
    private static final float MOD_BIOME_JITTER = 0.55f;
    private static final int COVER_GRID_STEP = 2;
    private static final float SURFACE_COVER_DENSITY = 0.68f;
    private static final float MOD_RIVER_CUTOFF = 0.04f;
    private static final float MIN_GRASS_VIABILITY = 0.03f;

    public static void placeVegetation(long seed, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, CompletableFuture<TerrainData> terrain, FeatureDecorator decorator) {
        FeatureDensityBudget budget = new FeatureDensityBudget(SURFACE_COVER_DENSITY);
        int offset = PositionSampler.placeTreesAndGrass(seed, chunk, level, terrain, generator, random, decorator, budget);
        PositionSampler.placeOther(seed, offset, origin, biome, level, generator, random, decorator, budget);
    }

    public static void placeVegetationWithoutTrees(long seed, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, CompletableFuture<TerrainData> terrain, FeatureDecorator decorator) {
        PositionSampler.placeOther(seed, 0, origin, biome, level, generator, random, decorator, new FeatureDensityBudget());
    }

    public static int placeTreesAndGrass(long seed, ChunkAccess chunk, WorldGenLevel level, CompletableFuture<TerrainData> terrain, Generator generator, WorldgenRandom random, FeatureDecorator decorator) {
        return PositionSampler.placeTreesAndGrass(seed, chunk, level, terrain, generator, random, decorator, new FeatureDensityBudget());
    }

    public static int placeTreesAndGrass(long seed, ChunkAccess chunk, WorldGenLevel level, CompletableFuture<TerrainData> terrain, Generator generator, WorldgenRandom random, FeatureDecorator decorator, FeatureDensityBudget budget) {
        SamplerContext context = SamplerContext.get();
        context.chunk = chunk;
        context.region = level;
        context.random = random;
        context.generator = generator;
        context.viabilityContext.terrainData = terrain;
        context.viabilityContext.biomeSampler = generator.getBiomeSource().getBiomeSampler();
        context.featureBudget = budget;
        PositionSampler.populate(context, decorator);
        int offset = 0;
        int x = chunk.getPos().getMinBlockX();
        int z = chunk.getPos().getMinBlockZ();
        HashSet<Holder<Biome>> processed = new HashSet<Holder<Biome>>();
        for (int i = 0; i < context.biomeList.size(); ++i) {
            Holder<Biome> biome = context.biomeList.get(i);
            if (!processed.add(biome) || biome.is(BiomeTags.IS_RIVER)) continue;
            BiomeVegetation vegetation = decorator.getVegetationManager().getVegetation(biome);
            VegetationConfig config = vegetation.config;
            boolean modBiome = biome.unwrapKey().map(key -> !"minecraft".equals(key.location().getNamespace())).orElse(true);
            context.push((Biome)biome.value(), vegetation, modBiome);
            if (config == VegetationConfig.NONE) {
                if (modBiome) {
                    offset = PositionSampler.sample(seed, offset + i, x, z, 0.16f, 0.55f, context, PositionSampler::placeModAt);
                    offset = PositionSampler.placeModGrassAt(seed, offset + i, x, z, context);
                    offset = PositionSampler.fillModSurfaceGaps(seed, offset + i, x, z, context);
                    continue;
                }
                offset = PositionSampler.placeAt(seed, offset + i, x, z, context);
                continue;
            }
            offset = PositionSampler.sample(seed, offset + i, x, z, config.frequency(), config.jitter(), context, PositionSampler::placeAt);
            offset = PositionSampler.placeGrassAt(seed, offset + i, x, z, context);
            if (modBiome) {
                offset = PositionSampler.fillModSurfaceGaps(seed, offset + i, x, z, context);
            }
        }
        return offset;
    }

    private static int fillModSurfaceGaps(long seed, int offset, int chunkMinX, int chunkMinZ, SamplerContext context) {
        WorldGenLevel region = context.region;
        Generator generator = context.generator;
        WorldgenRandom random = context.random;
        BlockPos.MutableBlockPos pos = context.pos;
        ChunkAccess chunk = context.chunk;
        FeatureDensityBudget budget = context.featureBudget;
        PlacedFeature[] grass = context.features.grass();
        if (grass.length == 0) {
            return offset;
        }
        for (int dz = 0; dz < 16; dz += COVER_GRID_STEP) {
            for (int dx = 0; dx < 16; dx += COVER_GRID_STEP) {
                if (context.terrainData().getRiver().get(dx, dz) < MOD_RIVER_CUTOFF) {
                    continue;
                }
                int x = chunkMinX + dx;
                int z = chunkMinZ + dz;
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (y <= generator.getSeaLevel()) {
                    continue;
                }
                pos.set(x, y, z);
                if (region.getBiome((BlockPos)pos).value() != context.biome) {
                    continue;
                }
                if (CavePlacementFilter.shouldSkipTree(generator, chunk, x, y, z)) {
                    continue;
                }
                for (PlacedFeature feature : grass) {
                    random.setFeatureSeed(seed, offset + dx * 3 + dz, VegetationFeatures.STAGE);
                    if (!FeatureDensity.tryPlace(feature, budget, dx, dz, region, generator, (Random)random, (BlockPos)pos, true)) {
                        continue;
                    }
                    ++offset;
                    break;
                }
            }
        }
        return offset;
    }

    public static void placeOther(long seed, int offset, BlockPos origin, Holder<Biome> biome, WorldGenLevel level, Generator generator, WorldgenRandom random, FeatureDecorator decorator) {
        PositionSampler.placeOther(seed, offset, origin, biome, level, generator, random, decorator, new FeatureDensityBudget());
    }

    public static void placeOther(long seed, int offset, BlockPos origin, Holder<Biome> biome, WorldGenLevel level, Generator generator, WorldgenRandom random, FeatureDecorator decorator, FeatureDensityBudget budget) {
        BiomeVegetation vegetation = decorator.getVegetationManager().getVegetation(biome);
        if (vegetation.features.other().length == 0) {
            return;
        }
        boolean modBiome = biome.unwrapKey().map(key -> !"minecraft".equals(key.location().getNamespace())).orElse(true);
        if (!modBiome) {
            for (PlacedFeature other : vegetation.features.other()) {
                random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
                if (!FeaturePlacement.place(other, level, (ChunkGenerator)generator, (Random)random, origin, false)) continue;
                ++offset;
            }
            return;
        }
        ChunkAccess chunk = level.getChunk(origin);
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        for (int dz = 0; dz < 16; dz += 4) {
            block2: for (int dx = 0; dx < 16; dx += 4) {
                BlockPos pos;
                int x = chunkMinX + dx;
                int z = chunkMinZ + dz;
                int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                if (y <= generator.getSeaLevel() || !level.getBiome(pos = new BlockPos(x, y, z)).equals(biome)) continue;
                for (PlacedFeature other : vegetation.features.other()) {
                    random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
                    if (!FeatureDensity.tryPlace(other, budget, dx, dz, level, generator, (Random)random, pos, true)) continue;
                    ++offset;
                    continue block2;
                }
            }
        }
    }

    public static void populate(SamplerContext context, FeatureDecorator decorator) {
        ChunkAccess chunk = context.chunk;
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                int x = startX + dx;
                int z = startZ + dz;
                int y = context.getHeight(dx, dz);
                Holder<Biome> biome = context.getBiome(x, y, z);
                BiomeVegetation vegetation = decorator.getVegetationManager().getVegetation(biome);
                Viability viability = vegetation.config.viability();
                float value = viability.getFitness(x, z, context.viabilityContext);
                context.viability.set(dx, dz, value);
                context.biomeList.add(biome);
            }
        }
    }

    public static <T> int sample(long seed, int offset, int x, int z, float freq, float jitter, T context, Sampler<T> sampler) {
        float freqX = freq;
        float freqZ = freq * SQUASH_FACTOR;
        int minX = NoiseUtil.floor(((float)x - 6.0f) * freqX);
        int minZ = NoiseUtil.floor(((float)z - 6.0f) * freqZ);
        int maxX = NoiseUtil.floor(((float)(x + 15) + 6.0f) * freqX);
        int maxZ = NoiseUtil.floor(((float)(z + 15) + 6.0f) * freqZ);
        return PositionSampler.sample(seed, offset, minX, minZ, maxX, maxZ, freqX, freqZ, jitter, context, sampler);
    }

    public static <T> int sample(long seed, int offset, int minX, int minZ, int maxX, int maxZ, float freqX, float freqZ, float jitter, T context, Sampler<T> sampler) {
        int cellSeed = (int)seed;
        for (int pz = minZ; pz <= maxZ; ++pz) {
            float ox = (float)(pz & 1) * 0.5f;
            for (int px = minX; px <= maxX; ++px) {
                int hash = MathUtil.hash(cellSeed, px, pz);
                float dx = MathUtil.randX(hash);
                float dz = MathUtil.randZ(hash);
                float sx = (float)px + ox + dx * jitter * 0.65f;
                float sz = (float)pz + dz * jitter;
                int posX = NoiseUtil.floor(sx / freqX);
                int posZ = NoiseUtil.floor(sz / freqZ);
                offset = sampler.sample(seed, offset, hash, posX, posZ, context);
            }
        }
        return offset;
    }

    private static int placeAt(long seed, int offset, int x, int z, SamplerContext context) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (chunkX != context.chunk.getPos().x || chunkZ != context.chunk.getPos().z) {
            return offset;
        }
        int y = context.chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (y <= context.generator.getSeaLevel()) {
            return offset;
        }
        context.pos.set(x, y, z);
        if (CavePlacementFilter.shouldSkipTree(context.generator, context.chunk, x, y, z)) {
            return offset;
        }
        for (PlacedFeature feature : context.features.trees()) {
            context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
            if (!FeatureDensity.tryPlace(feature, context.featureBudget, x & 0xF, z & 0xF, context.region, context.generator, (Random)context.random, (BlockPos)context.pos, context.modBiome)) continue;
            ++offset;
        }
        for (PlacedFeature feature : context.features.grass()) {
            context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
            if (!FeatureDensity.tryPlace(feature, context.featureBudget, x & 0xF, z & 0xF, context.region, context.generator, (Random)context.random, (BlockPos)context.pos, context.modBiome)) continue;
            ++offset;
        }
        return offset;
    }

    private static int placeAt(long seed, int offset, int hash, int x, int z, SamplerContext context) {
        float noise;
        if (!PositionSampler.isFeatureChunk(x, z, context)) {
            return offset;
        }
        int y = context.chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        if (y <= context.generator.getSeaLevel()) {
            return offset;
        }
        context.pos.set(x, y, z);
        Holder biome = context.region.getBiome((BlockPos)context.pos);
        if (biome.value() != context.biome) {
            return offset;
        }
        float viability = context.viability.get(x & 0xF, z & 0xF);
        if (viability < (noise = (1.0f - context.vegetation.density()) * MathUtil.rand(hash))) {
            return offset;
        }
        if (CavePlacementFilter.shouldSkipTree(context.generator, context.chunk, x, y, z)) {
            return offset;
        }
        for (PlacedFeature feature : context.features.trees()) {
            context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
            if (!FeatureDensity.tryPlace(feature, context.featureBudget, x & 0xF, z & 0xF, context.region, context.generator, (Random)context.random, (BlockPos)context.pos, context.modBiome)) continue;
            ++offset;
        }
        return offset;
    }

    private static int placeModAt(long seed, int offset, int hash, int x, int z, SamplerContext context) {
        PlacedFeature[] trees;
        if (!PositionSampler.isFeatureChunk(x, z, context)) {
            return offset;
        }
        int lx = x & 0xF;
        int lz = z & 0xF;
        if (context.terrainData().getRiver().get(lx, lz) < MOD_RIVER_CUTOFF) {
            return offset;
        }
        int y = context.chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
        if (y <= context.generator.getSeaLevel()) {
            return offset;
        }
        context.pos.set(x, y, z);
        Holder biome = context.region.getBiome((BlockPos)context.pos);
        if (biome.value() != context.biome) {
            return offset;
        }
        if (CavePlacementFilter.shouldSkipTree(context.generator, context.chunk, x, y, z)) {
            return offset;
        }
        for (PlacedFeature feature : trees = context.features.trees()) {
            context.random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
            if (!FeatureDensity.tryPlace(feature, context.featureBudget, lx, lz, context.region, context.generator, (Random)context.random, (BlockPos)context.pos, true)) continue;
            ++offset;
        }
        return offset;
    }

    private static int placeModGrassAt(long seed, int offset, int chunkMinX, int chunkMinZ, SamplerContext context) {
        WorldGenLevel region = context.region;
        Generator generator = context.generator;
        WorldgenRandom random = context.random;
        BlockPos.MutableBlockPos pos = context.pos;
        ChunkAccess chunk = context.chunk;
        FeatureDensityBudget budget = context.featureBudget;
        PlacedFeature[] grass = context.features.grass();
        if (grass.length == 0) {
            return offset;
        }
        for (int dz = 0; dz < 16; dz += COVER_GRID_STEP) {
            for (int dx = 0; dx < 16; dx += COVER_GRID_STEP) {
                if (context.terrainData().getRiver().get(dx, dz) < MOD_RIVER_CUTOFF) continue;
                int x = chunkMinX + dx;
                int z = chunkMinZ + dz;
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (y <= generator.getSeaLevel()) continue;
                pos.set(x, y, z);
                if (region.getBiome((BlockPos)pos).value() != context.biome) continue;
                for (PlacedFeature feature : grass) {
                    random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
                    if (!FeatureDensity.tryPlace(feature, budget, dx, dz, region, generator, (Random)random, (BlockPos)pos, true)) continue;
                    ++offset;
                }
            }
        }
        return offset;
    }

    private static int placeGrassAt(long seed, int offset, int chunkMinX, int chunkMinZ, SamplerContext context) {
        WorldGenLevel region = context.region;
        Generator generator = context.generator;
        WorldgenRandom random = context.random;
        BlockPos.MutableBlockPos pos = context.pos;
        ChunkAccess chunk = context.chunk;
        for (int dz = 0; dz < 16; dz += COVER_GRID_STEP) {
            for (int dx = 0; dx < 16; dx += COVER_GRID_STEP) {
                float viability = context.viability.get(dx, dz);
                if (viability < MIN_GRASS_VIABILITY) continue;
                int x = chunkMinX + dx;
                int z = chunkMinZ + dz;
                int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                if (y <= generator.getSeaLevel()) continue;
                pos.set(x, y, z);
                if (region.getBiome((BlockPos)pos).value() != context.biome) continue;
                for (PlacedFeature feature : context.features.grass()) {
                    random.setFeatureSeed(seed, offset, VegetationFeatures.STAGE);
                    if (!FeatureDensity.tryPlace(feature, context.featureBudget, dx, dz, region, generator, (Random)random, (BlockPos)pos, context.modBiome)) continue;
                    ++offset;
                }
            }
        }
        return offset;
    }

    private static boolean isFeatureChunk(int x, int z, SamplerContext context) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return chunkX == context.chunk.getPos().x && chunkZ == context.chunk.getPos().z;
    }

    public static interface Sampler<T> {
        public int sample(long var1, int var3, int var4, int var5, int var6, T var7);
    }
}
