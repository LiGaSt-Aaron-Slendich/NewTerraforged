package com.terraforged.mod.worldgen.profiler;

import com.terraforged.mod.TerraForged;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilerStages {
   public final GenStage starts = new GenStage("Starts:    ");
   public final GenStage refs = new GenStage("Refs:      ");
   public final GenStage biomes = new GenStage("Biomes:    ");
   public final GenStage noise = new GenStage("Noise:     ");
   public final GenStage carve = new GenStage("Carvers: ");
   public final GenStage surface = new GenStage("Surface: ");
   public final GenStage decoration = new GenStage("Features:");
   private final long start = System.currentTimeMillis() + 10000L;
   private final AtomicInteger chunkCount = new AtomicInteger();
   private final AtomicLong timestamp = new AtomicLong(0L);
   private final GenStage[] stages = new GenStage[]{this.starts, this.refs, this.biomes, this.noise, this.carve, this.surface, this.decoration};
   private final List<String> debugInfoCache = new ArrayList<>();

   public void incrementChunks() {
      this.chunkCount.incrementAndGet();
   }

   public void reset() {
      this.chunkCount.set(0);

      for (GenStage genstage : this.stages) {
         genstage.reset();
      }
   }

   public void tick(long interval) {
      this.tick(interval, this.debugInfoCache);

      for (String s : this.debugInfoCache) {
         TerraForged.LOG.info(s);
      }
   }

   public void addDebugInfo(long interval, List<String> lines) {
      this.tick(interval, this.debugInfoCache);
      lines.addAll(this.debugInfoCache);
   }

   private void tick(long interval, List<String> lines) {
      long i = this.timestamp.get();
      long j = System.currentTimeMillis();
      if (i == 0L) {
         this.timestamp.set(j + interval * 2L);
         if (j > this.start) {
            this.reset();
         }
      } else {
         if (j > i && this.timestamp.compareAndSet(i, j + interval)) {
            lines.clear();
            lines.add("");
            lines.add("[World-Gen Performance]");
            double d0 = 0.0;

            for (GenStage genstage : this.stages) {
               double d1 = genstage.getAverageMS();
               d0 += d1;
               lines.add(String.format("%s %.2fms", genstage.name(), d1));
            }

            lines.add(String.format("Chunk Average: %.2fms", d0));
            lines.add(String.format("Chunk Count:   %s", this.chunkCount.get()));
            lines.add("");
         }
      }
   }
}
