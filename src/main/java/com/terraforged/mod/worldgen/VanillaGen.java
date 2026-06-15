package com.terraforged.mod.worldgen;

import com.terraforged.mod.worldgen.util.NoiseChunkUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class VanillaGen {
    protected final Registry<StructureSet> structureSets;
    protected final NoiseBasedChunkGenerator vanillaGenerator;
    protected final NoiseRouter noiseRouter;
    protected final Holder<NoiseGeneratorSettings> settings;
    protected final Registry<NormalNoise.NoiseParameters> parameters;
    protected final int lavaLevel;
    protected final Aquifer.FluidStatus fluidStatus1;
    protected final Aquifer.FluidStatus fluidStatus2;
    protected final Aquifer.FluidPicker globalFluidPicker;
    protected final SurfaceSystem surfaceSystem;

    public VanillaGen(long seed, BiomeSource biomeSource, VanillaGen other) {
        this(seed, biomeSource, other.settings, other.parameters, other.structureSets);
    }

    public VanillaGen(long seed, BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, Registry<NormalNoise.NoiseParameters> parameters, Registry<StructureSet> structures) {
        this.settings = settings;
        this.parameters = parameters;
        this.structureSets = structures;
        NoiseGeneratorSettings settingsValue = (NoiseGeneratorSettings)settings.value();
        this.lavaLevel = Math.min(-54, settingsValue.seaLevel());
        this.fluidStatus1 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        this.fluidStatus2 = new Aquifer.FluidStatus(settingsValue.seaLevel(), settingsValue.defaultFluid());
        this.globalFluidPicker = (x, y, z) -> y < this.lavaLevel ? this.fluidStatus1 : this.fluidStatus2;
        BlockState defaultBlock = settingsValue.defaultBlock();
        this.surfaceSystem = new SurfaceSystem(parameters, defaultBlock, settingsValue.seaLevel(), seed, WorldgenRandom.Algorithm.XOROSHIRO);
        this.vanillaGenerator = new NoiseBasedChunkGenerator(structures, parameters, biomeSource, seed, settings);
        this.noiseRouter = NoiseChunkUtil.resolveRouter(this.vanillaGenerator);
    }

    public NoiseRouter getNoiseRouter() {
        return this.noiseRouter;
    }

    public NoiseBasedChunkGenerator getVanillaGenerator() {
        return this.vanillaGenerator;
    }

    public Holder<NoiseGeneratorSettings> getSettings() {
        return this.settings;
    }

    public Registry<StructureSet> getStructureSets() {
        return this.structureSets;
    }

    public Aquifer.FluidPicker getGlobalFluidPicker() {
        return this.globalFluidPicker;
    }

    public SurfaceSystem getSurfaceSystem() {
        return this.surfaceSystem;
    }

    public CarvingContext createCarvingContext(WorldGenRegion region, ChunkAccess chunk, NoiseChunk noiseChunk) {
        return new CarvingContext(this.vanillaGenerator, region.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk);
    }
}
