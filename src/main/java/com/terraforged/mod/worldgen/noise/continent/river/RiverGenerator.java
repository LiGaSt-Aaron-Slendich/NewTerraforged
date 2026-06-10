package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.storage.LongCache;
import com.terraforged.mod.util.storage.LossyCache;
import com.terraforged.mod.util.storage.ObjectPool;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.river.CarverSample;
import com.terraforged.mod.worldgen.noise.continent.river.NodeSample;
import com.terraforged.mod.worldgen.noise.continent.river.RiverCarver;
import com.terraforged.mod.worldgen.noise.continent.river.RiverNode;
import com.terraforged.mod.worldgen.noise.continent.river.RiverPieces;
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
    private final float lakeDensity;
    private final ContinentGenerator continent;
    private final RiverCarver riverCarver;
    private final Domain riverWarp;
    private final ThreadLocal<CarverSample> localRiverSample = ThreadLocal.withInitial(CarverSample::new);
    private final ObjectPool<RiverPieces> pool = ObjectPool.forCacheSize(1024, RiverPieces::new);
    private final LongCache<RiverPieces> cache = LossyCache.concurrent(1024, RiverPieces[]::new, this.pool);

    public RiverGenerator(ContinentGenerator continent, ContinentConfig config) {
        this.continent = continent;
        this.lakeDensity = config.rivers.lakeDensity;
        this.riverCarver = new RiverCarver(continent.levels, config);
        this.riverWarp = Domain.warp(Source.builder().seed(8657124).frequency(30.0).simplex(), Source.builder().seed(5123678).frequency(30.0).simplex(), Source.constant(0.004));
    }

    public void sample(int seed, float x, float y, NoiseSample sample) {
        float px = this.riverWarp.getX(x, y);
        float py = this.riverWarp.getY(x, y);
        CarverSample nodeSample = this.localRiverSample.get().reset();
        this.sample(seed, px, py, nodeSample);
        this.riverCarver.carve(seed, px, py, sample, nodeSample);
    }

    private void sample(int seed, float x, float y, CarverSample sample) {
        long centre = this.continent.getNearestCell(seed, x, y);
        int centreX = PosUtil.unpackLeft(centre);
        int centreY = PosUtil.unpackRight(centre);
        x = this.continent.cellShape.adjustX(x);
        y = this.continent.cellShape.adjustY(y);
        int minX = centreX - 1;
        int minY = centreY - 1;
        int maxX = centreX + 1;
        int maxY = centreY + 1;
        RiverNode river = null;
        RiverNode lake = null;
        for (int cy = minY; cy <= maxY; ++cy) {
            for (int cx = minX; cx <= maxX; ++cx) {
                RiverNode node;
                int i;
                RiverPieces pieces = this.getNodes(seed, cx, cy);
                for (i = 0; i < pieces.riverCount(); ++i) {
                    node = pieces.river(i);
                    river = this.sampleNode(x, y, node, river, sample.river);
                }
                for (i = 0; i < pieces.lakeCount(); ++i) {
                    node = pieces.lake(i);
                    lake = this.sampleNode(x, y, node, lake, sample.lake);
                }
            }
        }
        this.recordNode(river, sample.river);
        this.recordNode(lake, sample.lake);
    }

    private RiverNode sampleNode(float x, float y, RiverNode node, RiverNode nearest, NodeSample sample) {
        float t = node.getProjection(x, y);
        float d = node.getDistance2(x, y, t);
        if (d < sample.distance) {
            nearest = node;
            sample.distance = d;
            sample.projection = t;
        }
        return nearest;
    }

    private void recordNode(RiverNode node, NodeSample sample) {
        if (node != null) {
            float level = node.getHeight(sample.projection);
            float radius = node.getRadius(sample.projection);
            sample.distance = NoiseUtil.sqrt(sample.distance);
            sample.position = radius;
            sample.level = this.continent.shapeGenerator.getBaseNoise(level);
        } else {
            sample.invalidate();
        }
    }

    private RiverPieces getNodes(int seed, int x, int y) {
        long index = PosUtil.pack(x, y);
        return this.cache.computeIfAbsent(seed, index, this::computeNodes);
    }

    private RiverPieces computeNodes(int seed, long index) {
        int ay;
        int ax = PosUtil.unpackLeft(index);
        CellPoint a = this.continent.getCell(seed, ax, ay = PosUtil.unpackRight(index));
        if (this.continent.shapeGenerator.getThresholdValue(a) <= 0.0f) {
            return RiverPieces.NONE;
        }
        CellPoint min = a;
        float minValue = this.getBaseValue(a);
        float ah = this.getHeight(a.noise(), 0.0f, 1.0f);
        float ar = this.getRadius(a.noise(), 0.0f, 1.0f);
        boolean isSource = true;
        RiverPieces pieces = this.pool.take();
        for (Vec2i dir : DIRS) {
            int bx = ax + dir.x;
            int by = ay + dir.y;
            CellPoint b = this.continent.getCell(seed, bx, by);
            float value = this.getBaseValue(b);
            if (value <= minValue) {
                min = b;
                minValue = value;
                continue;
            }
            if (value <= 0.0f || !this.connects(seed, ax, ay, bx, by, value)) continue;
            float bh = this.getHeight(b.noise(), 0.0f, 1.0f);
            float br = this.getRadius(b.noise(), 0.0f, 1.0f);
            int hash = MathUtil.hash(seed + 827614, bx, by);
            this.addRiverNodes(a, b, seed, ah, bh, ar, br, hash, pieces);
            isSource = false;
        }
        if (min == a) {
            return pieces;
        }
        if (isSource && pieces.riverCount() == 0 && minValue <= 0.0f) {
            this.pool.restore(pieces);
            return RiverPieces.NONE;
        }
        float bh = this.getHeight(min.noise(), 0.0f, 1.0f);
        float br = this.getRadius(min.noise(), 0.0f, 1.0f);
        int hash = MathUtil.hash(seed + 827614, ax, ay);
        this.addRiverNodes(a, min, seed, ah, bh, ar, br, hash, pieces);
        if (isSource && this.hasLake(a, hash)) {
            this.addLakeNodes(a, min, seed, ah, hash, pieces);
        }
        return pieces;
    }

    private void addRiverNodes(CellPoint a, CellPoint b, int seed, float ah, float bh, float ar, float br, int hash, RiverPieces pieces) {
        float mx = (a.px + b.px) * 0.5f;
        float my = (a.py + b.py) * 0.5f;
        float mr = (ar + br) * 0.5f;
        float mh = (ah + bh) * 0.5f;
        float cx = (a.px + mx) * 0.5f;
        float cy = (a.py + my) * 0.5f;
        float cr = (ar + mr) * 0.5f;
        float ch = (ah + mh) * 0.5f;
        float nx = -(cy - a.py);
        float ny = cx - a.px;
        float dir = MathUtil.rand(seed + 20107, hash) < 0.5f ? -1.0f : 1.0f;
        float amp0 = 0.7f + MathUtil.rand(seed + 9803, hash) * 0.3f;
        float amp1 = 0.7f + MathUtil.rand(seed + 28387, hash) * 0.3f;
        float displacement = 0.35f * dir * amp0;
        float warpStrength = 0.275f * -dir * amp1;
        float warp1 = warpStrength * NoiseUtil.map(a.noise, 0.4f, 0.6f, 0.2f);
        float warp2 = -warpStrength * NoiseUtil.map(b.noise, 0.4f, 0.6f, 0.2f);
        pieces.addRiver(new RiverNode(a.px, a.py, cx += nx * displacement, cy += ny * displacement, ah, ch, ar, cr, warp1));
        pieces.addRiver(new RiverNode(cx, cy, mx, my, ch, mh, cr, mr, warp2));
        if (b.noise < this.continent.shapeGenerator.threshold) {
            pieces.addRiver(new RiverNode(mx, my, b.px, b.py, mh, bh, mr, br, warp1));
        }
    }

    private void addLakeNodes(CellPoint a, CellPoint b, int seed, float ah, int hash, RiverPieces pieces) {
        float size = (0.5f + MathUtil.rand(seed + 9803, hash) * 0.5f) * 0.12f;
        float dx = a.px - b.px;
        float dy = a.py - b.py;
        float cx = a.px + dx * size;
        float cy = a.py + dy * size;
        pieces.addLake(new RiverNode(a.px, a.py, cx, cy, ah, ah, 1.0f, 1.0f, 0.0f));
    }

    private boolean connects(int seed, int ax, int ay, int bx, int by, float minValue) {
        int minY = bx;
        int minX = by;
        for (Vec2i dir : DIRS) {
            int cx = bx + dir.x;
            int cy = by + dir.y;
            CellPoint c = this.continent.getCell(seed, cx, cy);
            float value = this.getBaseValue(c);
            if (!(value < minValue)) continue;
            minX = cx;
            minY = cy;
            minValue = value;
        }
        return minX == ax && minY == ay;
    }

    private boolean hasLake(CellPoint cell, int hash) {
        return MathUtil.rand(hash + 37171) <= this.lakeDensity || this.continent.shapeGenerator.getBaseNoise(cell.noise()) < 0.25f;
    }

    private float getBaseValue(CellPoint point) {
        return this.continent.shapeGenerator.getThresholdValue(point) <= 0.0f ? 0.0f : point.noise();
    }

    private float getHeight(float noise, float min, float max) {
        return noise;
    }

    private float getRadius(float noise, float min, float max) {
        float lower = 0.5f;
        float upper = 0.7f;
        noise = NoiseUtil.map(noise, lower, upper, upper - lower);
        noise = 1.0f - noise;
        return noise;
    }
}
