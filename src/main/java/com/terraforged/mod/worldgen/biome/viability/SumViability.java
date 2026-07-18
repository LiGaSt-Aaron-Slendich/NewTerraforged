package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataList;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.ArrayList;
import java.util.List;

public record SumViability(float initial, Viability[] rules, float[] amounts) implements Viability {
   public static final DataSpec<SumViability> SPEC = DataSpec.<SumViability>builder(
         "Sum",
         SumViability.class,
         (data, spec, context) -> new SumViability(
            spec.get("initial", data, DataValue::asFloat),
            spec.get("rules", data, v -> getRules(v, context)),
            spec.get("amounts", data, v -> getWeights(v, context))
         )
      )
      .add("initial", 1.0F, SumViability::initial)
      .addList("rules", SumViability::getRulesList)
      .addList("amounts", SumViability::getWeightList)
      .build();

   @Override
   public float getFitness(int x, int z, Viability.Context context) {
      float f = this.initial;

      for (int i = 0; i < this.rules.length; i++) {
         float f1 = this.rules[i].getFitness(x, z, context);
         float f2 = this.amounts[i];
         f += f1 * f2;
      }

      return NoiseUtil.clamp(f, 0.0F, 1.0F);
   }

   private List<Viability> getRulesList() {
      return List.of(this.rules);
   }

   private List<Float> getWeightList() {
      return new FloatArrayList(this.amounts);
   }

   public static Viability[] getRules(DataValue value, com.terraforged.cereal.spec.Context context) {
      return Cereal.deserialize(value.asList(), Viability.class, context).toArray(Viability[]::new);
   }

   public static float[] getWeights(DataValue value, com.terraforged.cereal.spec.Context context) {
      DataList datalist = value.asList();
      float[] afloat = new float[datalist.size()];

      for (int i = 0; i < afloat.length; i++) {
         afloat[i] = datalist.get(i).asFloat();
      }

      return afloat;
   }

   public static SumViability.Builder builder(float initial) {
      return new SumViability.Builder(initial);
   }

   public static class Builder {
      private final float initial;
      private final List<Viability> viabilities = new ArrayList<>();
      private final FloatList weights = new FloatArrayList();

      public Builder(float initial) {
         this.initial = initial;
      }

      public SumViability.Builder with(float weight, Viability viability) {
         this.viabilities.add(viability);
         this.weights.add(weight);
         return this;
      }

      public SumViability build() {
         return new SumViability(this.initial, this.viabilities.toArray(new Viability[0]), this.weights.toFloatArray());
      }
   }
}
