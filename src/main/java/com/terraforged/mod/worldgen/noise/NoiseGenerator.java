package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.noise.continent.ContinentNoise;
import com.terraforged.mod.worldgen.noise.erosion.ErodedNoiseGenerator;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.mod.worldgen.terrain.TerrainBlender;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Consumer;

public class NoiseGenerator implements INoiseGenerator {
   protected final float heightMultiplier = 1.2F;
   protected final long seed;
   protected final TerrainLevels levels;
   protected final Module ocean;
   protected final TerrainBlender land;
   protected final IContinentNoise continent;
   protected final ControlPoints controlPoints;
   protected final ThreadLocal<NoiseData> localChunk = ThreadLocal.withInitial(NoiseData::new);
   protected final ThreadLocal<NoiseSample> localSample = ThreadLocal.withInitial(NoiseSample::new);

   public NoiseGenerator(long seed, TerrainLevels levels, TerrainNoise[] terrainNoises) {
      this.seed = seed;
      this.levels = levels;
      this.ocean = createOceanTerrain(seed);
      this.land = createLandTerrain(seed, terrainNoises);
      this.continent = createContinentNoise(seed, levels);
      this.controlPoints = this.continent.getControlPoints();
   }

   public NoiseGenerator(long seed, TerrainLevels levels, NoiseGenerator other) {
      this.seed = seed;
      this.levels = levels;
      this.land = other.land.withSeed(seed);
      this.ocean = createOceanTerrain(seed);
      this.continent = createContinentNoise(seed, levels);
      this.controlPoints = this.continent.getControlPoints();
   }

