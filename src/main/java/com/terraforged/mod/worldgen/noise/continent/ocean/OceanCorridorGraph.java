package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.GuaranteedContinentMask;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Undirected corridor graph: each guaranteed continent links only to its
 * {@code partnersPerContinent} nearest neighbours (default 2). Prevents a full
 * mesh of seafloor ridges between every landmass pair.
 */
public final class OceanCorridorGraph {
    private final LongSet allowedPairs;
    private final boolean active;

    private OceanCorridorGraph(LongSet allowedPairs, boolean active) {
        this.allowedPairs = allowedPairs;
        this.active = active;
    }

    public static OceanCorridorGraph build(ContinentGenerator continent, int partnersPerContinent) {
        GuaranteedContinentMask mask = continent.guaranteeMask;
        if (mask == null || !mask.active() || mask.landCellKeys().isEmpty()) {
            return new OceanCorridorGraph(new LongOpenHashSet(), false);
        }
        int partners = Math.max(1, Math.min(4, partnersPerContinent));
        List<LandNode> nodes = new ArrayList<>();
        for (long key : mask.landCellKeys()) {
            int cx = PosUtil.unpackLeft(key);
            int cy = PosUtil.unpackRight(key);
            CellPoint cell = continent.getCell(cx, cy);
            nodes.add(new LandNode(key, cell.px, cell.py));
        }
        LongOpenHashSet pairs = new LongOpenHashSet();
        for (LandNode a : nodes) {
            List<LandNode> others = new ArrayList<>(nodes.size());
            for (LandNode b : nodes) {
                if (a.key != b.key) {
                    others.add(b);
                }
            }
            others.sort(Comparator.comparingDouble(b -> dist2(a.px, a.py, b.px, b.py)));
            int n = Math.min(partners, others.size());
            for (int i = 0; i < n; i++) {
                pairs.add(pairKey(a.key, others.get(i).key));
            }
        }
        return new OceanCorridorGraph(pairs, true);
    }

    public boolean active() {
        return this.active;
    }

    public boolean allowsPair(long cellKeyA, long cellKeyB) {
        if (!this.active) {
            // No guarantee graph — allow any land-land edge (legacy soft behaviour).
            return true;
        }
        if (cellKeyA == cellKeyB) {
            return false;
        }
        return this.allowedPairs.contains(pairKey(cellKeyA, cellKeyB));
    }

    public int pairCount() {
        return this.allowedPairs.size();
    }

    /** Stable undirected key from two packed cell keys. */
    public static long pairKey(long a, long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        return lo * 0x9E3779B97F4A7C15L ^ (hi + 0xC2B2AE3D27D4EB4FL);
    }

    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private record LandNode(long key, float px, float py) {
    }
}
