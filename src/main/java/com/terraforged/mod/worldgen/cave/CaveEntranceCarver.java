package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBreaches;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveSiteTags;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveTunnelRiverDecorator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveEntranceCarver {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState DRIPSTONE = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
    private static final float MIN_BREACH = 0.88f;
    private static final float ENTRANCE_GATE = 0.90f;
    private static final int MIN_CAVERN = 10;
    private static final int MIN_MOUTH_ABOVE_SEA = 6;
    private static final int MIN_ROCK_COVER = 18;
    private static final int MIN_RAMP_RUN = 10;
    private static final int MAX_SKYLIGHT_SPAN = 14;
    private static final float COASTAL_BIOME_START = 0.28f;
    private static final int LIP_ABOVE_SEA = 2;
    private static final int LIP_HEIGHT = 3;

    private CaveEntranceCarver() {
    }

    public static int minCavernForEntrance() {
        return 10;
    }

    public static void ensureMassifTunnelAxis(Generator generator, CaveEntranceClaims claims, int seed, int refX, int refZ) {
        if (claims == null) {
            return;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, refX, refZ);
        if (!systemType.isMegaOrGiga()) {
            return;
        }
        long key = CaveSystemGrid.systemKey(refX, refZ, systemType);
        if (claims.tunnelAxis(key) != null) {
            return;
        }
        int[] mouth = CaveSystemGrid.resolveTunnelMouthAnchor(seed, systemType, refX, refZ);
        int[] exit = CaveSystemGrid.resolveTunnelExit(mouth[0], mouth[1], systemType);
        CaveEntranceClaims.TunnelAxis axis = new CaveEntranceClaims.TunnelAxis(mouth[0], mouth[1], exit[0], exit[1]);
        if (!CaveSiteTags.validatesTunnelAxis(generator, seed, systemType, axis)) {
            return;
        }
        claims.registerTunnelIfAbsent(key, axis);
    }

    public static boolean isEntranceCandidate(Generator generator, CarverChunk carver, CaveEntranceClaims claims, int seed, int x, int z, int cavern, float breachMask, boolean megaGiga) {
        if (!megaGiga) {
            return false;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        long key = CaveSystemGrid.systemKey(x, z, systemType);
        CaveEntranceClaims.TunnelAxis axis = claims != null ? claims.tunnelAxis(key) : null;
        if (axis != null && CaveEntranceCarver.isNearColumn(x, z, axis.mouthX(), axis.mouthZ(), 4)) {
            return CaveEntranceCarver.isTunnelMouthCandidate(generator, claims, seed, x, z, cavern, breachMask, key);
        }
        boolean massif = CaveMassifCache.qualifiesMountainMassif(generator, seed, x, z);
        float minBreach = massif ? 0.55f : MIN_BREACH;
        if (cavern < MIN_CAVERN || breachMask < minBreach) {
            return false;
        }
        if (!CaveSystemGrid.isEntranceAnchorColumn(seed, x, z, systemType)) {
            return false;
        }
        if (claims != null && claims.isClaimed(key)) {
            return false;
        }
        int sea = generator.getSeaLevel();
        int surface = generator.getOceanFloorHeight(x, z);
        if (surface <= sea + 6) {
            return false;
        }
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
            return false;
        }
        boolean nearWater = CaveOceanFilter.isNearSea(generator, x, z);
        boolean riverHillside = nearWater && CaveOceanFilter.qualifiesRiverEntranceVicinity(generator, x, z);
        if (nearWater && !riverHillside) {
            return false;
        }
        if (!massif) {
            return false;
        }
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, CaveType.MEGA, x, z) && !CaveOceanFilter.isCoastalCliffColumn(generator, x, z)) {
            return false;
        }
        float gateThreshold = massif ? (riverHillside ? 0.58f : 0.72f) : ENTRANCE_GATE;
        float gate = (NoiseUtil.valCoord2D(seed ^ 0xE471A1, x, z) + 1.0f) * 0.5f;
        if (gate <= gateThreshold) {
            return false;
        }
        return massif || CaveEntranceCarver.isLocalBreachPeak(seed, x, z, breachMask);
    }

    private static boolean isTunnelMouthCandidate(Generator generator, CaveEntranceClaims claims, int seed, int x, int z, int cavern, float breachMask, long key) {
        if (cavern < 5 || breachMask < 0.4f) {
            return false;
        }
        if (claims != null && claims.isClaimed(key)) {
            return false;
        }
        int sea = generator.getSeaLevel();
        if (generator.getOceanFloorHeight(x, z) <= sea + 4) {
            return false;
        }
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, CaveType.MEGA, x, z) && !CaveOceanFilter.isCoastalCliffColumn(generator, x, z)) {
            return false;
        }
        return true;
    }

    private static boolean isNearColumn(int x, int z, int ax, int az, int radius) {
        int dx = x - ax;
        int dz = z - az;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static boolean isTunnelExitCandidate(Generator generator, CarverChunk carver, CaveEntranceClaims claims, int seed, int x, int z, int cavern, float breachMask, boolean megaGiga) {
        if (!megaGiga || cavern < 5) {
            return false;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        long key = CaveSystemGrid.systemKey(x, z, systemType);
        if (claims.hasExit(key)) {
            return false;
        }
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(key);
        if (axis == null) {
            return false;
        }
        boolean atExit = CaveEntranceCarver.isNearColumn(x, z, axis.exitX(), axis.exitZ(), 5) || CaveSystemGrid.isTunnelExitAnchorColumn(seed, x, z, systemType, axis.mouthX(), axis.mouthZ());
        if (!atExit) {
            return false;
        }
        int sea = generator.getSeaLevel();
        if (generator.getOceanFloorHeight(x, z) <= sea + 4) {
            return false;
        }
        if (breachMask < 0.3f) {
            return false;
        }
        return true;
    }

    public static void tryCarveTunnelAnchors(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, CaveEntranceClaims claims) {
        if (claims == null) {
            return;
        }
        int refX = chunk.getPos().getMinBlockX() + 8;
        int refZ = chunk.getPos().getMinBlockZ() + 8;
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, refX, refZ);
        long key = CaveSystemGrid.systemKey(refX, refZ, systemType);
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(key);
        if (axis == null) {
            return;
        }
        CaveEntranceCarver.tryCarveTunnelAnchor(seed, chunk, carver, generator, config, claims, key, axis.mouthX(), axis.mouthZ(), false);
        CaveEntranceCarver.tryCarveTunnelAnchor(seed, chunk, carver, generator, config, claims, key, axis.exitX(), axis.exitZ(), true);
    }

    private static void tryCarveTunnelAnchor(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, CaveEntranceClaims claims, long key, int wx, int wz, boolean exit) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int dx = wx - chunkMinX;
        int dz = wz - chunkMinZ;
        if (dx < 0 || dx > 15 || dz < 0 || dz > 15) {
            return;
        }
        if (exit && claims.hasExit(key)) {
            if (CaveEntranceCarver.hasSurfaceBreachAtAnchor(chunk, generator, carver, dx, dz, wx, wz)) {
                return;
            }
        } else if (!exit && claims.isClaimed(key)) {
            if (CaveEntranceCarver.hasSurfaceBreachAtAnchor(chunk, generator, carver, dx, dz, wx, wz)) {
                return;
            }
        }
        float value = CaveNoise.sampleMerged(carver.modifier, seed, wx, wz);
        int cavern = Math.max(14, config.getCavernSize(seed, wx, wz, value));
        float breachMask = Math.max(0.65f, carver.getCarvingMask(seed, wx, wz, true));
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(key);
        boolean forcedAxis = axis != null;
        if (forcedAxis) {
            cavern = Math.max(cavern, 12);
            breachMask = Math.max(breachMask, 0.65f);
            if (exit) {
                CaveEntranceCarver.carveTunnelExit(chunk, carver, generator, claims, config, seed, wx, wz, dx, dz, cavern, breachMask);
            } else {
                CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, claims, config, seed, wx, wz, dx, dz, cavern, breachMask);
            }
            return;
        }
        if (exit && claims.isClaimed(key)) {
            CaveEntranceCarver.carveTunnelExit(chunk, carver, generator, claims, config, seed, wx, wz, dx, dz, cavern, breachMask);
            return;
        }
        if (exit) {
            if (forcedAxis || CaveEntranceCarver.isTunnelExitCandidate(generator, carver, claims, seed, wx, wz, cavern, breachMask, true)) {
                CaveEntranceCarver.carveTunnelExit(chunk, carver, generator, claims, config, seed, wx, wz, dx, dz, cavern, breachMask);
            }
        } else if (forcedAxis || CaveEntranceCarver.isEntranceCandidate(generator, carver, claims, seed, wx, wz, cavern, breachMask, true)) {
            CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, claims, config, seed, wx, wz, dx, dz, cavern, breachMask);
        }
    }

    public static boolean carveTunnelExit(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave config, int seed, int x, int z, int dx, int dz, int cavern, float breachMask) {
        CaveEntranceClaims.TunnelAxis axis = claims != null ? claims.tunnelAxis(CaveSystemGrid.systemKey(x, z, CaveSystemGrid.dominantType(generator, seed, x, z))) : null;
        if (axis != null) {
            return CaveEntranceCarver.carveExitMiniSystem(chunk, carver, generator, claims, config, seed, x, z, dx, dz, cavern, breachMask);
        }
        return CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, claims, config, seed, x, z, dx, dz, cavern, breachMask, true);
    }

    private static boolean carveExitMiniSystem(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave config, int seed, int x, int z, int dx, int dz, int cavern, float breachMask) {
        int sea = generator.getSeaLevel();
        int surface = CaveEntranceCarver.resolveSurface(chunk, generator, carver, dx, dz, x, z);
        if (surface <= sea + 6) {
            return false;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        long systemKey = CaveSystemGrid.systemKey(x, z, systemType);
        int minY = generator.getMinY();
        int centerY = config.getHeight(seed, x, z);
        int chamberY = Math.max(minY + 8, Math.min(centerY, surface - 22));
        int midY = (chamberY + surface) >> 1;
        Holder<Biome> chamberBiome = carver.getBiome(x, z, midY, config, generator);
        if (chamberBiome == null) {
            return false;
        }
        int baseRadius = Math.max(5, Math.min(9, cavern / 2 + 3));
        float jitter = (NoiseUtil.valCoord2D(seed ^ 0xE1A17, x, z) + 1.0f) * 0.5f;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean carved = false;
        for (int oy = -2; oy <= 2; ++oy) {
            float hRadius = (float)baseRadius * (0.85f + jitter * 0.15f);
            float vRadius = Math.max(2.0f, hRadius * 0.55f);
            carved |= CaveEntranceCarver.carveRampBlob(chunk, carver, (float)dx + 0.5f, chamberY + oy * 2, (float)dz + 0.5f, hRadius, vRadius, chamberBiome, surface + 1, sea, pos, chunkMinX, chunkMinZ, false, false);
        }
        int layoutCx = CaveSystemGrid.snapCenter(x, systemType);
        int layoutCz = CaveSystemGrid.snapCenter(z, systemType);
        float toCenterX = (float)(layoutCx - x);
        float toCenterZ = (float)(layoutCz - z);
        float centerLen = NoiseUtil.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ);
        if (centerLen >= 12.0f) {
            float dirX = toCenterX / centerLen;
            float dirZ = toCenterZ / centerLen;
            int synapseRun = Math.min(48, Math.round(centerLen * 0.45f));
            CaveEntranceCarver.carveSynapseConnector(chunk, carver, generator, seed, x - Math.round(dirX * 4.0f), chamberY, z - Math.round(dirZ * 4.0f), x + Math.round(dirX * (float)synapseRun), chamberY, z + Math.round(dirZ * (float)synapseRun), baseRadius, breachMask, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        }
        float[] uphill = CaveEntranceCarver.sampleUphillDirection(generator, x, z);
        float upLen = NoiseUtil.sqrt(uphill[0] * uphill[0] + uphill[1] * uphill[1]);
        if (upLen < 0.05f) {
            uphill[0] = 0.0f;
            uphill[1] = 1.0f;
            upLen = 1.0f;
        }
        float ux = uphill[0] / upLen;
        float uz = uphill[1] / upLen;
        float mouthDist = (float)baseRadius + 5.0f + jitter * 3.0f;
        int mouthWx = x + Math.round(ux * mouthDist);
        int mouthWz = z + Math.round(uz * mouthDist);
        int mouthSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, mouthWx, mouthWz);
        int mouthY = mouthSurface - 1;
        if (mouthY > chamberY + 6) {
            carved |= CaveEntranceCarver.carveHillsideRamp(chunk, carver, generator, seed, x, chamberY, z, mouthWx, mouthY, mouthWz, mouthSurface, baseRadius, breachMask, jitter, ux, uz, chamberBiome, null, sea, chunkMinX, chunkMinZ, pos);
        }
        if (!carved) {
            return false;
        }
        if (claims != null) {
            claims.tryClaimExit(systemKey);
        }
        carver.expandEntranceZone(1);
        CaveEntranceClaims.TunnelAxis axis = claims != null ? claims.tunnelAxis(systemKey) : null;
        if (axis != null) {
            carver.restoreTunnel(axis);
        }
        return true;
    }

    private static boolean isLocalBreachPeak(int seed, int x, int z, float breachMask) {
        for (int ox = -4; ox <= 4; ++ox) {
            for (int oz = -4; oz <= 4; ++oz) {
                if (ox == 0 && oz == 0 || !(CaveBreaches.sample(seed, x + ox, z + oz) > breachMask)) continue;
                return false;
            }
        }
        return true;
    }

    public static boolean carveSlopeEntrance(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave config, int seed, int x, int z, int dx, int dz, int cavern, float breachMask) {
        return CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, claims, config, seed, x, z, dx, dz, cavern, breachMask, false);
    }

    private static boolean carveSlopeEntrance(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave config, int seed, int x, int z, int dx, int dz, int cavern, float breachMask, boolean exitMouth) {
        int sea = generator.getSeaLevel();
        int surface = CaveEntranceCarver.resolveSurface(chunk, generator, carver, dx, dz, x, z);
        if (surface <= sea + 6) {
            return false;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        long systemKey = CaveSystemGrid.systemKey(x, z, systemType);
        int minY = generator.getMinY();
        int centerY = config.getHeight(seed, x, z);
        int chamberBottom = Math.max(minY, centerY - Math.max(8, cavern));
        int anchorMouthY = surface - 1;
        int rockCover = anchorMouthY - centerY;
        CaveEntranceClaims.TunnelAxis tunnelAxis = claims != null ? claims.tunnelAxis(systemKey) : null;
        boolean tunnelMouth = !exitMouth && tunnelAxis != null && CaveEntranceCarver.isNearColumn(x, z, tunnelAxis.mouthX(), tunnelAxis.mouthZ(), 6);
        boolean massif = CaveMassifCache.qualifiesMountainMassif(generator, seed, x, z);
        int minRockCover = massif ? 10 : MIN_ROCK_COVER;
        if (!tunnelMouth && rockCover < minRockCover && anchorMouthY - chamberBottom < (massif ? 12 : 8)) {
            return false;
        }
        float[] uphill = CaveEntranceCarver.sampleUphillDirection(generator, x, z);
        float upLen = NoiseUtil.sqrt(uphill[0] * uphill[0] + uphill[1] * uphill[1]);
        if (upLen < 0.05f) {
            uphill[0] = 0.0f;
            uphill[1] = 1.0f;
            upLen = 1.0f;
        }
        float ux = uphill[0] / upLen;
        float uz = uphill[1] / upLen;
        int midY = chamberBottom + anchorMouthY >> 1;
        Holder<Biome> chamberBiome = carver.getBiome(x, z, midY, config, generator);
        if (chamberBiome == null) {
            return false;
        }
        Holder<Biome> surfaceBiome = generator.getBiomeSource().getNoiseBiome(x >> 2, surface >> 2, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        Holder coastalBiome = generator.getBiomeSource().getCoastalEntranceBiome(config.getSeed(), x, z, generator, surfaceBiome, midY, surface).orElse(null);
        if (anchorMouthY - chamberBottom < (tunnelMouth ? 3 : 6)) {
            return false;
        }
        int baseRadius = Math.max(5, Math.min(14, cavern / 2 + 4 + Math.round(breachMask * 3.0f)));
        float jitter = (NoiseUtil.valCoord2D(seed ^ 0xCAFE, x, z) + 1.0f) * 0.5f;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        float mouthDist = (float)baseRadius + 6.0f + jitter * 5.0f;
        float chamberInset = (float)baseRadius + 5.0f + jitter * 3.0f;
        int mouthWx = x + Math.round(ux * mouthDist);
        int mouthWz = z + Math.round(uz * mouthDist);
        int chamberWx = x - Math.round(ux * chamberInset);
        int chamberWz = z - Math.round(uz * chamberInset);
        int[] dryMouth = CaveOceanFilter.offsetMouthFromWater(generator, mouthWx, mouthWz, ux, uz, 4, 16);
        mouthWx = dryMouth[0];
        mouthWz = dryMouth[1];
        int mouthSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, mouthWx, mouthWz);
        int mouthY = mouthSurface - 1;
        if (mouthY <= sea + 6 || CaveOceanFilter.isSurfaceWaterColumn(generator, mouthWx, mouthWz)) {
            return false;
        }
        massif = massif || CaveMassifCache.qualifiesMountainMassif(generator, seed, mouthWx, mouthWz);
        int chamberY = Math.max(minY + 4, Math.min(centerY, mouthY - (massif ? 12 : 18)));
        float horizRun = CaveEntranceCarver.horizontalDistance(mouthWx, mouthWz, chamberWx, chamberWz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean nearWater = CaveOceanFilter.isNearSea(generator, x, z) || CaveOceanFilter.isNearSea(generator, mouthWx, mouthWz);
        float rampRun = massif ? 6.0f : (float)MIN_RAMP_RUN;
        boolean carved;
        if (horizRun >= rampRun || nearWater) {
            carved = CaveEntranceCarver.carveHillsideRamp(chunk, carver, generator, seed, chamberWx, chamberY, chamberWz, mouthWx, mouthY, mouthWz, mouthSurface, baseRadius, breachMask, jitter, ux, uz, chamberBiome, (Holder<Biome>)coastalBiome, sea, chunkMinX, chunkMinZ, pos);
        } else if (massif) {
            carved = CaveEntranceCarver.carveHillsideRamp(chunk, carver, generator, seed, chamberWx, chamberY, chamberWz, mouthWx, mouthY, mouthWz, mouthSurface, baseRadius, breachMask, jitter, ux, uz, chamberBiome, (Holder<Biome>)coastalBiome, sea, chunkMinX, chunkMinZ, pos);
        } else {
            return false;
        }
        if (carved && (horizRun >= 6.0f || tunnelMouth)) {
            CaveEntranceCarver.carveSynapseConnector(chunk, carver, generator, seed, chamberWx, chamberY, chamberWz, mouthWx, mouthY, mouthWz, baseRadius, breachMask, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        }
        if (carved) {
            if (claims != null) {
                if (exitMouth) {
                    claims.tryClaimExit(systemKey);
                } else {
                    claims.tryClaim(systemKey);
                }
            }
            carver.expandEntranceZone(2);
            if (tunnelMouth && tunnelAxis != null && coastalBiome == null) {
                carver.noteTunnelRiver(mouthWx, mouthWz, chamberWx, chamberWz, systemType);
            }
            if (coastalBiome != null) {
                carver.noteDecorateAnchor((Holder<Biome>)coastalBiome, new BlockPos(mouthWx, midY, mouthWz));
                int mouthLx = mouthWx - chunkMinX;
                int mouthLz = mouthWz - chunkMinZ;
                if (mouthLx >= 0 && mouthLx <= 15 && mouthLz >= 0 && mouthLz <= 15) {
                    CaveEntranceCarver.buildSeawallBasin(chunk, seed, sea, mouthSurface, mouthY, mouthLx, mouthLz, ux, uz, baseRadius, breachMask);
                }
            } else if (exitMouth && claims != null) {
                CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(systemKey);
                if (axis != null) {
                    carver.restoreTunnel(axis);
                }
            }
        }
        return carved;
    }

    public static boolean carveGrottoEntrance(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave synapseConfig, int seed, int x, int z, int dx, int dz) {
        int sea = generator.getSeaLevel();
        int surface = carver.cachedSurface(dx, dz);
        if (surface <= sea + 8) {
            return false;
        }
        float[] uphill = CaveEntranceCarver.sampleUphillDirection(generator, x, z);
        float upLen = NoiseUtil.sqrt(uphill[0] * uphill[0] + uphill[1] * uphill[1]);
        if (upLen < 0.05f) {
            uphill[0] = 0.0f;
            uphill[1] = 1.0f;
            upLen = 1.0f;
        }
        float ux = uphill[0] / upLen;
        float uz = uphill[1] / upLen;
        float jitter = (NoiseUtil.valCoord2D(seed ^ 0x607770, x, z) + 1.0f) * 0.5f;
        int baseRadius = 4 + Math.round(jitter * 2.0f);
        int mouthDist = baseRadius + 5 + Math.round(jitter * 4.0f);
        int mouthWx = x + Math.round(ux * (float)mouthDist);
        int mouthWz = z + Math.round(uz * (float)mouthDist);
        int[] dryMouth = CaveOceanFilter.offsetMouthFromWater(generator, mouthWx, mouthWz, ux, uz, 3, 14);
        mouthWx = dryMouth[0];
        mouthWz = dryMouth[1];
        int mouthSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, mouthWx, mouthWz);
        int mouthY = mouthSurface - 1;
        if (mouthY <= sea + 6 || CaveOceanFilter.isSurfaceWaterColumn(generator, mouthWx, mouthWz)) {
            return false;
        }
        int minY = generator.getMinY();
        int chamberY = Math.max(minY + 6, Math.min(mouthY - 10, surface - 14));
        int midY = chamberY + mouthY >> 1;
        Holder<Biome> chamberBiome = carver.getBiome(x, z, midY, synapseConfig, generator);
        if (chamberBiome == null) {
            return false;
        }
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean carved = CaveEntranceCarver.carveHillsideRamp(chunk, carver, generator, seed, x, chamberY, z, mouthWx, mouthY, mouthWz, mouthSurface, baseRadius, 0.45f, jitter, ux, uz, chamberBiome, null, sea, chunkMinX, chunkMinZ, pos);
        for (int oy = -1; oy <= 1; ++oy) {
            float hRadius = (float)baseRadius * (0.9f + jitter * 0.1f);
            carved |= CaveEntranceCarver.carveRampBlob(chunk, carver, (float)x + 0.5f, chamberY + oy * 2, (float)z + 0.5f, hRadius, Math.max(2.0f, hRadius * 0.5f), chamberBiome, surface, sea, pos, chunkMinX, chunkMinZ, false, false);
        }
        int[] target = CaveGrottoCarver.findSynapseTarget(generator, seed, x, z, ux, uz);
        if (target != null) {
            CaveEntranceCarver.carveSynapseConnector(chunk, carver, generator, seed, x, chamberY, z, target[0], chamberY, target[1], baseRadius + 2, 0.45f, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        } else {
            int deepX = x + Math.round(ux * 28.0f);
            int deepZ = z + Math.round(uz * 28.0f);
            CaveEntranceCarver.carveSynapseConnector(chunk, carver, generator, seed, x, chamberY, z, deepX, Math.max(minY + 8, chamberY - 6), deepZ, baseRadius + 1, 0.4f, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        }
        if (!carved) {
            return false;
        }
        int mouthLx = mouthWx - chunkMinX;
        int mouthLz = mouthWz - chunkMinZ;
        if (mouthLx >= 0 && mouthLx <= 15 && mouthLz >= 0 && mouthLz <= 15) {
            carver.markCoastalEntranceColumn(mouthLx, mouthLz);
        }
        int anchorLx = x - chunkMinX;
        int anchorLz = z - chunkMinZ;
        if (anchorLx >= 0 && anchorLx <= 15 && anchorLz >= 0 && anchorLz <= 15) {
            carver.markEntranceColumn(anchorLx, anchorLz);
        }
        carver.expandEntranceZone(1);
        carver.noteDecorateAnchor(chamberBiome, new BlockPos(x, chamberY, z));
        return true;
    }

    private static boolean carveHillsideRamp(ChunkAccess chunk, CarverChunk carver, Generator generator, int seed, int chamberWx, int chamberY, int chamberWz, int mouthWx, int mouthY, int mouthWz, int mouthSurface, int baseRadius, float breachMask, float jitter, float ux, float uz, Holder<Biome> chamberBiome, Holder<Biome> coastalBiome, int sea, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos pos) {
        int steps = Math.max(mouthY - chamberY, Math.round(CaveEntranceCarver.horizontalDistance(mouthWx, mouthWz, chamberWx, chamberWz)));
        steps = Math.min(steps, 96);
        boolean carved = false;
        for (int i = 0; i <= steps; ++i) {
            float t = (float)i / (float)steps;
            float ease = CaveEntranceCarver.smoothstep(t);
            float cx = (float)chamberWx + (float)(mouthWx - chamberWx) * ease;
            float cz = (float)chamberWz + (float)(mouthWz - chamberWz) * ease;
            int cy = chamberY + Math.round((float)(mouthY - chamberY) * ease);
            float hRadius = CaveEntranceCarver.rampHorizontalRadius(t, baseRadius, breachMask, jitter);
            float vRadius = Math.max(1.5f, hRadius * 0.42f);
            Holder<Biome> layerBiome = coastalBiome != null && t >= 0.28f ? coastalBiome : chamberBiome;
            int localSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, Math.round(cx), Math.round(cz));
            carved |= CaveEntranceCarver.carveRampBlob(chunk, carver, cx, cy, cz, hRadius, vRadius, layerBiome, localSurface, sea, pos, chunkMinX, chunkMinZ, coastalBiome != null, t >= 0.78f);
        }
        return carved;
    }

    private static boolean carveSynapseConnector(ChunkAccess chunk, CarverChunk carver, Generator generator, int seed, int chamberWx, int chamberY, int chamberWz, int mouthWx, int mouthY, int mouthWz, int baseRadius, float breachMask, float jitter, Holder<Biome> chamberBiome, int sea, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos pos) {
        int steps = Math.max(mouthY - chamberY, Math.round(CaveEntranceCarver.horizontalDistance(mouthWx, mouthWz, chamberWx, chamberWz)));
        steps = Math.min(steps, 96);
        boolean carved = false;
        for (int i = 0; i <= steps; ++i) {
            float t = (float)i / (float)steps;
            if (t < 0.05f || t > 0.95f) continue;
            float ease = CaveEntranceCarver.smoothstep(t);
            float cx = (float)chamberWx + (float)(mouthWx - chamberWx) * ease;
            float cz = (float)chamberWz + (float)(mouthWz - chamberWz) * ease;
            int cy = chamberY + Math.round((float)(mouthY - chamberY) * ease);
            int wx = Math.round(cx);
            int wz = Math.round(cz);
            float wobble = (NoiseUtil.valCoord2D(seed ^ 0x5FA7E1, wx, wz) + 1.0f) * 0.5f;
            float hRadius = (float)baseRadius * (1.15f + wobble * 0.45f + breachMask * 0.12f + jitter * 0.08f);
            float vRadius = Math.max(2.5f, hRadius * 0.55f);
            int localSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, wx, wz);
            carved |= CaveEntranceCarver.carveRampBlob(chunk, carver, cx, cy, cz, hRadius, vRadius, chamberBiome, localSurface, sea, pos, chunkMinX, chunkMinZ, false, false);
        }
        return carved;
    }

    private static boolean carveCeilingSkylight(ChunkAccess chunk, CarverChunk carver, Generator generator, int anchorWx, int anchorWz, int chamberY, int mouthY, int mouthSurface, int baseRadius, float breachMask, float jitter, float ux, float uz, Holder<Biome> chamberBiome, Holder<Biome> coastalBiome, int sea, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos pos, int minSpan) {
        int span = Math.min(14, mouthY - chamberY);
        if (span < minSpan) {
            return false;
        }
        float lateral = (float)baseRadius * 0.55f + jitter * 2.0f;
        int endWx = anchorWx + Math.round(ux * lateral);
        int endWz = anchorWz + Math.round(uz * lateral);
        int endY = Math.min(mouthSurface - 1, chamberY + span);
        int endSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, endWx, endWz);
        boolean carved = false;
        int steps = span + Math.round(lateral);
        for (int i = 0; i <= steps; ++i) {
            float t = (float)i / (float)steps;
            float ease = CaveEntranceCarver.smoothstep(t);
            float cx = (float)anchorWx + (float)(endWx - anchorWx) * ease;
            float cz = (float)anchorWz + (float)(endWz - anchorWz) * ease;
            int cy = chamberY + Math.round((float)(endY - chamberY) * ease);
            float hRadius = CaveEntranceCarver.rampHorizontalRadius(t, (float)baseRadius * 0.75f, breachMask, jitter);
            float vRadius = Math.max(1.2f, hRadius * 0.35f);
            Holder<Biome> layerBiome = coastalBiome != null && t >= 0.28f ? coastalBiome : chamberBiome;
            carved |= CaveEntranceCarver.carveRampBlob(chunk, carver, cx, cy, cz, hRadius, vRadius, layerBiome, endSurface, sea, pos, chunkMinX, chunkMinZ, coastalBiome != null, t >= 0.82f);
        }
        return carved;
    }

    private static float rampHorizontalRadius(float t, float baseRadius, float breachMask, float jitter) {
        if (t < 0.35f) {
            return baseRadius;
        }
        float throat = (t - 0.35f) / 0.65f;
        float pinch = 0.38f - breachMask * 0.15f + jitter * 0.05f;
        float factor = 1.0f - throat * throat * pinch;
        if (t > 0.88f) {
            factor += 0.06f + breachMask * 0.08f;
        }
        return Math.max(2.5f, baseRadius * Math.max(0.35f, factor));
    }

    private static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float horizontalDistance(int x0, int z0, int x1, int z1) {
        float dx = x0 - x1;
        float dz = z0 - z1;
        return NoiseUtil.sqrt(dx * dx + dz * dz);
    }

    private static boolean carveRampBlob(ChunkAccess chunk, CarverChunk carver, float centerX, int centerY, float centerZ, float hRadius, float vRadius, Holder<Biome> biome, int surface, int sea, BlockPos.MutableBlockPos pos, int chunkMinX, int chunkMinZ, boolean coastalMouth, boolean markMouth) {
        if (centerY <= sea) {
            return false;
        }
        int rH = Math.max(2, Math.round(hRadius));
        int rV = Math.max(1, Math.round(vRadius));
        int cx = Math.round(centerX);
        int cz = Math.round(centerZ);
        boolean placed = false;
        for (int ox = -rH; ox <= rH; ++ox) {
            for (int oy = -rV; oy <= rV; ++oy) {
                for (int oz = -rH; oz <= rH; ++oz) {
                    float nx = (float)ox / hRadius;
                    float ny = (float)oy / vRadius;
                    float nz = (float)oz / hRadius;
                    if (nx * nx + ny * ny + nz * nz > 1.0f) continue;
                    int px = cx + ox;
                    int pz = cz + oz;
                    int py = centerY + oy;
                    if (px < 0 || px > 15 || pz < 0 || pz > 15 || py <= sea) continue;
                    pos.set(px, py, pz);
                    BlockState state = chunk.getBlockState((BlockPos)pos);
                    if (!state.getFluidState().isEmpty() || py >= surface && state.isAir()) continue;
                    chunk.setBlockState((BlockPos)pos, AIR, false);
                    placed = true;
                    if (markMouth && py >= surface - 4) {
                        if (coastalMouth) {
                            carver.markCoastalEntranceColumn(px, pz);
                        } else {
                            carver.markEntranceColumn(px, pz);
                        }
                    }
                    if (biome != null && py < surface - CaveUndergroundGuard.ENTRANCE_BIOME_DEPTH) {
                        CaveEntranceCarver.setBiomeQuart(chunk, px, py, pz, biome);
                        continue;
                    }
                    if (biome == null || py >= surface - CaveUndergroundGuard.ENTRANCE_BIOME_DEPTH) continue;
                    CaveEntranceCarver.setBiomeQuart(chunk, px, py, pz, biome);
                    carver.noteDecorateAnchor(biome, new BlockPos(chunkMinX + px, py, chunkMinZ + pz));
                }
            }
        }
        return placed;
    }

    private static boolean hasSurfaceBreachAtAnchor(ChunkAccess chunk, Generator generator, CarverChunk carver, int dx, int dz, int x, int z) {
        int surface = CaveEntranceCarver.resolveSurface(chunk, generator, carver, dx, dz, x, z);
        for (int y = surface; y >= surface - 8; --y) {
            if (y < chunk.getMinBuildHeight()) {
                break;
            }
            if (chunk.getBlockState(new BlockPos(dx, y, dz)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean carveTunnelAxisSurfaceConnection(ChunkAccess chunk, CarverChunk carver, Generator generator, CaveEntranceClaims claims, NoiseCave config, int seed, int x, int z, int dx, int dz, int cavern, float breachMask, boolean exitMouth) {
        int sea = generator.getSeaLevel();
        int surface = CaveEntranceCarver.resolveSurface(chunk, generator, carver, dx, dz, x, z);
        if (surface <= sea + 6) {
            return false;
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        long systemKey = CaveSystemGrid.systemKey(x, z, systemType);
        int minY = generator.getMinY();
        int floorY = CaveColumnScan.findTopValidFloor(chunk, dx, dz, minY, Math.max(minY + 4, surface - 4), y -> true);
        if (floorY < 0) {
            int centerY = config.getHeight(seed, x, z);
            floorY = Math.max(minY + 4, centerY - Math.max(8, cavern / 2));
        }
        if (floorY >= surface - 4) {
            return false;
        }
        int midY = floorY + surface >> 1;
        Holder<Biome> chamberBiome = carver.getBiome(x, z, midY, config, generator);
        if (chamberBiome == null) {
            return false;
        }
        int baseRadius = Math.max(6, Math.min(14, cavern / 2 + 5 + Math.round(breachMask * 3.0f)));
        float jitter = (NoiseUtil.valCoord2D(seed ^ 0xCAFE, x, z) + 1.0f) * 0.5f;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean carved = CaveEntranceCarver.carveVerticalShaftToSurface(chunk, carver, dx, dz, floorY, surface, baseRadius, breachMask, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        if (!carved) {
            return false;
        }
        int cx = CaveSystemGrid.snapCenter(x, systemType);
        int cz = CaveSystemGrid.snapCenter(z, systemType);
        float toCenterX = (float)(cx - x);
        float toCenterZ = (float)(cz - z);
        float centerLen = NoiseUtil.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ);
        if (centerLen >= 1.0f) {
            CaveEntranceCarver.carveInteriorConnector(chunk, carver, generator, x, z, dx, dz, floorY, toCenterX / centerLen, toCenterZ / centerLen, baseRadius, breachMask, jitter, chamberBiome, sea, chunkMinX, chunkMinZ, pos);
        }
        carver.expandEntranceZone(3);
        if (claims != null) {
            if (exitMouth) {
                claims.tryClaimExit(systemKey);
            } else {
                claims.tryClaim(systemKey);
            }
            CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(systemKey);
            if (axis != null) {
                carver.restoreTunnel(axis);
            }
        }
        return true;
    }

    private static boolean carveVerticalShaftToSurface(ChunkAccess chunk, CarverChunk carver, int dx, int dz, int floorY, int surface, float baseRadius, float breachMask, float jitter, Holder<Biome> biome, int sea, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos pos) {
        int span = surface - floorY;
        if (span < 5) {
            return false;
        }
        boolean placed = false;
        for (int y = floorY; y <= surface + 1; ++y) {
            float t = (float)(y - floorY) / (float)span;
            float ease = CaveEntranceCarver.smoothstep(t);
            float hRadius = baseRadius * (0.5f + 0.5f * ease + breachMask * 0.08f + jitter * 0.06f);
            float vRadius = Math.max(2.0f, hRadius * 0.45f);
            placed |= CaveEntranceCarver.carveRampBlob(chunk, carver, (float)dx + 0.5f, y, (float)dz + 0.5f, hRadius, vRadius, biome, surface + 1, sea, pos, chunkMinX, chunkMinZ, false, ease >= 0.82f);
        }
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) continue;
                for (int y = surface; y >= surface - 2; --y) {
                    pos.set(px, y, pz);
                    BlockState state = chunk.getBlockState((BlockPos)pos);
                    if (state.getFluidState().isEmpty() && !state.isAir()) {
                        chunk.setBlockState((BlockPos)pos, AIR, false);
                        placed = true;
                    }
                    carver.markEntranceColumn(px, pz);
                }
            }
        }
        return placed;
    }

    private static void carveInteriorConnector(ChunkAccess chunk, CarverChunk carver, Generator generator, int anchorX, int anchorZ, int dx, int dz, int floorY, float dirX, float dirZ, float baseRadius, float breachMask, float jitter, Holder<Biome> biome, int sea, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos pos) {
        int run = Math.min(24, Math.max(8, Math.round(baseRadius + 6.0f + jitter * 4.0f)));
        for (int i = 1; i <= run; ++i) {
            float cx = (float)anchorX + dirX * (float)i;
            float cz = (float)anchorZ + dirZ * (float)i;
            int px = Math.round(cx) - chunkMinX;
            int pz = Math.round(cz) - chunkMinZ;
            if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                continue;
            }
            float hRadius = baseRadius * (1.05f - (float)i / (float)run * 0.25f);
            float vRadius = Math.max(2.0f, hRadius * 0.5f);
            int localSurface = CaveEntranceCarver.resolveSurfaceWorld(chunk, generator, carver, Math.round(cx), Math.round(cz));
            CaveEntranceCarver.carveRampBlob(chunk, carver, (float)px + 0.5f, floorY, (float)pz + 0.5f, hRadius, vRadius, biome, localSurface + 1, sea, pos, chunkMinX, chunkMinZ, false, false);
        }
    }

    private static int resolveSurfaceWorld(ChunkAccess chunk, Generator generator, CarverChunk carver, int wx, int wz) {
        int lx = wx - chunk.getPos().getMinBlockX();
        int lz = wz - chunk.getPos().getMinBlockZ();
        if (lx >= 0 && lx <= 15 && lz >= 0 && lz <= 15) {
            return CaveEntranceCarver.resolveSurface(chunk, generator, carver, lx, lz, wx, wz);
        }
        return Math.max(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Math.floorMod(lx, 16), Math.floorMod(lz, 16)), generator.getOceanFloorHeight(wx, wz));
    }

    private static void buildSeawallBasin(ChunkAccess chunk, int seed, int sea, int surface, int mouthY, int dx, int dz, float ux, float uz, int baseRadius, float breachMask) {
        float sx = -ux;
        float sz = -uz;
        int lipFloor = Math.max(sea + 2, mouthY - 2);
        int lipTop = Math.min(surface, lipFloor + 3);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int arc = baseRadius + 2 + Math.round(breachMask);
        for (int dist = 0; dist <= arc; ++dist) {
            int px = dx + Math.round(sx * (float)dist);
            int pz = dz + Math.round(sz * (float)dist);
            if (px < 0 || px > 15 || pz < 0 || pz > 15) continue;
            float edgeNoise = (NoiseUtil.valCoord2D(seed ^ 0xB0510, px, pz) + 1.0f) * 0.5f;
            int height = 3 + (edgeNoise > 0.72f ? 1 : 0);
            boolean seawardLip = dist >= Math.max(1, baseRadius);
            for (int h = 0; h < height; ++h) {
                int cy = lipFloor + h;
                if (cy > lipTop || cy > surface) continue;
                pos.set(px, cy, pz);
                BlockState state = chunk.getBlockState((BlockPos)pos);
                if (!state.isAir() || !state.getFluidState().isEmpty() || !seawardLip && h < height - 2) continue;
                chunk.setBlockState((BlockPos)pos, DRIPSTONE, false);
            }
            if (seawardLip || lipFloor <= sea + 1) continue;
            pos.set(px, lipFloor - 1, pz);
            BlockState floor = chunk.getBlockState((BlockPos)pos);
            if (floor.isAir() || !floor.getFluidState().isEmpty()) continue;
            chunk.setBlockState((BlockPos)pos, DRIPSTONE, false);
        }
    }

    private static float[] sampleUphillDirection(Generator generator, int x, int z) {
        int step = 4;
        float east = generator.getOceanFloorHeight(x + step, z);
        float west = generator.getOceanFloorHeight(x - step, z);
        float south = generator.getOceanFloorHeight(x, z + step);
        float north = generator.getOceanFloorHeight(x, z - step);
        return new float[]{(east - west) * 0.5f, (south - north) * 0.5f};
    }

    private static int resolveSurface(ChunkAccess chunk, Generator generator, CarverChunk carver, int dx, int dz, int x, int z) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
        if (carver.terrainData != null) {
            surface = Math.max(surface, carver.terrainData.getHeight(dx, dz));
        }
        return Math.max(surface, generator.getOceanFloorHeight(x, z));
    }

    private static void setBiomeQuart(ChunkAccess chunk, int dx, int cy, int dz, Holder<Biome> biome) {
        int biomeX = dx >> 2;
        int biomeZ = dz >> 2;
        int biomeY = (cy & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(cy);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
    }
}
