package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * River-hillside grotto entrance: small surface chamber linked by synapse to mega/giga/global caves.
 */
public final class CaveGrottoCarver {
    private static final float GROTTO_GATE = 0.68f;
    private static final int SCAN_STEP = 4;
    private static final int GROTTO_CLAIM_RADIUS = 48;

    private CaveGrottoCarver() {
    }

    public static void tryCarveChunk(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave synapseConfig, CaveEntranceClaims claims) {
        if (synapseConfig == null || claims == null) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        if (!columns.nearSea() || !columns.mayHaveRiver()) {
            return;
        }
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx += SCAN_STEP) {
            for (int dz = 0; dz < 16; dz += SCAN_STEP) {
                int x = startX + dx;
                int z = startZ + dz;
                if (claims.isGrottoClaimed(x, z, GROTTO_CLAIM_RADIUS)) {
                    continue;
                }
                if (!CaveGrottoCarver.isGrottoCandidate(generator, columns, seed, x, z, dx, dz)) {
                    continue;
                }
                if (CaveEntranceCarver.carveGrottoEntrance(chunk, carver, generator, claims, synapseConfig, seed, x, z, dx, dz)) {
                    claims.tryClaimGrotto(x, z);
                }
            }
        }
    }

    static boolean isGrottoCandidate(Generator generator, CarverColumnCache columns, int seed, int x, int z, int dx, int dz) {
        int sea = generator.getSeaLevel();
        if (columns.surfaceY(dx, dz) <= sea + 8) {
            return false;
        }
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
            return false;
        }
        if (!columns.nearRiver(dx, dz)) {
            return false;
        }
        if (columns.gradient(dx, dz) < 0.35f) {
            return false;
        }
        byte columnZone = columns.zone(dx, dz);
        if (columnZone == CarverColumnCache.ZONE_GIGA || columnZone == CarverColumnCache.ZONE_BOTH) {
            return false;
        }
        float gate = (NoiseUtil.valCoord2D(seed ^ 0x607770, x, z) + 1.0f) * 0.5f;
        return gate > GROTTO_GATE;
    }

    static int[] findSynapseTarget(Generator generator, int seed, int x, int z, float ux, float uz) {
        int[] best = null;
        float bestScore = 0.15f;
        for (int dist = 12; dist <= 96; dist += 8) {
            int tx = x + Math.round(ux * (float)dist);
            int tz = z + Math.round(uz * (float)dist);
            float mega = CaveNoise.sample(CaveModifiers.mega(), seed, tx, tz);
            float giga = CaveNoise.sample(CaveModifiers.giga(), seed, tx, tz);
            float score = Math.max(mega, giga * 1.15f);
            if (score <= bestScore) {
                continue;
            }
            bestScore = score;
            best = new int[]{tx, tz};
        }
        return best;
    }
}
