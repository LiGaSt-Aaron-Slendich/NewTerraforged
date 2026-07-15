/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.biome.modifier;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.world.biome.map.BiomeMap;
import com.terraforged.engine.world.biome.modifier.BiomeModifier;

public class VolcanoModifier
implements BiomeModifier {
    private final float chance;
    private final BiomeMap biomes;

    public VolcanoModifier(BiomeMap biomes, float usage) {
        this.biomes = biomes;
        this.chance = usage;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean exitEarly() {
        return true;
    }

    @Override
    public boolean test(int biome, Cell cell) {
        return cell.terrain.isVolcano() && cell.terrainRegionId < this.chance;
    }

    @Override
    public int modify(int in, Cell cell, int x, int z) {
        int volcano = this.biomes.getVolcano(cell);
        if (BiomeMap.isValid(volcano)) {
            return volcano;
        }
        return in;
    }
}

