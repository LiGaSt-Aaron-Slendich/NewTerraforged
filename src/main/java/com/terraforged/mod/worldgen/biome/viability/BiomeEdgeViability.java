package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.biome.viability.Viability;

public record BiomeEdgeViability(float distance) implements Viability
{
    public static final DataSpec<BiomeEdgeViability> SPEC = DataSpec.builder(BiomeEdgeViability.class, (data, spec, context) -> new BiomeEdgeViability(spec.get("distance", data, DataValue::asFloat).floatValue())).add("distance", Float.valueOf(1.0f), BiomeEdgeViability::distance).build();

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        if (context.edge()) {
            float edge = context.getClimateSampler().getShape(context.seed(), x, z);
            if (edge > this.distance) {
                return 0.0f;
            }
            return 1.0f - edge / this.distance;
        }
        return 0.0f;
    }
}
