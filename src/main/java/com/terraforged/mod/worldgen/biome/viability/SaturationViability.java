package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.biome.viability.Viability;

public record SaturationViability(float min, float max) implements Viability
{
    public static final DataSpec<SaturationViability> SPEC = DataSpec.builder("Saturation", SaturationViability.class, (data, spec, context) -> new SaturationViability(spec.get("min", data, DataValue::asFloat).floatValue(), spec.get("max", data, DataValue::asFloat).floatValue())).add("min", Float.valueOf(0.0f), SaturationViability::min).add("max", Float.valueOf(1.0f), SaturationViability::max).build();

    public SaturationViability(float max) {
        this(0.0f, max);
    }

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        float saturation = 1.0f - context.getTerrain().getRiver().get(x, z);
        if (saturation < this.min) {
            return 0.0f;
        }
        if (saturation > this.max) {
            return 1.0f;
        }
        return (saturation - this.min) / (this.max - this.min);
    }
}
