package com.terraforged.mod.worldgen.profiler;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.profiler.GenStage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilerStages {
    public final GenStage starts = new GenStage("Starts:    structures");
    public final GenStage refs = new GenStage("Refs:      struct refs");
    public final GenStage biomes = new GenStage("Biomes:    assignment");
    public final GenStage noise = new GenStage("Noise:     terrain cols");
    public final GenStage carve = new GenStage("Carvers:   cave carve");
    public final GenStage surface = new GenStage("Surface:   surf decor");
    public final GenStage decoration = new GenStage("Features:  feat decor");
    private final long start = System.currentTimeMillis() + 10000L;
    private final AtomicInteger chunkCount = new AtomicInteger();
    private final AtomicLong timestamp = new AtomicLong(0L);
    private final GenStage[] stages = new GenStage[]{this.starts, this.refs, this.biomes, this.noise, this.carve, this.surface, this.decoration};
    private final List<String> debugInfoCache = new ArrayList<String>();

    public void incrementChunks() {
        this.chunkCount.incrementAndGet();
    }

    public void reset() {
        this.chunkCount.set(0);
        for (GenStage stage : this.stages) {
            stage.reset();
        }
    }

    public void tick(long interval) {
        this.tick(interval, this.debugInfoCache);
        for (String line : this.debugInfoCache) {
            TerraForged.LOG.info(line);
        }
    }

    public void addDebugInfo(long interval, List<String> lines) {
        this.tick(interval, this.debugInfoCache);
        lines.addAll(this.debugInfoCache);
    }

    private void tick(long interval, List<String> lines) {
        long time = this.timestamp.get();
        long now = System.currentTimeMillis();
        if (time == 0L) {
            this.timestamp.set(now + interval * 2L);
            if (now > this.start) {
                this.reset();
            }
            return;
        }
        if (now > time && this.timestamp.compareAndSet(time, now + interval)) {
            lines.clear();
            lines.add("");
            lines.add("[World-Gen Performance] avg ms per chunk stage (5s window)");
            double sumAverage = 0.0;
            for (GenStage stage : this.stages) {
                double average = stage.getAverageMS();
                sumAverage += average;
                lines.add(String.format("%s %.2fms", stage.name(), average));
            }
            lines.add(String.format("Total/chunk:   %.2fms", sumAverage));
            lines.add(String.format("Chunks seen:   %s", this.chunkCount.get()));
            lines.add("");
        }
    }
}
