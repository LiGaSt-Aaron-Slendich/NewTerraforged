package com.terraforged.mod.worldgen.noise.continent.config;

import com.terraforged.mod.worldgen.noise.continent.cell.CellShape;
import com.terraforged.mod.worldgen.noise.continent.cell.CellSource;
import com.terraforged.mod.worldgen.noise.continent.config.RiverConfig;

public class ContinentConfig {
    public static final int CONTINENT_SAMPLE_SCALE = 400;
    public final Shape shape = new Shape();
    public final Noise noise = new Noise();
    public final Rivers rivers = new Rivers();

    public static class Shape {
        public int seed0;
        public int seed1;
        public int scale = 400;
        public float jitter = 0.75f;
        public float threshold = 0.525f;
        public float baseFalloffMin = 0.01f;
        public float baseFalloffMax = 0.25f;
        public CellShape cellShape = CellShape.SQUARE;
        public CellSource cellSource = CellSource.PERLIN;
    }

    public static class Noise {
        public float baseNoiseFalloff = 1.5f;
        public float continentNoiseFalloff = 1.0f;
    }

    public static class Rivers {
        public int seed = 0;
        public float lakeDensity = 0.75f;
        public final RiverConfig rivers = new RiverConfig();
        public final RiverConfig lakes = RiverConfig.lake();
    }
}
