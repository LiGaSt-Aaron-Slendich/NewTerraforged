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
import com.terraforged.mod.worldgen.noise.climate.ClimateNoise;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentNoise;
import com.terraforged.mod.worldgen.noise.continent.CoastalLiaOverlay;
import com.terraforged.mod.worldgen.noise.continent.ocean.IslandTerrainLabels;
import com.terraforged.mod.worldgen.noise.erosion.ErodedNoiseGenerator;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.MountainBeltBias;
import com.terraforged.mod.worldgen.terrain.MountainBeltField;
import com.terraforged.mod.worldgen.terrain.TerrainBlender;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Consumer;

public class NoiseGenerator implements INoiseGenerator {
   protected final float heightMultiplier = 1.55F;
   protected final long seed;
   protected final TerrainLevels levels;
   protected final Settings settings;
   protected final Module ocean;
   protected final TerrainBlender land;
   protected final IContinentNoise continent;
   protected final ClimateNoise climate;
   protected final ControlPoints controlPoints;
   protected final ThreadLocal<NoiseData> localChunk = ThreadLocal.withInitial(NoiseData::new);
   protected final ThreadLocal<NoiseSample> localSample = ThreadLocal.withInitial(NoiseSample::new);
   protected final ThreadLocal<ClimateSample> localClimate = ThreadLocal.withInitial(ClimateSample::new);

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
      this.climate = new ClimateNoise(this.continent.getContext());
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
      this.climate = new ClimateNoise(this.continent.getContext());
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
      if (terrain == null) {
         return 0L;
      }
      // Island / volcano / laguna are painted into continentNoise (warp+offset frame).
      if (isContinentPaintedSearchTerrain(terrain)) {
         return findContinentPaintedTerrain(x, z, minRadius, maxRadius, terrain);
      }
      if (!terrain.isOverground()) {
         return 0L;
      }
      float f = this.getNoiseCoord(x);
      float f1 = this.getNoiseCoord(z);
      SpiralIterator.PositionFinder spiraliterator$positionfinder = this.land.findNearest(f, f1, minRadius, maxRadius, terrain);
      if (spiraliterator$positionfinder == null) {
         return 0L;
      }
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

   /**
    * Spiral-sample continent layer for terrains painted there (laguna, volcano, stamped landforms).
    */
   private long findContinentPaintedTerrain(int x, int z, int minRadius, int maxRadius, Terrain terrain) {
      int step = 16;
      int minCell = Math.max(0, minRadius / step);
      // Cap search — uncapped spirals freeze structure/feature placement.
      int maxCell = Math.min(256, Math.max(minCell + 1, maxRadius / step));
      SpiralIterator spiral = new SpiralIterator(NoiseUtil.floor(x / (float) step), NoiseUtil.floor(z / (float) step), minCell, maxCell);
      NoiseSample sample = this.localSample.get().reset();
      String want = terrain.getName();
      int guard = 0;
      while (spiral.hasNext() && guard++ < 2048) {
         long packed = spiral.next();
         int cx = PosUtil.unpackLeft(packed);
         int cz = PosUtil.unpackRight(packed);
         int wx = cx * step + step / 2;
         int wz = cz * step + step / 2;
         float nx = this.getNoiseCoord(wx);
         float nz = this.getNoiseCoord(wz);
         this.continent.sampleContinent(nx, nz, sample);
         Terrain got = sample.terrainType;
         if (got != null && (got == terrain || want.equals(got.getName()))) {
            return PosUtil.pack(wx, wz);
         }
      }
      return 0L;
   }

