package com.terraforged.mod.data;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.TerrainTypes;

public interface ModTerrainTypes {
    public static final Terrain TORRIDONIAN = TerrainTypes.getOrCreate("torridonian", TerrainType.HILLS);
    public static final Terrain DOLOMITES = TerrainTypes.getOrCreate("dolomites", TerrainType.MOUNTAINS);

    public static void register() {
        TerrainType.forEach(terrain -> {
            com.terraforged.mod.worldgen.asset.TerrainType type = com.terraforged.mod.worldgen.asset.TerrainType.of(terrain);
            TerraForged.register(TerraForged.TERRAIN_TYPES, terrain.getName(), type);
        });
    }
}
