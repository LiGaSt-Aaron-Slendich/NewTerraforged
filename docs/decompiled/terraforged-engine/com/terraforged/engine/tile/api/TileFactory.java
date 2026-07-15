/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.tile.api;

import com.terraforged.engine.concurrent.Disposable;
import com.terraforged.engine.concurrent.task.LazyCallable;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.tile.api.TileProvider;

public interface TileFactory {
    public int chunkToRegion(int var1);

    public void setListener(Disposable.Listener<Tile> var1);

    public LazyCallable<Tile> getTile(int var1, int var2);

    public LazyCallable<Tile> getTile(float var1, float var2, float var3, boolean var4);

    public TileFactory async();

    public TileProvider cached();
}

