package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.noise.Module;

public record NoiseViability(Module noise) implements Viability {
   public static final DataSpec<NoiseViability> SPEC = DataSpec.<NoiseViability>builder(
         "Noise", NoiseViability.class, (data, spec, context) -> new NoiseViability(spec.get("noise", data, Module.class, context))
      )
      .addObj("noise", NoiseViability::noise)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      return this.noise.getValue(x, z);
   }
}
