/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise;

import com.terraforged.cereal.spec.SpecName;
import com.terraforged.noise.Noise;
import com.terraforged.noise.Source;
import com.terraforged.noise.combiner.Add;
import com.terraforged.noise.combiner.Max;
import com.terraforged.noise.combiner.Min;
import com.terraforged.noise.combiner.Multiply;
import com.terraforged.noise.combiner.Sub;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.func.CurveFunc;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.func.MidPointCurve;
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
import com.terraforged.noise.source.NoiseSource;

public interface Module
extends Noise,
SpecName {
    @Override
    default public String getSpecName() {
        return "";
    }

    default public Module abs() {
        if (this instanceof Abs) {
            return this;
        }
        return new Abs(this);
    }

    default public Module add(Module other) {
        return new Add(this, other);
    }

    default public Module alpha(double alpha) {
        return this.alpha(Source.constant(alpha));
    }

    default public Module alpha(Module alpha) {
        if (alpha.minValue() < 0.0f || alpha.maxValue() > 1.0f) {
            return this;
        }
        return new Alpha(this, alpha);
    }

    default public Module base(Module other, double falloff) {
        return this.base(other, falloff, Interpolation.CURVE3);
    }

    default public Module base(Module other, double falloff, Interpolation interpolation) {
        return new Base(this, other, (float)falloff, interpolation);
    }

    default public Module bias(Module bias) {
        if (bias.minValue() == 0.0f && bias.maxValue() == 0.0f) {
            return this;
        }
        return new Bias(this, bias);
    }

    default public Module bias(double bias) {
        return this.bias(Source.constant(bias));
    }

    default public Module blend(Module source0, Module source1, double midpoint, double blendRange) {
        return this.blend(source0, source1, midpoint, blendRange, Interpolation.LINEAR);
    }

    default public Module blend(Module source0, Module source1, double midpoint, double blendRange, Interpolation interpolation) {
        return new Blend(this, source0, source1, (float)midpoint, (float)blendRange, interpolation);
    }

    default public Module blendVar(Module variable, Module source0, Module source1, double midpoint, double min, double max, Interpolation interpolation) {
        return new VariableBlend(this, variable, source0, source1, (float)midpoint, (float)min, (float)max, interpolation);
    }

    default public Module boost() {
        return this.boost(1);
    }

    default public Module boost(int iterations) {
        if (iterations < 1) {
            return this;
        }
        return new Boost(this, iterations);
    }

    default public Module cache() {
        if (this instanceof Cache) {
            return this;
        }
        return new Cache(this);
    }

    default public Module clamp(Module min, Module max) {
        if (min.minValue() == min.maxValue() && min.minValue() == this.minValue() && max.minValue() == max.maxValue() && max.maxValue() == this.maxValue()) {
            return this;
        }
        return new Clamp(this, min, max);
    }

    default public Module clamp(double min, double max) {
        return this.clamp(Source.constant(min), Source.constant(max));
    }

    default public Module curve(CurveFunc func) {
        return new Curve(this, func);
    }

    default public Module curve(double mid, double steepness) {
        return new Curve(this, new MidPointCurve((float)mid, (float)steepness));
    }

    default public Module freq(double x, double y) {
        return this.freq(Source.constant(x), Source.constant(y));
    }

    default public Module freq(Module x, Module y) {
        return new Freq(this, x, y);
    }

    default public Module curve(Module mid, Module steepness) {
        return new VariableCurve(this, mid, steepness);
    }

    default public Module grad(double lower, double upper, double strength) {
        return this.grad(Source.constant(lower), Source.constant(upper), Source.constant(strength));
    }

    default public Module grad(Module lower, Module upper, Module strength) {
        return new Grad(this, lower, upper, strength);
    }

    default public Module invert() {
        return new Invert(this);
    }

    default public Module map(Module min, Module max) {
        if (min.minValue() == min.maxValue() && min.minValue() == this.minValue() && max.minValue() == max.maxValue() && max.maxValue() == this.maxValue()) {
            return this;
        }
        return new Map(this, min, max);
    }

    default public Module map(double min, double max) {
        return this.map(Source.constant(min), Source.constant(max));
    }

    default public Module max(Module other) {
        return new Max(this, other);
    }

    default public Module min(Module other) {
        return new Min(this, other);
    }

    default public Module mod(Module direction, Module strength) {
        return new Modulate(this, direction, strength);
    }

    default public Module mult(Module other) {
        if (other.minValue() == 1.0f && other.maxValue() == 1.0f) {
            return this;
        }
        return new Multiply(this, other);
    }

    default public Module blend(double blend, Module ... sources) {
        return this.blend(blend, Interpolation.LINEAR, sources);
    }

    default public Module blend(double blend, Interpolation interpolation, Module ... sources) {
        return new MultiBlend((float)blend, interpolation, this, sources);
    }

    default public Module pow(Module n) {
        if (n.minValue() == 0.0f && n.maxValue() == 0.0f) {
            return Source.ONE;
        }
        if (n.minValue() == 1.0f && n.maxValue() == 1.0f) {
            return this;
        }
        return new Power(this, n);
    }

    default public Module pow(double n) {
        return this.pow(Source.constant(n));
    }

    default public Module powCurve(double n) {
        return new PowerCurve(this, (float)n);
    }

    default public Module scale(Module scale) {
        if (scale.minValue() == 1.0f && scale.maxValue() == 1.0f) {
            return this;
        }
        return new Scale(this, scale);
    }

    default public Module scale(double scale) {
        return this.scale(Source.constant(scale));
    }

    default public Module select(Module lower, Module upper, double lowerBound, double upperBound, double falloff) {
        return this.select(lower, upper, lowerBound, upperBound, falloff, Interpolation.CURVE3);
    }

    default public Module select(Module lower, Module upper, double lowerBound, double upperBound, double falloff, Interpolation interpolation) {
        return new Select(this, lower, upper, (float)lowerBound, (float)upperBound, (float)falloff, interpolation);
    }

    default public Module steps(int steps) {
        return this.steps(steps, 0.0, 0.0);
    }

    default public Module steps(int steps, double slopeMin, double slopeMax) {
        return this.steps(steps, slopeMin, slopeMax, (CurveFunc)Interpolation.LINEAR);
    }

    default public Module steps(int steps, double slopeMin, double slopeMax, CurveFunc curveFunc) {
        return this.steps(Source.constant(steps), Source.constant(slopeMin), Source.constant(slopeMax), curveFunc);
    }

    default public Module steps(Module steps, Module slopeMin, Module slopeMax) {
        return this.steps(steps, slopeMin, slopeMax, (CurveFunc)Interpolation.LINEAR);
    }

    default public Module steps(Module steps, Module slopeMin, Module slopeMax, CurveFunc curveFunc) {
        return new Steps(this, steps, slopeMin, slopeMax, curveFunc);
    }

    default public Module sub(Module other) {
        return new Sub(this, other);
    }

    default public Module legacyTerrace(double lowerCurve, double upperCurve, int steps, double blendRange) {
        return this.legacyTerrace(Source.constant(lowerCurve), Source.constant(upperCurve), steps, blendRange);
    }

    default public Module legacyTerrace(Module lowerCurve, Module upperCurve, int steps, double blendRange) {
        return new LegacyTerrace(this, lowerCurve, upperCurve, steps, (float)blendRange);
    }

    default public Module terrace(double lowerCurve, double upperCurve, double lowerHeight, int steps, double blendRange) {
        return this.terrace(Source.constant(lowerCurve), Source.constant(upperCurve), Source.constant(lowerHeight), steps, blendRange);
    }

    default public Module terrace(Module lowerCurve, Module upperCurve, Module lowerHeight, int steps, double blendRange) {
        return new Terrace(this, lowerCurve, upperCurve, lowerHeight, steps, (float)blendRange);
    }

    default public Module terrace(Module modulation, double slope, double blendMin, double blendMax, int steps) {
        return this.terrace(modulation, Source.ONE, slope, blendMin, blendMax, steps);
    }

    default public Module terrace(Module modulation, double slope, double blendMin, double blendMax, int steps, int octaves) {
        return this.terrace(modulation, Source.ONE, Source.constant(slope), blendMin, blendMax, steps, octaves);
    }

    default public Module terrace(Module modulation, Module mask, double slope, double blendMin, double blendMax, int steps) {
        return this.terrace(modulation, mask, Source.constant(slope), blendMin, blendMax, steps, 1);
    }

    default public Module terrace(Module modulation, Module mask, Module slope, double blendMin, double blendMax, int steps) {
        return new AdvancedTerrace(this, modulation, mask, slope, (float)blendMin, (float)blendMax, steps, 1);
    }

    default public Module terrace(Module modulation, Module mask, Module slope, double blendMin, double blendMax, int steps, int octaves) {
        return new AdvancedTerrace(this, modulation, mask, slope, (float)blendMin, (float)blendMax, steps, octaves);
    }

    default public Module threshold(double threshold) {
        return new Threshold(this, Source.constant(threshold));
    }

    default public Module threshold(Module threshold) {
        return new Threshold(this, threshold);
    }

    default public Module warp(Domain domain) {
        return new Warp(this, domain);
    }

    default public Module warp(Module warpX, Module warpZ, double power) {
        return this.warp(warpX, warpZ, Source.constant(power));
    }

    default public Module warp(Module warpX, Module warpZ, Module power) {
        return this.warp(Domain.warp(warpX, warpZ, power));
    }

    default public Module warp(int seed, int scale, int octaves, double power) {
        return this.warp(Source.PERLIN, seed, scale, octaves, power);
    }

    default public Module warp(Source source, int seed, int scale, int octaves, double power) {
        Module x = Source.build(seed, scale, octaves).build(source);
        Module y = Source.build(seed + 1, scale, octaves).build(source);
        Module p = Source.constant(power);
        return this.warp(x, y, p);
    }

    default public Module warp(Class<? extends NoiseSource> source, int seed, int scale, int octaves, double power) {
        Module x = Source.build(seed, scale, octaves).build(source);
        Module y = Source.build(seed + 1, scale, octaves).build(source);
        Module p = Source.constant(power);
        return this.warp(x, y, p);
    }
}

