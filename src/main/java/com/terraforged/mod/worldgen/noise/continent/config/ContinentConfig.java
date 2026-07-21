package com.terraforged.mod.worldgen.noise.continent.config;

import com.terraforged.mod.worldgen.noise.continent.cell.CellShape;
import com.terraforged.mod.worldgen.noise.continent.cell.CellSource;

public class ContinentConfig {
   public static final int CONTINENT_SAMPLE_SCALE = 400;
   public final ContinentConfig.Shape shape = new ContinentConfig.Shape();
   public final ContinentConfig.Noise noise = new ContinentConfig.Noise();
   public final ContinentConfig.Rivers rivers = new ContinentConfig.Rivers();

   public static class Noise {
      public float baseNoiseFalloff = 1.5F;
      public float continentNoiseFalloff = 1.0F;
   }

   public static class Rivers {
      public int seed = 0;
      public float lakeDensity = 0.75F;
      /** 1.0 = place every drainage link (default riverCount=8). 0 = none. */
      public float riverDensity = 1.0F;
      public final RiverConfig rivers = new RiverConfig();
      public final RiverConfig lakes = RiverConfig.lake();
   }

   public static class Shape {
      public int seed0;
      public int seed1;
      public int scale = 400;
      public float jitter = 0.75F;
      public float threshold = 0.525F;
      public float baseFalloffMin = 0.01F;
      public float baseFalloffMax = 0.25F;
      public int noiseOctaves = 2;
      public float noiseGain = 0.3F;
      public float noiseLacunarity = 2.75F;
      public float sizeVariance = 0.25F;
      public CellShape cellShape = CellShape.SQUARE;
      public CellSource cellSource = CellSource.PERLIN;
      /** Target continent count inside the 640k window (actual may be ±1; ignored when guarantee disabled). */
      public int guaranteedContinents = 3;
      public boolean guaranteedContinentsEnabled = true;
      public float continentsSpread = 0.5F;
      public float coastalIslandsChance = 0.45F;
      public float volcanicIslandsChance = 0.25F;
      public boolean scatteredArchipelago = true;
      public float scatteredArchipelagoChance = 0.35F;
   }
}
