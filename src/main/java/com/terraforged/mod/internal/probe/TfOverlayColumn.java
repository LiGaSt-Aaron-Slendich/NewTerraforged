package com.terraforged.mod.internal.probe;

/** Sparse column/volume for overlay: 3D cave region or 2D surface cover tile. */
public record TfOverlayColumn(int x, int z, int yMin, int yMax, int rgb, String label, boolean surface, int xSize, int zSize) {
    public TfOverlayColumn(int x, int z, int yMin, int yMax, int rgb, String label) {
        this(x, z, yMin, yMax, rgb, label, false, 1, 1);
    }

    public TfOverlayColumn(int x, int z, int yMin, int yMax, int rgb, String label, boolean surface) {
        this(x, z, yMin, yMax, rgb, label, surface, 1, 1);
    }

    public TfOverlayColumn(int x, int z, int yMin, int yMax, int rgb, String label, boolean surface, int xSize) {
        this(x, z, yMin, yMax, rgb, label, surface, xSize, xSize);
    }

    public int xEnd() {
        return this.x + Math.max(1, this.xSize);
    }

    public int zEnd() {
        return this.z + Math.max(1, this.zSize);
    }
}
