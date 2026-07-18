package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;

public record HeightViability(float minOffset, float midOffset, float maxOffset) implements Viability {
   public static final DataSpec<HeightViability> SPEC = DataSpec.<HeightViability>builder(
         "Height",
         HeightViability.class,
         (data, spec, context) -> new HeightViability(
            spec.get("min", data, DataValue::asFloat), spec.get("mid", data, DataValue::asFloat), spec.get("max", data, DataValue::asFloat)
         )
      )
      .add("min", 0.0F, HeightViability::minOffset)
      .add("mid", 0.5F, HeightViability::midOffset)
      .add("max", 1.0F, HeightViability::maxOffset)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      int i = context.getTerrain().getBaseHeight(x, z);
      int j = context.getTerrain().getHeight(x, z);
      TerrainLevels terrainlevels = context.getLevels();
      float f = this.getScaler(terrainlevels);
      float f1 = i + this.minOffset() * f;
      float f2 = i + this.midOffset() * f;
      float f3 = i + this.maxOffset() * f;
      if (j < f1) {
         return 1.0F;
      } else if (j > f3) {
         return 1.0F;
      } else {
         return j < f2 ? (f2 - j) / (f2 - f1) : (j - f2) / (f3 - f2);
      }
   }
}
