package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.util.storage.LongCache;
import com.terraforged.mod.util.storage.LossyCache;
import com.terraforged.mod.util.storage.ObjectPool;
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
    public final NoiseLevels levels;
    public final ControlPoints controlPoints;
    public final CellShape cellShape;
    public final CellSource cellSource;
    public final RiverGenerator riverGenerator;
    public final ShapeGenerator shapeGenerator;
    private final ObjectPool<CellPoint> cellPool = ObjectPool.forCacheSize(2048, CellPoint::new);
    private final LongCache<CellPoint> cellCache = LossyCache.concurrent(2048, CellPoint[]::new, this.cellPool);
    private volatile Vec2f offset = null;

    public ContinentGenerator(ContinentConfig config, NoiseLevels levels, ControlPoints controlPoints) {
        this.levels = levels;
        this.controlPoints = controlPoints;
        this.seed = config.shape.seed0;
        this.sampleSeed = config.shape.seed1 + 6569;
        this.jitter = config.shape.jitter;
        this.cellShape = config.shape.cellShape;
        this.cellSource = config.shape.cellSource;
        this.riverGenerator = new RiverGenerator(this, config);
        this.shapeGenerator = new ShapeGenerator(this, config, controlPoints);
    }

    public Vec2f getWorldOffset(int seed) {
        Vec2f offset = this.offset;
        if (offset == null) {
            this.offset = offset = this.computeWorldOffset(seed);
        }
        return offset;
    }

    public CellPoint getCell(int seed, int cx, int cy) {
        long index = PosUtil.pack(cx, cy);
        return this.cellCache.computeIfAbsent(seed, index, this::computeCell);
    }

    public long getNearestCell(int seed, float x, float y) {
        x = this.cellShape.adjustX(x);
        y = this.cellShape.adjustY(y);
        int minX = NoiseUtil.floor(x) - 1;
        int minY = NoiseUtil.floor(y) - 1;
        int maxX = minX + 2;
        int maxY = minY + 2;
        int nearestX = 0;
        int nearestY = 0;
        float distance = Float.MAX_VALUE;
        int i = 0;
        for (int cy = minY; cy <= maxY; ++cy) {
            int cx = minX;
            while (cx <= maxX) {
                CellPoint cell = this.getCell(seed, cx, cy);
                float dist2 = NoiseUtil.dist2(x, y, cell.px, cell.py);
                if (dist2 < distance) {
                    distance = dist2;
                    nearestX = cx;
                    nearestY = cy;
                }
                ++cx;
                ++i;
            }
        }
        return PosUtil.pack(nearestX, nearestY);
    }

    private CellPoint computeCell(int seed, long index) {
        return this.computeCell(seed, index, 0, 0, this.cellPool.take());
    }

    private CellPoint computeCell(int seed, long index, int ox, int oy, CellPoint cell) {
        int cx = PosUtil.unpackLeft(index) + ox;
        int cy = PosUtil.unpackRight(index) + oy;
        int hash = MathUtil.hash(this.seed + seed, cx, cy);
        float px = this.cellShape.getCellX(hash, cx, cy, this.jitter);
        float py = this.cellShape.getCellY(hash, cx, cy, this.jitter);
        cell.px = px;
        cell.py = py;
        float target = 4000.0f;
        float freq = 400.0f / target;
        ContinentGenerator.sampleCell(seed + this.sampleSeed, px, py, this.cellSource, 2, freq, 2.75f, 0.3f, cell);
        return cell;
    }

    private static void sampleCell(int seed, float x, float y, CellSource cellSource, int octaves, float frequency, float lacunarity, float gain, CellPoint cell) {
        float amp;
        float sum = cellSource.getValue(seed, x *= frequency, y *= frequency);
        float sumAmp = amp = 1.0f;
        cell.noise0 = sum;
        for (int i = 1; i < octaves; ++i) {
            sum += cellSource.getValue(seed, x *= lacunarity, y *= lacunarity) * (amp *= gain);
            sumAmp += amp;
        }
        cell.noise = sum / sumAmp;
    }

    private Vec2f computeWorldOffset(int seed) {
        SpiralIterator iterator = new SpiralIterator(0, 0, 0, 100000);
        CellPoint cell = new CellPoint();
        while (iterator.hasNext()) {
            long pos = iterator.next();
            this.computeCell(seed, pos, 0, 0, cell);
            if (this.shapeGenerator.getThresholdValue(cell) == 0.0f) continue;
            float px = cell.px;
            float py = cell.py;
            if (!this.isValidSpawn(seed, pos, 3, cell)) continue;
            return new Vec2f(px, py);
        }
        return Vec2f.ZERO;
    }

    private boolean isValidSpawn(int seed, long pos, int radius, CellPoint cell) {
        int radius2 = radius * radius;
        for (int dy = -radius; dy <= radius; ++dy) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int d2 = dx * dx + dy * dy;
                if (dy < 1 || d2 >= radius2) continue;
                this.computeCell(seed, pos, dx, dy, cell);
                if (this.shapeGenerator.getThresholdValue(cell) != 0.0f) continue;
                return false;
            }
        }
        return true;
    }
}
