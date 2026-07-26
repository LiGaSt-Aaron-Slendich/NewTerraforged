package com.terraforged.mod.data;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.TerraForged;

/**
 * Extra landform labels used by NewTF island / ocean overlays.
 * Registered into the stock datapack terrain_type registry.
 */
public final class ModTerrainTypes {
    private ModTerrainTypes() {
    }

    public static final Terrain TORRIDONIAN = TerrainType.getOrCreate("torridonian", TerrainType.HILLS);
    public static final Terrain DOLOMITES = TerrainType.getOrCreate("dolomites", TerrainType.MOUNTAINS);
    /** Shallow water between archipelago islands — laguna, not rivers. */
    public static final Terrain LAGUNA = TerrainType.getOrCreate("laguna", TerrainType.SHALLOW_OCEAN);
    public static final Terrain ISLAND_HILLS = TerrainType.getOrCreate("island_hills", TerrainType.HILLS);
    public static final Terrain ISLAND_PLATEAU = TerrainType.getOrCreate("island_plateau", TerrainType.PLATEAU);
    public static final Terrain ISLAND_MOUNTAINS = TerrainType.getOrCreate("island_mountains", TerrainType.MOUNTAINS);
    public static final Terrain ISLAND_FLATS = TerrainType.getOrCreate("island_flats", TerrainType.FLATS);
    public static final Terrain ISLAND_VOLCANO = TerrainType.getOrCreate("island_volcano", TerrainType.VOLCANO);

    public static void register() {
        TerrainType.forEach(terrain -> {
            com.terraforged.mod.worldgen.asset.TerrainType asset =
                    com.terraforged.mod.worldgen.asset.TerrainType.of(terrain);
            TerraForged.register(TerraForged.TERRAIN_TYPES, terrain.getName(), asset);
        });
    }
}
