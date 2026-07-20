package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.ObjectPool;
import com.terraforged.mod.util.map.LongCache;
import com.terraforged.mod.util.map.LossyCache;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2i;

public class RiverGenerator {
   public static final Vec2i[] DIRS = new Vec2i[]{new Vec2i(1, 0), new Vec2i(0, 1), new Vec2i(-1, 0), new Vec2i(0, -1)};
   private static final int X_OFFSET = 8657124;
   private static final int Y_OFFSET = 5123678;
   private static final int DIR_OFFSET = 20107;
   private static final int SIZE_A_OFFSET = 9803;
   private static final int SIZE_B_OFFSET = 28387;
   private static final int LAKE_CHANCE_OFFSET = 37171;
   private static final int RIVER_CACHE_SIZE = 1024;
   private final int seed;
   private final float lakeDensity;
   private final float riverDensity;
   private final ContinentGenerator continent;
   private final RiverCarver riverCarver;
   private final Domain riverWarp;
   private final ThreadLocal<CarverSample> localRiverSample = ThreadLocal.withInitial(CarverSample::new);
   private final ObjectPool<RiverPieces> pool = ObjectPool.forCacheSize(1024, RiverPieces::new);
   private final LongCache<RiverPieces> cache = LossyCache.concurrent(1024, RiverPieces[]::new, this.pool);

   public RiverGenerator(ContinentGenerator continent, ContinentConfig config) {
      this.continent = continent;
      this.seed = config.rivers.seed;
      this.lakeDensity = config.rivers.lakeDensity;
      this.riverDensity = config.rivers.riverDensity;
      this.riverCarver = new RiverCarver(continent.levels, config);
      this.riverWarp = Domain.warp(
         Source.builder().seed(this.seed + 8657124).frequency(30.0).simplex(),
         Source.builder().seed(this.seed + 5123678).frequency(30.0).simplex(),
         Source.constant(0.004)
      );
   }

   public void sample(float x, float y, NoiseSample sample) {
      float f = this.riverWarp.getX(x, y);
      float f1 = this.riverWarp.getY(x, y);
      CarverSample carversample = this.localRiverSample.get().reset();
      this.sample(f, f1, carversample);
      this.riverCarver.carve(f, f1, sample, carversample);
   }

   private void sample(float x, float y, CarverSample sample) {
      long i = this.continent.getNearestCell(x, y);
      int j = PosUtil.unpackLeft(i);
      int k = PosUtil.unpackRight(i);
      x = this.continent.cellShape.adjustX(x);
      y = this.continent.cellShape.adjustY(y);
      int l = j - 1;
      int i1 = k - 1;
      int j1 = j + 1;
      int k1 = k + 1;
      RiverNode rivernode = null;
      RiverNode rivernode1 = null;

      for (int l1 = i1; l1 <= k1; l1++) {
         for (int i2 = l; i2 <= j1; i2++) {
            RiverPieces riverpieces = this.getNodes(i2, l1);

            for (int j2 = 0; j2 < riverpieces.riverCount(); j2++) {
               RiverNode rivernode2 = riverpieces.river(j2);
               rivernode = this.sampleNode(x, y, rivernode2, rivernode, sample.river);
            }

            for (int k2 = 0; k2 < riverpieces.lakeCount(); k2++) {
               RiverNode rivernode3 = riverpieces.lake(k2);
               rivernode1 = this.sampleNode(x, y, rivernode3, rivernode1, sample.lake);
            }
         }
      }

      this.recordNode(rivernode, sample.river);
      this.recordNode(rivernode1, sample.lake);
   }

   private RiverNode sampleNode(float x, float y, RiverNode node, RiverNode nearest, NodeSample sample) {
      float f = node.getProjection(x, y);
      float f1 = node.getDistance2(x, y, f);
      if (f1 < sample.distance) {
         nearest = node;
         sample.distance = f1;
         sample.projection = f;
      }

      return nearest;
   }

   private void recordNode(RiverNode node, NodeSample sample) {
      if (node != null) {
         float f = node.getHeight(sample.projection);
         float f1 = node.getRadius(sample.projection);
         sample.distance = NoiseUtil.sqrt(sample.distance);
         sample.position = f1;
         sample.level = this.continent.shapeGenerator.getBaseNoise(f);
      } else {
         sample.invalidate();
      }
   }

   private RiverPieces getNodes(int x, int y) {
      long i = PosUtil.pack(x, y);
      return this.cache.computeIfAbsent(i, this::computeNodes);
   }

