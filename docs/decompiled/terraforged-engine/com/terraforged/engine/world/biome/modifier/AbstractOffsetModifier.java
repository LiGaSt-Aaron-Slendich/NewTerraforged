/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.biome.modifier;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.world.biome.modifier.BiomeModifier;
import com.terraforged.engine.world.climate.Climate;

public abstract class AbstractOffsetModifier
implements BiomeModifier {
    private final Climate climate;

    public AbstractOffsetModifier(Climate climate) {
        this.climate = climate;
    }

    @Override
    public int modify(int in, Cell cell, int x, int z) {
        float dx = this.climate.getOffsetX(x, z, 50.0f);
        float dz = this.climate.getOffsetX(x, z, 50.0f);
        return this.modify(in, cell, x, z, (float)x + dx, (float)z + dz);
    }

    protected abstract int modify(int var1, Cell var2, int var3, int var4, float var5, float var6);
}

