package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Places exactly {@code N} continent cell centres inside a fixed world window
 * ({@link #AREA}×{@link #AREA} blocks). Inside that window every other cell is ocean;
 * outside the window the caller keeps its normal density logic.
 */
public final class GuaranteedContinentMask {
    public static final int AREA = 640_000;
    public static final int HALF = AREA / 2;

    private final LongSet landCells;
    private final int cellMin;
    private final int cellMax;
    private final boolean active;

    private GuaranteedContinentMask(LongSet landCells, int cellMin, int cellMax, boolean active) {
        this.landCells = landCells;
        this.cellMin = cellMin;
        this.cellMax = cellMax;
        this.active = active;
    }

    /**
     * @param worldBlocksPerCell approximate world blocks spanned by one continent cell
     *                           (engine MULTI ≈ continentScale×4, NewTF ≈ continentScale)
     */
    public static GuaranteedContinentMask create(WorldSettings.Islands islands, int seed, int worldBlocksPerCell) {
        int n = Math.max(1, Math.min(16, islands.guaranteedContinents));
        int pitch = Math.max(100, worldBlocksPerCell);
        int halfCells = Math.max(2, HALF / pitch);
        int cellMin = -halfCells;
        int cellMax = halfCells;
        int span = cellMax - cellMin + 1;
        int capacity = span * span;
        if (n > capacity) {
            n = capacity;
        }

        LongSet land = new LongOpenHashSet(n * 2);
        // Minimum spacing grows with Continents Spread so N landmasses stay distinct.
        float spread = clamp01(islands.continentsSpread);
        int minSep = Math.max(1, (int) (span / (Math.sqrt(n) * (2.2F - spread))));

        int attempts = 0;
        int placed = 0;
        int guard = n * 200 + 500;
        while (placed < n && attempts < guard) {
            attempts++;
            int h = MathUtil.hash(seed, attempts, placed + 17);
            int cx = cellMin + Math.floorMod(h, span);
            int cy = cellMin + Math.floorMod(MathUtil.hash(seed ^ 0x9E3779B9, attempts, placed), span);
            if (tooClose(land, cx, cy, minSep)) {
                continue;
            }
            land.add(PosUtil.pack(cx, cy));
            placed++;
        }
        // If spacing was too strict, fill remaining without spacing so count stays exact.
        attempts = 0;
        while (placed < n && attempts < capacity * 2) {
            attempts++;
            int h = MathUtil.hash(seed ^ 0x85EBCA6B, attempts, placed);
            int cx = cellMin + Math.floorMod(h, span);
            int cy = cellMin + Math.floorMod(MathUtil.hash(seed ^ 0xC2B2AE35, attempts, placed), span);
            long key = PosUtil.pack(cx, cy);
            if (land.add(key)) {
                placed++;
            }
        }

        return new GuaranteedContinentMask(land, cellMin, cellMax, true);
    }

    public boolean active() {
        return this.active;
    }

    public boolean inWindow(int cellX, int cellY) {
        return cellX >= this.cellMin && cellX <= this.cellMax && cellY >= this.cellMin && cellY <= this.cellMax;
    }

    /** Inside the window: true = must be land. Outside: unused. */
    public boolean isGuaranteedLand(int cellX, int cellY) {
        return this.landCells.contains(PosUtil.pack(cellX, cellY));
    }

    public int count() {
        return this.landCells.size();
    }

    private static boolean tooClose(LongSet land, int cx, int cy, int minSep) {
        if (minSep <= 1 || land.isEmpty()) {
            return false;
        }
        int sep2 = minSep * minSep;
        for (long key : land) {
            int ox = PosUtil.unpackLeft(key);
            int oy = PosUtil.unpackRight(key);
            int dx = cx - ox;
            int dy = cy - oy;
            if (dx * dx + dy * dy < sep2) {
                return true;
            }
        }
        return false;
    }

    private static float clamp01(float v) {
        if (v < 0.0F) {
            return 0.0F;
        }
        if (v > 1.0F) {
            return 1.0F;
        }
        return v;
    }
}
