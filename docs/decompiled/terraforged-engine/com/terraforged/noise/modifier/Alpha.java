/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;

public class Alpha
extends Modifier {
    private final Module alpha;
    private static final DataFactory<Alpha> factory = (data, spec, context) -> new Alpha(spec.get("source", data, Module.class, context), spec.get("alpha", data, Module.class, context));

    public Alpha(Module source, Module alpha) {
        super(source);
        this.alpha = alpha;
    }

    @Override
    public String getSpecName() {
        return "Alpha";
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float a = this.alpha.getValue(x, y);
        return noiseValue * a + (1.0f - a);
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
        Alpha alpha1 = (Alpha)o;
        return this.alpha.equals(alpha1.alpha);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.alpha.hashCode();
        return result;
    }

    public static DataSpec<Alpha> spec() {
        return Modifier.sourceBuilder(Alpha.class, factory).addObj("alpha", Module.class, a -> a.alpha).build();
    }
}

