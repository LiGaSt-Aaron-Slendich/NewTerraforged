package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.mod.worldgen.biome.viability.SumViability;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import java.util.List;

public record MultViability(Viability[] rules) implements Viability
{
    public static final DataSpec<MultViability> SPEC = DataSpec.builder("Multiply", MultViability.class, (data, spec, context) -> new MultViability(spec.get("rules", data, v -> SumViability.getRules(v, context)))).addList("rules", MultViability::getRulesList).build();

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        float value = 1.0f;
        for (int i = 0; i < this.rules.length; ++i) {
            value *= this.rules[i].getFitness(x, z, context);
        }
        return value;
    }

    private List<Viability> getRulesList() {
        return List.of(this.rules);
    }
}
