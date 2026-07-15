/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.concurrent.cache;

public interface SafeCloseable
extends AutoCloseable {
    public static final SafeCloseable NONE = () -> {};

    @Override
    public void close();
}

