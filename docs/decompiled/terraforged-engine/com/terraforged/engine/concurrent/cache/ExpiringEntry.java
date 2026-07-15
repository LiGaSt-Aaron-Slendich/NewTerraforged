/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.concurrent.cache;

public interface ExpiringEntry {
    public long getTimestamp();

    default public void close() {
    }
}

