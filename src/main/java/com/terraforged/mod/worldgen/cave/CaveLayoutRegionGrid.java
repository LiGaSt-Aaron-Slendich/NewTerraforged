package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class CaveLayoutRegionGrid {
    public static final int MEGA_CELL_SIZE = 48;
    public static final int GIGA_CELL_SIZE = 56;
    private static final float STAT_DECAY_PER_HOP = 0.25f;
    private static final int MAX_STAT_HOPS = 4;
    private final float centerX;
    private final float centerZ;
    private final int cellSize;
    private final int radius;
    private final CaveStatVector globalPool;
    private final Map<Long, CaveBiomeEntry> biomes = new HashMap<Long, CaveBiomeEntry>();
    private final Map<Long, CaveStatVector> stats = new HashMap<Long, CaveStatVector>();
    private static final int[][] NEIGHBORS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private CaveLayoutRegionGrid(float centerX, float centerZ, int cellSize, int radius, CaveStatVector globalPool) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.cellSize = cellSize;
        this.radius = radius;
        this.globalPool = globalPool.clamped();
    }

    public int cellSize() {
        return this.cellSize;
    }

    public CaveBiomeEntry biomeAt(int x, int z) {
        return this.biomes.get(this.keyAt(x, z));
    }

    public void overrideBiome(int x, int z, CaveBiomeEntry biome) {
        if (biome == null) {
            return;
        }
        this.biomes.put(this.keyAt(x, z), biome);
    }

    public void forEachCell(BiConsumer<int[], CaveBiomeEntry> consumer) {
        for (Map.Entry<Long, CaveBiomeEntry> entry : this.biomes.entrySet()) {
            long key = entry.getKey();
            int ix = (int)(key >> 32);
            int iz = (int)key;
            int cx = (int)this.centerX + ix * this.cellSize + this.cellSize / 2;
            int cz = (int)this.centerZ + iz * this.cellSize + this.cellSize / 2;
            consumer.accept(new int[]{cx, cz}, entry.getValue());
        }
    }

    public CaveStatVector statsAt(int x, int z) {
        return this.stats.getOrDefault(this.keyAt(x, z), this.globalPool);
    }

    public int snapX(int x) {
        int half = this.cellSize / 2;
        int base = (int)this.centerX + Math.floorDiv(x - (int)this.centerX, this.cellSize) * this.cellSize;
        return base + half;
    }

    public int snapZ(int z) {
        int half = this.cellSize / 2;
        int base = (int)this.centerZ + Math.floorDiv(z - (int)this.centerZ, this.cellSize) * this.cellSize;
        return base + half;
    }

    public long keyAt(int x, int z) {
        return CaveLayoutRegionGrid.pack(this.regionIndexX(x), this.regionIndexZ(z));
    }

    public int regionIndexX(int x) {
        return Math.floorDiv(x - (int)this.centerX, this.cellSize);
    }

    public int regionIndexZ(int z) {
        return Math.floorDiv(z - (int)this.centerZ, this.cellSize);
    }

    public static CaveLayoutRegionGrid build(float centerX, float centerZ, int radius, boolean isMega, CaveStatVector globalPool, BiFunction<Integer, Integer, CaveBiomeEntry> biomeResolver, List<CaveMegaGigaLayout.GeneratorNode> generators, Function<CaveMegaGigaLayout.GeneratorNode, CaveStatVector> generatorStatSource) {
        int cellSize = isMega ? 48 : 56;
        CaveLayoutRegionGrid grid = new CaveLayoutRegionGrid(centerX, centerZ, cellSize, radius, globalPool);
        int halfCells = (int)Math.ceil((float)radius / (float)cellSize) + 1;
        for (int ix = -halfCells; ix <= halfCells; ++ix) {
            for (int iz = -halfCells; iz <= halfCells; ++iz) {
                CaveBiomeEntry biome;
                int cx = (int)centerX + ix * cellSize + cellSize / 2;
                float dx = (float)cx - centerX;
                int cz = (int)centerZ + iz * cellSize + cellSize / 2;
                float dz = (float)cz - centerZ;
                if (NoiseUtil.sqrt(dx * dx + dz * dz) > (float)radius * 0.98f || (biome = biomeResolver.apply(cx, cz)) == null) continue;
                grid.biomes.put(CaveLayoutRegionGrid.pack(ix, iz), biome);
                grid.stats.put(CaveLayoutRegionGrid.pack(ix, iz), globalPool);
            }
        }
        grid.propagateStats(generators, generatorStatSource);
        return grid;
    }

    private void propagateStats(List<CaveMegaGigaLayout.GeneratorNode> generators, Function<CaveMegaGigaLayout.GeneratorNode, CaveStatVector> generatorStatSource) {
        for (CaveMegaGigaLayout.GeneratorNode generator : generators) {
            int startIz;
            int startIx;
            long startKey;
            CaveStatVector source = generatorStatSource.apply(generator);
            if (source == null || !this.biomes.containsKey(startKey = CaveLayoutRegionGrid.pack(startIx = this.regionIndexX((int)generator.x()), startIz = this.regionIndexZ((int)generator.z())))) continue;
            ArrayDeque<Hop> queue = new ArrayDeque<Hop>();
            HashMap<Long, Integer> visited = new HashMap<Long, Integer>();
            queue.add(new Hop(startIx, startIz, 0));
            visited.put(startKey, 0);
            while (!queue.isEmpty()) {
                long key;
                Hop hop = (Hop)queue.poll();
                float factor = 1.0f - 0.25f * (float)hop.hop;
                if (factor <= 0.0f || !this.biomes.containsKey(key = CaveLayoutRegionGrid.pack(hop.ix, hop.iz))) continue;
                this.stats.merge(key, source.scale(factor), (a, b) -> a.add((CaveStatVector)b));
                if (hop.hop + 1 >= 4) continue;
                for (int[] offset : NEIGHBORS) {
                    int nix = hop.ix + offset[0];
                    int niz = hop.iz + offset[1];
                    long nKey = CaveLayoutRegionGrid.pack(nix, niz);
                    if (!this.biomes.containsKey(nKey)) continue;
                    int nextHop = hop.hop + 1;
                    Integer seen = (Integer)visited.get(nKey);
                    if (seen != null && seen <= nextHop) continue;
                    visited.put(nKey, nextHop);
                    queue.add(new Hop(nix, niz, nextHop));
                }
            }
        }
        for (Long key : this.biomes.keySet()) {
            this.stats.compute(key, (k, v) -> (v == null ? this.globalPool : v).clamped());
        }
    }

    private static long pack(int ix, int iz) {
        return (long)ix << 32 | (long)iz & 0xFFFFFFFFL;
    }

    private record Hop(int ix, int iz, int hop) {
    }
}
