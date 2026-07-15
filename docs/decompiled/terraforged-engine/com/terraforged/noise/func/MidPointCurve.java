/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.func;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.util.NoiseUtil;

public class MidPointCurve
implements CurveFunc {
    private final float mid;
    private final float steepness;
    private static final DataFactory<MidPointCurve> factory = (data, spec, context) -> new MidPointCurve(spec.get("midpoint", data, DataValue::asFloat).floatValue(), spec.get("steepness", data, DataValue::asFloat).floatValue());

    public MidPointCurve(float mid, float steepness) {
        this.mid = mid;
        this.steepness = steepness;
    }

    @Override
    public String getSpecName() {
        return "MidCurve";
    }

    @Override
    public float apply(float value) {
        return NoiseUtil.curve(value, this.mid, this.steepness);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MidPointCurve that = (MidPointCurve)o;
        if (Float.compare(that.mid, this.mid) != 0) {
            return false;
        }
        return Float.compare(that.steepness, this.steepness) == 0;
    }

    public int hashCode() {
        int result = this.mid != 0.0f ? Float.floatToIntBits(this.mid) : 0;
        result = 31 * result + (this.steepness != 0.0f ? Float.floatToIntBits(this.steepness) : 0);
        return result;
    }

    public static DataSpec<MidPointCurve> spec() {
        return DataSpec.builder("MidCurve", MidPointCurve.class, factory).add("midpoint", (Object)Float.valueOf(0.5f), m -> Float.valueOf(m.mid)).add("steepness", (Object)4, m -> Float.valueOf(m.steepness)).build();
    }
}

