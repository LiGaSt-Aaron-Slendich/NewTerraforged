package com.terraforged.mod.worldgen.noise.continent.cell;

public class CellPoint {
    public float px;
    public float py;
    public float noise;
    public float noise0;

    public float noise() {
        return this.noise0;
    }
}
