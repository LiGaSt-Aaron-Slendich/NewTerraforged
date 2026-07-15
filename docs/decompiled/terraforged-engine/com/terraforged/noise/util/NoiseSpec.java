/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.util;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataAccessor;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.spec.DataSpecs;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.combiner.Add;
import com.terraforged.noise.combiner.Max;
import com.terraforged.noise.combiner.Min;
import com.terraforged.noise.combiner.Multiply;
import com.terraforged.noise.combiner.Sub;
import com.terraforged.noise.domain.AddWarp;
import com.terraforged.noise.domain.CacheWarp;
import com.terraforged.noise.domain.CompoundWarp;
import com.terraforged.noise.domain.CumulativeWarp;
import com.terraforged.noise.domain.DirectionWarp;
import com.terraforged.noise.domain.DomainWarp;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.func.MidPointCurve;
import com.terraforged.noise.func.SCurve;
import com.terraforged.noise.modifier.Abs;
import com.terraforged.noise.modifier.AdvancedTerrace;
import com.terraforged.noise.modifier.Alpha;
import com.terraforged.noise.modifier.Bias;
import com.terraforged.noise.modifier.Boost;
import com.terraforged.noise.modifier.Cache;
import com.terraforged.noise.modifier.Clamp;
import com.terraforged.noise.modifier.Curve;
import com.terraforged.noise.modifier.Freq;
import com.terraforged.noise.modifier.Grad;
import com.terraforged.noise.modifier.Invert;
import com.terraforged.noise.modifier.LegacyTerrace;
import com.terraforged.noise.modifier.Map;
import com.terraforged.noise.modifier.Modulate;
import com.terraforged.noise.modifier.Power;
import com.terraforged.noise.modifier.PowerCurve;
import com.terraforged.noise.modifier.Scale;
import com.terraforged.noise.modifier.Steps;
import com.terraforged.noise.modifier.Terrace;
import com.terraforged.noise.modifier.Threshold;
import com.terraforged.noise.modifier.VariableCurve;
import com.terraforged.noise.modifier.Warp;
import com.terraforged.noise.selector.Base;
import com.terraforged.noise.selector.Blend;
import com.terraforged.noise.selector.MultiBlend;
import com.terraforged.noise.selector.Select;
import com.terraforged.noise.selector.VariableBlend;
import com.terraforged.noise.source.BillowNoise;
import com.terraforged.noise.source.CellEdgeNoise;
import com.terraforged.noise.source.CellNoise;
import com.terraforged.noise.source.Constant;
import com.terraforged.noise.source.CubicNoise;
import com.terraforged.noise.source.Line;
import com.terraforged.noise.source.PerlinNoise;
import com.terraforged.noise.source.PerlinNoise2;
import com.terraforged.noise.source.Rand;
import com.terraforged.noise.source.RidgeNoise;
import com.terraforged.noise.source.SimplexNoise;
import com.terraforged.noise.source.SimplexNoise2;
import com.terraforged.noise.source.SimplexRidgeNoise;
import com.terraforged.noise.source.Sin;
import com.terraforged.noise.util.Vec2f;
import com.terraforged.noise.util.Vec2i;
import java.util.function.Function;

public class NoiseSpec {
    public static void init() {
    }

    public static int seed(Context context) {
        return context.getData().get("seed").asInt();
    }

    public static int seed(DataObject data, DataSpec<?> spec, Context context) {
        return spec.get("seed", data, DataValue::asInt) + NoiseSpec.seed(context);
    }

    public static <T> DataAccessor<T, Integer> seed(Function<T, Integer> getter) {
        return (t, context) -> (Integer)getter.apply(t) - NoiseSpec.seed(context);
    }

    static {
        DataSpecs.register(Constant.spec());
        DataSpecs.register(BillowNoise.billowSpec());
        DataSpecs.register(CellNoise.spec());
        DataSpecs.register(CellEdgeNoise.spec());
        DataSpecs.register(CubicNoise.spec());
        DataSpecs.register(PerlinNoise.spec());
        DataSpecs.register(PerlinNoise2.spec());
        DataSpecs.register(RidgeNoise.ridgeSpec());
        DataSpecs.register(SimplexNoise.spec());
        DataSpecs.register(SimplexNoise2.spec());
        DataSpecs.register(SimplexRidgeNoise.ridgeSpec());
        DataSpecs.register(Sin.spec());
        DataSpecs.register(Line.spec());
        DataSpecs.register(Rand.spec());
        DataSpecs.register(Add.spec());
        DataSpecs.register(Max.spec());
        DataSpecs.register(Min.spec());
        DataSpecs.register(Multiply.spec());
        DataSpecs.register(Sub.spec());
        DataSpecs.register(Abs.spec());
        DataSpecs.register(AdvancedTerrace.spec());
        DataSpecs.register(Alpha.spec());
        DataSpecs.register(Bias.spec());
        DataSpecs.register(Boost.spec());
        DataSpecs.register(Cache.spec());
        DataSpecs.register(Clamp.spec());
        DataSpecs.register(Curve.spec());
        DataSpecs.register(Freq.spec());
        DataSpecs.register(Grad.spec());
        DataSpecs.register(Invert.spec());
        DataSpecs.register(LegacyTerrace.spec());
        DataSpecs.register(Map.spec());
        DataSpecs.register(Modulate.spec());
        DataSpecs.register(Power.spec());
        DataSpecs.register(PowerCurve.spec());
        DataSpecs.register(Scale.spec());
        DataSpecs.register(Steps.spec());
        DataSpecs.register(Terrace.spec());
        DataSpecs.register(Threshold.spec());
        DataSpecs.register(VariableCurve.spec());
        DataSpecs.register(Warp.spec());
        DataSpecs.register(Base.spec());
        DataSpecs.register(Blend.spec());
        DataSpecs.register(MultiBlend.spec());
        DataSpecs.register(Select.spec());
        DataSpecs.register(VariableBlend.spec());
        DataSpecs.register(Interpolation.spec());
        DataSpecs.registerSub(CurveFunc.class, Interpolation.spec());
        DataSpecs.registerSub(CurveFunc.class, MidPointCurve.spec());
        DataSpecs.registerSub(CurveFunc.class, SCurve.spec());
        DataSpecs.register(CacheWarp.spec());
        DataSpecs.register(AddWarp.spec());
        DataSpecs.register(CompoundWarp.spec());
        DataSpecs.register(CumulativeWarp.spec());
        DataSpecs.register(DirectionWarp.spec());
        DataSpecs.register(DomainWarp.spec());
        DataSpecs.register(Vec2f.spec());
        DataSpecs.register(Vec2i.spec());
    }
}

