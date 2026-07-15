/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class Power
extends Modifier {
    private final Module n;
    public static final DataFactory<Power> factory = (data, spec, context) -> new Power(spec.get("source", data, Module.class, context), spec.get("power", data, Module.class, context));

    public Power(Module source, Module n) {
        super(source);
        this.n = n;
    }

    @Override
    public String getSpecName() {
        return "Pow";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return NoiseUtil.pow(noiseValue, this.n.getValue(x, y));
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
        Power power = (Power)o;
        return this.n.equals(power.n);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.n.hashCode();
        return result;
    }

    public static DataSpec<Power> spec() {
        return Power.sourceBuilder("Pow", Power.class, factory).addObj("power", Module.class, p -> p.n).build();
    }
}

