package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.MathUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Places {@code N±1} continent cell centres inside a fixed world window
 * ({@link #AREA}×{@link #AREA} blocks). Inside that window non-selected cells use a
 * soft cut (mostly ocean, rare island-scale land) so cut zones are not empty;
 * outside the window the caller keeps its normal density logic.
 */
public final class GuaranteedContinentMask {
    public static final int AREA = 640_000;
    public static final int HALF = AREA / 2;

    private final LongSet landCells;
    private final int cellMin;
    private final int cellMax;
    private final boolean active;
    private final float softSkipThreshold;
    private final float softNoiseThreshold;

    private GuaranteedContinentMask(
            LongSet landCells,
            int cellMin,
            int cellMax,
            boolean active,
            float softSkipThreshold,
            float softNoiseThreshold) {
        this.landCells = landCells;
        this.cellMin = cellMin;
        this.cellMax = cellMax;
        this.active = active;
        this.softSkipThreshold = softSkipThreshold;
        this.softNoiseThreshold = softNoiseThreshold;
    }

    /**
     * @param worldBlocksPerCell approximate world blocks spanned by one continent cell
     *                           (engine MULTI ≈ continentScale×4, NewTF ≈ continentScale)
     */
    public static GuaranteedContinentMask create(WorldSettings world, int seed, int worldBlocksPerCell) {
        if (world == null || world.continent == null || !world.continent.guaranteedContinentsEnabled) {
            return inactive();
        }
        WorldSettings.Continent continent = world.continent;
        WorldSettings.Islands islands = world.islands != null ? world.islands : new WorldSettings.Islands();
        int n = Math.max(1, Math.min(16, continent.guaranteedContinents));
        int delta = Math.floorMod(MathUtil.hash(seed, 0xC0117, n), 3) - 1;
        n = Math.max(1, Math.min(16, n + delta));

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
        float spread = clamp01(continent.continentsSpread);
        // spread=0 → clustered (smaller minSep); spread=1 → widely spaced.
        // Divisor goes 2.4 → 0.75 so high spread roughly triples separation vs low.
        float sepDiv = 2.6F - spread * 2.0F;
        int minSep = Math.max(1, (int) (span / (Math.sqrt(n) * Math.max(0.40F, sepDiv))));

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

        float softSkip = softSkipThreshold(islands);
        float softNoise = softNoiseThreshold(islands);
        return new GuaranteedContinentMask(land, cellMin, cellMax, true, softSkip, softNoise);
    }

    /** @deprecated prefer {@link #create(WorldSettings, int, int)} */
    @Deprecated
    public static GuaranteedContinentMask create(WorldSettings.Islands islands, int seed, int worldBlocksPerCell) {
        WorldSettings world = new WorldSettings();
        if (islands != null) {
            world.islands = islands;
            // Legacy callers stuffed guarantee knobs onto Islands — map what we can from Continent defaults.
            world.continent.guaranteedContinentsEnabled = true;
            world.continent.guaranteedContinents = 3;
            world.continent.continentsSpread = 0.5F;
        }
        return create(world, seed, worldBlocksPerCell);
    }

    public static GuaranteedContinentMask inactive() {
        return new GuaranteedContinentMask(new LongOpenHashSet(), 0, 0, false, 1.0F, 1.0F);
    }

    public boolean active() {
        return this.active;
    }

    public boolean inWindow(int cellX, int cellY) {
        return cellX >= this.cellMin && cellX <= this.cellMax && cellY >= this.cellMin && cellY <= this.cellMax;
    }

    public boolean isGuaranteedLand(int cellX, int cellY) {
        return this.landCells.contains(PosUtil.pack(cellX, cellY));
    }

    public float softSkipThreshold() {
        return this.softSkipThreshold;
    }

    public float softNoiseThreshold() {
        return this.softNoiseThreshold;
    }

    public int count() {
        return this.landCells.size();
    }

    /** Guaranteed land cell keys ({@link PosUtil#pack} of cell X/Y). Empty when inactive. */
    public LongSet landCellKeys() {
        return this.landCells;
    }

    /**
     * Soft cut in the guarantee window: fixed thresholds so island chances
     * (especially volcanic) never inflate continent / landmass count.
     * Island freckles are painted by {@code IslandFeatureOverlay}, not by soft-cut survivors.
     */
    private static float softSkipThreshold(WorldSettings.Islands islands) {
        return 0.94F;
    }

    private static float softNoiseThreshold(WorldSettings.Islands islands) {
        return 0.90F;
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
        return clamp(v, 0.0F, 1.0F);
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
