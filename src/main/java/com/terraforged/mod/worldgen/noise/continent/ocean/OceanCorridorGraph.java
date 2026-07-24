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
 * Directed corridor graph: each continent may emit up to {@code partnersPerContinent}
 * outgoing corridors toward nearest neighbours within {@code maxDistance}
 * (cell-pitch ≈ × continent scale). Incoming corridors are unlimited.
 *
 * <p>Only large landmasses participate (guaranteed centres, or clustered Voronoi land
 * components) — never island freckles. When inactive, no corridors exist.
 */
public final class OceanCorridorGraph {
    public static final int LIA_MAJORITY_INCOMING = 3;
    private static final int MIN_CONTINENT_CELLS = 3;
    private static final int MAX_CONTINENTS = 32;

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
            return empty();
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
                if (dist2(a.px, a.py, b.px, b.py) <= maxDist2) {
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

    private static OceanCorridorGraph empty() {
        return new OceanCorridorGraph(
                new LongOpenHashSet(),
                List.of(),
                new LongOpenHashSet(),
                new Long2IntOpenHashMap(),
                false);
    }

    private static List<LandNode> collectLandNodes(ContinentGenerator continent) {
        GuaranteedContinentMask mask = continent.guaranteeMask;
        if (mask != null && mask.active() && !mask.landCellKeys().isEmpty()) {
            List<LandNode> nodes = new ArrayList<>();
            for (long key : mask.landCellKeys()) {
                int cx = PosUtil.unpackLeft(key);
                int cy = PosUtil.unpackRight(key);
                CellPoint cell = continent.getCell(cx, cy);
                nodes.add(new LandNode(key, cell.px, cell.py));
            }
            return nodes;
        }
        return clusterContinentNodes(continent);
    }

    /**
     * Cluster adjacent Voronoi land cells into landmasses; drop island-scale freckles.
     * One node per landmass at the component centroid.
     */
    private static List<LandNode> clusterContinentNodes(ContinentGenerator continent) {
        int halfCells = Math.max(10, Math.min(48, GuaranteedContinentMask.HALF / 4000));
        LongOpenHashSet land = new LongOpenHashSet();
        for (int cy = -halfCells; cy <= halfCells; cy++) {
            for (int cx = -halfCells; cx <= halfCells; cx++) {
                CellPoint cell = continent.getCell(cx, cy);
                if (continent.shapeGenerator.getThresholdValue(cell) > 0.0F) {
                    land.add(PosUtil.pack(cx, cy));
                }
            }
        }
        if (land.isEmpty()) {
            return List.of();
        }

        LongOpenHashSet seen = new LongOpenHashSet();
        List<Component> components = new ArrayList<>();
        long[] queue = new long[Math.max(1, land.size())];

        for (long start : land) {
            if (!seen.add(start)) {
                continue;
            }
            int qh = 0;
            int qt = 0;
            queue[qt++] = start;
            float sumX = 0.0F;
            float sumY = 0.0F;
            int area = 0;
            List<Long> members = new ArrayList<>();

            while (qh < qt) {
                long key = queue[qh++];
                int cx = PosUtil.unpackLeft(key);
                int cy = PosUtil.unpackRight(key);
                CellPoint cell = continent.getCell(cx, cy);
                sumX += cell.px;
                sumY += cell.py;
                area++;
                members.add(key);
                qt = enqueue(queue, qt, seen, land, cx + 1, cy);
                qt = enqueue(queue, qt, seen, land, cx - 1, cy);
                qt = enqueue(queue, qt, seen, land, cx, cy + 1);
                qt = enqueue(queue, qt, seen, land, cx, cy - 1);
            }
            if (area >= MIN_CONTINENT_CELLS) {
                components.add(new Component(sumX / area, sumY / area, area, members));
            }
        }

        components.sort(Comparator.comparingInt((Component c) -> c.area).reversed());
        if (components.size() > MAX_CONTINENTS) {
            components = new ArrayList<>(components.subList(0, MAX_CONTINENTS));
        }

        List<LandNode> nodes = new ArrayList<>(components.size());
        for (Component c : components) {
            long bestKey = c.members.get(0);
            float bestD2 = Float.MAX_VALUE;
            for (long key : c.members) {
                int cx = PosUtil.unpackLeft(key);
                int cy = PosUtil.unpackRight(key);
                CellPoint cell = continent.getCell(cx, cy);
                float d2 = dist2(cell.px, cell.py, c.cx, c.cy);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    bestKey = key;
                }
            }
            nodes.add(new LandNode(bestKey, c.cx, c.cy));
        }
        return nodes;
    }

    private static int enqueue(
            long[] queue,
            int qt,
            LongOpenHashSet seen,
            LongOpenHashSet land,
            int cx,
            int cy
    ) {
        long key = PosUtil.pack(cx, cy);
        if (!land.contains(key) || !seen.add(key)) {
            return qt;
        }
        queue[qt] = key;
        return qt + 1;
    }

    public boolean active() {
        return this.active;
    }

    public boolean isLinkedLand(long cellKey) {
        return this.active && this.landKeys.contains(cellKey);
    }

    public boolean hasDirected(long fromKey, long toKey) {
        if (!this.active || fromKey == toKey) {
            return false;
        }
        return this.directedEdges.contains(directedKey(fromKey, toKey));
    }

    /** Seafloor corridor if either direction is linked. Inactive graph → none. */
    public boolean allowsCorridor(long cellKeyA, long cellKeyB) {
        if (!this.active) {
            return false;
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

    public float liaFactor(long cellKey) {
        if (!this.active) {
            return 0.0F;
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
        return 0.68F;
    }

    public int pairCount() {
        return this.directedEdges.size();
    }

    public LongSet landKeys() {
        return this.landKeys;
    }

    public List<DirectedEdge> edges() {
        return this.edgeList;
    }

    public static long directedKey(long from, long to) {
        return from * 0x9E3779B97F4A7C15L ^ (to + 0xC2B2AE3D27D4EB4FL);
    }

    /** @deprecated prefer {@link #directedKey}. */
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

    public record DirectedEdge(long fromKey, long toKey, float fromX, float fromY, float toX, float toY) {
    }

    private record LandNode(long key, float px, float py) {
    }

    private record Component(float cx, float cy, int area, List<Long> members) {
    }
}
