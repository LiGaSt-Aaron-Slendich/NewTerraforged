package com.terraforged.mod.worldgen.biome.decorator;

public enum FeatureMass {
    SCATTER(10),
    SMALL(6),
    MEDIUM(3),
    LARGE(1),
    BLOCKED(0);

    private final int maxPerSubcell;

    private FeatureMass(int maxPerSubcell) {
        this.maxPerSubcell = maxPerSubcell;
    }

    public int maxPerSubcell() {
        return this.maxPerSubcell;
    }

    public boolean isPlaceable() {
        return this.maxPerSubcell > 0;
    }
}
