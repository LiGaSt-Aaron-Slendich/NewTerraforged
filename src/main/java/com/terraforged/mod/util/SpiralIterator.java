package com.terraforged.mod.util;

import com.terraforged.engine.util.pos.PosUtil;

public class SpiralIterator {
    private final int x;
    private final int z;
    private final int minRadius;
    private final int maxRadius;
    private int i = -1;
    private int radius;
    private int length;
    private int maxIndex;

    public SpiralIterator(int x, int z, int radius, int maxRadius) {
        this.x = x;
        this.z = z;
        this.minRadius = radius;
        this.maxRadius = maxRadius;
        this.setRadius(radius);
    }

    public void reset() {
        this.i = -1;
        this.setRadius(this.minRadius);
    }

    public boolean hasNext() {
        return this.i + 1 < this.maxIndex || this.radius < this.maxRadius;
    }

    public long next() {
        this.nextIndex();
        int edge = this.i / this.length;
        int step = this.i % this.length;
        int dx = -this.radius;
        int dz = -this.radius;
        switch (edge) {
            case 0: {
                dx = -this.radius + step;
                break;
            }
            case 1: {
                dx = this.radius;
                dz = -this.radius + step;
                break;
            }
            case 2: {
                dx = this.radius - step;
                dz = this.radius;
                break;
            }
            case 3: {
                dz = this.radius - step;
            }
        }
        return PosUtil.pack(this.x + dx, this.z + dz);
    }

    public PositionFinder finder(Object2Long<SpiralIterator> function) {
        return new PositionFinder(function);
    }

    private void nextIndex() {
        if (this.radius > this.maxRadius) {
            return;
        }
        if (++this.i < this.maxIndex) {
            return;
        }
        this.setRadius(this.radius + 1);
        this.i = 0;
    }

    private void setRadius(int radius) {
        int diameter = 1 + radius * 2;
        this.radius = radius;
        this.length = diameter - 1;
        this.maxIndex = (diameter - 1) * 4;
    }

    public class PositionFinder {
        private final Object2Long<SpiralIterator> function;

        public PositionFinder(Object2Long<SpiralIterator> function) {
            this.function = function;
        }

        public boolean hasNext() {
            return SpiralIterator.this.hasNext();
        }

        public long next() {
            if (!this.hasNext()) {
                return 0L;
            }
            return this.function.apply(SpiralIterator.this);
        }
    }

    public static interface Object2Long<T> {
        public long apply(T var1);
    }
}
