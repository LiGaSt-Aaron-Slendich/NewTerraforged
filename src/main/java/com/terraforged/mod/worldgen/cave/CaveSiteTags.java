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
import com.terraforged.mod.worldgen.cave.CaveTunnelRiverDecorator;
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

    public static boolean qualifiesTunnelMegaGiga(Generator generator, int seed, int x, int z) {
        if (CaveOceanFilter.isNearSea(generator, x, z)) {
            return false;
        }
        return CaveTunnelRiverDecorator.qualifiesMountainMassif(generator, seed, x, z);
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