   public NoiseGenerator with(long seed, TerrainLevels levels) {
      return new NoiseGenerator(seed, levels, this);
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
   public float getHeightNoise(int x, int z) {
      return this.getNoiseSample(x, z).heightNoise;
   }

   @Override
   public long find(int x, int z, int minRadius, int maxRadius, Terrain terrain) {
      if (!terrain.isOverground()) {
         return 0L;
      } else {
         float f = this.getNoiseCoord(x);
         float f1 = this.getNoiseCoord(z);
         SpiralIterator.PositionFinder spiraliterator$positionfinder = this.land.findNearest(f, f1, minRadius, maxRadius, terrain);
         NoiseSample noisesample = this.localSample.get().reset();

         while (spiraliterator$positionfinder.hasNext()) {
            long i = spiraliterator$positionfinder.next();
            if (i != 0L) {
               float f2 = PosUtil.unpackLeftf(i) / this.levels.noiseLevels.frequency;
               float f3 = PosUtil.unpackRightf(i) / this.levels.noiseLevels.frequency;
               this.continent.sampleContinent(f2, f3, noisesample);
               if (!(noisesample.continentNoise < 0.5F)) {
                  this.continent.sampleRiver(f2, f3, noisesample);
                  if (terrain.isRiver() || !(noisesample.riverNoise < 0.75F)) {
                     int j = NoiseUtil.floor(f2);
                     int k = NoiseUtil.floor(f3);
                     return PosUtil.pack(j, k);
                  }
               }
            }
         }

         return 0L;
      }
   }

   @Override
   public void generate(int chunkX, int chunkZ, Consumer<NoiseData> consumer) {
      NoiseData noisedata = this.localChunk.get();
      TerrainBlender.Blender terrainblender$blender = this.land.getBlenderResource();
      NoiseSample noisesample = noisedata.sample;
      int i = chunkX << 4;
      int j = chunkZ << 4;

      for (int k = -1; k < 17; k++) {
         for (int l = -1; l < 17; l++) {
            int i1 = i + l;
            int j1 = j + k;
            this.sample(i1, j1, noisesample, terrainblender$blender);
            noisedata.setNoise(l, k, noisesample);
         }
      }

      consumer.accept(noisedata);
   }

   public INoiseGenerator withErosion() {
      return new ErodedNoiseGenerator(this.seed, getNoiseTileSize(), this);
   }

   public TerrainBlender.Blender getBlenderResource() {
      return this.land.getBlenderResource();
   }

   @Override
   public NoiseSample getNoiseSample(int x, int z) {
      NoiseSample noisesample = this.localSample.get().reset();
      this.sample(x, z, noisesample);
      return noisesample;
   }

   @Override
   public void sample(int x, int z, NoiseSample sample) {
      TerrainBlender.Blender terrainblender$blender = this.land.getBlenderResource();
      this.sample(x, z, sample, terrainblender$blender);
   }

   public void sampleContinentNoise(int x, int z, NoiseSample sample) {
      float f = this.getNoiseCoord(x);
      float f1 = this.getNoiseCoord(z);
      this.continent.sampleContinent(f, f1, sample);
   }

   public void sampleRiverNoise(int x, int z, NoiseSample sample) {
      float f = this.getNoiseCoord(x);
      float f1 = this.getNoiseCoord(z);
      this.continent.sampleRiver(f, f1, sample);
   }

   public NoiseSample sample(int x, int z, NoiseSample sample, TerrainBlender.Blender blender) {
      float f = this.getNoiseCoord(x);
      float f1 = this.getNoiseCoord(z);
      this.sampleTerrain(f, f1, sample, blender);
      this.sampleRiver(f, f1, sample);
      return sample;
   }

   public NoiseSample sampleTerrain(float nx, float nz, NoiseSample sample, TerrainBlender.Blender blender) {
      this.continent.sampleContinent(nx, nz, sample);
      float f = sample.continentNoise;
      if (f < 0.25F) {
         this.getOcean(nx, nz, sample, blender);
      } else if (f < 0.55F) {
         this.getBlend(nx, nz, sample, blender);
      } else {
         this.getInland(nx, nz, sample, blender);
      }

      return sample;
   }

   public NoiseSample sampleRiver(float nx, float nz, NoiseSample sample) {
      this.continent.sampleRiver(nx, nz, sample);
      return sample;
   }

   protected void getOcean(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      float f = this.ocean.getValue(x, z);
      sample.heightNoise = this.levels.noiseLevels.toDepthNoise(f);
      sample.terrainType = TerrainType.DEEP_OCEAN;
   }

   protected void getInland(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      float f = sample.baseNoise;
      float f1 = this.land.getValue(x, z, blender) * 1.2F;
      sample.heightNoise = this.levels.noiseLevels.toHeightNoise(f, f1);
      sample.terrainType = this.land.getTerrain(blender);
   }

   protected void getBlend(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      if (sample.continentNoise < 0.5F) {
         float f = this.ocean.getValue(x, z);
         float f1 = this.levels.noiseLevels.toDepthNoise(f);
         float f2 = this.levels.noiseLevels.heightMin;
         float f3 = (sample.continentNoise - 0.25F) / 0.25F;
         sample.heightNoise = NoiseUtil.lerp(f1, f2, f3);
      } else if (sample.continentNoise < 0.55F) {
         float f5 = this.levels.noiseLevels.heightMin;
         float f6 = sample.baseNoise;
         float f7 = this.land.getValue(x, z, blender) * 1.2F;
         float f8 = this.levels.noiseLevels.toHeightNoise(f6, f7);
         float f4 = (sample.continentNoise - 0.5F) / 0.050000012F;
         sample.heightNoise = NoiseUtil.lerp(f5, f8, f4);
         sample.terrainType = this.land.getTerrain(blender);
      }
   }

   protected Terrain getTerrain(float value, TerrainBlender.Blender blender) {
      return value < this.levels.noiseLevels.heightMin ? TerrainType.SHALLOW_OCEAN : this.land.getTerrain(blender);
   }

   protected static NoiseTileSize getNoiseTileSize() {
      return new NoiseTileSize(2);
   }

   protected static Module createOceanTerrain(long seed) {
      return Source.simplex((int)seed, 64, 3).scale(0.4);
   }

   protected static Module createBaseTerrain(long seed) {
      return Source.simplex((int)seed, 200, 2);
   }

   protected static TerrainBlender createLandTerrain(long seed, TerrainNoise[] terrainNoises) {
      return new TerrainBlender(seed, 800, 0.8F, 0.4F, terrainNoises);
   }

   protected static IContinentNoise createContinentNoise(long seed, TerrainLevels levels) {
      Settings settings = new Settings();
      settings.world.seed = seed;
      settings.world.properties.seaLevel = levels.seaLevel;
      settings.world.properties.worldHeight = levels.maxY;
      settings.climate.biomeShape.biomeSize = 220;
      settings.climate.temperature.falloff = 2;
      settings.climate.temperature.bias = 0.1F;
      settings.climate.moisture.falloff = 1;
      settings.climate.moisture.bias = -0.05F;
      GeneratorContext generatorcontext = new GeneratorContext(settings);
      settings.world.continent.continentScale = 400;
      settings.world.controlPoints.deepOcean = 0.05F;
      settings.world.controlPoints.shallowOcean = 0.3F;
      settings.world.controlPoints.beach = 0.45F;
      settings.world.controlPoints.coast = 0.75F;
      settings.world.controlPoints.inland = 0.8F;
      return new ContinentNoise(levels, generatorcontext);
   }
}
