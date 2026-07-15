package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.Module;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Per-chunk column data computed once before cave passes.
 */
final class CarverColumnCache {
    static final byte ZONE_NONE = 0;
    static final byte ZONE_MEGA = 1;
    static final byte ZONE_GIGA = 2;
    static final byte ZONE_BOTH = 3;
    static final byte FLAG_SKIP_TREE = 1;
    static final byte FLAG_OPEN_AIR = 4;
    private static final float MEGA_THRESHOLD = 0.08f;
    private static final float GIGA_THRESHOLD = 0.07f;
    private static final float MEGA_RELAX_THRESHOLD = 0.05f;
    private static final float GIGA_RELAX_THRESHOLD = 0.05f;
    private static final int MIN_SYNAPSE_CAVERN = 1;
    /** Max excess of terrain-data height over heightmap — river/lake banks inflate far higher. */
    private static final int MAX_TERRAIN_INFLATION = 12;
    private final int[] surfaceY = new int[256];
    private final byte[] zone = new byte[256];
    private final boolean[] synapseEligible = new boolean[256];
    private final float[] gradient = new float[256];
    private final boolean[] nearRiver = new boolean[256];
    private final boolean[] gradientReady = new boolean[256];
    private final boolean[] nearRiverReady = new boolean[256];
    private final boolean[] oceanBlocked = new boolean[256];
    private final boolean[] riverCarveBlocked = new boolean[256];
    private final boolean[] riverSurfaceSuppressed = new boolean[256];
    static final byte ENVELOPE_SUPPRESS_BREACH = 1;
    static final byte ENVELOPE_ENTRANCE = 2;
    private final byte[] sampleShiftX = new byte[256];
    private final byte[] sampleShiftZ = new byte[256];
    private final byte[] extraCenterDrop = new byte[256];
    private final byte[] envelopeFlags = new byte[256];
    private final byte[] decorationFlags = new byte[256];
    private boolean megaPresent;
    private boolean gigaPresent;
    private boolean synapseEligibleBuilt;
    private boolean anySynapseEligible;
    private boolean chunkNearSea;
    private boolean chunkMayHaveRiver;
    private boolean envelopeBuilt;
    private boolean chunkInland;
    private boolean chunkMassif;
    private boolean chunkGigaRelief;
    private boolean decorationFlagsBuilt;
    private int cachedStartX;
    private int cachedStartZ;
    private Generator cachedGenerator;
    private TerrainData cachedTerrain;

