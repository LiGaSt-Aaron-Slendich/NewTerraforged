package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.cave.CaveFeatureDensity;

public class FeatureDensityBudget {
    private static final int SUBCELLS = 16;
    private final float densityMultiplier;
    private final boolean caveTiered;
    private final int[][] used = new int[FeatureMass.values().length][16];

    public FeatureDensityBudget() {
        this(1.0f, false);
    }

    public FeatureDensityBudget(float densityMultiplier) {
        this(densityMultiplier, false);
    }

    public static FeatureDensityBudget forCaves() {
        return new FeatureDensityBudget(1.0f, true);
    }

    public static FeatureDensityBudget forCaveCeiling() {
        return new FeatureDensityBudget(1.0f, false);
    }

    public static FeatureDensityBudget forCaveCeilingMegaGiga() {
        return new FeatureDensityBudget(0.28f, false);
    }

    private FeatureDensityBudget(float densityMultiplier, boolean caveTiered) {
        this.densityMultiplier = densityMultiplier;
        this.caveTiered = caveTiered;
    }

    public static int subcellIndex(int localX, int localZ) {
        int sx = localX >> 2;
        int sz = localZ >> 2;
        return sx + (sz << 2);
    }

    public boolean canPlace(FeatureMass mass, int localX, int localZ) {
        if (!mass.isPlaceable()) {
            return false;
        }
        int cell = FeatureDensityBudget.subcellIndex(localX, localZ);
        return this.used[mass.ordinal()][cell] < this.limitFor(mass);
    }

    public int remaining(FeatureMass mass, int localX, int localZ) {
        if (!mass.isPlaceable()) {
            return 0;
        }
        int cell = FeatureDensityBudget.subcellIndex(localX, localZ);
        return Math.max(0, this.limitFor(mass) - this.used[mass.ordinal()][cell]);
    }

    public void record(FeatureMass mass, int localX, int localZ) {
        if (!mass.isPlaceable()) {
            return;
        }
        int cell = FeatureDensityBudget.subcellIndex(localX, localZ);
        int[] nArray = this.used[mass.ordinal()];
        int n = cell;
        nArray[n] = nArray[n] + 1;
    }

    private int limitFor(FeatureMass mass) {
        float mult;
        int base = mass.maxPerSubcell();
        if (base <= 0) {
            return 0;
        }
        float f = mult = this.caveTiered ? CaveFeatureDensity.multiplierFor(mass) : this.densityMultiplier;
        if (mult >= 1.0f) {
            return base;
        }
        return Math.max(1, Math.round((float)base * mult));
    }
}
