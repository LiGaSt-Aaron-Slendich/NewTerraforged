/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.concurrent.Resource;
import com.terraforged.engine.concurrent.SimpleResource;

public class ThreadContext {
    public final Resource<Cell> cell = new SimpleResource<Cell>(new Cell(), Cell::reset);
}

