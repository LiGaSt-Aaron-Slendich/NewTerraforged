/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.filter;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.filter.Filterable;

public interface Filter {
    public void apply(Filterable var1, int var2, int var3, int var4);

    default public void iterate(Filterable map, Visitor visitor) {
        for (int dz = 0; dz < map.getSize().total; ++dz) {
            for (int dx = 0; dx < map.getSize().total; ++dx) {
                Cell cell = map.getCellRaw(dx, dz);
                visitor.visit(map, cell, dx, dz);
            }
        }
    }

    public static interface Visitor {
        public void visit(Filterable var1, Cell var2, int var3, int var4);
    }
}

