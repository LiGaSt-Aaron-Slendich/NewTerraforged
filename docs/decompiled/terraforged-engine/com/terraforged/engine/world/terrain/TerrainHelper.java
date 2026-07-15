/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;

public class TerrainHelper {
    public static Terrain getOrCreate(String name, Terrain parent) {
        if (parent == null || parent == TerrainType.NONE) {
            return TerrainType.NONE;
        }
        Terrain current = TerrainType.get(name);
        if (current != null) {
            return current;
        }
        return TerrainType.register(new Terrain(0, name, parent));
    }
}