    void build(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator) {
        java.util.Arrays.fill(this.envelopeFlags, (byte)0);
        java.util.Arrays.fill(this.sampleShiftX, (byte)0);
        java.util.Arrays.fill(this.sampleShiftZ, (byte)0);
        java.util.Arrays.fill(this.extraCenterDrop, (byte)0);
        java.util.Arrays.fill(this.gradientReady, false);
        java.util.Arrays.fill(this.nearRiverReady, false);
        java.util.Arrays.fill(this.synapseEligible, false);
        this.synapseEligibleBuilt = false;
        this.anySynapseEligible = false;
        this.envelopeBuilt = false;
        this.decorationFlagsBuilt = false;
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        this.cachedStartX = startX;
        this.cachedStartZ = startZ;
        this.cachedGenerator = generator;
        this.cachedTerrain = carver.terrainData;
        TerrainData terrain = carver.terrainData;
        this.megaPresent = false;
        this.gigaPresent = false;
        int centerX = startX + 8;
        int centerZ = startZ + 8;
        this.chunkNearSea = CaveOceanProximityCache.chunkNearSea(generator, centerX, centerZ);
        this.chunkMayHaveRiver = CaveRiverProximityCache.chunkMayHaveRiver(generator, centerX, centerZ);
        this.chunkInland = generator.getTerrainSample(centerX, centerZ).continentNoise >= 0.25f;
        this.chunkMassif = false;
        this.chunkGigaRelief = CaveReliefFilter.qualifiesGigaTerrain(generator, centerX, centerZ);
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
            if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                surface = Math.min(surface, chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz));
            }
            this.surfaceY[i] = surface;
            byte flags = ZONE_NONE;
            if (carver.megaModifier != null) {
                float mega = CaveNoise.sampleMerged(carver.megaModifier, seed, x, z);
                if (mega > MEGA_THRESHOLD) {
                    flags = (byte)(flags | ZONE_MEGA);
                    this.megaPresent = true;
                }
            }
            if (carver.gigaModifier != null) {
                float giga = CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z);
                if (giga > GIGA_THRESHOLD) {
                    flags = (byte)(flags | ZONE_GIGA);
                    this.gigaPresent = true;
                }
            }
            this.zone[i] = flags;
            if (flags == ZONE_NONE) {
                this.oceanBlocked[i] = false;
                continue;
            }
            this.oceanBlocked[i] = surface <= sea;
        }
        this.ensureMegaGigaCoverage(seed, chunk, carver, generator, sea);
        if (!this.megaPresent && !this.gigaPresent) {
            this.ensureFootprintSystemEligibility(seed, carver, generator, startX, startZ);
            this.ensureBorderConnectivity(seed, carver, generator, startX, startZ);
            this.fillMegaGigaZoneGaps(seed, carver, generator, true);
        } else {
            this.fillMegaGigaZoneGaps(seed, carver, generator, false);
            this.ensureMegaGigaFullChunkCarve();
        }
        this.computeMegaGigaCenterDrops();
        if (this.megaPresent || this.gigaPresent) {
            this.chunkMassif = CaveMassifCache.qualifiesMountainMassif(generator, seed, centerX, centerZ);
        }
        this.buildRiverGuards(chunk, sea);
    }

    /** Block synapse/mega/giga carve in river beds, narrow channels, and low-bank corridors. */
    private void buildRiverGuards(ChunkAccess chunk, int sea) {
        java.util.Arrays.fill(this.riverCarveBlocked, false);
        java.util.Arrays.fill(this.riverSurfaceSuppressed, false);
        for (int i = 0; i < 256; ++i) {
            int dx = i & 0xF;
            int dz = i >> 4;
            if (!this.isRiverCorridorColumn(chunk, dx, dz, sea)) {
                continue;
            }
            this.riverCarveBlocked[i] = true;
            this.riverSurfaceSuppressed[i] = true;
        }
    }

    private boolean isRiverCorridorColumn(ChunkAccess chunk, int dx, int dz, int sea) {
        if (this.cachedTerrain != null && CaveChunkSurfaceRepair.isRiverBedColumn(this.cachedTerrain, dx, dz)) {
            return true;
        }
        if (CaveOceanFilter.isSubmergedWaterColumn(chunk, dx, dz, sea)) {
            return true;
        }
        if (this.surfaceY(dx, dz) > sea && this.nearRiver(dx, dz)) {
            return true;
        }
        if (CaveOceanFilter.hasSubmergedWaterNeighborInChunk(chunk, dx, dz, sea, 12)) {
            return true;
        }
        if (this.nearRiver(dx, dz) && this.localTerrainDip(dx, dz) >= 2) {
            return true;
        }
        if (!this.chunkMayHaveRiver) {
            return false;
        }
        int x = this.cachedStartX + dx;
        int z = this.cachedStartZ + dz;
        float river = this.cachedGenerator.getTerrainSample(x, z).riverNoise;
        if (river >= 0.90f) {
            return false;
        }
        return river < 0.72f;
    }

    boolean riverCarveBlocked(int dx, int dz) {
        return this.riverCarveBlocked[this.index(dx, dz)];
    }

    boolean riverSurfaceSuppressed(int dx, int dz) {
        return this.riverSurfaceSuppressed[this.index(dx, dz)];
    }

    boolean anyRiverCarveBlocked() {
        for (boolean blocked : this.riverCarveBlocked) {
            if (blocked) {
                return true;
            }
        }
        return false;
    }

    /** When no column passed the primary threshold, probe corners so mega/giga caves do not leave solid chunk pillars. */
    private void ensureMegaGigaCoverage(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, int sea) {
        if (this.megaPresent && this.gigaPresent) {
            return;
        }
        int[][] probes = new int[][]{{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}, {4, 4}, {11, 11}, {4, 11}, {11, 4}};
        TerrainData terrain = carver.terrainData;
        for (int[] probe : probes) {
            int dx = probe[0];
            int dz = probe[1];
            int i = dx | dz << 4;
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            byte flags = this.zone[i];
            if (!this.megaPresent && carver.megaModifier != null && (flags & ZONE_MEGA) == 0) {
                float mega = CaveNoise.sampleMerged(carver.megaModifier, seed, x, z);
                if (mega > MEGA_RELAX_THRESHOLD) {
                    flags = (byte)(flags | ZONE_MEGA);
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
                float giga = CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z);
                if (giga > GIGA_RELAX_THRESHOLD) {
                    this.zone[i] = (byte)(this.zone[i] | ZONE_GIGA);
                    this.gigaPresent = true;
                }
            }
            if (this.megaPresent && this.gigaPresent) {
                return;
            }
        }
    }

    /** Fill ZONE_NONE columns inside chunks that already have mega/giga (single pass; optional stride for perf). */
    private void fillMegaGigaZoneGaps(int seed, CarverChunk carver, Generator generator, boolean aggressive) {
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
        float threshold = aggressive ? MEGA_RELAX_THRESHOLD : MEGA_RELAX_THRESHOLD;
        for (int i = 0; i < 256; i += stride) {
            if (this.zone[i] != ZONE_NONE) {
                continue;
            }
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            float mega = CaveNoise.sampleMerged(carver.megaModifier, seed, x, z);
            if (mega > threshold) {
                this.zone[i] = (byte)(this.zone[i] | ZONE_MEGA);
                this.megaPresent = true;
            }
        }
        if (stride > 1) {
            this.ensureMegaGigaFullChunkCarve();
        }
    }

    /** @deprecated merged into {@link #fillMegaGigaZoneGaps} */
    private void ensureMegaGigaGapFill(int seed, CarverChunk carver, Generator generator) {
        this.fillMegaGigaZoneGaps(seed, carver, generator, false);
    }

    /** Sample just outside chunk borders when the interior has no mega/giga activity. */
    private void ensureBorderConnectivity(int seed, CarverChunk carver, Generator generator, int startX, int startZ) {
        if (this.megaPresent || this.gigaPresent) {
            return;
        }
        for (int d = 0; d < 16; ++d) {
            this.probeBorderColumn(seed, carver, generator, startX - 1, startZ + d);
            this.probeBorderColumn(seed, carver, generator, startX + 16, startZ + d);
            this.probeBorderColumn(seed, carver, generator, startX + d, startZ - 1);
            this.probeBorderColumn(seed, carver, generator, startX + d, startZ + 16);
        }
        this.probeBorderColumn(seed, carver, generator, startX - 1, startZ - 1);
        this.probeBorderColumn(seed, carver, generator, startX + 16, startZ - 1);
        this.probeBorderColumn(seed, carver, generator, startX - 1, startZ + 16);
        this.probeBorderColumn(seed, carver, generator, startX + 16, startZ + 16);
    }

    private void probeBorderColumn(int seed, CarverChunk carver, Generator generator, int x, int z) {
        if (!this.megaPresent && carver.megaModifier != null) {
            if (CaveNoise.sampleMerged(carver.megaModifier, seed, x, z) > MEGA_RELAX_THRESHOLD) {
                this.megaPresent = true;
            }
        }
        if (!this.gigaPresent && carver.gigaModifier != null) {
            if (CaveNoise.sampleMerged(carver.gigaModifier, seed, x, z) > GIGA_RELAX_THRESHOLD) {
                this.gigaPresent = true;
            }
        }
    }

    /** Activate chunk when it sits inside an active mega/giga system cell (footprint), not only on local noise peaks. */
    private void ensureFootprintSystemEligibility(int seed, CarverChunk carver, Generator generator, int startX, int startZ) {
        if (!this.megaPresent) {
            this.tryFootprintActivation(seed, carver, generator, startX, startZ, CaveType.MEGA, carver.megaModifier);
        }
        if (!this.gigaPresent) {
            this.tryFootprintActivation(seed, carver, generator, startX, startZ, CaveType.GIGA, carver.gigaModifier);
        }
    }

    private void tryFootprintActivation(int seed, CarverChunk carver, Generator generator, int startX, int startZ, CaveType type, Module modifier) {
        if (modifier == null || !this.chunkOverlapsFootprint(startX, startZ, type)) {
            return;
        }
        int cx = startX + 8;
        int cz = startZ + 8;
        int snapCx = CaveSystemBounds.snapCenter(cx, type);
        int snapCz = CaveSystemBounds.snapCenter(cz, type);
        if (!CaveSystemBounds.hasCarveInfluence(seed, snapCx, snapCz, type) && !this.chunkHasMergedActivity(seed, modifier, generator, startX, startZ, type)) {
            return;
        }
        if (type == CaveType.MEGA) {
            this.megaPresent = true;
        } else {
            this.gigaPresent = true;
        }
    }

    private boolean chunkOverlapsFootprint(int startX, int startZ, CaveType type) {
        int[][] probes = new int[][]{{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}, {0, 8}, {8, 0}, {15, 8}, {8, 15}};
        for (int[] probe : probes) {
            if (CaveSystemBounds.isWithinFootprint(startX + probe[0], startZ + probe[1], type)) {
                return true;
            }
        }
        return false;
    }

    private boolean chunkHasMergedActivity(int seed, Module modifier, Generator generator, int startX, int startZ, CaveType type) {
        for (int i = 0; i < 256; i += 4) {
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = startX + dx;
            int z = startZ + dz;
            if (!CaveSystemBounds.isWithinFootprint(x, z, type)) {
                continue;
            }
            if (CaveNoise.sampleMerged(modifier, seed, x, z) > MEGA_RELAX_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /** Lower cavern-size bar when a thinned chunk would otherwise carve nothing. */
    void relaxSynapseEligibility(NoiseCave synapse, int seed) {
        if (this.anySynapseEligible || synapse == null || this.megaPresent || this.gigaPresent) {
            return;
        }
        for (int i = 0; i < 256; i += 2) {
            if (this.zone[i] != ZONE_NONE) {
                continue;
            }
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            if (synapse.getCavernSize(seed, x, z, 1.0f) >= MIN_SYNAPSE_CAVERN) {
                this.anySynapseEligible = true;
                return;
            }
        }
    }

    /** Full-column synapse scan — sparse chunk gate + border columns only in mega/giga chunks. */
    void ensureSynapseEligibility(NoiseCave synapse, int seed) {
        if (this.synapseEligibleBuilt || synapse == null) {
            return;
        }
        this.synapseEligibleBuilt = true;
        this.anySynapseEligible = false;
        for (int i = 0; i < 256; i += 4) {
            if (this.probeSynapseColumn(synapse, seed, i)) {
                break;
            }
        }
        if (!this.anySynapseEligible) {
            for (int i = 0; i < 256; ++i) {
                if ((i & 3) != 0 && this.probeSynapseColumn(synapse, seed, i)) {
                    break;
                }
            }
        }
        if (!this.anySynapseEligible) {
            this.relaxSynapseEligibility(synapse, seed);
        }
        this.ensureSynapseBorderConnectivity(synapse, seed);
    }

    /**
     * When synapse noise is weak inside a chunk but active just across the border, keep the GLOBAL pass
     * and border columns eligible — prevents vertical chunk-face walls in continuous synapse networks.
     */
    private void ensureSynapseBorderConnectivity(NoiseCave synapse, int seed) {
        int startX = this.cachedStartX;
        int startZ = this.cachedStartZ;
        for (int d = 0; d < 16; ++d) {
            this.probeSynapseOutside(synapse, seed, startX - 1, startZ + d, 0, d);
            this.probeSynapseOutside(synapse, seed, startX + 16, startZ + d, 15, d);
            this.probeSynapseOutside(synapse, seed, startX + d, startZ - 1, d, 0);
            this.probeSynapseOutside(synapse, seed, startX + d, startZ + 16, d, 15);
        }
        this.probeSynapseOutside(synapse, seed, startX - 1, startZ - 1, 0, 0);
        this.probeSynapseOutside(synapse, seed, startX + 16, startZ - 1, 15, 0);
        this.probeSynapseOutside(synapse, seed, startX - 1, startZ + 16, 0, 15);
        this.probeSynapseOutside(synapse, seed, startX + 16, startZ + 16, 15, 15);
    }

    private void probeSynapseOutside(NoiseCave synapse, int seed, int wx, int wz, int borderDx, int borderDz) {
        if (synapse.getCavernSize(seed, wx, wz, 1.0f) < MIN_SYNAPSE_CAVERN) {
            return;
        }
        this.anySynapseEligible = true;
        int i = this.index(borderDx, borderDz);
        if (this.zone[i] != ZONE_NONE && (this.megaPresent || this.gigaPresent)) {
            this.synapseEligible[i] = true;
        }
    }

    /** Sample synapse cavern at this column, borrowing from just-outside chunk when on a border and local noise is dry. */
    SynapseSample resolveSynapseSample(NoiseCave config, Module modifier, int seed, int wx, int wz, int dx, int dz) {
        int sampleX = wx + this.sampleShiftX(dx, dz);
        int sampleZ = wz + this.sampleShiftZ(dx, dz);
        float value = CaveNoise.sample(modifier, seed, sampleX, sampleZ);
        int cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
        if (cavern >= MIN_SYNAPSE_CAVERN || !this.isBorderColumn(dx, dz)) {
            return new SynapseSample(sampleX, sampleZ, value, cavern, false);
        }
        int bestCavern = cavern;
        int bestX = sampleX;
        int bestZ = sampleZ;
        float bestValue = value;
        for (int[] step : this.outwardBorderSteps(dx, dz)) {
            int nx = wx + step[0];
            int nz = wz + step[1];
            float neighborValue = CaveNoise.sample(modifier, seed, nx, nz);
            int neighborCavern = config.getCavernSize(seed, nx, nz, neighborValue);
            if (neighborCavern > bestCavern) {
                bestCavern = neighborCavern;
                bestX = nx;
                bestZ = nz;
                bestValue = neighborValue;
            }
        }
        return new SynapseSample(bestX, bestZ, bestValue, bestCavern, bestCavern > cavern);
    }

    private int[][] outwardBorderSteps(int dx, int dz) {
        if (dx == 0) {
            if (dz == 0) {
                return new int[][]{{-1, 0}, {0, -1}, {-1, -1}};
            }
            if (dz == 15) {
                return new int[][]{{-1, 0}, {0, 1}, {-1, 1}};
            }
            return new int[][]{{-1, 0}, {-1, -1}, {-1, 1}};
        }
        if (dx == 15) {
            if (dz == 0) {
                return new int[][]{{1, 0}, {0, -1}, {1, -1}};
            }
            if (dz == 15) {
                return new int[][]{{1, 0}, {0, 1}, {1, 1}};
            }
            return new int[][]{{1, 0}, {1, -1}, {1, 1}};
        }
        if (dz == 0) {
            return new int[][]{{0, -1}, {-1, -1}, {1, -1}};
        }
        if (dz == 15) {
            return new int[][]{{0, 1}, {-1, 1}, {1, 1}};
        }
        return new int[0][0];
    }

    boolean isBorderColumn(int dx, int dz) {
        return this.isChunkBorder(dx, dz);
    }

    static final class SynapseSample {
        final int sampleX;
        final int sampleZ;
        final float modifierValue;
        final int cavern;
        final boolean stitchedFromNeighbor;

        SynapseSample(int sampleX, int sampleZ, float modifierValue, int cavern, boolean stitchedFromNeighbor) {
            this.sampleX = sampleX;
            this.sampleZ = sampleZ;
            this.modifierValue = modifierValue;
            this.cavern = cavern;
            this.stitchedFromNeighbor = stitchedFromNeighbor;
        }
    }

    void invalidateDecorationFlags() {
        this.decorationFlagsBuilt = false;
    }

    /** Cheap slope estimate from cached heightmap — avoids terrain gradient sampling per column. */
    boolean localSurfaceSlope(int dx, int dz, int minDelta) {
        int center = this.surfaceY[this.index(dx, dz)];
        for (int ox = -2; ox <= 2; ++ox) {
            for (int oz = -2; oz <= 2; ++oz) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                    continue;
                }
                if (Math.abs(center - this.surfaceY[this.index(px, pz)]) >= minDelta) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureMegaGigaCoverageAggressive(int seed, CarverChunk carver, Generator generator) {
        this.fillMegaGigaZoneGaps(seed, carver, generator, true);
    }

    /** Fill isolated columns adjacent to mega/giga so chunk interiors carve cohesively (v0.4.3 model). */
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
                    flags = (byte)(flags | ZONE_GIGA);
                }
                flags = (byte)(flags | ZONE_MEGA);
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

    private boolean isChunkBorder(int dx, int dz) {
        return dx == 0 || dx == 15 || dz == 0 || dz == 15;
    }

    /**
     * Lower noise center only under river columns so carve shifts down instead of clipping the ceiling.
     * Disabled while per-column carve is preferred over landscape fitting.
     */
    private void computeMegaGigaCenterDrops() {
    }

    /**
     * Sparse probe — early exit on first eligible column; fills remaining columns only when probe misses.
     */
    void probeSynapseEligibility(NoiseCave synapse, int seed) {
        if (this.synapseEligibleBuilt || synapse == null) {
            return;
        }
        this.synapseEligibleBuilt = true;
        this.anySynapseEligible = false;
        for (int i = 0; i < 256; i += 4) {
            if (this.probeSynapseColumn(synapse, seed, i)) {
                return;
            }
        }
        for (int i = 0; i < 256; ++i) {
            if ((i & 3) == 0) {
                continue;
            }
            if (this.probeSynapseColumn(synapse, seed, i)) {
                return;
            }
        }
    }

    private boolean probeSynapseColumn(NoiseCave synapse, int seed, int i) {
        int dx = i & 0xF;
        int dz = i >> 4;
        if (this.zone[i] != ZONE_NONE && (this.megaPresent || this.gigaPresent) && !this.isChunkBorder(dx, dz)) {
            return false;
        }
        int x = this.cachedStartX + dx;
        int z = this.cachedStartZ + dz;
        if (synapse.getCavernSize(seed, x, z, 1.0f) < MIN_SYNAPSE_CAVERN) {
            return false;
        }
        this.synapseEligible[i] = this.isChunkBorder(dx, dz) && (this.megaPresent || this.gigaPresent);
        this.anySynapseEligible = true;
        return true;
    }

    void buildSynapseEligibility(NoiseCave synapse, int seed) {
        this.probeSynapseEligibility(synapse, seed);
        this.ensureSynapseBorderConnectivity(synapse, seed);
    }

    boolean anySynapseEligible() {
        return this.anySynapseEligible;
    }

    void buildDecorationFlags(CarverChunk carver, ChunkAccess chunk) {
        if (this.decorationFlagsBuilt) {
            return;
        }
        boolean anyMegaGiga = this.anyMegaGiga();
        for (int i = 0; i < 256; ++i) {
            int dx = i & 0xF;
            int dz = i >> 4;
            byte flags = 0;
            boolean megaGiga = this.zone[i] != ZONE_NONE;
            if (megaGiga) {
                flags = (byte)(flags | FLAG_SKIP_TREE);
            }
            if (anyMegaGiga && megaGiga && CarverColumnCache.hasOpenCaveAir(chunk, dx, dz)) {
                flags = (byte)(flags | FLAG_SKIP_TREE);
            }
            int surface = this.surfaceY[i];
            int probeY = Math.max(chunk.getMinBuildHeight(), surface - 1);
            if (CaveOpenAirCheck.isOpenAir(chunk, dx, probeY, dz)) {
                flags = (byte)(flags | FLAG_OPEN_AIR);
                if (megaGiga) {
                    flags = (byte)(flags | FLAG_SKIP_TREE);
                }
            }
            if (carver.isEntranceColumn(dx, dz) && !megaGiga) {
                flags = (byte)(flags & ~FLAG_SKIP_TREE);
            }
            if ((flags & FLAG_SKIP_TREE) == 0) {
                int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                if (floor < surface - 6 && CaveOpenAirCheck.isOpenAir(chunk, dx, floor, dz)) {
                    flags = (byte)(flags | FLAG_SKIP_TREE);
                }
            }
            this.decorationFlags[i] = flags;
        }
        this.decorationFlagsBuilt = true;
    }

    private static boolean hasOpenCaveAir(ChunkAccess chunk, int lx, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        int air = 0;
        for (int dy = 1; dy <= 6; ++dy) {
            int py = surface + dy;
            if (py > chunk.getMaxBuildHeight()) {
                break;
            }
            if (!chunk.getBlockState(new net.minecraft.core.BlockPos(lx, py, lz)).isAir()) {
                break;
            }
            if (++air >= 4) {
                return true;
            }
        }
        return false;
    }

    private float computeGradient(int dx, int dz) {
        int x = this.cachedStartX + dx;
        int z = this.cachedStartZ + dz;
        if (this.chunkMayHaveRiver) {
            return CaveOceanFilter.sampleHeightGradient(this.cachedGenerator, x, z);
        }
        if (this.cachedTerrain != null) {
            return this.cachedTerrain.getGradient(dx, dz, 55.0f * ((float)this.cachedGenerator.getGenDepth() / 255.0f));
        }
        return CaveOceanFilter.sampleHeightGradient(this.cachedGenerator, x, z);
    }

    int index(int dx, int dz) {
        return dz << 4 | dx;
    }

    int surfaceY(int dx, int dz) {
        return this.surfaceY[this.index(dx, dz)];
    }

    byte zone(int dx, int dz) {
        return this.zone[this.index(dx, dz)];
    }

    float gradient(int dx, int dz) {
        int i = this.index(dx, dz);
        if (!this.gradientReady[i]) {
            this.gradient[i] = this.computeGradient(dx, dz);
            this.gradientReady[i] = true;
        }
        return this.gradient[i];
    }

    boolean nearRiver(int dx, int dz) {
        if (!this.chunkMayHaveRiver) {
            return false;
        }
        int i = this.index(dx, dz);
        if (!this.nearRiverReady[i]) {
            int x = this.cachedStartX + dx;
            int z = this.cachedStartZ + dz;
            this.nearRiver[i] = CaveRiverProximityCache.columnNearRiver(this.cachedGenerator, x, z);
            this.nearRiverReady[i] = true;
        }
        return this.nearRiver[i];
    }

    boolean nearSea() {
        return this.chunkNearSea;
    }

    boolean chunkMassif() {
        return this.chunkMassif;
    }

    boolean riverHillside(int dx, int dz) {
        return this.nearRiver(dx, dz) && this.gradient(dx, dz) >= 0.35f;
    }

    /** Local surface depression depth (blocks below highest neighbor within 3), excluding river columns. */
    int localTerrainDip(int dx, int dz) {
        if (this.nearRiver(dx, dz)) {
            return 0;
        }
        int i = this.index(dx, dz);
        int surface = this.surfaceY[i];
        int neighborMax = surface;
        for (int ox = -3; ox <= 3; ++ox) {
            for (int oz = -3; oz <= 3; ++oz) {
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                    continue;
                }
                neighborMax = Math.max(neighborMax, this.surfaceY[this.index(px, pz)]);
            }
        }
        return neighborMax - surface;
    }

    boolean mayHaveRiver() {
        return this.chunkMayHaveRiver;
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
        if (type == CaveType.GLOBAL) {
            int i = this.index(dx, dz);
            return this.zone(dx, dz) == ZONE_NONE || this.synapseEligible[i];
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

    boolean skipTree(int dx, int dz) {
        return (this.decorationFlags[this.index(dx, dz)] & FLAG_SKIP_TREE) != 0;
    }

    boolean anySkipTreeColumn() {
        for (byte flags : this.decorationFlags) {
            if ((flags & FLAG_SKIP_TREE) != 0) {
                return true;
            }
        }
        return false;
    }

    byte megaGigaFlag(int dx, int dz) {
        byte z = this.zone(dx, dz);
        if ((z & ZONE_GIGA) != 0) {
            return MegaGigaChunkCache.GIGA;
        }
        if ((z & ZONE_MEGA) != 0) {
            return MegaGigaChunkCache.MEGA;
        }
        return MegaGigaChunkCache.NONE;
    }

    void setSampleShift(int dx, int dz, int sx, int sz) {
        int i = this.index(dx, dz);
        this.sampleShiftX[i] = (byte)Math.max(-127, Math.min(127, sx));
        this.sampleShiftZ[i] = (byte)Math.max(-127, Math.min(127, sz));
    }

    int sampleShiftX(int dx, int dz) {
        return this.sampleShiftX[this.index(dx, dz)];
    }

    int sampleShiftZ(int dx, int dz) {
        return this.sampleShiftZ[this.index(dx, dz)];
    }

    void setExtraCenterDrop(int dx, int dz, int drop) {
        this.extraCenterDrop[this.index(dx, dz)] = (byte)Math.max(0, Math.min(127, drop));
    }

    int extraCenterDrop(int dx, int dz) {
        return this.extraCenterDrop[this.index(dx, dz)] & 0xFF;
    }

    void setEnvelopeFlag(int dx, int dz, byte flag) {
        int i = this.index(dx, dz);
        this.envelopeFlags[i] = (byte)(this.envelopeFlags[i] | flag);
    }

    boolean suppressSurfaceBreach(int dx, int dz) {
        return (this.envelopeFlags[this.index(dx, dz)] & ENVELOPE_SUPPRESS_BREACH) != 0;
    }

    boolean reserveEntrance(int dx, int dz) {
        return (this.envelopeFlags[this.index(dx, dz)] & ENVELOPE_ENTRANCE) != 0;
    }

    boolean isEnvelopeBuilt() {
        return this.envelopeBuilt;
    }

    void markEnvelopeBuilt() {
        this.envelopeBuilt = true;
    }

    boolean forbidsUndergroundWrite(int lx, int y, int lz, ChunkAccess chunk, boolean entranceColumn) {
        if (entranceColumn && y >= this.surfaceY(lx, lz) - CaveUndergroundGuard.ENTRANCE_BIOME_DEPTH) {
            return false;
        }
        boolean megaGiga = this.zone(lx, lz) != ZONE_NONE;
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return true;
        }
        byte flags = this.decorationFlags[this.index(lx, lz)];
        int openAirBand = megaGiga ? 16 : 12;
        if ((flags & FLAG_OPEN_AIR) != 0 && y >= this.surfaceY(lx, lz) - openAirBand) {
            return true;
        }
        return false;
    }
}
