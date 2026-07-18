package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;

public class ViabilityContext implements Viability.Context {
   public CompletableFuture<TerrainData> terrainData;
   public IBiomeSampler biomeSampler;

   @Override
   public boolean edge() {
      return false;
   }

   @Override
   public TerrainLevels getLevels() {
      return this.getTerrain().getLevels();
   }

   @Override
   public TerrainData getTerrain() {
      return this.terrainData.join();
   }

   @Override
   public IBiomeSampler getClimateSampler() {
      return this.biomeSampler;
   }

   public void assign(ChunkPos pos, Generator generator) {
      this.terrainData = generator.getChunkDataAsync(pos);
      this.biomeSampler = generator.getBiomeSource().getBiomeSampler();
   }
}
