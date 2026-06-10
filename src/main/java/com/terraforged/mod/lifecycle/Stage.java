package com.terraforged.mod.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Stage {
    private final AtomicBoolean lock = new AtomicBoolean(false);

    public final boolean run() {
        if (this.lock.compareAndSet(false, true)) {
            this.doInit();
            return true;
        }
        return false;
    }

    protected abstract void doInit();
}
