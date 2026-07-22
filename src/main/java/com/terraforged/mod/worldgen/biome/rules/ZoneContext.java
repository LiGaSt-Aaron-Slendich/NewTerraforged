package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;

/**
 * Local placement context for zone flags (near volcano, etc.).
 * v1: volcano footprint ≈ painted volcano / pipe / island_volcano cells.
 * Later: true radial distance field from volcano centres.
 */
public final class ZoneContext {
    public final boolean nearActiveVolcano;

    public ZoneContext(boolean nearActiveVolcano) {
        this.nearActiveVolcano = nearActiveVolcano;
    }

    public static ZoneContext from(ClimateSample sample) {
        if (sample == null || sample.terrainType == null) {
            return new ZoneContext(false);
        }
        Terrain t = sample.terrainType;
        boolean volcano = t == TerrainType.VOLCANO
                || t == TerrainType.VOLCANO_PIPE
                || t == ModTerrainTypes.ISLAND_VOLCANO
                || t.isVolcano()
                || "volcano".equalsIgnoreCase(t.getName())
                || "volcano_pipe".equalsIgnoreCase(t.getName())
                || "island_volcano".equalsIgnoreCase(t.getName());
        return new ZoneContext(volcano);
    }
}
