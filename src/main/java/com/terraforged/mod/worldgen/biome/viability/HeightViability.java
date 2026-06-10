package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;

public record HeightViability(float minOffset, float midOffset, float maxOffset) implements Viability
{
    public static final DataSpec<HeightViability> SPEC = DataSpec.builder("Height", HeightViability.class, (data, spec, context) -> new HeightViability(spec.get("min", data, DataValue::asFloat).floatValue(), spec.get("mid", data, DataValue::asFloat).floatValue(), spec.get("max", data, DataValue::asFloat).floatValue())).add("min", Float.valueOf(0.0f), HeightViability::minOffset).add("mid", Float.valueOf(0.5f), HeightViability::midOffset).add("max", Float.valueOf(1.0f), HeightViability::maxOffset).build();

    @Override
    public float getFitness(int x, int z, Viability.Context context) {
        int base = context.getTerrain().getBaseHeight(x, z);
        int height = context.getTerrain().getHeight(x, z);
        TerrainLevels levels = context.getLevels();
        float scale = this.getScaler(levels);
        float min = (float)base + this.minOffset() * scale;
        float mid = (float)base + this.midOffset() * scale;
        float max = (float)base + this.maxOffset() * scale;
        if ((float)height < min) {
            return 1.0f;
        }
        if ((float)height > max) {
            return 1.0f;
        }
        if ((float)height < mid) {
            return (mid - (float)height) / (mid - min);
        }
        return ((float)height - mid) / (max - mid);
    }
}
