/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.func;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.util.NoiseUtil;

public class SCurve
implements CurveFunc {
    private final float lower;
    private final float upper;
    private static final DataFactory<SCurve> factory = (data, spec, context) -> new SCurve(spec.get("lower", data, DataValue::asFloat).floatValue(), spec.get("upper", data, DataValue::asFloat).floatValue());

    public SCurve(float lower, float upper) {
        this.lower = lower;
        this.upper = upper < 0.0f ? Math.max(-lower, upper) : upper;
    }

    @Override
    public String getSpecName() {
        return "SCurve";
    }

    @Override
    public float apply(float value) {
        return NoiseUtil.pow(value, this.lower + this.upper * value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SCurve sCurve = (SCurve)o;
        if (Float.compare(sCurve.lower, this.lower) != 0) {
            return false;
        }
        return Float.compare(sCurve.upper, this.upper) == 0;
    }

    public int hashCode() {
        int result = this.lower != 0.0f ? Float.floatToIntBits(this.lower) : 0;
        result = 31 * result + (this.upper != 0.0f ? Float.floatToIntBits(this.upper) : 0);
        return result;
    }

    public static DataSpec<SCurve> spec() {
        return DataSpec.builder("SCurve", SCurve.class, factory).add("lower", (Object)0, s -> Float.valueOf(s.lower)).add("upper", (Object)1, s -> Float.valueOf(s.upper)).build();
    }
}

