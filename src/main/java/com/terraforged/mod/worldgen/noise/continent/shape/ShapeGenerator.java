package com.terraforged.mod.worldgen.noise.continent.shape;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.ContinentPoints;
import com.terraforged.mod.worldgen.noise.continent.GuaranteedContinentMask;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.island.IslandScatter;
import com.terraforged.noise.util.NoiseUtil;

public class ShapeGenerator {
   private static final int RADIUS = 2;
   private final float baseFalloff;
   private final float continentFalloff;
   public final float threshold;
   public final float baseFalloffMin;
   public final float baseFalloffMax;
   private final ContinentGenerator continent;
   private final FalloffPoint[] falloffPoints;
   private final ThreadLocal<long[]> edgeBuffer = ThreadLocal.withInitial(() -> new long[9]);
   private final ThreadLocal<ShapeGenerator.CellLocal[]> cellBuffer = ThreadLocal.withInitial(ShapeGenerator.CellLocal::init);

   public ShapeGenerator(ContinentGenerator continent, ContinentConfig config, ControlPoints controlPoints) {
      this.continent = continent;
      this.baseFalloff = config.noise.baseNoiseFalloff;
      this.continentFalloff = config.noise.continentNoiseFalloff;
      this.falloffPoints = ContinentPoints.getFalloff(controlPoints);
      this.threshold = config.shape.threshold;
      this.baseFalloffMin = config.shape.threshold + config.shape.baseFalloffMin;
      this.baseFalloffMax = config.shape.threshold + config.shape.baseFalloffMax;
   }

   public float getThresholdValue(CellPoint cell) {
      return cell.noise < this.threshold ? 0.0F : 1.0F;
   }

   public float getFalloff(float continentNoise) {
      return ContinentPoints.getFalloff(continentNoise, this.falloffPoints);
   }

   public float getBaseNoise(float value) {
      float f = this.baseFalloffMin;
      float f1 = this.baseFalloffMax;
      return NoiseUtil.map(value, f, f1, f1 - f);
   }

   public float getValue(float x, float y) {
      long i = this.continent.getNearestCell(x, y);
      int j = PosUtil.unpackLeft(i);
      int k = PosUtil.unpackRight(i);
      x = this.continent.cellShape.adjustX(x);
      y = this.continent.cellShape.adjustY(y);
      int l = j - 1;
      int i1 = k - 1;
      int j1 = j + 1;
      int k1 = k + 1;
      float f = Float.MAX_VALUE;
      float f1 = Float.MAX_VALUE;
      long[] along = this.edgeBuffer.get();
      int l1 = i1;

      for (int i2 = 0; l1 <= k1; l1++) {
         for (int j2 = l; j2 <= j1; i2++) {
            CellPoint cellpoint = this.continent.getCell(j2, l1);
            float f2 = this.getThresholdValue(cellpoint);
            float f3 = this.distanceToCell(x, y, cellpoint, j2, l1);
            along[i2] = PosUtil.packf(f2, f3);
            if (f3 < f) {
               f1 = f;
               f = f3;
            } else if (f3 < f1) {
               f1 = f3;
            }

            j2++;
         }
      }

      return this.getFalloff(this.getEdge(f, f1, this.continentFalloff, along));
   }

   public NoiseSample sample(float x, float y, NoiseSample sample) {
      long i = this.continent.getNearestCell(x, y);
      int j = PosUtil.unpackLeft(i);
      int k = PosUtil.unpackRight(i);
      x = this.continent.cellShape.adjustX(x);
      y = this.continent.cellShape.adjustY(y);
      int l = j - 2;
      int i1 = k - 2;
      int j1 = j + 2;
      int k1 = k + 2;
      int l1 = -1;
      float f = Float.MAX_VALUE;
      float f1 = Float.MAX_VALUE;
      ShapeGenerator.CellLocal[] ashapegenerator$celllocal = this.cellBuffer.get();
      int i2 = i1;

      for (int j2 = 0; i2 <= k1; i2++) {
         for (int k2 = l; k2 <= j1; j2++) {
            CellPoint cellpoint = this.continent.getCell(k2, i2);
            ShapeGenerator.CellLocal shapegenerator$celllocal = ashapegenerator$celllocal[j2];
            float f2 = this.distanceToCell(x, y, cellpoint, k2, i2);
            shapegenerator$celllocal.cell = cellpoint;
            shapegenerator$celllocal.context = f2;
            if (f2 < f) {
               f1 = f;
               f = f2;
               l1 = j2;
            } else if (f2 < f1) {
               f1 = f2;
            }

            k2++;
         }
      }

      return this.sampleEdges(l1, f, f1, ashapegenerator$celllocal, sample);
   }

