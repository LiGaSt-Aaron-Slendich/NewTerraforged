package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;

/**
 * Regional cave density limits from {@code caves.toml [caves]}.
 * {@link #cavePercent()} thins synapse columns via world-space hash (no chunk stripes).
 * Set {@code xy_limit} for an explicit per-chunk column cap instead.
 */
public record CaveDensitySettings(Integer xyLimit, Integer yzLimit, float cavePercent) {
    public static final int DEFAULT_XY_COLUMNS = 96;
    public static final int DEFAULT_YZ_BLOCKS = 8192;
    public static final CaveDensitySettings DEFAULT = new CaveDensitySettings(null, null, 100.0f);

    public CaveDensitySettings {
        cavePercent = Math.max(0.0f, Math.min(100.0f, cavePercent));
    }

    public int resolveXyBudget() {
        if (this.xyLimit != null) {
            return Math.max(0, this.xyLimit);
        }
        return Math.max(0, Math.round((float)DEFAULT_XY_COLUMNS * this.cavePercent / 100.0f));
    }

    public int resolveYzBudget() {
        if (this.yzLimit != null) {
            return Math.max(0, this.yzLimit);
        }
        return DEFAULT_YZ_BLOCKS;
    }

    public boolean limitsSecondaryCaves() {
        return this.xyLimit != null || this.yzLimit != null || this.useSpatialThinning();
    }

    /** Percent-based thinning by 2×2-chunk cells — skips whole regions, not individual columns (avoids ribbon artifacts). */
    public boolean useSpatialThinning() {
        return this.xyLimit == null && this.yzLimit == null && this.cavePercent < 100.0f;
    }

    /** Whole macro-cells carve at full column density; others skip synapse entirely. */
    public boolean regionPassesSpatialThinning(int seed, int chunkX, int chunkZ) {
        if (!this.useSpatialThinning()) {
            return true;
        }
        int cellX = Math.floorDiv(chunkX, 2);
        int cellZ = Math.floorDiv(chunkZ, 2);
        int gate = NoiseUtil.hash2D(seed ^ 0x5CA7E501, cellX, cellZ) & 0xFF;
        return gate < Math.max(1, Math.round(this.cavePercent * 2.55f));
    }
}
