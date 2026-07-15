/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.func;

import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.util.NoiseUtil;
import java.util.function.Function;

public enum Interpolation implements CurveFunc
{
    LINEAR{

        @Override
        public float apply(float f) {
            return f;
        }
    }
    ,
    CURVE3{

        @Override
        public float apply(float f) {
            return NoiseUtil.interpHermite(f);
        }
    }
    ,
    CURVE4{

        @Override
        public float apply(float f) {
            return NoiseUtil.interpQuintic(f);
        }
    };

    private static final DataFactory<Interpolation> factory;

    @Override
    public String getSpecName() {
        return "Interpolation";
    }

    @Override
    public abstract float apply(float var1);

    public static DataSpec<Interpolation> spec() {
        return DataSpec.builder("Interpolation", Interpolation.class, factory).add("mode", (Object)LINEAR, Function.identity()).build();
    }

    static {
        factory = (data, spec, context) -> spec.getEnum("mode", data, Interpolation.class);
    }
}

