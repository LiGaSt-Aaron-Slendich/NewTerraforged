/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.selector;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.selector.Selector;

public class VariableBlend
extends Selector {
    private final Module source0;
    private final Module source1;
    private final Module variator;
    private final float midpoint;
    private final float maxBlend;
    private final float minBlend;
    private static final DataFactory<VariableBlend> factory = (data, spec, context) -> new VariableBlend(spec.get("control", data, Module.class, context), spec.get("variator", data, Module.class, context), spec.get("lower", data, Module.class, context), spec.get("upper", data, Module.class, context), spec.get("midpoint", data, DataValue::asFloat).floatValue(), spec.get("blend_min", data, DataValue::asFloat).floatValue(), spec.get("blend_max", data, DataValue::asFloat).floatValue(), spec.get("interp", data, v -> v.asEnum(Interpolation.class)));

    public VariableBlend(Module control, Module variator, Module source0, Module source1, float midpoint, float minBlend, float maxBlend, Interpolation interpolation) {
        super(control, new Module[]{source0, source1}, interpolation);
        this.source0 = source0;
        this.source1 = source1;
        this.midpoint = midpoint;
        this.maxBlend = maxBlend;
        this.minBlend = minBlend;
        this.variator = variator;
    }

    @Override
    public String getSpecName() {
        return "VariBlend";
    }

    @Override
    protected float selectValue(float x, float y, float selector) {
        float radius = this.minBlend + this.variator.getValue(x, y) * this.maxBlend;
        float min = Math.max(0.0f, this.midpoint - radius);
        if (selector < min) {
            return this.source0.getValue(x, y);
        }
        float max = Math.min(1.0f, this.midpoint + radius);
        if (selector > max) {
            return this.source1.getValue(x, y);
        }
        float alpha = (selector - min) / (max - min);
        return this.blendValues(this.source0.getValue(x, y), this.source1.getValue(x, y), alpha);
    }

    public static DataSpec<VariableBlend> spec() {
        return DataSpec.builder("VariBlend", VariableBlend.class, factory).add("midpoint", (Object)Float.valueOf(0.5f), v -> Float.valueOf(v.midpoint)).add("blend_min", (Object)Float.valueOf(0.0f), v -> Float.valueOf(v.minBlend)).add("blend_max", (Object)Float.valueOf(1.0f), v -> Float.valueOf(v.maxBlend)).add("interp", (Object)Interpolation.LINEAR, v -> v.interpolation).addObj("control", Module.class, v -> v.selector).addObj("variator", Module.class, v -> v.variator).addObj("lower", Module.class, v -> v.source0).addObj("upper", Module.class, v -> v.source1).build();
    }
}

