package com.terraforged.mod.data;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainHelper;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;

public interface ModTerrainTypes extends ModRegistry {
   Terrain TORRIDONIAN = TerrainHelper.getOrCreate("torridonian", TerrainType.HILLS);
   Terrain DOLOMITES = TerrainHelper.getOrCreate("dolomites", TerrainType.MOUNTAINS);
   /** Shallow water between archipelago islands — laguna, not rivers. */
   Terrain LAGUNA = TerrainHelper.getOrCreate("laguna", TerrainType.SHALLOW_OCEAN);

   static void register() {
      TerrainType.forEach(terrain -> {
         com.terraforged.mod.worldgen.asset.TerrainType terraintype = com.terraforged.mod.worldgen.asset.TerrainType.of(terrain);
         ModRegistries.register(TERRAIN_TYPE, terrain.getName(), terraintype);
      });
   }
}
