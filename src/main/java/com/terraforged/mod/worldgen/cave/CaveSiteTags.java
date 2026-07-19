package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CaveBreaches;
import com.terraforged.mod.worldgen.cave.CaveEntranceCarver;
import com.terraforged.mod.worldgen.cave.CaveLocator;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveSubtype;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.Module;

public final class CaveSiteTags {
    private CaveSiteTags() {
    }

    public static CaveSubtype detectSubtype(Generator generator, CaveType type, int seed, int x, int z) {
        if (!type.isMegaOrGiga()) {
            return CaveSubtype.ANY;
        }
        if (CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, seed, x, z)) {
            return CaveSubtype.COASTAL;
        }
        if (CaveSiteTags.qualifiesTunnelMegaGiga(generator, seed, x, z)) {
            return CaveSubtype.TUNNEL;
        }
        return CaveSubtype.ANY;
    }

    /**
     * Tunnel mega/giga: a validated pair of mouth + exit anchors, not merely mountainous terrain.
     */
    public static boolean qualifiesTunnelMegaGiga(Generator generator, int seed, int x, int z) {
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        if (!systemType.isMegaOrGiga()) {
            return false;
        }
        long key = CaveSystemGrid.systemKey(x, z, systemType);
        CaveEntranceClaims claims = generator.getCaveEntranceClaims();
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(key);
        if (axis == null) {
            axis = CaveSiteTags.prospectiveTunnelAxis(seed, systemType, x, z);
        }
        if (axis == null || !CaveSiteTags.validatesTunnelAxis(generator, seed, systemType, axis)) {
            return false;
        }
        return claims.isClaimed(key) && claims.hasExit(key);
    }

    static CaveEntranceClaims.TunnelAxis prospectiveTunnelAxis(int seed, CaveType type, int refX, int refZ) {
        int[] mouth = CaveSystemGrid.resolveTunnelMouthAnchor(seed, type, refX, refZ);
        int[] exit = CaveSystemGrid.resolveTunnelExit(mouth[0], mouth[1], type);
        return new CaveEntranceClaims.TunnelAxis(mouth[0], mouth[1], exit[0], exit[1]);
    }

    static boolean validatesTunnelAxis(Generator generator, int seed, CaveType type, CaveEntranceClaims.TunnelAxis axis) {
        if (!CaveReliefFilter.validatesTunnelSpan(axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ())) {
            return false;
        }
        if (!CaveReliefFilter.qualifiesTunnelEndpoint(generator, seed, axis.mouthX(), axis.mouthZ())) {
            return false;
        }
        if (!CaveReliefFilter.qualifiesTunnelEndpoint(generator, seed, axis.exitX(), axis.exitZ())) {
            return false;
        }
        if (CaveOceanFilter.isNearSea(generator, axis.mouthX(), axis.mouthZ())
                && CaveOceanFilter.isNearSea(generator, axis.exitX(), axis.exitZ())) {
            return false;
        }
        return CaveSystemGrid.isEntranceAnchorColumn(seed, axis.mouthX(), axis.mouthZ(), type)
                && (CaveSystemGrid.isTunnelExitAnchorColumn(seed, axis.exitX(), axis.exitZ(), type, axis.mouthX(), axis.mouthZ())
                || CaveSystemGrid.isEntranceAnchorColumn(seed, axis.exitX(), axis.exitZ(), type));
    }

    public static boolean qualifiesProspectiveTunnel(Generator generator, int seed, int x, int z) {
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, x, z);
        if (!systemType.isMegaOrGiga()) {
            return false;
        }
        CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(CaveSystemGrid.systemKey(x, z, systemType));
        if (axis == null) {
            axis = CaveSiteTags.prospectiveTunnelAxis(seed, systemType, x, z);
        }
        return CaveSiteTags.validatesTunnelAxis(generator, seed, systemType, axis);
    }

    public static boolean qualifiesCoastalMegaGiga(Generator generator, CaveType type, int seed, int x, int z) {
        float breachMask;
        if (!type.isMegaOrGiga()) {
            return false;
        }
        NoiseCave config = CaveLocator.findConfig(generator, type);
        if (config == null) {
            return false;
        }
        Module modifier = CaveModifiers.get(type);
        float value = CaveNoise.sample(modifier, seed, x, z);
        int cavern = config.getCavernSize(seed, x, z, value);
        return CaveEntranceCarver.isEntranceCandidate(generator, null, null, seed, x, z, cavern, breachMask = CaveBreaches.sample(seed, x, z), true) && CaveOceanFilter.isNearSea(generator, x, z);
    }
}
