package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.noise.Module;
import net.minecraft.world.level.chunk.ChunkAccess;

final class MegaGigaChunkCache {
    static final byte NONE = 0;
    static final byte MEGA = 1;
    static final byte GIGA = 2;
    private static final float MEGA_THRESHOLD = 0.22f;
    private static final float GIGA_THRESHOLD = 0.1f;
    private static final ThreadLocal<MegaGigaChunkCache> ACTIVE = ThreadLocal.withInitial(MegaGigaChunkCache::new);
    private int minX = Integer.MIN_VALUE;
    private int minZ = Integer.MIN_VALUE;
    private int sizeX;
    private int sizeZ;
    private byte[] flags = new byte[0];
    private boolean populated;
    private int chunkMinX;
    private int chunkMinZ;
    private Generator boundGenerator;
    private CarverColumnCache boundColumnCache;

    private MegaGigaChunkCache() {
    }

    static void begin(Generator generator, ChunkAccess chunk, int padding, CarverColumnCache columnCache) {
        ACTIVE.get().prepareBounds(generator, chunk, padding, columnCache);
    }

    static void begin(Generator generator, ChunkAccess chunk, int padding) {
        MegaGigaChunkCache.begin(generator, chunk, padding, null);
    }

    static void end() {
        ACTIVE.get().clear();
    }

    static boolean isInMegaOrGiga(Generator generator, int x, int z) {
        MegaGigaChunkCache cache = ACTIVE.get();
        cache.ensurePopulated(generator);
        Byte flag = cache.resolveFlag(x, z);
        if (flag != null) {
            return flag != NONE;
        }
        return MegaGigaChunkCache.sampleMegaOrGiga(generator, x, z) != NONE;
    }

    static boolean isInMegaOrGigaAt(Generator generator, int x, int y, int z) {
        MegaGigaChunkCache cache = ACTIVE.get();
        cache.ensurePopulated(generator);
        Byte flag = cache.resolveFlag(x, z);
        if (flag == null) {
            flag = MegaGigaChunkCache.sampleMegaOrGiga(generator, x, z);
        }
        if (flag == NONE) {
            return false;
        }
        return y < generator.getOceanFloorHeight(x, z) - 6;
    }

    private void prepareBounds(Generator generator, ChunkAccess chunk, int padding, CarverColumnCache columnCache) {
        this.boundGenerator = generator;
        this.boundColumnCache = columnCache;
        this.populated = false;
        this.chunkMinX = chunk.getPos().getMinBlockX();
        this.chunkMinZ = chunk.getPos().getMinBlockZ();
        this.minX = this.chunkMinX - padding;
        this.minZ = this.chunkMinZ - padding;
        this.sizeX = 16 + padding * 2;
        this.sizeZ = 16 + padding * 2;
        int needed = this.sizeX * this.sizeZ;
        if (this.flags.length < needed) {
            this.flags = new byte[needed];
        }
    }

    private void ensurePopulated(Generator generator) {
        if (this.populated || this.minX == Integer.MIN_VALUE) {
            return;
        }
        this.populated = true;
        this.populate(generator, this.boundColumnCache);
    }

    private Byte resolveFlag(int x, int z) {
        if (this.minX == Integer.MIN_VALUE) {
            return null;
        }
        int lx = x - this.minX;
        int lz = z - this.minZ;
        if (lx < 0 || lz < 0 || lx >= this.sizeX || lz >= this.sizeZ) {
            return null;
        }
        return this.flags[lz * this.sizeX + lx];
    }

    private void populate(Generator generator, CarverColumnCache columnCache) {
        int seed = Seeds.get(generator.getSeed());
        Module mega = CaveModifiers.mega();
        Module giga = CaveModifiers.giga();
        for (int lz = 0; lz < this.sizeZ; ++lz) {
            for (int lx = 0; lx < this.sizeX; ++lx) {
                int x = this.minX + lx;
                int z = this.minZ + lz;
                int localX = x - this.chunkMinX;
                int localZ = z - this.chunkMinZ;
                if (columnCache != null && localX >= 0 && localX < 16 && localZ >= 0 && localZ < 16) {
                    this.flags[lz * this.sizeX + lx] = columnCache.megaGigaFlag(localX, localZ);
                    continue;
                }
                if ((lx & 1) != 0 && (lz & 1) != 0) {
                    this.flags[lz * this.sizeX + lx] = NONE;
                    continue;
                }
                this.flags[lz * this.sizeX + lx] = MegaGigaChunkCache.classify(generator, seed, mega, giga, x, z);
            }
        }
        for (int lz = 0; lz < this.sizeZ; ++lz) {
            for (int lx = 0; lx < this.sizeX; ++lx) {
                if (this.flags[lz * this.sizeX + lx] != NONE) {
                    continue;
                }
                int x = this.minX + lx;
                int z = this.minZ + lz;
                int localX = x - this.chunkMinX;
                int localZ = z - this.chunkMinZ;
                if (columnCache != null && localX >= 0 && localX < 16 && localZ >= 0 && localZ < 16) {
                    continue;
                }
                byte north = lz > 0 ? this.flags[(lz - 1) * this.sizeX + lx] : NONE;
                byte south = lz + 1 < this.sizeZ ? this.flags[(lz + 1) * this.sizeX + lx] : NONE;
                byte west = lx > 0 ? this.flags[lz * this.sizeX + (lx - 1)] : NONE;
                byte east = lx + 1 < this.sizeX ? this.flags[lz * this.sizeX + (lx + 1)] : NONE;
                this.flags[lz * this.sizeX + lx] = MegaGigaChunkCache.mergeNeighborFlags(north, south, west, east);
            }
        }
    }

    private static byte mergeNeighborFlags(byte north, byte south, byte west, byte east) {
        byte best = NONE;
        for (byte flag : new byte[]{north, south, west, east}) {
            if (flag == GIGA) {
                return GIGA;
            }
            if (flag == MEGA) {
                best = MEGA;
            }
        }
        return best;
    }

    private void clear() {
        this.minX = Integer.MIN_VALUE;
        this.populated = false;
        this.boundGenerator = null;
        this.boundColumnCache = null;
    }

    private static byte sampleMegaOrGiga(Generator generator, int x, int z) {
        int seed = Seeds.get(generator.getSeed());
        return MegaGigaChunkCache.classify(generator, seed, CaveModifiers.mega(), CaveModifiers.giga(), x, z);
    }

    private static byte classify(Generator generator, int seed, Module mega, Module giga, int x, int z) {
        if (CaveNoise.sample(giga, seed, x, z) > GIGA_THRESHOLD && CaveReliefFilter.qualifiesGigaTerrain(generator, x, z)) {
            return GIGA;
        }
        if (CaveNoise.sample(mega, seed, x, z) > MEGA_THRESHOLD) {
            return MEGA;
        }
        return NONE;
    }
}
