package com.terraforged.mod.util.storage;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import java.util.function.Predicate;

public class WeightMap<T> {
    protected final T[] values;
    protected final float[] weights;
    protected final float sumWeight;
    protected final float zeroWeight;

    public WeightMap(T[] values, float[] weights) {
        this.values = values;
        this.weights = WeightMap.getCumulativeWeights(values.length, weights);
        this.zeroWeight = weights.length > 0 ? weights[0] : 0.0f;
        this.sumWeight = MathUtil.sum(weights) * 0.99999f;
    }

    public boolean isEmpty() {
        return this.values.length == 0;
    }

    public T[] getValues() {
        return this.values;
    }

    public T getValue(float noise) {
        if ((noise *= this.sumWeight) < this.zeroWeight) {
            return this.values[0];
        }
        for (int i = 1; i < this.weights.length; ++i) {
            if (!(noise < this.weights[i])) continue;
            return this.values[i];
        }
        return null;
    }

    public T find(Predicate<T> predicate) {
        for (T t : this.values) {
            if (!predicate.test(t)) continue;
            return t;
        }
        return null;
    }

    public long getBand(T value) {
        float lower = 0.0f;
        for (int i = 0; i < this.values.length; ++i) {
            float upper = this.weights[i];
            if (this.values[i] == value) {
                return PosUtil.packf(lower / this.sumWeight, upper / this.sumWeight);
            }
            lower = upper;
        }
        return 0L;
    }

    public static <T extends Weighted> WeightMap<T> of(T[] values) {
        float[] weights = new float[values.length];
        for (int i = 0; i < weights.length; ++i) {
            weights[i] = values[i].weight();
        }
        return new WeightMap<T>(values, weights);
    }

    private static float[] getCumulativeWeights(int len, float[] weights) {
        float[] cumulativeWeights = new float[len];
        float weight = 0.0f;
        for (int i = 0; i < len; ++i) {
            cumulativeWeights[i] = weight += i < weights.length ? weights[i] : 1.0f;
        }
        return cumulativeWeights;
    }

    public static interface Weighted {
        public float weight();
    }
}
