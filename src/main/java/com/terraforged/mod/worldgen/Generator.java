package com.terraforged.mod.worldgen;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terraforged.mod.data.codec.WorldGenCodec;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.GeneratorResource;
import com.terraforged.mod.worldgen.IGenerator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.biome.BiomeGenerator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveDebugInfo;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveMassifCache;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.profiler.GenTimer;
import com.terraforged.mod.worldgen.profiler.ProfilerStages;
import com.terraforged.mod.worldgen.terrain.TerrainCache;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.mod.worldgen.util.ThreadPool;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class Generator
extends ChunkGenerator
implements IGenerator {
    public static final ProfilerStages PROFILER = new ProfilerStages();
    private static final int CHUNK_HEIGHT_PADDING = 16;
    public static final Codec<Generator> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.LONG.optionalFieldOf("seed", 0L).forGetter(g -> g.seed), TerrainLevels.CODEC.optionalFieldOf("levels", TerrainLevels.DEFAULT.get()).forGetter(g -> g.levels), WorldGenCodec.CODEC.forGetter(Generator::getRegistries)).apply(instance, instance.stable(GeneratorPreset::build)));
    protected final long seed;
    protected final Source biomeSource;
    protected final TerrainLevels levels;
    protected final VanillaGen vanillaGen;
    protected final BiomeGenerator biomeGenerator;
    protected final INoiseGenerator noiseGenerator;
    protected final TerrainCache terrainCache;
    protected final ThreadLocal<GeneratorResource> localResource = ThreadLocal.withInitial(GeneratorResource::new);
    private final ThreadLocal<ChunkOceanHeightCache> chunkOceanHeightCache = ThreadLocal.withInitial(ChunkOceanHeightCache::new);
    private final ThreadLocal<Integer> chunkHeightCacheDepth = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Boolean> chunkMegaGigaCacheActive = ThreadLocal.withInitial(() -> false);

    public Generator(long seed, TerrainLevels levels, VanillaGen vanillaGen, Source biomeSource, BiomeGenerator biomeGenerator, INoiseGenerator noiseGenerator) {
        super(vanillaGen.getStructureSets(), Optional.empty(), (BiomeSource)biomeSource, (BiomeSource)biomeSource, seed);
        this.seed = seed;
        this.levels = levels;
        this.vanillaGen = vanillaGen;
        this.biomeSource = biomeSource;
        this.biomeGenerator = biomeGenerator;
        this.noiseGenerator = noiseGenerator;
        this.terrainCache = new TerrainCache(levels, noiseGenerator, seed);
    }

    public long getSeed() {
        return this.seed;
    }

    public CaveEntranceClaims getCaveEntranceClaims() {
        return this.biomeGenerator.getCaveEntranceClaims();
    }

    public CarverChunk peekCaveCarver(ChunkPos pos) {
        return this.biomeGenerator.peekCaveCarver(pos);
    }

    protected RegistryAccess getRegistries() {
        return this.biomeSource.getRegistries();
    }

    public VanillaGen getVanillaGen() {
        return this.vanillaGen;
    }

    public INoiseGenerator getNoiseGenerator() {
        return this.noiseGenerator;
    }

    public NoiseSample getTerrainSample(int x, int z) {
        return this.terrainCache.getSample(x, z);
    }

    public TerrainData getChunkData(ChunkPos pos) {
        return this.terrainCache.getNow(pos);
    }

    @Nullable
    public TerrainData getChunkDataIfReady(ChunkPos pos) {
        return this.terrainCache.getIfReady(pos);
    }

    public CompletableFuture<TerrainData> getChunkDataAsync(ChunkPos pos) {
        return this.terrainCache.getAsync(pos);
    }

    public Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public int getMinY() {
        return this.levels.minY;
    }

    public int getSeaLevel() {
        return this.levels.seaLevel;
    }

    public void beginChunkHeightCache(ChunkAccess chunk) {
        this.beginChunkHeightCache(chunk, true);
    }

    public void beginChunkHeightCache(ChunkAccess chunk, boolean megaGigaCache) {
        int depth = this.chunkHeightCacheDepth.get();
        if (depth == 0) {
            this.chunkOceanHeightCache.get().begin(chunk, CHUNK_HEIGHT_PADDING);
            this.chunkMegaGigaCacheActive.set(megaGigaCache);
            if (megaGigaCache) {
                MegaCaveStructureFilter.beginChunkCache(this, chunk, CHUNK_HEIGHT_PADDING);
            }
        }
        this.chunkHeightCacheDepth.set(depth + 1);
    }

    public void endChunkHeightCache() {
        int depth = this.chunkHeightCacheDepth.get();
        if (depth <= 1) {
            this.chunkHeightCacheDepth.set(0);
            this.chunkOceanHeightCache.get().clear();
            if (this.chunkMegaGigaCacheActive.get()) {
                MegaCaveStructureFilter.endChunkCache();
            }
            this.chunkMegaGigaCacheActive.set(false);
            CaveMassifCache.clear();
            CaveOceanFilter.clearProximityCache();
            return;
        }
        this.chunkHeightCacheDepth.set(depth - 1);
    }

    public int getOceanFloorHeight(int x, int z) {
        ChunkOceanHeightCache cache = this.chunkOceanHeightCache.get();
        Integer cached = cache.get(x, z);
        if (cached != null) {
            return cached;
        }
        NoiseSample sample = this.terrainCache.getSample(x, z);
        float scaledHeight = this.levels.getScaledHeight(sample.heightNoise);
        int height = this.levels.getHeight(scaledHeight) + 1;
        cache.put(x, z, height);
        return height;
    }

    public int getGenDepth() {
        return this.levels.maxY;
    }

    public Source getBiomeSource() {
        return this.biomeSource;
    }

    public Generator withSeed(long seed) {
        INoiseGenerator noiseGenerator = this.noiseGenerator.with(seed, this.levels);
        Source biomeSource = new Source(seed, noiseGenerator, this.biomeSource);
        VanillaGen vanillaGen = new VanillaGen(seed, biomeSource, this.vanillaGen);
        BiomeGenerator biomeGenerator = new BiomeGenerator(seed, this.biomeGenerator);
        return new Generator(seed, this.levels, vanillaGen, biomeSource, biomeGenerator, noiseGenerator);
    }

    public void createStructures(RegistryAccess access, StructureFeatureManager structures, ChunkAccess chunk, StructureManager templates, long seed) {
        this.terrainCache.hint(chunk.getPos());
        GenTimer timer = PROFILER.starts.start();
        super.createStructures(access, structures, chunk, templates, seed);
        timer.punchOut();
    }

    public void createReferences(WorldGenLevel level, StructureFeatureManager structureFeatures, ChunkAccess chunk) {
        this.terrainCache.hint(chunk.getPos());
        GenTimer timer = PROFILER.refs.start();
        super.createReferences(level, structureFeatures, chunk);
        timer.punchOut();
    }

    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> registry, Executor executor, Blender blender, StructureFeatureManager structures, ChunkAccess chunk) {
        this.terrainCache.hint(chunk.getPos());
        return CompletableFuture.supplyAsync(() -> {
            GenTimer timer = PROFILER.biomes.start();
            ChunkUtil.fillNoiseBiomes(chunk, this.biomeSource, this.climateSampler(), this.localResource.get());
            timer.punchOut();
            return chunk;
        }, ThreadPool.EXECUTOR);
    }

    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureManager, ChunkAccess chunkAccess) {
        return this.terrainCache.combineAsync(executor, chunkAccess, (chunk, terrainData) -> {
            GenTimer timer = PROFILER.noise.start();
            ChunkUtil.fillChunk(this.getSeaLevel(), chunk, terrainData, ChunkUtil.FILLER, this.localResource.get());
            ChunkUtil.primeHeightmaps(this.getSeaLevel(), chunk, terrainData, ChunkUtil.FILLER);
            ChunkUtil.buildStructureTerrain(chunk, terrainData, structureManager);
            timer.punchOut();
            return chunk;
        });
    }

    public void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk) {
        this.beginChunkHeightCache(chunk, false);
        GenTimer timer = PROFILER.surface.start();
        this.biomeGenerator.surface(chunk, region, this);
        timer.punchOut();
        this.endChunkHeightCache();
    }

    public void applyCarvers(WorldGenRegion region, long seed, BiomeManager biomes, StructureFeatureManager structures, ChunkAccess chunk, GenerationStep.Carving stage) {
        GenTimer timer = PROFILER.carve.start();
        this.biomeGenerator.carve(seed, chunk, region, biomes, stage, this);
        timer.punchOut();
    }

    public void applyBiomeDecoration(WorldGenLevel region, ChunkAccess chunk, StructureFeatureManager structures) {
        this.beginChunkHeightCache(chunk);
        GenTimer timer = PROFILER.decoration.start();
        this.biomeGenerator.decorate(chunk, region, structures, this);
        timer.punchOut();
        this.endChunkHeightCache();
        PROFILER.incrementChunks();
        this.terrainCache.drop(chunk.getPos());
    }

    public void spawnOriginalMobs(WorldGenRegion region) {
        NoiseGeneratorSettings settings = (NoiseGeneratorSettings)this.vanillaGen.getSettings().value();
        if (settings.disableMobGeneration()) {
            return;
        }
        ChunkPos chunkPos = region.getCenter();
        BlockPos position = chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1);
        Holder holder = region.getBiome(position);
        WorldgenRandom random = new WorldgenRandom((RandomSource)new LegacyRandomSource(region.getSeed()));
        random.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration((ServerLevelAccessor)region, (Holder)holder, (ChunkPos)chunkPos, (Random)random);
    }

    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor) {
        NoiseSample sample = this.terrainCache.getSample(x, z);
        float scaledBase = this.levels.getScaledBaseLevel(sample.baseNoise);
        float scaledHeight = this.levels.getScaledHeight(sample.heightNoise);
        int base = this.levels.getHeight(scaledBase);
        int height = this.levels.getHeight(scaledHeight);
        return switch (types) {
            default -> throw new IncompatibleClassChangeError();
            case WORLD_SURFACE, WORLD_SURFACE_WG, MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES -> Math.max(base, height) + 1;
            case OCEAN_FLOOR, OCEAN_FLOOR_WG -> height + 1;
        };
    }

    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor) {
        NoiseSample sample = this.terrainCache.getSample(x, z);
        float scaledBase = this.levels.getScaledBaseLevel(sample.baseNoise);
        float scaledHeight = this.levels.getScaledHeight(sample.heightNoise);
        int base = this.levels.getHeight(scaledBase);
        int height = this.levels.getHeight(scaledHeight);
        int surface = Math.max(base, height);
        Object[] states = new BlockState[surface];
        Arrays.fill(states, 0, height, Blocks.STONE.defaultBlockState());
        if (surface > height) {
            Arrays.fill(states, height, surface, Blocks.WATER.defaultBlockState());
        }
        return new NoiseColumn(height, (BlockState[])states);
    }

    public void addDebugScreenInfo(List<String> lines, BlockPos pos) {
        ClimateSample sample = this.biomeSource.getBiomeSampler().getSample();
        this.terrainCache.sample(pos.getX(), pos.getZ(), sample);
        this.biomeSource.getBiomeSampler().sample(Seeds.get(this.seed), pos.getX(), pos.getZ(), sample);
        lines.add("");
        lines.add("[NewTerraForged]");
        lines.add("Terrain Type: " + sample.terrainType.getName());
        lines.add("Climate Type: " + sample.climateType.name());
        lines.add("Base Noise: " + sample.baseNoise);
        lines.add("Height Noise: " + sample.heightNoise);
        lines.add("Ocean Proximity: " + (1.0f - sample.continentNoise));
        lines.add("River Proximity: " + (1.0f - sample.riverNoise));
        PROFILER.addDebugInfo(5000L, lines);
        try {
            CaveDebugInfo.append(this, pos, lines);
        }
        catch (Throwable ignored) {
            lines.add("Cave debug unavailable");
        }
    }

    public static boolean isTerraForged(ChunkGenerator generator) {
        return generator instanceof Generator;
    }

    private static final class ChunkOceanHeightCache {
        private static final int UNSET = Integer.MIN_VALUE;
        private int minX = Integer.MIN_VALUE;
        private int minZ = Integer.MIN_VALUE;
        private int sizeX;
        private int sizeZ;
        private int[] heights = new int[0];

        private ChunkOceanHeightCache() {
        }

        void begin(ChunkAccess chunk, int padding) {
            this.minX = chunk.getPos().getMinBlockX() - padding;
            this.minZ = chunk.getPos().getMinBlockZ() - padding;
            this.sizeX = 16 + padding * 2;
            this.sizeZ = 16 + padding * 2;
            int needed = this.sizeX * this.sizeZ;
            if (this.heights.length < needed) {
                this.heights = new int[needed];
            }
            Arrays.fill(this.heights, 0, needed, UNSET);
        }

        void clear() {
            this.minX = Integer.MIN_VALUE;
        }

        Integer get(int x, int z) {
            if (this.minX == Integer.MIN_VALUE) {
                return null;
            }
            int lx = x - this.minX;
            int lz = z - this.minZ;
            if (lx < 0 || lz < 0 || lx >= this.sizeX || lz >= this.sizeZ) {
                return null;
            }
            int value = this.heights[lz * this.sizeX + lx];
            return value == UNSET ? null : value;
        }

        void put(int x, int z, int height) {
            if (this.minX == Integer.MIN_VALUE) {
                return;
            }
            int lx = x - this.minX;
            int lz = z - this.minZ;
            if (lx < 0 || lz < 0 || lx >= this.sizeX || lz >= this.sizeZ) {
                return;
            }
            this.heights[lz * this.sizeX + lx] = height;
        }
    }

    public Climate.Sampler climateSampler() {
        return Source.NOOP_CLIMATE_SAMPLER;
    }
}
