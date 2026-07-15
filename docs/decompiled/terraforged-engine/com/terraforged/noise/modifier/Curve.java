/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.func.MidPointCurve;
import com.terraforged.noise.modifier.Modifier;

public class Curve
extends Modifier {
    private final CurveFunc func;
    private static final DataFactory<Curve> factory = (data, spec, context) -> new Curve(spec.get("source", data, Module.class, context), spec.get("curve", data, CurveFunc.class, context));

    public Curve(Module source, CurveFunc func) {
        super(source);
        this.func = func;
    }

    public Curve(Module source, float mid, float steepness) {
        this(source, new MidPointCurve(mid, steepness));
    }

    @Override
    public String getSpecName() {
        return "Curve";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return this.func.apply(noiseValue);
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
        Curve curve = (Curve)o;
        return this.func.equals(curve.func);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.func.hashCode();
        return result;
    }

    public static DataSpec<Curve> spec() {
        return Curve.sourceBuilder(Curve.class, factory).addObj("curve", CurveFunc.class, c -> c.func).build();
    }
}

