/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.selector;

import com.terraforged.noise.Module;
import com.terraforged.noise.combiner.Combiner;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.util.NoiseUtil;

public abstract class Selector
extends Combiner {
    protected final Module selector;
    protected final Interpolation interpolation;

    public Selector(Module control, Module[] sources, Interpolation interpolation) {
        super(sources);
        this.selector = control;
        this.interpolation = interpolation;
    }

    @Override
    public float getValue(float x, float y) {
        float select = this.selector.getValue(x, y);
        return this.selectValue(x, y, select);
    }

    @Override
    protected float minTotal(float result, Module next) {
        return Math.min(result, next.minValue());
    }

    @Override
    protected float maxTotal(float result, Module next) {
        return Math.max(result, next.maxValue());
    }

    @Override
    protected float combine(float result, float value) {
        return 0.0f;
    }

    protected float blendValues(float lower, float upper, float alpha) {
        alpha = this.interpolation.apply(alpha);
        return NoiseUtil.lerp(lower, upper, alpha);
    }

    protected abstract float selectValue(float var1, float var2, float var3);
}