   private RiverPieces computeNodes(long index) {
      int i = PosUtil.unpackLeft(index);
      int j = PosUtil.unpackRight(index);
      CellPoint cellpoint = this.continent.getCell(i, j);
      if (this.continent.shapeGenerator.getThresholdValue(cellpoint) <= 0.0F) {
         return RiverPieces.NONE;
      } else {
         CellPoint cellpoint1 = cellpoint;
         float f = this.getBaseValue(cellpoint);
         float f1 = this.getHeight(cellpoint.noise(), 0.0F, 1.0F);
         float f2 = this.getRadius(cellpoint.noise(), 0.0F, 1.0F);
         boolean flag = true;
         RiverPieces riverpieces = this.pool.take();

         for (Vec2i vec2i : DIRS) {
            int k = i + vec2i.x;
            int l = j + vec2i.y;
            CellPoint cellpoint2 = this.continent.getCell(k, l);
            float f3 = this.getBaseValue(cellpoint2);
            if (f3 <= f) {
               cellpoint1 = cellpoint2;
               f = f3;
            } else if (!(f3 <= 0.0F) && this.connects(i, j, k, l, f3)) {
               float f4 = this.getHeight(cellpoint2.noise(), 0.0F, 1.0F);
               float f5 = this.getRadius(cellpoint2.noise(), 0.0F, 1.0F);
               int i1 = MathUtil.hash(this.seed + 827614, k, l);
               this.addRiverNodes(cellpoint, cellpoint2, f1, f4, f2, f5, i1, riverpieces);
               flag = false;
            }
         }

         if (cellpoint1 == cellpoint) {
            return riverpieces;
         } else if (flag && riverpieces.riverCount() == 0 && f <= 0.0F) {
            this.pool.restore(riverpieces);
            return RiverPieces.NONE;
         } else {
            float f6 = this.getHeight(cellpoint1.noise(), 0.0F, 1.0F);
            float f7 = this.getRadius(cellpoint1.noise(), 0.0F, 1.0F);
            int j1 = MathUtil.hash(this.seed + 827614, i, j);
            this.addRiverNodes(cellpoint, cellpoint1, f1, f6, f2, f7, j1, riverpieces);
            if (flag && this.hasLake(cellpoint, j1)) {
               this.addLakeNodes(cellpoint, cellpoint1, f1, j1, riverpieces);
            }

            return riverpieces;
         }
      }
   }

   private void addRiverNodes(CellPoint a, CellPoint b, float ah, float bh, float ar, float br, int hash, RiverPieces pieces) {
      if (this.riverDensity <= 0.0F) {
         return;
      }
      if (this.riverDensity < 1.0F && MathUtil.rand(this.seed + 99173, hash) > this.riverDensity) {
         return;
      }
      float f = (a.px + b.px) * 0.5F;
      float f1 = (a.py + b.py) * 0.5F;
      float f2 = (ar + br) * 0.5F;
      float f3 = (ah + bh) * 0.5F;
      float f4 = (a.px + f) * 0.5F;
      float f5 = (a.py + f1) * 0.5F;
      float f6 = (ar + f2) * 0.5F;
      float f7 = (ah + f3) * 0.5F;
      float f8 = -(f5 - a.py);
      float f9 = f4 - a.px;
      float f10 = MathUtil.rand(this.seed + 20107, hash) < 0.5F ? -1.0F : 1.0F;
      float f11 = 0.7F + MathUtil.rand(this.seed + 9803, hash) * 0.3F;
      float f12 = 0.7F + MathUtil.rand(this.seed + 28387, hash) * 0.3F;
      float f13 = 0.35F * f10 * f11;
      f4 += f8 * f13;
      f5 += f9 * f13;
      float f14 = 0.275F * -f10 * f12;
      float f15 = f14 * NoiseUtil.map(a.noise, 0.4F, 0.6F, 0.2F);
      float f16 = -f14 * NoiseUtil.map(b.noise, 0.4F, 0.6F, 0.2F);
      pieces.addRiver(new RiverNode(a.px, a.py, f4, f5, ah, f7, ar, f6, f15));
      pieces.addRiver(new RiverNode(f4, f5, f, f1, f7, f3, f6, f2, f16));
      if (b.noise < this.continent.shapeGenerator.threshold) {
         pieces.addRiver(new RiverNode(f, f1, b.px, b.py, f3, bh, f2, br, f15));
      }
   }

   private void addLakeNodes(CellPoint a, CellPoint b, float ah, int hash, RiverPieces pieces) {
      float f = (0.5F + MathUtil.rand(this.seed + 9803, hash) * 0.5F) * 0.12F;
      float f1 = a.px - b.px;
      float f2 = a.py - b.py;
      float f3 = a.px + f1 * f;
      float f4 = a.py + f2 * f;
      pieces.addLake(new RiverNode(a.px, a.py, f3, f4, ah, ah, 1.0F, 1.0F, 0.0F));
   }

   private boolean connects(int ax, int ay, int bx, int by, float minValue) {
      int i = bx;
      int j = by;

      for (Vec2i vec2i : DIRS) {
         int k = bx + vec2i.x;
         int l = by + vec2i.y;
         CellPoint cellpoint = this.continent.getCell(k, l);
         float f = this.getBaseValue(cellpoint);
         if (f < minValue) {
            j = k;
            i = l;
            minValue = f;
         }
      }

      return j == ax && i == ay;
   }

   private boolean hasLake(CellPoint cell, int hash) {
      return MathUtil.rand(hash + 37171) <= this.lakeDensity || this.continent.shapeGenerator.getBaseNoise(cell.noise()) < 0.25F;
   }

   private float getBaseValue(CellPoint point) {
      return this.continent.shapeGenerator.getThresholdValue(point) <= 0.0F ? 0.0F : point.noise();
   }

   private float getHeight(float noise, float min, float max) {
      return noise;
   }

   private float getRadius(float noise, float min, float max) {
      float f = 0.5F;
      float f1 = 0.7F;
      noise = NoiseUtil.map(noise, f, f1, f1 - f);
      return 1.0F - noise;
   }
}
