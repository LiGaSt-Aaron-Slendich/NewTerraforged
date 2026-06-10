package com.terraforged.mod.worldgen.profiler;

import com.terraforged.mod.worldgen.profiler.GenTimer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class GenStage {
    private static final double NANO_TO_MS = 1.0 / (double)TimeUnit.MILLISECONDS.toNanos(1L);
    private final AtomicLong time = new AtomicLong();
    private final AtomicInteger count = new AtomicInteger();
    private final String name;

    public GenStage(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public void reset() {
        this.time.set(0L);
        this.count.set(0);
    }

    public GenTimer start() {
        return new GenTimer(this, System.nanoTime());
    }

    public void push(long duration) {
        this.time.addAndGet(duration);
        this.count.incrementAndGet();
    }

    public double getAverageNanos() {
        double average = (double)this.time.get() / (double)this.count.get();
        return Double.isNaN(average) ? 0.0 : average;
    }

    public double getAverageMS() {
        return this.getAverageNanos() * NANO_TO_MS;
    }
}
