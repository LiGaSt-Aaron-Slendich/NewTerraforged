package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;

public record BiomeEdgeViability(float distance) implements Viability {
   public static final DataSpec<BiomeEdgeViability> SPEC = DataSpec.<BiomeEdgeViability>builder(
         BiomeEdgeViability.class, (data, spec, context) -> new BiomeEdgeViability(spec.get("distance", data, DataValue::asFloat))
      )
      .add("distance", 1.0F, BiomeEdgeViability::distance)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      if (context.edge()) {
         float f = context.getClimateSampler().getShape(x, z);
         return f > this.distance ? 0.0F : 1.0F - f / this.distance;
      } else {
         return 0.0F;
      }
   }
}
