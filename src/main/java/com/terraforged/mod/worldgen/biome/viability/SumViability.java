package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataList;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.ArrayList;
import java.util.List;

public record SumViability(float initial, Viability[] rules, float[] amounts) implements Viability
{
    public static final DataSpec<SumViability> SPEC = DataSpec.builder("Sum", SumViability.class, (data, spec, context) -> new SumViability(spec.get("initial", data, DataValue::asFloat).floatValue(), spec.get("rules", data, v -> SumViability.getRules(v, context)), spec.get("amounts", data, v -> SumViability.getWeights(v, context)))).add("initial", Float.valueOf(1.0f), SumViability::initial).addList("rules", SumViability::getRulesList).addList("amounts", SumViability::getWeightList).build();

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        float sumValue = this.initial;
        for (int i = 0; i < this.rules.length; ++i) {
            float value = this.rules[i].getFitness(x, z, context);
            float weight = this.amounts[i];
            sumValue += value * weight;
        }
        return NoiseUtil.clamp(sumValue, 0.0f, 1.0f);
    }

    private List<Viability> getRulesList() {
        return List.of(this.rules);
    }

    private List<Float> getWeightList() {
        return new FloatArrayList(this.amounts);
    }

    public static Viability[] getRules(DataValue value, com.terraforged.cereal.spec.Context context) {
        return (Viability[])Cereal.deserialize(value.asList(), Viability.class, context).toArray(Viability[]::new);
    }

    public static float[] getWeights(DataValue value, com.terraforged.cereal.spec.Context context) {
        DataList list = value.asList();
        float[] weights = new float[list.size()];
        for (int i = 0; i < weights.length; ++i) {
            weights[i] = list.get(i).asFloat();
        }
        return weights;
    }

    public static Builder builder(float initial) {
        return new Builder(initial);
    }

    public static class Builder {
        private final float initial;
        private final List<Viability> viabilities = new ArrayList<Viability>();
        private final FloatList weights = new FloatArrayList();

        public Builder(float initial) {
            this.initial = initial;
        }

        public Builder with(float weight, Viability viability) {
            this.viabilities.add(viability);
            this.weights.add(weight);
            return this;
        }

        public SumViability build() {
            return new SumViability(this.initial, this.viabilities.toArray(new Viability[0]), this.weights.toFloatArray());
        }
    }
}
