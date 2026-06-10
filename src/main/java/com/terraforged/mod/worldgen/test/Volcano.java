package com.terraforged.mod.worldgen.test;

import com.terraforged.mod.worldgen.test.VolcanoConfig;
import java.util.Arrays;
import net.minecraft.util.Mth;

public class Volcano {
    public static int toHeightValue(double height) {
        return 64 + Mth.floor((double)(height * 0.5));
    }

    public static Value getHighest(int x, int z, VolcanoConfig config, Cache cache) {
        Value maxValue = cache.value0.reset();
        Value value = cache.value1.reset();
        for (int i = 0; i < cache.size(); ++i) {
            boolean higher;
            Point point = cache.at(i);
            if (!point.valid()) continue;
            Volcano.evalPoint(x, z, point, value.reset(), config);
            boolean bl = higher = value.height > maxValue.height;
            if (!(maxValue.mouth ? value.mouth && higher : value.mouth || higher)) continue;
            maxValue.height = value.height;
            maxValue.mouth = value.mouth;
            maxValue.hash = point.hash;
        }
        return maxValue;
    }

    private static void evalPoint(int x, int y, Point point, Value value, VolcanoConfig config) {
        double alpha = Volcano.getDistanceNoise(x, y, point, value, config);
        if (Double.isNaN(alpha)) {
            return;
        }
        double h0 = 0.0;
        double h1 = config.height1().get(Noise.rand(point.hash, 31643));
        if (value.mouth) {
            h0 = config.height0().get(Noise.rand(point.hash, 30047));
        } else {
            alpha *= alpha;
        }
        double height = Mth.lerp((double)alpha, (double)h0, (double)h1);
        value.height = Volcano.toHeightValue(height);
    }

    private static double getDistanceNoise(int x, int y, Point point, Value value, VolcanoConfig config) {
        double outer;
        double distance2 = Mth.lengthSquared((double)(point.x - x), (double)(point.y - y));
        if (distance2 >= (outer = config.radius2().get(Noise.rand(point.hash, 26921))) * outer) {
            return Double.NaN;
        }
        double origin = 1.0;
        double inner = config.radius1().get(Noise.rand(point.hash, 21701));
        if (distance2 < inner * inner) {
            value.mouth = true;
            outer = inner;
            inner = config.radius0().get(Noise.rand(point.hash, 18899));
            if (distance2 <= inner * inner) {
                return 0.0;
            }
            return (Math.sqrt(distance2) - inner) / (outer - inner);
        }
        return origin - (Math.sqrt(distance2) - inner) / (outer - inner);
    }

    public static <T> void collectPoints(long seed, int chunkX, int chunkZ, T context, VolcanoConfig config, Cache cache, VolcanoPredicate<T> filter) {
        int x0 = chunkX << 4;
        int y0 = chunkZ << 4;
        int x1 = x0 + 15;
        int y1 = y0 + 15;
        double frequency = 1.0 / config.scale();
        double fx0 = (double)x0 * frequency;
        double fy0 = (double)y0 * frequency;
        double fx1 = (double)x1 * frequency;
        double fy1 = (double)y1 * frequency;
        int minX = Mth.floor((double)fx0) - 1;
        int minY = Mth.floor((double)fy0) - 1;
        int maxX = Mth.floor((double)fx1) + 1;
        int maxY = Mth.floor((double)fy1) + 1;
        Volcano.collectPoints(seed, minX, minY, maxX, maxY, frequency, context, config, cache, filter);
    }

    private static <T> void collectPoints(long seed, int minX, int minY, int maxX, int maxY, double frequency, T context, VolcanoConfig config, Cache cache, VolcanoPredicate<T> filter) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                int posY;
                long hash = Noise.mix(seed, x, y);
                if (Noise.rand(hash, 6869) > config.density()) continue;
                double px = Volcano.point(hash, 12343, x, config.jitter());
                double py = Volcano.point(hash, 16477, y, config.jitter());
                int posX = Mth.floor((double)(px / frequency));
                if (!filter.test(posX, posY = Mth.floor((double)(py / frequency)), context)) continue;
                Point point = cache.next();
                point.x = posX;
                point.y = posY;
                point.hash = hash;
            }
        }
    }

    private static double point(long hash, int hashOffset, int cell, double jitter) {
        return (double)cell + Noise.rand(hash, hashOffset) * jitter;
    }

    public static class Cache {
        protected int size;
        protected Point[] points = new Point[9];
        protected final Value value0 = new Value();
        protected final Value value1 = new Value();

        public Cache() {
            for (int i = 0; i < this.points.length; ++i) {
                this.points[i] = new Point();
            }
        }

        public int size() {
            return this.size;
        }

        public Cache reset() {
            this.size = 0;
            return this;
        }

        public Point at(int index) {
            return this.points[index];
        }

        public Point next() {
            int index = this.size++;
            this.ensure(index);
            return this.points[index].reset();
        }

        protected void ensure(int index) {
            if (index < this.points.length) {
                return;
            }
            int oldLength = this.points.length;
            int newLength = oldLength << 1;
            this.points = Arrays.copyOf(this.points, newLength);
            for (int i = oldLength; i < newLength; ++i) {
                this.points[i] = new Point();
            }
        }
    }

    public static class Value {
        public long hash = 0L;
        public int height = 0;
        public boolean mouth = false;

        public Value reset() {
            this.hash = 0L;
            this.height = 0;
            this.mouth = false;
            return this;
        }
    }

    public static class Point {
        public long hash = Long.MAX_VALUE;
        public int x = Integer.MAX_VALUE;
        public int y = Integer.MAX_VALUE;

        public boolean valid() {
            return this.hash != Long.MAX_VALUE && this.x != Integer.MAX_VALUE && this.y != Integer.MAX_VALUE;
        }

        public Point reset() {
            this.hash = Long.MAX_VALUE;
            this.x = Integer.MAX_VALUE;
            this.y = Integer.MAX_VALUE;
            return this;
        }
    }

    public static interface Noise {
        public static final int DENSITY = 6869;
        public static final int POINT_X = 12343;
        public static final int POINT_Y = 16477;
        public static final int RADIUS_0 = 18899;
        public static final int RADIUS_1 = 21701;
        public static final int RADIUS_2 = 26921;
        public static final int HEIGHT_0 = 30047;
        public static final int HEIGHT_1 = 31643;
        public static final int HEIGHT_2 = 33199;
        public static final int FLUID_FILLER = 39761;

        public static long mixGamma(long z) {
            z = (z ^ z >>> 33) * -49064778989728563L;
            z = (z ^ z >>> 33) * -4265267296055464877L;
            int n = Long.bitCount((z = z ^ z >>> 33 | 1L) ^ z >>> 1);
            return n < 24 ? z ^ 0xAAAAAAAAAAAAAAAAL : z;
        }

        public static long mix(long z) {
            z = (z ^ z >>> 30) * -4658895280553007687L;
            z = (z ^ z >>> 27) * -7723592293110705685L;
            return z ^ z >>> 31;
        }

        public static long mix(long seed, long offset) {
            return Noise.mix(seed + Noise.mixGamma(offset));
        }

        public static long mix(long seed, int x, int y) {
            long hash = Noise.mix(seed, x);
            hash = Noise.mix(hash, y);
            return hash;
        }

        public static double rand(long hash, int offset) {
            return Noise.rand(Noise.mix(hash, offset));
        }

        public static double rand(long hash) {
            return (double)(hash >>> 11) * (double)1.110223E-16f;
        }
    }

    public static interface VolcanoPredicate<T> {
        public boolean test(int var1, int var2, T var3);
    }
}
