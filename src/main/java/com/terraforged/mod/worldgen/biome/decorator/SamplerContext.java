package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.util.storage.FloatMap;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDensityBudget;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.biome.vegetation.BiomeVegetation;
import com.terraforged.mod.worldgen.biome.vegetation.VegetationFeatures;
import com.terraforged.mod.worldgen.biome.viability.ViabilityContext;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class SamplerContext {
    private static final ThreadLocal<SamplerContext> LOCAL_CONTEXT = ThreadLocal.withInitial(SamplerContext::new);
    public ChunkAccess chunk;
    public WorldGenLevel region;
    public Generator generator;
    public WorldgenRandom random;
    public Biome biome;
    public boolean modBiome;
    public VegetationConfig vegetation;
    public VegetationFeatures features;
    public float maxViability = 0.0f;
    public final FloatMap viability = new FloatMap();
    public final BiomeList biomeList = new BiomeList();
    public final ViabilityContext viabilityContext = new ViabilityContext();
    public final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    public FeatureDensityBudget featureBudget;

    public int getHeight(int x, int z) {
        return this.chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
    }

    public Holder<Biome> getBiome(int x, int y, int z) {
        return this.region.getBiome((BlockPos)this.pos.set(x, y, z));
    }

    public TerrainData terrainData() {
        return this.viabilityContext.getTerrain();
    }

    public SamplerContext reset() {
        this.biomeList.reset();
        return this;
    }

    public void push(Biome biome, BiomeVegetation vegetation, boolean modBiome) {
        this.maxViability = 0.0f;
        this.biome = biome;
        this.modBiome = modBiome;
        this.vegetation = vegetation.config;
        this.features = vegetation.features;
    }

    public static SamplerContext get() {
        return LOCAL_CONTEXT.get().reset();
    }
}
