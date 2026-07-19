package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.Module;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Minimal per-chunk mega/giga footprint cache for live carve.
 * Activates whole system cells (not only UniqueCaveDistributor ridges).
 */
final class CarverColumnCache {
    static final byte ZONE_NONE = 0;
    static final byte ZONE_MEGA = 1;
    static final byte ZONE_GIGA = 2;
    private static final float MEGA_THRESHOLD = 0.08f;
    private static final float GIGA_THRESHOLD = 0.07f;
    private static final float MEGA_RELAX_THRESHOLD = 0.05f;
    private static final float GIGA_RELAX_THRESHOLD = 0.05f;
    private static final int MAX_TERRAIN_INFLATION = 12;

    private final int[] surfaceY = new int[256];
    private final byte[] zone = new byte[256];
    private final boolean[] oceanBlocked = new boolean[256];
    private boolean megaPresent;
    private boolean gigaPresent;
    private int cachedStartX;
    private int cachedStartZ;

    void build(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        this.cachedStartX = startX;
        this.cachedStartZ = startZ;
        this.megaPresent = false;
        this.gigaPresent = false;
        TerrainData terrain = carver.terrainData;
        int sea = generator.getSeaLevel();

        for (int i = 0; i < 256; ++i) {
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = startX + dx;
            int z = startZ + dz;
            int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
            if (terrain != null) {
                int terrainH = terrain.getHeight(dx, dz);
                surface = Math.max(surface, Math.min(terrainH, surface + MAX_TERRAIN_INFLATION));
            }
            this.surfaceY[i] = surface;
            byte flags = ZONE_NONE;
            if (carver.megaModifier != null) {
                float mega = CaveNoise.sampleMerged(carver.megaModifier, seed, x, z);
                if (mega > MEGA_THRESHOLD) {
                    flags = (byte) (flags | ZONE_MEGA);
                    this.megaPresent = true;
                }
            }
            if (carver.gigaModifier != null) {
                float giga = CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z);
                if (giga > GIGA_THRESHOLD) {
                    flags = (byte) (flags | ZONE_GIGA);
                    this.gigaPresent = true;
                }
            }
            this.zone[i] = flags;
            this.oceanBlocked[i] = surface <= sea;
        }

