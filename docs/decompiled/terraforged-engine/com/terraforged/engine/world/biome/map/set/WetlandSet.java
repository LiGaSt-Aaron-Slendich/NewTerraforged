/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntList
 */
package com.terraforged.engine.world.biome.map.set;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.world.biome.TempCategory;
import com.terraforged.engine.world.biome.map.BiomeContext;
import com.terraforged.engine.world.biome.map.BiomeMap;
import com.terraforged.engine.world.biome.map.defaults.DefaultBiome;
import com.terraforged.engine.world.biome.map.set.TemperatureSet;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;

public class WetlandSet
extends TemperatureSet {
    private final BiomeMap<?> fallback;

    public WetlandSet(Map<TempCategory, IntList> map, BiomeMap<?> fallback, DefaultBiome defaultBiome, BiomeContext<?> context) {
        super(map, defaultBiome, context);
        this.fallback = fallback;
    }

    @Override
    public int getBiome(Cell cell) {
        int biome = super.getBiome(cell);
        if (biome == Integer.MIN_VALUE) {
            return this.fallback.getLand(cell);
        }
        return biome;
    }
}

