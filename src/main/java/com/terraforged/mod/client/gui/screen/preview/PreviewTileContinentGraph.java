package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Extracts large landmass centroids from a preview {@link Tile} and builds a directed
 * continent→continent corridor graph so overlays match the painted map.
 *
 * <p>Small islands / freckles / island-overlay terrains are excluded.
 */
public final class PreviewTileContinentGraph {
    public record Node(int id, float worldX, float worldZ, int px, int pz, int area) {
    }

    public record Edge(int fromId, int toId, int x0, int y0, int x1, int y1) {
    }

    public record Graph(List<Node> nodes, List<Edge> edges) {
        public boolean active() {
            return nodes != null && nodes.size() >= 2 && edges != null && !edges.isEmpty();
        }
    }

    private PreviewTileContinentGraph() {
    }

    public static Graph build(
            Tile tile,
            Levels levels,
            int centerX,
            int centerZ,
            int zoom,
            int outgoingPartners,
            float maxDistanceScale,
            int continentScale
    ) {
        if (tile == null || levels == null) {
            return new Graph(List.of(), List.of());
        }
        int size = tile.getBlockSize().size;
        int half = size / 2;
        int minArea = Math.max(48, (size * size) / 96);

        boolean[] land = new boolean[size * size];
        for (int lz = 0; lz < size; lz++) {
            for (int lx = 0; lx < size; lx++) {
                land[idx(lx, lz, size)] = isContinentLand(tile.getCell(lx, lz), levels);
            }
        }

        boolean[] seen = new boolean[size * size];
        List<Node> nodes = new ArrayList<>();
        int[] qx = new int[size * size];
        int[] qz = new int[size * size];
        int id = 0;

        for (int lz = 0; lz < size; lz++) {
            for (int lx = 0; lx < size; lx++) {
                if (!land[idx(lx, lz, size)] || seen[idx(lx, lz, size)]) {
                    continue;
                }
                int qh = 0;
                int qt = 0;
                qx[qt] = lx;
                qz[qt] = lz;
                qt++;
                seen[idx(lx, lz, size)] = true;
                long sumLx = 0L;
                long sumLz = 0L;
                int area = 0;
                while (qh < qt) {
                    int x = qx[qh];
                    int z = qz[qh];
                    qh++;
                    sumLx += x;
                    sumLz += z;
                    area++;
                    qt = offer(qx, qz, qt, seen, land, size, x + 1, z);
                    qt = offer(qx, qz, qt, seen, land, size, x - 1, z);
                    qt = offer(qx, qz, qt, seen, land, size, x, z + 1);
                    qt = offer(qx, qz, qt, seen, land, size, x, z - 1);
                }
                if (area < minArea) {
                    continue;
                }
                int cx = (int) (sumLx / area);
                int cz = (int) (sumLz / area);
                float worldX = centerX + (cx - half) * (float) zoom;
                float worldZ = centerZ + (cz - half) * (float) zoom;
                nodes.add(new Node(id++, worldX, worldZ, cx, cz, area));
            }
        }

        int partners = Math.max(1, Math.min(4, outgoingPartners));
        float maxWorld = Math.max(continentScale * 2.0F, continentScale * Math.max(2.0F, maxDistanceScale));
        float maxWorld2 = maxWorld * maxWorld;
        List<Edge> edges = new ArrayList<>();
        for (Node a : nodes) {
            List<Node> others = new ArrayList<>();
            for (Node b : nodes) {
                if (a.id == b.id) {
                    continue;
                }
                float d2 = dist2(a.worldX, a.worldZ, b.worldX, b.worldZ);
                if (d2 <= maxWorld2) {
                    others.add(b);
                }
            }
            others.sort(Comparator.comparingDouble(b -> dist2(a.worldX, a.worldZ, b.worldX, b.worldZ)));
            int n = Math.min(partners, others.size());
            for (int i = 0; i < n; i++) {
                Node b = others.get(i);
                edges.add(new Edge(a.id, b.id, a.px, a.pz, b.px, b.pz));
            }
        }
        return new Graph(List.copyOf(nodes), List.copyOf(edges));
    }

    private static int offer(int[] qx, int[] qz, int qt, boolean[] seen, boolean[] land, int size, int x, int z) {
        if (x < 0 || z < 0 || x >= size || z >= size) {
            return qt;
        }
        int i = idx(x, z, size);
        if (seen[i] || !land[i]) {
            return qt;
        }
        seen[i] = true;
        qx[qt] = x;
        qz[qt] = z;
        return qt + 1;
    }

    private static int idx(int x, int z, int size) {
        return z * size + x;
    }

    private static float dist2(float ax, float az, float bx, float bz) {
        float dx = ax - bx;
        float dz = az - bz;
        return dx * dx + dz * dz;
    }

    /** Mainland only — exclude oceans, beaches, and island-overlay terrains. */
    public static boolean isContinentLand(Cell cell, Levels levels) {
        if (cell == null || cell.terrain == null) {
            return false;
        }
        TerrainCategory cat = cell.terrain.getCategory();
        if (cat == TerrainCategory.DEEP_OCEAN || cat == TerrainCategory.SHALLOW_OCEAN || cat == TerrainCategory.BEACH) {
            return false;
        }
        String name = cell.terrain.getName();
        if (name != null) {
            String n = name.toLowerCase(Locale.ROOT);
            if (n.contains("island") || n.contains("laguna") || n.contains("archipelago")) {
                return false;
            }
        }
        if (cell.value < levels.water) {
            return false;
        }
        return cell.continentEdge >= 0.52F;
    }
}
