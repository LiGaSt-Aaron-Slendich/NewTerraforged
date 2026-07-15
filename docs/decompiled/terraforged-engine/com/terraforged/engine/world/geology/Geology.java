/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.geology;

import com.terraforged.engine.world.geology.Strata;
import com.terraforged.noise.Module;
import java.util.ArrayList;
import java.util.List;

public class Geology<T> {
    private final Module selector;
    private final List<Strata<T>> backing = new ArrayList<Strata<T>>();

    public Geology(Module selector) {
        this.selector = selector;
    }

    public Geology<T> add(Geology<T> geology) {
        this.backing.addAll(geology.backing);
        return this;
    }

    public Geology<T> add(Strata<T> strata) {
        this.backing.add(strata);
        return this;
    }

    public Strata<T> getStrata(float x, int y) {
        float noise = this.selector.getValue(x, y);
        return this.getStrata(noise);
    }

    public Strata<T> getStrata(float value) {
        int index = (int)(value * (float)this.backing.size());
        index = Math.min(this.backing.size() - 1, index);
        return this.backing.get(index);
    }
}

