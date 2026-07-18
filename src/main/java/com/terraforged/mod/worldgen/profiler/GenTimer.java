package com.terraforged.mod.worldgen.profiler;

public record GenTimer(GenStage stage, long start) {
   public void punchOut() {
      long i = System.nanoTime();
      this.stage.push(i - this.start);
   }

   public <T> T punchOut(T t) {
      this.punchOut();
      return t;
   }
}
