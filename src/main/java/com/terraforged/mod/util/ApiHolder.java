package com.terraforged.mod.util;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.lifecycle.Stage;

public class ApiHolder<T>
extends Stage {
    private volatile T value;

    public ApiHolder(T defaultValue) {
        this.value = defaultValue;
    }

    @Override
    protected void doInit() {
    }

    public void set(T value) {
        if (this.run()) {
            this.value = value;
            TerraForged.LOG.info("Set TerraForged API: {}", value.getClass().getSimpleName());
        }
    }

    public T get() {
        return this.value;
    }
}
