package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.engine.world.terrain.Terrain;

public class NodeSample {
   public float projection = 0.0F;
   public float distance = Float.NaN;
   public float position = 0.0F;
   public float level = 0.0F;
   public final Terrain type;

   public NodeSample(Terrain type) {
      this.type = type;
   }

   public boolean isInvalid() {
      return Float.isNaN(this.distance);
   }

   public NodeSample reset() {
      this.distance = Float.MAX_VALUE;
      this.projection = 0.0F;
      this.position = 0.0F;
      this.level = 0.0F;
      return this;
   }

   public NodeSample invalidate() {
      this.distance = Float.NaN;
      return this;
   }
}
