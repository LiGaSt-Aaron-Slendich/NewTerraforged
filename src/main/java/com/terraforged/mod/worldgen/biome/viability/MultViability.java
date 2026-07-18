package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import java.util.List;

public record MultViability(Viability... rules) implements Viability {
   public static final DataSpec<MultViability> SPEC = DataSpec.<MultViability>builder(
         "Multiply", MultViability.class, (data, spec, context) -> new MultViability(spec.get("rules", data, v -> SumViability.getRules(v, context)))
      )
      .addList("rules", MultViability::getRulesList)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      float f = 1.0F;

      for (int i = 0; i < this.rules.length; i++) {
         f *= this.rules[i].getFitness(x, z, context);
      }

      return f;
   }

   private List<Viability> getRulesList() {
      return List.of(this.rules);
   }
}
