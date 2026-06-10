package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.biome.viability.Viability;

public record SlopeViability(float normalize, float max) implements Viability
{
    public static final DataSpec<SlopeViability> SPEC = DataSpec.builder("Slope", SlopeViability.class, (data, spec, context) -> new SlopeViability(spec.get("normalize", data, DataValue::asFloat).floatValue(), spec.get("max", data, DataValue::asFloat).floatValue())).add("normalize", Float.valueOf(1.0f), SlopeViability::normalize).add("max", Float.valueOf(1.0f), SlopeViability::max).build();

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        float norm = this.normalize * this.getScaler(context.getLevels());
        float gradient = context.getTerrain().getGradient(x, z, norm);
        if (gradient >= this.max) {
            return 1.0f;
        }
        return gradient / this.max;
    }
}
