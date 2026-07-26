package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;

/** Minimal stub — full caves.toml density wiring deferred with NewTF cave extras. */
public final class TFCaveSystemConfig {
    public static TFCaveSystemConfig INSTANCE = new TFCaveSystemConfig();

    public float cavePercent = 100.0f;
    public int surfaceRoofBufferMegaGiga = 26;
    public Density caveDensity = new Density();

    public static void load() {
        INSTANCE = new TFCaveSystemConfig();
        TerraForged.LOG.info("[TFCaveSystemConfig] stub load");
    }

    public static final class Density {
        public Float xyLimit() { return null; }
        public Float yzLimit() { return null; }
        public float cavePercent() { return TFCaveSystemConfig.INSTANCE.cavePercent; }
    }
}