   private static boolean isContinentPaintedSearchTerrain(Terrain terrain) {
      if (terrain == null) {
         return false;
      }
      if (terrain == ModTerrainTypes.LAGUNA
            || terrain == TerrainType.VOLCANO
            || terrain == TerrainType.VOLCANO_PIPE) {
         return true;
      }
      String n = terrain.getName();
      return "laguna".equals(n) || "volcano".equals(n) || "volcano_pipe".equals(n);
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
      Terrain painted = preservePaintedTerrain(sample.terrainType);
      float paintedH = sample.heightNoise;
      if (painted == TerrainType.VOLCANO_PIPE) {
         return;
      }
      float f = this.ocean.getValue(x, z);
      sample.heightNoise = this.levels.noiseLevels.toDepthNoise(f);
      sample.terrainType = TerrainType.DEEP_OCEAN;
      this.applyMountainBeltUnderwater(x, z, sample);
      // Ocean-landscape submerged ridges: lift bathymetry without flipping biomes.
      if (sample.oceanRelief > 0.0F) {
         float lift = NoiseUtil.clamp(sample.oceanRelief, 0.0F, 1.0F);
         float towardSea = NoiseUtil.lerp(sample.heightNoise, this.levels.noiseLevels.heightMin, lift * 0.85F);
         float peak = this.levels.noiseLevels.heightMin + lift * 0.04F;
         sample.heightNoise = Math.max(towardSea, NoiseUtil.lerp(sample.heightNoise, peak, lift));
      }
      restorePainted(sample, painted, paintedH);
   }

   protected void getInland(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      Terrain painted = preservePaintedTerrain(sample.terrainType);
      float paintedH = sample.heightNoise;
      if (painted == TerrainType.VOLCANO_PIPE) {
         return;
      }
      float belt = this.prepareLandClimateAndBelt(x, z, blender);
      float f = sample.baseNoise;
      float f1 = Math.min(1.0F, this.land.getValue(x, z, blender) * this.heightMultiplier);
      sample.heightNoise = this.levels.noiseLevels.toHeightNoise(f, f1);
      sample.terrainType = this.land.getTerrain(blender);
      this.applyMountainBeltLand(sample, belt);
      restorePainted(sample, painted, paintedH);
   }

   protected void getBlend(float x, float z, NoiseSample sample, TerrainBlender.Blender blender) {
      Terrain painted = preservePaintedTerrain(sample.terrainType);
      float paintedH = sample.heightNoise;
      if (painted == TerrainType.VOLCANO_PIPE) {
         return;
      }
      if (sample.continentNoise < 0.5F) {
         float f = this.ocean.getValue(x, z);
         float f1 = this.levels.noiseLevels.toDepthNoise(f);
         float f2 = this.levels.noiseLevels.heightMin;
         float f3 = (sample.continentNoise - 0.25F) / 0.25F;
         sample.heightNoise = NoiseUtil.lerp(f1, f2, f3);
         this.applyMountainBeltUnderwater(x, z, sample);
         if (sample.oceanRelief > 0.0F) {
            float lift = NoiseUtil.clamp(sample.oceanRelief, 0.0F, 1.0F);
            sample.heightNoise = NoiseUtil.lerp(sample.heightNoise,
                  Math.min(1.0F, f2 + lift * 0.06F), lift * 0.9F);
         }
      } else if (sample.continentNoise < 0.55F) {
         float belt = this.prepareLandClimateAndBelt(x, z, blender);
         float f5 = this.levels.noiseLevels.heightMin;
         float f6 = sample.baseNoise;
         float f7 = Math.min(1.0F, this.land.getValue(x, z, blender) * this.heightMultiplier);
         float f8 = this.levels.noiseLevels.toHeightNoise(f6, f7);
         float f4 = (sample.continentNoise - 0.5F) / 0.050000012F;
         sample.heightNoise = NoiseUtil.lerp(f5, f8, f4);
         sample.terrainType = this.land.getTerrain(blender);
         this.applyMountainBeltLand(sample, belt * f4);
      }
      // LIA mild cliff/carve after base blend height (before rivers/erosion tile).
      CoastalLiaOverlay lia = this.continent.getCoastalLia();
      if (lia != null) {
         float freq = this.levels.noiseLevels.frequency;
         float inv = freq > 1.0E-6F ? 1.0F / freq : 1.0F;
         lia.applyHeight(x * inv, z * inv, sample);
      }
      restorePainted(sample, painted, paintedH);
   }

   /** Sample climate + mountain-belt field before landform WeightMap. */
   protected float prepareLandClimateAndBelt(float x, float z, TerrainBlender.Blender blender) {
      ClimateSample climateSample = this.localClimate.get().reset();
      this.climate.sample(x, z, climateSample);
      blender.prepareClimate(climateSample.temperature, climateSample.moisture, this.land.getTerrains());
      float belt = this.sampleMountainBelt(x, z);
      blender.prepareMountainBelt(belt);
      return belt;
   }

