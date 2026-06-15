package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.noise.IContinentNoise;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseData;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentNoise;
import com.terraforged.mod.worldgen.noise.erosion.ErodedNoiseGenerator;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.mod.worldgen.terrain.TerrainBlender;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Consumer;

public class NoiseGenerator
implements INoiseGenerator {
    protected static final int OCEAN_OFFSET = 8763214;
    protected static final int TERRAIN_OFFSET = 45763218;
    protected static final int CONTINENT_OFFSET = 18749560;
    protected final float heightMultiplier = 1.2f;
    protected final TerrainLevels levels;
    protected final Module ocean;
    protected final TerrainBlender land;
    protected final IContinentNoise continent;
    protected final ControlPoints controlPoints;
    protected final ThreadLocal<NoiseData> localChunk = ThreadLocal.withInitial(NoiseData::new);
    protected final ThreadLocal<NoiseSample> localSample = ThreadLocal.withInitial(NoiseSample::new);
    protected final TerrainNoise[] terrainNoises;

    public NoiseGenerator(long seed, TerrainLevels levels, TerrainNoise[] terrainNoises) {
        this.terrainNoises = terrainNoises;
        this.levels = levels;
        this.ocean = NoiseGenerator.createOceanTerrain();
        this.land = NoiseGenerator.createLandTerrain(terrainNoises).withSeed(seed);
        this.continent = NoiseGenerator.createContinentNoise(levels);
        this.controlPoints = this.continent.getControlPoints();
    }

    public NoiseGenerator(TerrainLevels levels, NoiseGenerator other) {
        this.terrainNoises = other.terrainNoises;
        this.levels = levels;
        this.land = other.land;
        this.ocean = other.ocean;
        this.continent = other.continent;
        this.controlPoints = this.continent.getControlPoints();
    }

    @Override
    public NoiseLevels getLevels() {
        return this.levels.noiseLevels;
    }

    @Override
    public TerrainLevels getTerrainLevels() {
        return this.levels;
    }

    @Override
    public IContinentNoise getContinent() {
        return this.continent;
    }

    @Override
    public float getHeightNoise(int seed, int x, int z) {
        return this.getNoiseSample((int)seed, (int)x, (int)z).heightNoise;
    }

    @Override
    public INoiseGenerator with(long seed, TerrainLevels levels) {
        return new NoiseGenerator(seed, levels, this.terrainNoises).withErosion();
    }

    @Override
    public long find(int seed, int x, int z, int minRadius, int maxRadius, Terrain terrain) {
        if (!terrain.isOverground()) {
            return 0L;
        }
        float nx = this.getNoiseCoord(x);
        float nz = this.getNoiseCoord(z);
        SpiralIterator.PositionFinder finder = this.land.findNearest(seed, nx, nz, minRadius, maxRadius, terrain);
        NoiseSample sample = this.localSample.get().reset();
        while (finder.hasNext()) {
            long pos = finder.next();
            if (pos == 0L) continue;
            float px = PosUtil.unpackLeftf(pos) / this.levels.noiseLevels.frequency;
            float pz = PosUtil.unpackRightf(pos) / this.levels.noiseLevels.frequency;
            this.continent.sampleContinent(seed, px, pz, sample);
            if (sample.continentNoise < 0.5f) continue;
            this.continent.sampleRiver(seed, px, pz, sample);
            if (!terrain.isRiver() && sample.riverNoise < 0.75f) continue;
            int xi = NoiseUtil.floor(px);
            int zi = NoiseUtil.floor(pz);
            return PosUtil.pack(xi, zi);
        }
        return 0L;
    }

    @Override
    public void generate(int seed, int chunkX, int chunkZ, Consumer<NoiseData> consumer) {
        NoiseData noiseData = this.localChunk.get();
        TerrainBlender.Blender blender = this.land.getBlenderResource();
        NoiseSample sample = noiseData.sample;
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        for (int dz = -1; dz < 17; ++dz) {
            for (int dx = -1; dx < 17; ++dx) {
                int x = startX + dx;
                int z = startZ + dz;
                this.sample(seed, x, z, sample, blender);
                noiseData.setNoise(dx, dz, sample);
            }
        }
        consumer.accept(noiseData);
    }

    public INoiseGenerator withErosion() {
        return new ErodedNoiseGenerator(NoiseGenerator.getNoiseTileSize(), this);
    }

    public TerrainBlender.Blender getBlenderResource() {
        return this.land.getBlenderResource();
    }

    @Override
    public NoiseSample getNoiseSample(int seed, int x, int z) {
        NoiseSample sample = this.localSample.get().reset();
        this.sample(seed, x, z, sample);
        return sample;
    }

    @Override
    public void sample(int seed, int x, int z, NoiseSample sample) {
        TerrainBlender.Blender blender = this.land.getBlenderResource();
        this.sample(seed, x, z, sample, blender);
    }

    public void sampleContinentNoise(int seed, int x, int z, NoiseSample sample) {
        float nx = this.getNoiseCoord(x);
        float nz = this.getNoiseCoord(z);
        this.continent.sampleContinent(seed, nx, nz, sample);
    }

    public void sampleRiverNoise(int seed, int x, int z, NoiseSample sample) {
        float nx = this.getNoiseCoord(x);
        float nz = this.getNoiseCoord(z);
        this.continent.sampleRiver(seed, nx, nz, sample);
    }

    public NoiseSample sample(int seed, int x, int z, NoiseSample sample, TerrainBlender.Blender blender) {
        float nx = this.getNoiseCoord(x);
        float nz = this.getNoiseCoord(z);
        this.sampleTerrain(seed, nx, nz, sample, blender);
        this.sampleRiver(seed, nx, nz, sample);
        return sample;
    }

    public NoiseSample sampleTerrain(int seed, float nx, float nz, NoiseSample sample, TerrainBlender.Blender blender) {
        this.continent.sampleContinent(seed, nx, nz, sample);
        float continentNoise = sample.continentNoise;
        if (continentNoise < 0.25f) {
            this.getOcean(seed, nx, nz, sample, blender);
        } else if (continentNoise < 0.55f) {
            this.getBlend(seed, nx, nz, sample, blender);
        } else {
            this.getInland(seed, nx, nz, sample, blender);
        }
        return sample;
    }

    public NoiseSample sampleRiver(int seed, float nx, float nz, NoiseSample sample) {
        this.continent.sampleRiver(seed, nx, nz, sample);
        return sample;
    }

    protected void getOcean(int seed, float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
        float rawNoise = this.ocean.getValue(x, z);
        sample.heightNoise = this.levels.noiseLevels.toDepthNoise(rawNoise);
        sample.terrainType = TerrainType.DEEP_OCEAN;
    }

    protected void getInland(int seed, float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
        float baseNoise = sample.baseNoise;
        float heightNoise = this.land.getValue(seed, x, z, blender) * 1.2f;
        sample.heightNoise = this.levels.noiseLevels.toHeightNoise(baseNoise, heightNoise);
        sample.terrainType = this.land.getTerrain(blender);
    }

    protected void getBlend(int seed, float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
        if (sample.continentNoise < 0.5f) {
            float lowerRaw = this.ocean.getValue(x, z);
            float lower = this.levels.noiseLevels.toDepthNoise(lowerRaw);
            float upper = this.levels.noiseLevels.heightMin;
            float alpha = (sample.continentNoise - 0.25f) / 0.25f;
            sample.heightNoise = NoiseUtil.lerp(lower, upper, alpha);
        } else if (sample.continentNoise < 0.55f) {
            float lower = this.levels.noiseLevels.heightMin;
            float baseNoise = sample.baseNoise;
            float upperRaw = this.land.getValue(seed, x, z, blender) * 1.2f;
            float upper = this.levels.noiseLevels.toHeightNoise(baseNoise, upperRaw);
            float alpha = (sample.continentNoise - 0.5f) / 0.050000012f;
            sample.heightNoise = NoiseUtil.lerp(lower, upper, alpha);
            sample.terrainType = this.land.getTerrain(blender);
        }
    }

    protected Terrain getTerrain(float value, TerrainBlender.Blender blender) {
        if (value < this.levels.noiseLevels.heightMin) {
            return TerrainType.SHALLOW_OCEAN;
        }
        return this.land.getTerrain(blender);
    }

    protected static NoiseTileSize getNoiseTileSize() {
        return new NoiseTileSize(1);
    }

    protected static Module createOceanTerrain() {
        return Source.simplex(8763214, 64, 3).scale(0.4);
    }

    protected static TerrainBlender createLandTerrain(TerrainNoise[] terrainNoises) {
        return new TerrainBlender(45763218L, 800, 0.8f, 0.4f, terrainNoises);
    }

    protected static IContinentNoise createContinentNoise(TerrainLevels levels) {
        Settings settings = new Settings();
        settings.world.seed = 18749560L;
        settings.world.properties.seaLevel = levels.seaLevel;
        settings.world.properties.worldHeight = levels.maxY;
        settings.climate.biomeShape.biomeSize = 220;
        settings.climate.temperature.falloff = 2;
        settings.climate.temperature.bias = 0.1f;
        settings.climate.moisture.falloff = 1;
        settings.climate.moisture.bias = -0.05f;
        GeneratorContext context = new GeneratorContext(settings);
        settings.world.continent.continentScale = 400;
        settings.world.controlPoints.deepOcean = 0.05f;
        settings.world.controlPoints.shallowOcean = 0.3f;
        settings.world.controlPoints.beach = 0.45f;
        settings.world.controlPoints.coast = 0.75f;
        settings.world.controlPoints.inland = 0.8f;
        return new ContinentNoise(levels, context);
    }
}
