/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.modifier;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.Module;
import com.terraforged.noise.modifier.Modifier;
import com.terraforged.noise.util.NoiseUtil;

public class Modulate
extends Modifier {
    private final Module direction;
    private final Module strength;
    private static final DataFactory<Modulate> factory = (data, spec, context) -> new Modulate(spec.get("source", data, Module.class, context), spec.get("direction", data, Module.class, context), spec.get("strength", data, Module.class, context));

    public Modulate(Module source, Module direction, Module strength) {
        super(source);
        this.direction = direction;
        this.strength = strength;
    }

    @Override
    public String getSpecName() {
        return "Modulate";
    }

    @Override
    public float getValue(float x, float y) {
        float angle = this.direction.getValue(x, y) * ((float)Math.PI * 2);
        float strength = this.strength.getValue(x, y);
        float dx = NoiseUtil.sin(angle) * strength;
        float dy = NoiseUtil.cos(angle) * strength;
        return this.source.getValue(x + dx, y + dy);
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        return 0.0f;
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
        Modulate modulate = (Modulate)o;
        if (!this.direction.equals(modulate.direction)) {
            return false;
        }
        return this.strength.equals(modulate.strength);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.direction.hashCode();
        result = 31 * result + this.strength.hashCode();
        return result;
    }

    public static DataSpec<Modulate> spec() {
        return Modifier.sourceBuilder(Modulate.class, factory).addObj("direction", Module.class, m -> m.direction).addObj("strength", Module.class, m -> m.strength).build();
    }
}

