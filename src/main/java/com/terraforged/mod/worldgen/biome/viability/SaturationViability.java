package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;

public record SaturationViability(float min, float max) implements Viability {
   public static final DataSpec<SaturationViability> SPEC = DataSpec.<SaturationViability>builder(
         "Saturation",
         SaturationViability.class,
         (data, spec, context) -> new SaturationViability(spec.get("min", data, DataValue::asFloat), spec.get("max", data, DataValue::asFloat))
      )
      .add("min", 0.0F, SaturationViability::min)
      .add("max", 1.0F, SaturationViability::max)
      .build();

   public SaturationViability(float max) {
      this(0.0F, max);
   }

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      float f = 1.0F - context.getTerrain().getRiver().get(x, z);
      if (f < this.min) {
         return 0.0F;
      } else {
         return f > this.max ? 1.0F : (f - this.min) / (this.max - this.min);
      }
   }
}
