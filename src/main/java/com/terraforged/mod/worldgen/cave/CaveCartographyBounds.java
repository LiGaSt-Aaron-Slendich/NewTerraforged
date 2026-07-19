package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.Module;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Axis-aligned bounds of the carved mega/giga footprint for debug cartography.
 * Expands along carved tunnel until solid wall — not limited to voronoi zone radius.
 */
public final class CaveCartographyBounds {
    private static final int PADDING_BLOCKS = 56;
    private static final int MAX_SEARCH_RADIUS = 768;

    private CaveCartographyBounds() {
    }

    public record Box(int minX, int minZ, int maxX, int maxZ) {
        public int width() {
            return this.maxX - this.minX;
        }

        public int depth() {
            return this.maxZ - this.minZ;
        }

        public int centerX() {
            return (this.minX + this.maxX) / 2;
        }

        public int centerZ() {
            return (this.minZ + this.maxZ) / 2;
        }
    }

    public static Box compute(Generator generator, int seed, NoiseCave caveConfig, Module modifier, CaveType type, float layoutCenterX, float layoutCenterZ, int step) {
        int cx = Math.round(layoutCenterX);
        int cz = Math.round(layoutCenterZ);
        if (CaveColumnSimulator.sampleMegaGigaForCartography(generator, caveConfig, seed, modifier, cx, cz) == null) {
            int radius = CaveSystemGrid.caveRadius(type);
            return new Box(cx - radius, cz - radius, cx + radius, cz + radius);
        }
        Set<Long> carved = CaveCartographyBounds.floodCarvedCells(generator, seed, caveConfig, modifier, cx, cz, step);
        if (carved.isEmpty()) {
            int radius = CaveSystemGrid.caveRadius(type);
            return new Box(cx - radius, cz - radius, cx + radius, cz + radius);
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long packed : carved) {
            int wx = (int)(packed >> 32);
            int wz = (int)packed;
            minX = Math.min(minX, wx);
            minZ = Math.min(minZ, wz);
            maxX = Math.max(maxX, wx);
            maxZ = Math.max(maxZ, wz);
        }
        return new Box(minX - PADDING_BLOCKS, minZ - PADDING_BLOCKS, maxX + PADDING_BLOCKS, maxZ + PADDING_BLOCKS);
    }

    static Set<Long> floodCarvedCells(Generator generator, int seed, NoiseCave caveConfig, Module modifier, int centerX, int centerZ, int step) {
        HashSet<Long> carved = new HashSet<Long>();
        Queue<long[]> queue = new ArrayDeque<long[]>();
        long start = CaveCartographyBounds.pack(centerX, centerZ);
        carved.add(start);
        queue.add(new long[]{centerX, centerZ});
        int[][] dirs = new int[][]{{step, 0}, {-step, 0}, {0, step}, {0, -step}};
        while (!queue.isEmpty()) {
            long[] cell = queue.poll();
            int wx = (int)cell[0];
            int wz = (int)cell[1];
            if (Math.abs(wx - centerX) > MAX_SEARCH_RADIUS || Math.abs(wz - centerZ) > MAX_SEARCH_RADIUS) {
                continue;
            }
            for (int[] dir : dirs) {
                int nx = wx + dir[0];
                int nz = wz + dir[1];
                long packed = CaveCartographyBounds.pack(nx, nz);
                if (carved.contains(packed)) {
                    continue;
                }
                if (CaveColumnSimulator.sampleMegaGigaForCartography(generator, caveConfig, seed, modifier, nx, nz) == null) {
                    continue;
                }
                carved.add(packed);
                queue.add(new long[]{nx, nz});
            }
        }
        return carved;
    }

    private static long pack(int x, int z) {
        return (long)x << 32 ^ (long)z & 0xFFFFFFFFL;
    }

    public static int gridWidth(Box box, int step) {
        return box.width() / step + 1;
    }

    public static int gridDepth(Box box, int step) {
        return box.depth() / step + 1;
    }
}
