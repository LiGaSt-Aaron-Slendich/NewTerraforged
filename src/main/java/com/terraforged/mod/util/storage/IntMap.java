package com.terraforged.mod.util.storage;

import com.terraforged.mod.util.storage.Index;

public class IntMap {
    private final Index index;
    private final int[] data;
    private int max = 0;

    public IntMap() {
        this.index = Index.CHUNK;
        this.data = new int[256];
    }

    public IntMap(int border) {
        int size = 16 + border * 2;
        this.index = Index.borderedChunk(border);
        this.data = new int[size * size];
    }

    public Index getIndex() {
        return this.index;
    }

    public int getMax() {
        return this.max;
    }

    public int get(int x, int z) {
        return this.get(this.index.of(x, z));
    }

    public void set(int x, int z, int value) {
        this.set(this.index.of(x, z), value);
    }

    public int get(int index) {
        return this.data[index];
    }

    public void set(int index, int value) {
        this.data[index] = value;
        this.max = Math.max(value, this.max);
    }
}
