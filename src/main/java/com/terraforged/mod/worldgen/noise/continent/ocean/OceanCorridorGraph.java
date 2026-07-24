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
 * Undirected corridor graph: each landmass links only to its
 * {@code partnersPerContinent} nearest neighbours within {@code maxDistance}
 * (cell-pitch units ≈ multiples of continent scale). Prevents long deep-ocean bridges
 * and a full mesh of seafloor ridges.
 */
public final class OceanCorridorGraph {
    private final LongSet allowedPairs;
    private final LongSet landKeys;
    private final boolean active;

    private OceanCorridorGraph(LongSet allowedPairs, LongSet landKeys, boolean active) {
        this.allowedPairs = allowedPairs;
        this.landKeys = landKeys;
        this.active = active;
    }

    public static OceanCorridorGraph build(ContinentGenerator continent, int partnersPerContinent) {
        return build(continent, partnersPerContinent, 8.0F);
    }

    public static OceanCorridorGraph build(
            ContinentGenerator continent,
            int partnersPerContinent,
            float maxDistanceCellPitch
    ) {
        int partners = Math.max(1, Math.min(4, partnersPerContinent));
        float maxDist = Math.max(1.5F, maxDistanceCellPitch);
        float maxDist2 = maxDist * maxDist;

        List<LandNode> nodes = collectLandNodes(continent);
        if (nodes.size() < 2) {
            return new OceanCorridorGraph(new LongOpenHashSet(), new LongOpenHashSet(), false);
        }

        LongOpenHashSet landKeys = new LongOpenHashSet(nodes.size() * 2);
        for (LandNode n : nodes) {
            landKeys.add(n.key);
        }

        LongOpenHashSet pairs = new LongOpenHashSet();
        for (LandNode a : nodes) {
            List<LandNode> others = new ArrayList<>(nodes.size());
            for (LandNode b : nodes) {
                if (a.key == b.key) {
                    continue;
                }
                float d2 = dist2(a.px, a.py, b.px, b.py);
                if (d2 <= maxDist2) {
                    others.add(b);
                }
            }
            others.sort(Comparator.comparingDouble(b -> dist2(a.px, a.py, b.px, b.py)));
            int n = Math.min(partners, others.size());
            for (int i = 0; i < n; i++) {
                pairs.add(pairKey(a.key, others.get(i).key));
            }
        }
        return new OceanCorridorGraph(pairs, landKeys, !pairs.isEmpty());
    }

    private static List<LandNode> collectLandNodes(ContinentGenerator continent) {
        GuaranteedContinentMask mask = continent.guaranteeMask;
        List<LandNode> nodes = new ArrayList<>();
        if (mask != null && mask.active() && !mask.landCellKeys().isEmpty()) {
            for (long key : mask.landCellKeys()) {
                int cx = PosUtil.unpackLeft(key);
                int cy = PosUtil.unpackRight(key);
                CellPoint cell = continent.getCell(cx, cy);
                nodes.add(new LandNode(key, cell.px, cell.py));
            }
            return nodes;
        }

        // Guarantee off: sample land cells inside a window so corridors still track continents.
        int halfCells = Math.max(10, Math.min(40, GuaranteedContinentMask.HALF / 4000));
        for (int cy = -halfCells; cy <= halfCells; cy++) {
            for (int cx = -halfCells; cx <= halfCells; cx++) {
                CellPoint cell = continent.getCell(cx, cy);
                if (continent.shapeGenerator.getThresholdValue(cell) <= 0.0F) {
                    continue;
                }
                nodes.add(new LandNode(PosUtil.pack(cx, cy), cell.px, cell.py));
            }
        }
        return nodes;
    }

    public boolean active() {
        return this.active;
    }

    public boolean isLinkedLand(long cellKey) {
        return this.active && this.landKeys.contains(cellKey);
    }

    public boolean allowsPair(long cellKeyA, long cellKeyB) {
        if (!this.active) {
            // No graph — allow any land-land edge (legacy soft behaviour).
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

    public LongSet landKeys() {
        return this.landKeys;
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
