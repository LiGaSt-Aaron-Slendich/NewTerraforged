/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.combiner;

import com.terraforged.cereal.Cereal;
import com.terraforged.cereal.spec.DataFactory;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataList;
import com.terraforged.noise.Module;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class Combiner
implements Module {
    private final float min;
    private final float max;
    protected final Module[] sources;

    public Combiner(Module ... sources) {
        float min = 0.0f;
        float max = 0.0f;
        if (sources.length > 0) {
            min = sources[0].minValue();
            max = sources[0].maxValue();
            for (int i = 1; i < sources.length; ++i) {
                Module next = sources[i];
                min = this.minTotal(min, next);
                max = this.maxTotal(max, next);
            }
        }
        this.min = min;
        this.max = max;
        this.sources = sources;
    }

    @Override
    public float getValue(float x, float y) {
        float result = 0.0f;
        if (this.sources.length > 0) {
            result = this.sources[0].getValue(x, y);
            for (int i = 1; i < this.sources.length; ++i) {
                Module module = this.sources[i];
                float value = module.getValue(x, y);
                result = this.combine(result, value);
            }
        }
        return result;
    }

    @Override
    public float minValue() {
        return this.min;
    }

    @Override
    public float maxValue() {
        return this.max;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Combiner combiner = (Combiner)o;
        if (Float.compare(combiner.min, this.min) != 0) {
            return false;
        }
        if (Float.compare(combiner.max, this.max) != 0) {
            return false;
        }
        return Arrays.equals(this.sources, combiner.sources);
    }

    public int hashCode() {
        int result = this.min != 0.0f ? Float.floatToIntBits(this.min) : 0;
        result = 31 * result + (this.max != 0.0f ? Float.floatToIntBits(this.max) : 0);
        result = 31 * result + Arrays.hashCode(this.sources);
        return result;
    }

    protected abstract float minTotal(float var1, Module var2);

    protected abstract float maxTotal(float var1, Module var2);

    protected abstract float combine(float var1, float var2);

    private static DataFactory<Combiner> constructor(Function<Module[], Combiner> constructor) {
        return (data, spec, context) -> {
            DataList list = data.getList("modules");
            List<Module> modules = Cereal.deserialize(list, Module.class);
            return (Combiner)constructor.apply(modules.toArray(new Module[0]));
        };
    }

    public static DataSpec<Combiner> spec(String name, Function<Module[], Combiner> constructor) {
        return DataSpec.builder(name, Combiner.class, Combiner.constructor(constructor)).addList("modules", Module.class, c -> Arrays.asList(c.sources)).build();
    }
}