   private float getEdge(float min0, float min1, float falloff, long[] data) {
      float f = (min0 + min1) * 0.5F;
      float f1 = f * falloff;
      float f2 = 0.0F;
      float f3 = 0.0F;

      for (long i : data) {
         float f4 = PosUtil.unpackLeftf(i);
         float f5 = PosUtil.unpackRightf(i);
         float f6 = getWeight(f5, min0, f1);
         f2 += f4 * f6;
         f3 += f6;
      }

      return NoiseUtil.clamp(f2 / f3, 0.0F, 1.0F);
   }

   /**
    * Soft-cut Guaranteed-Continents freckles use Euclidean CN falloff → perfect circles on
    * JourneyMap height (thin beach cliff at CN≈0.5). Rag the distance for those cells only.
    */
   private float distanceToCell(float x, float y, CellPoint cell, int cellX, int cellY) {
      float dist = NoiseUtil.sqrt(NoiseUtil.dist2(x, y, cell.px, cell.py));
      GuaranteedContinentMask mask = this.continent.guaranteeMask;
      if (mask == null || !mask.active() || !mask.inWindow(cellX, cellY)) {
         return dist;
      }
      if (mask.isGuaranteedLand(cellX, cellY) || cell.noise <= 0.05F) {
         return dist;
      }
      float dx = x - cell.px;
      float dy = y - cell.py;
      float ang = (float) Math.atan2(dy, dx);
      float n = IslandScatter.valueNoise2(this.continent.seed ^ 0x51ED, x * 2.8F, y * 2.8F);
      float ragged = 1.0F
            + 0.28F * NoiseUtil.sin(ang * 3.0F + n * NoiseUtil.PI2)
            + 0.14F * (n - 0.5F);
      return dist * ragged;
   }

   private NoiseSample sampleEdges(int index, float min0, float min1, ShapeGenerator.CellLocal[] buffer, NoiseSample sample) {
      float f = (min0 + min1) * 0.5F;
      float f1 = f * this.baseFalloff;
      float f2 = f * this.continentFalloff;
      float f3 = 0.0F;
      float f4 = 0.0F;
      float f5 = 0.0F;
      float f6 = 0.0F;

      for (ShapeGenerator.CellLocal shapegenerator$celllocal : buffer) {
         float f7 = shapegenerator$celllocal.context;
         float f8 = shapegenerator$celllocal.cell.noise();
         float f9 = this.getThresholdValue(shapegenerator$celllocal.cell);
         float f10 = getWeight(f7, min0, f1);
         float f11 = getWeight(f7, min0, f2);
         f3 += f8 * f10;
         f4 += f9 * f11;
         f5 += f10;
         f6 += f11;
      }

      sample.baseNoise = this.getBaseNoise(f3 / f5);
      sample.continentNoise = this.getFalloff(f4 / f6);
      return sample;
   }

   private static float getWeight(float dist, float origin, float blendRange) {
      float f = dist - origin;
      if (f <= 0.0F) {
         return 1.0F;
      } else {
         return f >= blendRange ? 0.0F : 1.0F - f / blendRange;
      }
   }

   protected static class CellLocal {
      public CellPoint cell;
      public float context;

      protected static ShapeGenerator.CellLocal[] init() {
         int i = 5;
         ShapeGenerator.CellLocal[] ashapegenerator$celllocal = new ShapeGenerator.CellLocal[i * i];

         for (int j = 0; j < ashapegenerator$celllocal.length; j++) {
            ashapegenerator$celllocal[j] = new ShapeGenerator.CellLocal();
         }

         return ashapegenerator$celllocal;
      }
   }
}
