/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.filter;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.filter.Filter;
import com.terraforged.engine.filter.Filterable;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.TerrainType;

public class BeachDetect
implements Filter,
Filter.Visitor {
    private final ControlPoints transition;
    private final float grad2;
    private final int radius = 8;
    private final int diameter = 17;

    public BeachDetect(GeneratorContext context) {
        this.transition = new ControlPoints(context.settings.world.controlPoints);
        float delta = 0.0018382353f;
        this.grad2 = delta * delta;
    }

    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        this.iterate(map, this);
    }

    @Override
    public void visit(Filterable cellMap, Cell cell, int dx, int dz) {
        if (cell.terrain.isCoast() && cell.continentEdge < this.transition.beach) {
            float gz;
            Cell w;
            Cell n = cellMap.getCellRaw(dx, dz - 8);
            Cell s = cellMap.getCellRaw(dx, dz + 8);
            Cell e = cellMap.getCellRaw(dx + 8, dz);
            float gx = this.grad(e, w = cellMap.getCellRaw(dx - 8, dz), cell);
            float d2 = gx * gx + (gz = this.grad(n, s, cell)) * gz;
            if (d2 < 0.275f) {
                cell.terrain = TerrainType.BEACH;
            }
        }
    }

    private float grad(Cell a, Cell b, Cell def) {
        int distance = 17;
        if (a.isAbsent()) {
            a = def;
            distance -= 8;
        }
        if (b.isAbsent()) {
            b = def;
            distance -= 8;
        }
        return (a.value - b.value) / (float)distance;
    }
}

