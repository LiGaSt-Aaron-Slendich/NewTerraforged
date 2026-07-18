package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;

public record SlopeViability(float normalize, float max) implements Viability {
   public static final DataSpec<SlopeViability> SPEC = DataSpec.<SlopeViability>builder(
         "Slope",
         SlopeViability.class,
         (data, spec, context) -> new SlopeViability(spec.get("normalize", data, DataValue::asFloat), spec.get("max", data, DataValue::asFloat))
      )
      .add("normalize", 1.0F, SlopeViability::normalize)
      .add("max", 1.0F, SlopeViability::max)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      float f = this.normalize * this.getScaler(context.getLevels());
      float f1 = context.getTerrain().getGradient(x, z, f);
      return f1 >= this.max ? 1.0F : f1 / this.max;
   }
}
