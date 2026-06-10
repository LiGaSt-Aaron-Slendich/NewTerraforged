package com.terraforged.mod.worldgen.noise.erosion;

public class NoiseTileSize {
    public static final int CHUNK_SIZE = 16;
    public static final NoiseTileSize DEFAULT = new NoiseTileSize(1);
    public final int chunkLength;
    public final int chunkSize;
    public final int chunkMin;
    public final int chunkMax;
    public final int regionLength;
    public final int regionSize;
    public final int min;
    public final int max;

    public NoiseTileSize(int chunkRadius) {
        this.chunkLength = 1 + chunkRadius * 2;
        this.chunkSize = this.chunkLength * this.chunkLength;
        this.chunkMin = -chunkRadius;
        this.chunkMax = this.chunkMin + this.chunkLength;
        this.regionLength = this.chunkLength * 16;
        this.regionSize = this.regionLength * this.regionLength;
        this.min = -16 * chunkRadius;
        this.max = this.min + this.regionLength;
    }

    public int chunkIndexOf(int x, int z) {
        return z * this.chunkLength + x;
    }

    public int chunkIndexOfRel(int x, int z) {
        return (z -= this.chunkMin) * this.chunkLength + (x -= this.chunkMin);
    }

    public int indexOf(int x, int z) {
        return z * this.regionLength + x;
    }

    public int indexOfRel(int x, int z) {
        return this.indexOf(x -= this.min, z -= this.min);
    }

    public String toString() {
        return "NoiseTileSize{chunkDiameter=" + this.chunkLength + ", chunkSize=" + this.chunkSize + ", chunkMin=" + this.chunkMin + ", chunkMax=" + this.chunkMax + ", regionDiameter=" + this.regionLength + ", regionSize=" + this.regionSize + ", min=" + this.min + ", max=" + this.max + "}";
    }
}
