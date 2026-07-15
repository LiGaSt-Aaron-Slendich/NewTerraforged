/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.selector;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.selector.Selector;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;

public class MultiBlend
extends Selector {
    private final Node[] nodes;
    private final int maxIndex;
    private final float blend;
    private final float blendRange;
    private static final DataFactory<MultiBlend> factory = (data, spec, context) -> new MultiBlend(spec.get("blend_range", data, DataValue::asInt).intValue(), spec.get("interp", data, v -> v.asEnum(Interpolation.class)), spec.get("control", data, Module.class, context), Cereal.deserialize(data.getList("modules"), Module.class, context).toArray(new Module[0]));

    public MultiBlend(float blend, Interpolation interpolation, Module control, Module ... sources) {
        super(control, sources, interpolation);
        float spacing = 1.0f / (float)sources.length;
        float radius = spacing / 2.0f;
        float blendRange = radius * blend;
        float cellRadius = (radius - blendRange) / 2.0f;
        this.blend = blend;
        this.nodes = new Node[sources.length];
        this.maxIndex = sources.length - 1;
        this.blendRange = blendRange;
        for (int i = 0; i < sources.length; ++i) {
            float pos = (float)i * spacing + radius;
            float min = i == 0 ? 0.0f : pos - cellRadius;
            float max = i == this.maxIndex ? 1.0f : pos + cellRadius;
            this.nodes[i] = new Node(sources[i], min, max);
        }
    }

    @Override
    public String getSpecName() {
        return "MultiBlend";
    }

    @Override
    public float selectValue(float x, float y, float selector) {
        Node min;
        int index = NoiseUtil.round(selector * (float)this.maxIndex);
        Node max = min = this.nodes[index];
        if (this.blendRange == 0.0f) {
            return min.source.getValue(x, y);
        }
        if (selector > min.max) {
            max = this.nodes[index + 1];
        } else if (selector < min.min) {
            min = this.nodes[index - 1];
        } else {
            return min.source.getValue(x, y);
        }
        float alpha = (selector - min.max) / this.blendRange;
        alpha = NoiseUtil.clamp(alpha, 0.0f, 1.0f);
        return this.blendValues(min.source.getValue(x, y), max.source.getValue(x, y), alpha);
    }

    public static DataSpec<MultiBlend> spec() {
        return DataSpec.builder(MultiBlend.class, factory).add("blend_range", (Object)0, m -> Float.valueOf(m.blend)).add("interp", (Object)Interpolation.LINEAR, m -> m.interpolation).addObj("control", Module.class, m -> m.selector).addList("modules", Module.class, m -> Arrays.asList(m.sources)).build();
    }

    private static class Node {
        private final Module source;
        private final float min;
        private final float max;

        private Node(Module source, float min, float max) {
            this.source = source;
            this.min = Math.max(0.0f, min);
            this.max = Math.min(1.0f, max);
        }

        public String toString() {
            return "Slot{min=" + this.min + ", max=" + this.max + '}';
        }
    }
}

