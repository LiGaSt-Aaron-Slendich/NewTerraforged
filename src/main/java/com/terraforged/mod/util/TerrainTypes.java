package com.terraforged.mod.util;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import java.lang.reflect.Constructor;

public final class TerrainTypes {
    private TerrainTypes() {
    }

    public static Terrain getOrCreate(String name, Terrain parent) {
        Terrain existing = TerrainType.get(name);
        if (existing != null && existing != TerrainType.NONE) {
            return existing;
        }
        try {
            Constructor ctor = Terrain.class.getDeclaredConstructor(Integer.TYPE, String.class, Terrain.class);
            ctor.setAccessible(true);
            return TerrainType.register((Terrain)ctor.newInstance(-1, name, parent));
        }
        catch (ReflectiveOperationException e) {
            throw new Error("Failed to register terrain: " + name, e);
        }
    }
}
