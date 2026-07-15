/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class PowerCurve
extends Modifier {
    private final float min;
    private final float max;
    private final float mid;
    private final float range;
    private final float power;
    private static final DataFactory<PowerCurve> factory = (data, spec, context) -> new PowerCurve(spec.get("source", data, Module.class, context), spec.get("power", data, DataValue::asFloat).floatValue());

    public PowerCurve(Module source, float power) {
        super(source);
        float min = source.minValue();
        float max = source.maxValue();
        float mid = min + (max - min) / 2.0f;
        this.power = power;
        this.min = mid - NoiseUtil.pow(mid - source.minValue(), power);
        this.max = mid + NoiseUtil.pow(source.maxValue() - mid, power);
        this.range = this.max - this.min;
        this.mid = this.min + this.range / 2.0f;
    }

    @Override
    public String getSpecName() {
        return "PowCurve";
    }

    @Override
    public float modify(float x, float y, float value) {
        if (value >= this.mid) {
            float part = value - this.mid;
            value = this.mid + NoiseUtil.pow(part, this.power);
        } else {
            float part = this.mid - value;
            value = this.mid - NoiseUtil.pow(part, this.power);
        }
        return NoiseUtil.map(value, this.min, this.max, this.range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PowerCurve that = (PowerCurve)o;
        if (Float.compare(that.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(that.max, this.max) != 0) {
            return false;
        }
        if (Float.compare(that.mid, this.mid) != 0) {
            return false;
        }
        if (Float.compare(that.range, this.range) != 0) {
            return false;
        }
        return Float.compare(that.power, this.power) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.min != 0.0f ? Float.floatToIntBits(this.min) : 0);
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        result = 31 * result + (this.mid != 0.0f ? Float.floatToIntBits(this.mid) : 0);
        result = 31 * result + (this.range != 0.0f ? Float.floatToIntBits(this.range) : 0);
        result = 31 * result + (this.power != 0.0f ? Float.floatToIntBits(this.power) : 0);
        return result;
    }

    public static DataSpec<PowerCurve> spec() {
        return Modifier.sourceBuilder("PowCurve", PowerCurve.class, factory).add("power", (Object)Float.valueOf(1.0f), p -> Float.valueOf(p.power)).build();
    }
}

