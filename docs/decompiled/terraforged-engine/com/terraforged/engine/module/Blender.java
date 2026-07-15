/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.module;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.cell.Populator;
import com.terraforged.engine.module.Select;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.util.NoiseUtil;

public class Blender
extends Select
implements Populator {
    private final Populator lower;
    private final Populator upper;
    private final float blendLower;
    private final float blendUpper;
    private final float blendRange;
    private final float midpoint;
    private final float tagThreshold;

    public Blender(Module control, Populator lower, Populator upper, float min, float max, float split) {
        super(control);
        this.lower = lower;
        this.upper = upper;
        this.blendLower = min;
        this.blendUpper = max;
        this.blendRange = this.blendUpper - this.blendLower;
        this.tagThreshold = this.midpoint = this.blendLower + this.blendRange * split;
    }

    public Blender(Populator control, Populator lower, Populator upper, float min, float max, float split, float tagThreshold) {
        super(control);
        this.lower = lower;
        this.upper = upper;
        this.blendLower = min;
        this.blendUpper = max;
        this.blendRange = this.blendUpper - this.blendLower;
        this.midpoint = this.blendLower + this.blendRange * split;
        this.tagThreshold = tagThreshold;
    }

    @Override
    public void apply(Cell cell, float x, float y) {
        float select = this.getSelect(cell, x, y);
        if (select < this.blendLower) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (select > this.blendUpper) {
            this.upper.apply(cell, x, y);
            return;
        }
        float alpha = Interpolation.LINEAR.apply((select - this.blendLower) / this.blendRange);
        this.lower.apply(cell, x, y);
        float lowerVal = cell.value;
        Terrain lowerType = cell.terrain;
        this.upper.apply(cell, x, y);
        float upperVal = cell.value;
        cell.value = NoiseUtil.lerp(lowerVal, upperVal, alpha);
        if (select < this.midpoint) {
            cell.terrain = lowerType;
        }
    }
}

