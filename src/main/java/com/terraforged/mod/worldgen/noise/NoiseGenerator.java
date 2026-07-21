package com.terraforged.mod.worldgen.noise;

import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.noise.continent.ContinentNoise;
import com.terraforged.mod.worldgen.noise.erosion.ErodedNoiseGenerator;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
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
   protected final Settings settings;
   protected final Module ocean;
   protected final TerrainBlender land;
   protected final IContinentNoise continent;
   protected final ControlPoints controlPoints;
   protected final ThreadLocal<NoiseData> localChunk = ThreadLocal.withInitial(NoiseData::new);
   protected final ThreadLocal<NoiseSample> localSample = ThreadLocal.withInitial(NoiseSample::new);

   public NoiseGenerator(long seed, TerrainLevels levels, TerrainNoise[] terrainNoises) {
      this(seed, levels, terrainNoises, GeneratorSettings.DEFAULT.toEngine(seed, levels));
   }

   public NoiseGenerator(long seed, TerrainLevels levels, TerrainNoise[] terrainNoises, Settings settings) {
      this.seed = seed;
      this.levels = levels;
      this.settings = settings;
      settings.world.seed = seed;
      settings.world.properties.seaLevel = levels.seaLevel;
      settings.world.properties.worldHeight = levels.maxY;
      this.ocean = createOceanTerrain(seed);
      this.land = createLandTerrain(seed, terrainNoises, settings);
      this.continent = createContinentNoise(seed, levels, settings);
      this.controlPoints = this.continent.getControlPoints();
   }

   public NoiseGenerator(long seed, TerrainLevels levels, NoiseGenerator other) {
      this.seed = seed;
      this.levels = levels;
      this.settings = other.settings;
      this.settings.world.seed = seed;
      this.settings.world.properties.seaLevel = levels.seaLevel;
      this.settings.world.properties.worldHeight = levels.maxY;
      this.land = other.land.withSeed(seed);
      this.ocean = createOceanTerrain(seed);
      this.continent = createContinentNoise(seed, levels, this.settings);
      this.controlPoints = this.continent.getControlPoints();
   }

   public NoiseGenerator with(long seed, TerrainLevels levels) {
      return new NoiseGenerator(seed, levels, this);
   }

   public Settings getSettings() {
      return this.settings;
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
      return new ErodedNoiseGenerator(this.seed, getNoiseTileSize(), this, this.settings.filters.erosion.copy());
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
      Terrain island = islandTerrain(sample.terrainType);
      float islandH = sample.heightNoise;
      float f = this.ocean.getValue(x, z);
      sample.heightNoise = this.levels.noiseLevels.toDepthNoise(f);
      sample.terrainType = TerrainType.DEEP_OCEAN;
      restoreIsland(sample, island, islandH);
   }

   protected void getInland(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      Terrain island = islandTerrain(sample.terrainType);
      float islandH = sample.heightNoise;
      float f = sample.baseNoise;
      float f1 = this.land.getValue(x, z, blender) * 1.2F;
      sample.heightNoise = this.levels.noiseLevels.toHeightNoise(f, f1);
      sample.terrainType = this.land.getTerrain(blender);
      restoreIsland(sample, island, islandH);
   }

   protected void getBlend(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      Terrain island = islandTerrain(sample.terrainType);
      float islandH = sample.heightNoise;
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
      restoreIsland(sample, island, islandH);
   }

   private static Terrain islandTerrain(Terrain terrain) {
      if (terrain == ModTerrainTypes.VOLCANIC_ISLAND
            || terrain == ModTerrainTypes.COASTAL_ISLAND
            || terrain == ModTerrainTypes.SCATTERED_ARCHIPELAGO
            || terrain == ModTerrainTypes.LAGUNA) {
         return terrain;
      }
      return null;
   }

   private static void restoreIsland(NoiseSample sample, Terrain island, float islandHeight) {
      if (island == null) {
         return;
      }
      sample.terrainType = island;
      if (island == ModTerrainTypes.LAGUNA) {
         sample.heightNoise = Math.min(sample.heightNoise, islandHeight);
      } else {
         sample.heightNoise = Math.max(sample.heightNoise, islandHeight);
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
      return createLandTerrain(seed, terrainNoises, GeneratorSettings.DEFAULT.toEngine(seed, null));
   }

   protected static TerrainBlender createLandTerrain(long seed, TerrainNoise[] terrainNoises, Settings settings) {
      int regionSize = settings.terrain.general.terrainRegionSize;
      float horizontal = settings.terrain.general.globalHorizontalScale;
      int scale = Math.round(regionSize * horizontal);
      scale = Math.max(125, Math.min(5000, scale));
      return new TerrainBlender(seed, scale, 0.8F, 0.4F, terrainNoises);
   }

   protected static IContinentNoise createContinentNoise(long seed, TerrainLevels levels) {
      return createContinentNoise(seed, levels, GeneratorSettings.DEFAULT.toEngine(seed, levels));
   }

   protected static IContinentNoise createContinentNoise(long seed, TerrainLevels levels, Settings settings) {
      settings.world.seed = seed;
      settings.world.properties.seaLevel = levels.seaLevel;
      settings.world.properties.worldHeight = levels.maxY;
      GeneratorContext generatorcontext = new GeneratorContext(settings);
      return new ContinentNoise(levels, generatorcontext);
   }
}
