package com.terraforged.mod.util.storage;

import java.util.IdentityHashMap;
import java.util.Map;

public class Object2FloatCache<T> {
    protected final Value[] values;
    protected final Map<T, Value> map;

    public Object2FloatCache(int size) {
        this.values = new Value[size];
        this.map = new IdentityHashMap<T, Value>();
        for (int i = 0; i < this.values.length; ++i) {
            this.values[i] = new Value();
        }
    }

    public void clear() {
        this.map.clear();
    }

    public void put(T t, float value) {
        int index = this.map.size();
        Value holder = this.values[index];
        holder.value = value;
        this.map.put(t, holder);
    }

    public float get(T t) {
        Value holder = this.map.get(t);
        if (holder == null) {
            return Float.NaN;
        }
        return holder.value;
    }

    protected static class Value {
        protected float value;

        protected Value() {
        }
    }
}
