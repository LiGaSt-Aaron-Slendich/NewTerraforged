package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.GuaranteedContinentMask;
import com.terraforged.mod.worldgen.noise.continent.cell.CellPoint;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Directed corridor graph: each landmass may emit up to {@code partnersPerContinent}
 * outgoing corridors toward its nearest neighbours within {@code maxDistance}
 * (cell-pitch ≈ × continent scale). Incoming corridors are unlimited.
 *
 * <p>Seafloor ridges exist for either direction of an edge. Coastal LIA uses
 * {@link #incomingCount(long)} / {@link #liaFactor(long)}: 0 incoming → no LIA;
 * 3+ incoming → absolute-majority ragged LIA coast.
 */
public final class OceanCorridorGraph {
    /** Incoming corridors at/above this → majority LIA coast. */
    public static final int LIA_MAJORITY_INCOMING = 3;

    private final LongSet directedEdges;
    private final List<DirectedEdge> edgeList;
    private final LongSet landKeys;
    private final Long2IntOpenHashMap incoming;
    private final boolean active;

    private OceanCorridorGraph(
            LongSet directedEdges,
            List<DirectedEdge> edgeList,
            LongSet landKeys,
            Long2IntOpenHashMap incoming,
            boolean active
    ) {
        this.directedEdges = directedEdges;
        this.edgeList = edgeList;
        this.landKeys = landKeys;
        this.incoming = incoming;
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
        int outgoingLimit = Math.max(1, Math.min(4, partnersPerContinent));
        float maxDist = Math.max(1.5F, maxDistanceCellPitch);
        float maxDist2 = maxDist * maxDist;

        List<LandNode> nodes = collectLandNodes(continent);
        if (nodes.size() < 2) {
            return new OceanCorridorGraph(
                    new LongOpenHashSet(),
                    List.of(),
                    new LongOpenHashSet(),
                    new Long2IntOpenHashMap(),
                    false);
        }

        LongOpenHashSet landKeys = new LongOpenHashSet(nodes.size() * 2);
        for (LandNode n : nodes) {
            landKeys.add(n.key);
        }

        LongOpenHashSet edges = new LongOpenHashSet();
        List<DirectedEdge> edgeList = new ArrayList<>();
        Long2IntOpenHashMap incoming = new Long2IntOpenHashMap();
        incoming.defaultReturnValue(0);

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
            int n = Math.min(outgoingLimit, others.size());
            for (int i = 0; i < n; i++) {
                LandNode b = others.get(i);
                long edge = directedKey(a.key, b.key);
                if (edges.add(edge)) {
                    incoming.addTo(b.key, 1);
                    edgeList.add(new DirectedEdge(a.key, b.key, a.px, a.py, b.px, b.py));
                }
            }
        }
        return new OceanCorridorGraph(edges, List.copyOf(edgeList), landKeys, incoming, !edges.isEmpty());
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

        // Cap density — a full ±40 cell scan can yield thousands of nodes and freeze the
        // preview render thread (O(n²) partner search when the tile image paints).
        final int maxNodes = 48;
        int halfCells = Math.max(10, Math.min(40, GuaranteedContinentMask.HALF / 4000));
        int step = Math.max(1, (2 * halfCells + 1) / 24);
        for (int cy = -halfCells; cy <= halfCells; cy += step) {
            for (int cx = -halfCells; cx <= halfCells; cx += step) {
                CellPoint cell = continent.getCell(cx, cy);
                if (continent.shapeGenerator.getThresholdValue(cell) <= 0.0F) {
                    continue;
                }
                nodes.add(new LandNode(PosUtil.pack(cx, cy), cell.px, cell.py));
            }
        }
        if (nodes.size() <= maxNodes) {
            return nodes;
        }
        nodes.sort(Comparator.comparingDouble(n -> n.px * n.px + n.py * n.py));
        return new ArrayList<>(nodes.subList(0, maxNodes));
    }

    public boolean active() {
        return this.active;
    }

    public boolean isLinkedLand(long cellKey) {
        return this.active && this.landKeys.contains(cellKey);
    }

    /** True if A→B exists. */
    public boolean hasDirected(long fromKey, long toKey) {
        if (!this.active || fromKey == toKey) {
            return false;
        }
        return this.directedEdges.contains(directedKey(fromKey, toKey));
    }

    /**
     * Seafloor corridor between two centres if either direction is linked
     * (physical ridge does not care about arrow; LIA does).
     */
    public boolean allowsCorridor(long cellKeyA, long cellKeyB) {
        if (!this.active) {
            return true;
        }
        if (cellKeyA == cellKeyB) {
            return false;
        }
        return hasDirected(cellKeyA, cellKeyB) || hasDirected(cellKeyB, cellKeyA);
    }

    /** @deprecated use {@link #allowsCorridor(long, long)} */
    @Deprecated
    public boolean allowsPair(long cellKeyA, long cellKeyB) {
        return allowsCorridor(cellKeyA, cellKeyB);
    }

    public int incomingCount(long cellKey) {
        return this.incoming.get(cellKey);
    }

    /**
     * Coastal LIA strength for a landmass: 0 if no incoming corridors,
     * ramps up, and reaches 1.0 (majority ragged coast) at {@link #LIA_MAJORITY_INCOMING}+.
     */
    public float liaFactor(long cellKey) {
        if (!this.active) {
            return 1.0F;
        }
        int inc = incomingCount(cellKey);
        if (inc <= 0) {
            return 0.0F;
        }
        if (inc >= LIA_MAJORITY_INCOMING) {
            return 1.0F;
        }
        if (inc == 1) {
            return 0.38F;
        }
        return 0.68F; // 2 incoming
    }

    public int pairCount() {
        return this.directedEdges.size();
    }

    public LongSet landKeys() {
        return this.landKeys;
    }

    /** Directed edges with shape-space endpoints (for preview overlays). */
    public List<DirectedEdge> edges() {
        return this.edgeList;
    }

    /** Directed edge key: from → to (order matters). */
    public static long directedKey(long from, long to) {
        return from * 0x9E3779B97F4A7C15L ^ (to + 0xC2B2AE3D27D4EB4FL);
    }

    /** @deprecated undirected helper kept for callers; prefer {@link #directedKey}. */
    @Deprecated
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

    /** Shape-space directed corridor A→B. */
    public record DirectedEdge(long fromKey, long toKey, float fromX, float fromY, float toX, float toY) {
    }

    private record LandNode(long key, float px, float py) {
    }
}
