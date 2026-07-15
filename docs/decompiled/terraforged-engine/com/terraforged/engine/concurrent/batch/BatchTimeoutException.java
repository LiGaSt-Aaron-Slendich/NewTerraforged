/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.concurrent.batch;

import com.terraforged.engine.concurrent.batch.BatchTaskException;

public class BatchTimeoutException
extends BatchTaskException {
    public BatchTimeoutException(String message) {
        super(message);
    }
}

