package com.terraforged.mod.worldgen.profiler;

import com.terraforged.mod.worldgen.profiler.GenStage;

public record GenTimer(GenStage stage, long start) {
    public void punchOut() {
        long now = System.nanoTime();
        this.stage.push(now - this.start);
    }

    public <T> T punchOut(T t) {
        this.punchOut();
        return t;
    }
}
