/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.terrain.provider;

import com.terraforged.engine.cell.Populator;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.heightmap.RegionConfig;
import com.terraforged.engine.world.terrain.provider.TerrainProvider;

public interface TerrainProviderFactory {
    public TerrainProvider create(GeneratorContext var1, RegionConfig var2, Populator var3);
}

