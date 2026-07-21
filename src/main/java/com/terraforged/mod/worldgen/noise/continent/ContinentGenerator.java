package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.util.map.LongCache;
import com.terraforged.mod.util.map.LossyCache;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.cell.CellShape;
import com.terraforged.mod.worldgen.noise.continent.cell.CellSource;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.river.RiverGenerator;
import com.terraforged.mod.worldgen.noise.continent.shape.ShapeGenerator;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;

public class ContinentGenerator {
   public static final int CONTINENT_SAMPLE_SCALE = 400;
   protected static final int SAMPLE_SEED_OFFSET = 6569;
   protected static final int VALID_SPAWN_RADIUS = 3;
   protected static final int SPAWN_SEARCH_RADIUS = 100000;
   protected static final int CELL_POINT_CACHE_SIZE = 2048;
   public final int seed;
   public final float jitter;
   public final int sampleSeed;
   public final int noiseOctaves;
   public final float noiseGain;
   public final float noiseLacunarity;
   public final float sizeVariance;
   public final NoiseLevels levels;
   public final ControlPoints controlPoints;
   public final CellShape cellShape;
   public final CellSource cellSource;
   public final RiverGenerator riverGenerator;
   public final ShapeGenerator shapeGenerator;
   public final GuaranteedContinentMask guaranteeMask;
   private final ObjectPool<CellPoint> cellPool = ObjectPool.forCacheSize(2048, CellPoint::new);
   private final LongCache<CellPoint> cellCache = LossyCache.concurrent(2048, CellPoint[]::new, this.cellPool);

   public ContinentGenerator(ContinentConfig config, NoiseLevels levels, ControlPoints controlPoints) {
      this.levels = levels;
      this.controlPoints = controlPoints;
      this.seed = config.shape.seed0;
      this.sampleSeed = config.shape.seed1 + 6569;
      this.jitter = config.shape.jitter;
      this.noiseOctaves = Math.max(1, config.shape.noiseOctaves);
      this.noiseGain = config.shape.noiseGain;
      this.noiseLacunarity = config.shape.noiseLacunarity;
      this.sizeVariance = config.shape.sizeVariance;
      this.cellShape = config.shape.cellShape;
      this.cellSource = config.shape.cellSource;
      this.riverGenerator = new RiverGenerator(this, config);
      this.shapeGenerator = new ShapeGenerator(this, config, controlPoints);
      WorldSettings world = new WorldSettings();
      world.continent.guaranteedContinents = config.shape.guaranteedContinents;
      world.continent.guaranteedContinentsEnabled = config.shape.guaranteedContinentsEnabled;
      world.continent.continentsSpread = config.shape.continentsSpread;
      world.islands.coastalIslandsChance = config.shape.coastalIslandsChance;
      world.islands.volcanicIslandsChance = config.shape.volcanicIslandsChance;
      world.islands.scatteredArchipelago = config.shape.scatteredArchipelago;
      world.islands.scatteredArchipelagoChance = config.shape.scatteredArchipelagoChance;
      this.guaranteeMask = GuaranteedContinentMask.create(world, this.seed, Math.max(100, config.shape.scale));
   }

   public Vec2f getWorldOffset() {
      SpiralIterator spiraliterator = new SpiralIterator(0, 0, 0, 100000);
      CellPoint cellpoint = new CellPoint();

      while (spiraliterator.hasNext()) {
         long i = spiraliterator.next();
         this.computeCell(i, 0, 0, cellpoint);
         if (this.shapeGenerator.getThresholdValue(cellpoint) != 0.0F) {
            float f = cellpoint.px;
            float f1 = cellpoint.py;
            if (this.isValidSpawn(i, 3, cellpoint)) {
               return new Vec2f(f, f1);
            }
         }
      }

      return Vec2f.ZERO;
   }

   public CellPoint getCell(int cx, int cy) {
      long i = PosUtil.pack(cx, cy);
      return this.cellCache.computeIfAbsent(i, this::computeCell);
   }

   public long getNearestCell(float x, float y) {
      x = this.cellShape.adjustX(x);
      y = this.cellShape.adjustY(y);
      int i = NoiseUtil.floor(x) - 1;
      int j = NoiseUtil.floor(y) - 1;
      int k = i + 2;
      int l = j + 2;
      int i1 = 0;
      int j1 = 0;
      float f = Float.MAX_VALUE;
      int k1 = j;

      for (int l1 = 0; k1 <= l; k1++) {
         for (int i2 = i; i2 <= k; l1++) {
            CellPoint cellpoint = this.getCell(i2, k1);
            float f1 = NoiseUtil.dist2(x, y, cellpoint.px, cellpoint.py);
            if (f1 < f) {
               f = f1;
               i1 = i2;
               j1 = k1;
            }

            i2++;
         }
      }

      return PosUtil.pack(i1, j1);
   }

   private CellPoint computeCell(long index) {
      return this.computeCell(index, 0, 0, this.cellPool.take());
   }

   private CellPoint computeCell(long index, int ox, int oy, CellPoint cell) {
      int i = PosUtil.unpackLeft(index) + ox;
      int j = PosUtil.unpackRight(index) + oy;
      int k = MathUtil.hash(this.seed, i, j);
      float f = this.cellShape.getCellX(k, i, j, this.jitter);
      float f1 = this.cellShape.getCellY(k, i, j, this.jitter);
      cell.px = f;
      cell.py = f1;
      float f2 = 4000.0F;
      float variance = 1.0F + this.sizeVariance;
      float f3 = 400.0F / f2 * variance;
      sampleCell(this.sampleSeed, f, f1, this.cellSource, this.noiseOctaves, f3, this.noiseLacunarity, this.noiseGain, cell);
      // N±1 landmasses inside the 640k window; other cells soft-cut to island peaks / ocean.
      if (this.guaranteeMask != null && this.guaranteeMask.active() && this.guaranteeMask.inWindow(i, j)) {
         if (this.guaranteeMask.isGuaranteedLand(i, j)) {
            cell.noise = 1.0F;
         } else {
            float soft = this.guaranteeMask.softNoiseThreshold();
            if (cell.noise < soft) {
               cell.noise = 0.0F;
            } else {
               float t = (cell.noise - soft) / Math.max(1.0E-3F, 1.0F - soft);
               // Remap surviving peaks to island-scale land (not full continents).
               cell.noise = 0.32F + t * 0.48F;
            }
         }
         cell.noise0 = cell.noise;
      }
      return cell;
   }

   private static void sampleCell(int seed, float x, float y, CellSource cellSource, int octaves, float frequency, float lacunarity, float gain, CellPoint cell) {
      x *= frequency;
      y *= frequency;
      float f = cellSource.getValue(seed, x, y);
      float f1 = 1.0F;
      float f2 = f1;
      cell.noise0 = f;

      for (int i = 1; i < octaves; i++) {
         f1 *= gain;
         x *= lacunarity;
         y *= lacunarity;
         f += cellSource.getValue(seed, x, y) * f1;
         f2 += f1;
      }

      cell.noise = f / f2;
   }

   private boolean isValidSpawn(long pos, int radius, CellPoint cell) {
      int i = radius * radius;

      for (int j = -radius; j <= radius; j++) {
         for (int k = -radius; k <= radius; k++) {
            int l = k * k + j * j;
            if (j >= 1 && l < i) {
               this.computeCell(pos, k, j, cell);
               if (this.shapeGenerator.getThresholdValue(cell) == 0.0F) {
                  return false;
               }
            }
         }
      }

      return true;
   }
}
