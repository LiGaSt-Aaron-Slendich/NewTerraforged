package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.compat.DynamicTreesCompat;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.decorator.PositionSampler;
import com.terraforged.mod.worldgen.biome.decorator.VanillaDecorator;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetationManager;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class FeatureDecorator {
    public static final GenerationStep.Decoration[] STAGES = GenerationStep.Decoration.values();
    private static final int VEGETATION_STAGE = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
    private static final int MAX_DECORATION_STAGE = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
    private final BiomeVegetationManager vegetation;
    private final Map<GenerationStep.Decoration, List<Holder<ConfiguredStructureFeature<?, ?>>>> structures;

    public FeatureDecorator(RegistryAccess access) {
        this.vegetation = new BiomeVegetationManager(access);
        this.structures = VanillaDecorator.buildStructureMap(access);
    }

    public BiomeVegetationManager getVegetationManager() {
        return this.vegetation;
    }

    public List<Holder<ConfiguredStructureFeature<?, ?>>> getStageStructures(int stage) {
        return this.structures.get(STAGES[stage]);
    }

    public HolderSet<PlacedFeature> getStageFeatures(int stage, Biome biome) {
        List stages = biome.getGenerationSettings().features();
        if (stage >= stages.size()) {
            return null;
        }
        return (HolderSet)stages.get(stage);
    }

    public void decorate(ChunkAccess chunk, WorldGenLevel level, StructureFeatureManager structures, CompletableFuture<TerrainData> terrain, Generator generator) {
        BlockPos origin = FeatureDecorator.getSurfaceOrigin(chunk);
        Holder<Biome> biome = FeatureDecorator.resolveSurfaceBiome(level, chunk, generator, origin);
        WorldgenRandom random = FeatureDecorator.getRandom(level.getSeed());
        long seed = random.setDecorationSeed(level.getSeed(), origin.getX(), origin.getZ());
        this.decoratePre(seed, origin, biome, chunk, level, generator, random, structures);
        this.decorateVegetation(seed, origin, biome, chunk, level, generator, random, terrain, structures);
        this.decoratePost(seed, origin, biome, chunk, level, generator, random, structures);
    }

    private void decoratePre(long seed, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, StructureFeatureManager structureManager) {
        VanillaDecorator.decorate(seed, 0, VEGETATION_STAGE - 1, origin, biome, chunk, level, generator, random, structureManager, this);
    }

    private void decoratePost(long seed, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, StructureFeatureManager structureManager) {
        VanillaDecorator.decorate(seed, VEGETATION_STAGE + 1, MAX_DECORATION_STAGE, origin, biome, chunk, level, generator, random, structureManager, this);
    }

    public void decorateVegetation(ChunkAccess chunk, WorldGenLevel level, StructureFeatureManager structures, CompletableFuture<TerrainData> terrain, Generator generator) {
        BlockPos origin = FeatureDecorator.getSurfaceOrigin(chunk);
        Holder<Biome> biome = FeatureDecorator.resolveSurfaceBiome(level, chunk, generator, origin);
        WorldgenRandom random = FeatureDecorator.getRandom(level.getSeed());
        long seed = random.setDecorationSeed(level.getSeed(), origin.getX(), origin.getZ());
        this.decorateVegetation(seed, origin, biome, chunk, level, generator, random, terrain, structures);
    }

    private void decorateVegetation(long seed, BlockPos origin, Holder<Biome> biome, ChunkAccess chunk, WorldGenLevel level, Generator generator, WorldgenRandom random, CompletableFuture<TerrainData> terrain, StructureFeatureManager structureManager) {
        if (DynamicTreesCompat.isLoaded()) {
            VanillaDecorator.decorate(seed, VEGETATION_STAGE, VEGETATION_STAGE, origin, biome, chunk, level, generator, random, structureManager, this);
            PositionSampler.placeVegetationWithoutTrees(seed, origin, biome, chunk, level, generator, random, terrain, this);
            return;
        }
        PositionSampler.placeVegetation(seed, origin, biome, chunk, level, generator, random, terrain, this);
    }

    private static BlockPos getSurfaceOrigin(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int lx = 8;
        int lz = 8;
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        int y = Math.max(surface, chunk.getMinBuildHeight() + 1);
        return new BlockPos(chunkPos.getMinBlockX() + lx, y, chunkPos.getMinBlockZ() + lz);
    }

    private static Holder<Biome> resolveSurfaceBiome(WorldGenLevel level, ChunkAccess chunk, Generator generator, BlockPos origin) {
        Holder biome = level.getBiome(origin);
        if (!CaveBiomeIds.isUndergroundBiome((Holder<Biome>)biome)) {
            return biome;
        }
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock((int)origin.getX()), QuartPos.fromBlock((int)origin.getY()), QuartPos.fromBlock((int)origin.getZ()), Source.NOOP_CLIMATE_SAMPLER);
    }

    private static WorldgenRandom getRandom(long seed) {
        return new WorldgenRandom((RandomSource)new LegacyRandomSource(seed));
    }
}
