package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.mod.worldgen.noise.continent.river.RiverGenerator;
import com.terraforged.mod.worldgen.noise.continent.river.RiverNode;
import java.util.Arrays;

public class RiverPieces {
    public static final RiverPieces NONE = new RiverPieces();
    private static final int INITIAL_SIZE = RiverGenerator.DIRS.length;
    private static final int GROW_AMOUNT = 1;
    private int riverCount = 0;
    private int lakeCount = 0;
    private RiverNode[] riverNodes = new RiverNode[INITIAL_SIZE];
    private RiverNode[] lakeNodes = new RiverNode[INITIAL_SIZE];

    public RiverPieces reset() {
        this.riverCount = 0;
        this.lakeCount = 0;
        return this;
    }

    public int riverCount() {
        return this.riverCount;
    }

    public int lakeCount() {
        return this.lakeCount;
    }

    public RiverNode river(int i) {
        return this.riverNodes[i];
    }

    public RiverNode lake(int i) {
        return this.lakeNodes[i];
    }

    public void addRiver(RiverNode node) {
        this.riverNodes = this.ensureCapacity(this.riverCount, this.riverNodes);
        this.riverNodes[this.riverCount++] = node;
    }

    public void addLake(RiverNode node) {
        this.lakeNodes = this.ensureCapacity(this.lakeCount, this.lakeNodes);
        this.lakeNodes[this.lakeCount++] = node;
    }

    private RiverNode[] ensureCapacity(int size, RiverNode[] array) {
        if (size < array.length) {
            return array;
        }
        return Arrays.copyOf(array, size + 1);
    }
}