        this.ensureMegaGigaCoverage(seed, chunk, carver, sea);
        if (!this.megaPresent && !this.gigaPresent) {
            this.ensureFootprintSystemEligibility(seed, carver, startX, startZ);
            this.ensureBorderConnectivity(seed, carver, startX, startZ);
            this.fillMegaGigaZoneGaps(seed, carver, true);
        } else {
            this.fillMegaGigaZoneGaps(seed, carver, false);
            this.ensureMegaGigaFullChunkCarve();
        }
    }

    private void ensureMegaGigaCoverage(int seed, ChunkAccess chunk, CarverChunk carver, int sea) {
        if (this.megaPresent && this.gigaPresent) {
            return;
        }
        int[][] probes = {{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}, {4, 4}, {11, 11}, {4, 11}, {11, 4}};
        TerrainData terrain = carver.terrainData;
        for (int[] probe : probes) {
            int dx = probe[0];
            int dz = probe[1];
            int i = dx | dz << 4;
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            byte flags = this.zone[i];
            if (!this.megaPresent && carver.megaModifier != null && (flags & ZONE_MEGA) == 0) {
                if (CaveNoise.sampleMerged(carver.megaModifier, seed, x, z) > MEGA_RELAX_THRESHOLD) {
                    flags = (byte) (flags | ZONE_MEGA);
                    this.zone[i] = flags;
                    this.megaPresent = true;
                    int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                    if (terrain != null) {
                        int terrainH = terrain.getHeight(dx, dz);
                        surface = Math.max(surface, Math.min(terrainH, surface + MAX_TERRAIN_INFLATION));
                    }
                    this.oceanBlocked[i] = surface <= sea;
                }
            }
            if (!this.gigaPresent && carver.gigaModifier != null && (this.zone[i] & ZONE_GIGA) == 0) {
                if (CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z) > GIGA_RELAX_THRESHOLD) {
                    this.zone[i] = (byte) (this.zone[i] | ZONE_GIGA);
                    this.gigaPresent = true;
                }
            }
            if (this.megaPresent && this.gigaPresent) {
                return;
            }
        }
    }

    private void fillMegaGigaZoneGaps(int seed, CarverChunk carver, boolean aggressive) {
        if ((!this.megaPresent && !this.gigaPresent) || carver.megaModifier == null) {
            return;
        }
        int noneCount = 0;
        for (byte zoneFlag : this.zone) {
            if (zoneFlag == ZONE_NONE) {
                ++noneCount;
            }
        }
        if (noneCount == 0) {
            return;
        }
        int stride = aggressive ? 1 : (noneCount > 48 ? 2 : 1);
        for (int i = 0; i < 256; i += stride) {
            if (this.zone[i] != ZONE_NONE) {
                continue;
            }
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            if (CaveNoise.sampleMerged(carver.megaModifier, seed, x, z) > MEGA_RELAX_THRESHOLD) {
                this.zone[i] = (byte) (this.zone[i] | ZONE_MEGA);
                this.megaPresent = true;
            }
            if (carver.gigaModifier != null
                && CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z) > GIGA_RELAX_THRESHOLD) {
                this.zone[i] = (byte) (this.zone[i] | ZONE_GIGA);
                this.gigaPresent = true;
            }
        }
        if (stride > 1) {
            this.ensureMegaGigaFullChunkCarve();
        }
    }

    private void ensureBorderConnectivity(int seed, CarverChunk carver, int startX, int startZ) {
        if (this.megaPresent || this.gigaPresent) {
            return;
        }
        for (int d = 0; d < 16; ++d) {
            this.probeBorderColumn(seed, carver, startX - 1, startZ + d);
            this.probeBorderColumn(seed, carver, startX + 16, startZ + d);
            this.probeBorderColumn(seed, carver, startX + d, startZ - 1);
            this.probeBorderColumn(seed, carver, startX + d, startZ + 16);
        }
    }

    private void probeBorderColumn(int seed, CarverChunk carver, int x, int z) {
        if (!this.megaPresent && carver.megaModifier != null
            && CaveNoise.sampleMerged(carver.megaModifier, seed, x, z) > MEGA_RELAX_THRESHOLD) {
            this.megaPresent = true;
        }
        if (!this.gigaPresent && carver.gigaModifier != null
            && CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z) > GIGA_RELAX_THRESHOLD) {
            this.gigaPresent = true;
        }
    }

    /** Activate chunk when it sits inside an active mega/giga system cell. */
    private void ensureFootprintSystemEligibility(int seed, CarverChunk carver, int startX, int startZ) {
        if (!this.megaPresent) {
            this.tryFootprintActivation(seed, carver, startX, startZ, CaveType.MEGA, carver.megaModifier);
        }
        if (!this.gigaPresent) {
            this.tryFootprintActivation(seed, carver, startX, startZ, CaveType.GIGA, carver.gigaModifier);
        }
    }

    private void tryFootprintActivation(int seed, CarverChunk carver, int startX, int startZ, CaveType type, Module modifier) {
        if (modifier == null || !this.chunkOverlapsFootprint(startX, startZ, type)) {
            return;
        }
        int cx = startX + 8;
        int cz = startZ + 8;
        int snapCx = CaveSystemBounds.snapCenter(cx, type);
        int snapCz = CaveSystemBounds.snapCenter(cz, type);
        if (!CaveSystemBounds.hasCarveInfluence(seed, snapCx, snapCz, type)
            && !this.chunkHasMergedActivity(seed, modifier, startX, startZ, type)) {
            return;
        }
        if (type == CaveType.MEGA) {
            this.megaPresent = true;
        } else {
            this.gigaPresent = true;
        }
        // Seed interior columns so matches() passes for this system.
        byte flag = type == CaveType.GIGA ? ZONE_GIGA : ZONE_MEGA;
        for (int i = 0; i < 256; ++i) {
            int dx = i & 0xF;
            int dz = i >> 4;
            if (CaveSystemBounds.isWithinFootprint(startX + dx, startZ + dz, type)) {
                float noise = CaveNoise.sampleMerged(modifier, seed, startX + dx, startZ + dz);
                float threshold = type == CaveType.GIGA ? GIGA_RELAX_THRESHOLD : MEGA_RELAX_THRESHOLD;
                if (noise > threshold || this.hasMegaGigaNeighbor(dx, dz)) {
                    this.zone[i] = (byte) (this.zone[i] | flag | ZONE_MEGA);
                }
            }
        }
        this.ensureMegaGigaFullChunkCarve();
    }

    private boolean chunkOverlapsFootprint(int startX, int startZ, CaveType type) {
        int[][] probes = {{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}, {0, 8}, {8, 0}, {15, 8}, {8, 15}};
        for (int[] probe : probes) {
            if (CaveSystemBounds.isWithinFootprint(startX + probe[0], startZ + probe[1], type)) {
                return true;
            }
        }
        return false;
    }

    private boolean chunkHasMergedActivity(int seed, Module modifier, int startX, int startZ, CaveType type) {
        float threshold = type == CaveType.GIGA ? GIGA_RELAX_THRESHOLD : MEGA_RELAX_THRESHOLD;
        int[][] probes = {{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}};
        for (int[] probe : probes) {
            if (CaveNoise.sampleMerged(modifier, seed, startX + probe[0], startZ + probe[1]) > threshold) {
                return true;
            }
        }
        return false;
    }

    private void ensureMegaGigaFullChunkCarve() {
        if (!this.megaPresent && !this.gigaPresent) {
            return;
        }
        for (int pass = 0; pass < 3; ++pass) {
            boolean expanded = false;
            for (int i = 0; i < 256; ++i) {
                if (this.zone[i] != ZONE_NONE || this.oceanBlocked[i]) {
                    continue;
                }
                int dx = i & 0xF;
                int dz = i >> 4;
                if (!this.hasMegaGigaNeighbor(dx, dz)) {
                    continue;
                }
                byte flags = this.zone[i];
                if (this.gigaPresent) {
                    flags = (byte) (flags | ZONE_GIGA);
                }
                flags = (byte) (flags | ZONE_MEGA);
                this.zone[i] = flags;
                this.megaPresent = true;
                expanded = true;
            }
            if (!expanded) {
                break;
            }
        }
    }

    private boolean hasMegaGigaNeighbor(int dx, int dz) {
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                    continue;
                }
                if (this.zone[this.index(px, pz)] != ZONE_NONE) {
                    return true;
                }
            }
        }
        return false;
    }

    private int index(int dx, int dz) {
        return (dx & 0xF) | (dz & 0xF) << 4;
    }

    private byte zone(int dx, int dz) {
        return this.zone[this.index(dx, dz)];
    }

    int surfaceY(int dx, int dz) {
        return this.surfaceY[this.index(dx, dz)];
    }

    boolean hasMega() {
        return this.megaPresent;
    }

    boolean hasGiga() {
        return this.gigaPresent;
    }

    boolean isMegaGigaZone(int dx, int dz) {
        return this.zone(dx, dz) != ZONE_NONE;
    }

    boolean matches(CaveType type, int dx, int dz) {
        if (type == CaveType.GLOBAL || type == CaveType.UNIQUE) {
            // Synapse/unique still carve via their own modifier; zone cache is mega/giga only.
            return true;
        }
        byte flags = this.zone(dx, dz);
        return switch (type) {
            case MEGA -> (flags & ZONE_MEGA) != 0;
            case GIGA -> (flags & ZONE_GIGA) != 0;
            default -> flags != ZONE_NONE;
        };
    }

    boolean anyMegaGiga() {
        return this.megaPresent || this.gigaPresent;
    }

    boolean oceanBlocked(int dx, int dz) {
        return this.oceanBlocked[this.index(dx, dz)];
    }

    byte megaGigaFlag(int dx, int dz) {
        byte z = this.zone(dx, dz);
        if ((z & ZONE_GIGA) != 0) {
            return MegaGigaZoneProbe.GIGA;
        }
        if ((z & ZONE_MEGA) != 0) {
            return MegaGigaZoneProbe.MEGA;
        }
        return MegaGigaZoneProbe.NONE;
    }
}
