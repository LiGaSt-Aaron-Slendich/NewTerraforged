package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;

/**
 * Final island terrain labels after generation so Find/filters can locate
 * mountain / volcanic / flat islands distinctly from mainland forms.
 */
public final class IslandTerrainLabels {
    private IslandTerrainLabels() {
    }

    public static Terrain mountains() {
        return ModTerrainTypes.ISLAND_MOUNTAINS;
    }

    public static Terrain hills() {
        return ModTerrainTypes.ISLAND_HILLS;
    }

    public static Terrain flats() {
        return ModTerrainTypes.ISLAND_FLATS;
    }

    public static Terrain plateau() {
        return ModTerrainTypes.ISLAND_PLATEAU;
    }

    public static Terrain volcano() {
        return ModTerrainTypes.ISLAND_VOLCANO;
    }

    public static Terrain volcanoPipe() {
        return TerrainType.VOLCANO_PIPE;
    }

    /**
     * Remap stock mainland terrain tags that were stamped on island tips
     * into explicit island_* labels (idempotent for already-labelled types).
     */
    public static Terrain finalizeIsland(Terrain terrain) {
        if (terrain == null) {
            return null;
        }
        if (terrain == ModTerrainTypes.ISLAND_MOUNTAINS
                || terrain == ModTerrainTypes.ISLAND_HILLS
                || terrain == ModTerrainTypes.ISLAND_FLATS
                || terrain == ModTerrainTypes.ISLAND_PLATEAU
                || terrain == ModTerrainTypes.ISLAND_VOLCANO
                || terrain == TerrainType.VOLCANO_PIPE
                || terrain == ModTerrainTypes.LAGUNA) {
            return terrain;
        }
        if (terrain == TerrainType.MOUNTAINS) {
            return ModTerrainTypes.ISLAND_MOUNTAINS;
        }
        if (terrain == TerrainType.PLATEAU) {
            return ModTerrainTypes.ISLAND_PLATEAU;
        }
        if (terrain == TerrainType.HILLS) {
            return ModTerrainTypes.ISLAND_HILLS;
        }
        if (terrain == TerrainType.FLATS) {
            return ModTerrainTypes.ISLAND_FLATS;
        }
        if (terrain == TerrainType.VOLCANO) {
            return ModTerrainTypes.ISLAND_VOLCANO;
        }
        return terrain;
    }
}
