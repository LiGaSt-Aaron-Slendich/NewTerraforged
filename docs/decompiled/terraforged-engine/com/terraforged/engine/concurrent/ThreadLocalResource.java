/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.concurrent;

import com.terraforged.engine.concurrent.Resource;
import com.terraforged.engine.concurrent.SimpleResource;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ThreadLocalResource<T>
extends ThreadLocal<Resource<T>> {
    private final Supplier<Resource<T>> supplier;

    private ThreadLocalResource(Supplier<Resource<T>> supplier) {
        this.supplier = supplier;
    }

    public T open() {
        return ((Resource)this.get()).get();
    }

    public void close() {
        ((Resource)this.get()).close();
    }

    @Override
    protected Resource<T> initialValue() {
        return this.supplier.get();
    }

    public static <T> ThreadLocalResource<T> withInitial(Supplier<T> supplier, Consumer<T> consumer) {
        return new ThreadLocalResource<T>(() -> new SimpleResource(supplier.get(), consumer));
    }
}

