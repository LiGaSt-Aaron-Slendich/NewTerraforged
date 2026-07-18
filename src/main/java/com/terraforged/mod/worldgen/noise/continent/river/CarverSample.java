package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.engine.world.terrain.TerrainType;

public class CarverSample {
   public final NodeSample river = new NodeSample(TerrainType.RIVER);
   public final NodeSample lake = new NodeSample(TerrainType.LAKE);

   public CarverSample reset() {
      this.river.reset();
      this.lake.reset();
      return this;
   }
}
