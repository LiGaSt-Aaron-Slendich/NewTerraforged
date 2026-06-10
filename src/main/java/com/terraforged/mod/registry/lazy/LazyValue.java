package com.terraforged.mod.registry.lazy;

import com.terraforged.mod.TerraForged;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public abstract class LazyValue<T>
implements Supplier<T> {
    protected static final ResourceLocation UNDEFINED = TerraForged.location("undefined");
    protected final ResourceLocation name;
    protected volatile T value;

    protected LazyValue(ResourceLocation name) {
        this.name = name;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public T get() {
        T value = this.value;
        if (value != null) {
            return value;
        }
        ResourceLocation resourceLocation = this.name;
        synchronized (resourceLocation) {
            value = this.value;
            if (value == null) {
                this.value = value = this.compute();
            }
        }
        return value;
    }

    protected void set(T value) {
        if (this.value != null) {
            return;
        }
        this.value = value;
    }

    protected abstract T compute();
}
