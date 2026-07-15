/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.filter;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.filter.Filter;
import com.terraforged.engine.filter.Filterable;
import com.terraforged.engine.world.heightmap.Levels;

public class Steepness
implements Filter,
Filter.Visitor {
    private final int radius;
    private final float scaler;
    private final float waterLevel;
    private final float maxBeachLevel;

    public Steepness(int radius, float scaler, Levels levels) {
        this.radius = radius;
        this.scaler = scaler;
        this.waterLevel = levels.water;
        this.maxBeachLevel = levels.water(6);
    }

    @Override
    public void apply(Filterable cellMap, int seedX, int seedZ, int iterations) {
        this.iterate(cellMap, this);
    }

    @Override
    public void visit(Filterable cellMap, Cell cell, int cx, int cz) {
        float totalHeightDif = 0.0f;
        for (int dz = -1; dz <= 2; ++dz) {
            for (int dx = -1; dx <= 2; ++dx) {
                int z;
                int x;
                Cell neighbour;
                if (dx == 0 && dz == 0 || (neighbour = cellMap.getCellRaw(x = cx + dx * this.radius, z = cz + dz * this.radius)).isAbsent()) continue;
                float height = Math.max(neighbour.value, this.waterLevel);
                totalHeightDif += Math.abs(cell.value - height) / (float)this.radius;
            }
        }
        cell.gradient = Math.min(1.0f, totalHeightDif * this.scaler);
    }
}