   /** @deprecated use {@link #prepareLandClimateAndBelt} */
   protected void prepareLandClimate(float x, float z, TerrainBlender.Blender blender) {
      this.prepareLandClimateAndBelt(x, z, blender);
   }

   protected float sampleMountainBelt(float x, float z) {
      float freq = this.levels.noiseLevels.frequency;
      float inv = freq > 1.0E-6F ? 1.0F / freq : 1.0F;
      int continentScale = this.settings.world != null && this.settings.world.continent != null
            ? this.settings.world.continent.continentScale
            : 3000;
      return MountainBeltField.strength(x * inv, z * inv, this.seed, continentScale);
   }

   protected void applyMountainBeltLand(NoiseSample sample, float belt) {
      if (belt < 0.05F || sample == null) {
         return;
      }
      // Extra spine height on top of mountain landforms — readable in HEIGHT normals.
      float boost = belt * belt * 0.16F;
      sample.heightNoise = NoiseUtil.clamp(sample.heightNoise + boost * (1.0F - sample.heightNoise * 0.35F), 0.0F, 1.0F);
      if (belt > 0.72F && sample.terrainType != null && sample.terrainType.isOverground()
            && !sample.terrainType.isRiver() && !sample.terrainType.isLake()
            && !MountainBeltBias.isMountainLandform(sample.terrainType)) {
         sample.terrainType = TerrainType.MOUNTAINS;
      }
   }

   protected void applyMountainBeltUnderwater(float x, float z, NoiseSample sample) {
      float freq = this.levels.noiseLevels.frequency;
      float inv = freq > 1.0E-6F ? 1.0F / freq : 1.0F;
      int continentScale = this.settings.world != null && this.settings.world.continent != null
            ? this.settings.world.continent.continentScale
            : 3000;
      float under = MountainBeltField.underwaterStrength(x * inv, z * inv, this.seed, continentScale);
      if (under < 0.04F) {
         return;
      }
      // Fade out far from shore so mid-ocean doesn't get a global tectonic grid.
      float cn = NoiseUtil.clamp(sample.continentNoise, 0.0F, 1.0F);
      float nearShore = NoiseUtil.clamp(1.0F - cn / 0.45F, 0.0F, 1.0F);
      float relief = under * (0.35F + 0.65F * nearShore);
      sample.oceanRelief = Math.max(sample.oceanRelief, relief);
   }

   /**
    * Terrains stamped by the continent island contributor (not ContinentPoints ocean/coast/none).
    * Restored after WeightMap so island mountains stay mountains; height stays from blender.
    */
   private static Terrain preservePaintedTerrain(Terrain terrain) {
      if (terrain == null) {
         return null;
      }
      if (terrain == TerrainType.HILLS
            || terrain == TerrainType.PLATEAU
            || terrain == TerrainType.MOUNTAINS
            || terrain == TerrainType.FLATS
            || terrain == TerrainType.VOLCANO
            || terrain == TerrainType.VOLCANO_PIPE
            || terrain == TerrainType.RIVER
            || terrain == TerrainType.LAKE
            || terrain == ModTerrainTypes.LAGUNA
            || terrain == ModTerrainTypes.ISLAND_HILLS
            || terrain == ModTerrainTypes.ISLAND_PLATEAU
            || terrain == ModTerrainTypes.ISLAND_MOUNTAINS
            || terrain == ModTerrainTypes.ISLAND_FLATS
            || terrain == ModTerrainTypes.ISLAND_VOLCANO) {
         return terrain;
      }
      return null;
   }

   private static void restorePainted(NoiseSample sample, Terrain painted, float paintedHeight) {
      if (painted == null) {
         return;
      }
      // Keep explicit island_* labels after WeightMap so Find filters can see them.
      sample.terrainType = IslandTerrainLabels.finalizeIsland(painted);
      if (painted == ModTerrainTypes.LAGUNA || painted.isRiver() || painted.isLake()) {
         sample.heightNoise = Math.min(sample.heightNoise, paintedHeight);
      } else if (painted == TerrainType.VOLCANO_PIPE) {
         sample.heightNoise = paintedHeight;
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
      GeneratorContext generatorcontext = GeneratorContext.createNoCache(settings);
      return new ContinentNoise(levels, generatorcontext);
   }
}
