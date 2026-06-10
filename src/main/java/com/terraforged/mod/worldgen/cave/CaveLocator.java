package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveSiteTags;
import com.terraforged.mod.worldgen.cave.CaveSubtype;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.Module;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class CaveLocator {
    private static final float GIGA_THRESHOLD = 0.12f;
    private static final float MEGA_THRESHOLD = 0.3f;
    public static final int DEFAULT_RADIUS_GIGA = 50000;
    public static final int DEFAULT_RADIUS_MEGA = 8000;
    public static final int MAX_SEARCH_RADIUS = 100000;

    private CaveLocator() {
    }

    public static Result find(Generator generator, CaveType type, int originX, int originZ, int radius) {
        return CaveLocator.find(generator, type, CaveSubtype.ANY, originX, originZ, radius);
    }

    public static Result find(Generator generator, CaveType type, CaveSubtype subtype, int originX, int originZ, int radius) {
        int worldSeed = Seeds.get(generator.getSeed());
        Module modifier = CaveModifiers.get(type);
        float threshold = type == CaveType.GIGA ? 0.12f : 0.3f;
        int step = CaveLocator.searchStep(type, radius);
        float best = threshold;
        int bestX = 0;
        int bestZ = 0;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                float strength;
                int x = originX + dx;
                int z = originZ + dz;
                if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, x, z) || subtype.isCoastal() && !CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, x, z) || subtype.isTunnel() && !CaveSiteTags.qualifiesTunnelMegaGiga(generator, worldSeed, x, z) || !((strength = CaveNoise.sample(modifier, worldSeed, x, z)) > best)) continue;
                best = strength;
                bestX = x;
                bestZ = z;
            }
        }
        if (best <= threshold) {
            return null;
        }
        int[] refined = CaveLocator.refine(modifier, worldSeed, bestX, bestZ, step / 2, generator, type, subtype);
        bestX = refined[0];
        bestZ = refined[1];
        best = CaveNoise.sample(modifier, worldSeed, bestX, bestZ);
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, bestX, bestZ)) {
            return null;
        }
        if (subtype.isCoastal() && !CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, bestX, bestZ)) {
            return null;
        }
        if (subtype.isTunnel() && !CaveSiteTags.qualifiesTunnelMegaGiga(generator, worldSeed, bestX, bestZ)) {
            return null;
        }
        NoiseCave cave = CaveLocator.findConfig(generator, type);
        int y = cave != null ? cave.getHeight(worldSeed, bestX, bestZ) : 32;
        CaveSubtype detected = CaveSiteTags.detectSubtype(generator, type, worldSeed, bestX, bestZ);
        return new Result(bestX, bestZ, y, best, type, detected);
    }

    private static int searchStep(CaveType type, int radius) {
        if (type == CaveType.GIGA) {
            return Math.min(512, Math.max(64, radius / 400));
        }
        return Math.min(256, Math.max(32, radius / 400));
    }

    public static int defaultRadius(CaveType type) {
        return type == CaveType.GIGA ? 50000 : 8000;
    }

    private static int[] refine(Module modifier, int seed, int x, int z, int range, Generator generator, CaveType type, CaveSubtype subtype) {
        float best = CaveNoise.sample(modifier, seed, x, z);
        int bestX = x;
        int bestZ = z;
        for (int dx = -range; dx <= range; dx += 8) {
            for (int dz = -range; dz <= range; dz += 8) {
                float v;
                int px = x + dx;
                int pz = z + dz;
                if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, px, pz) || subtype.isCoastal() && !CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, seed, px, pz) || subtype.isTunnel() && !CaveSiteTags.qualifiesTunnelMegaGiga(generator, seed, px, pz) || !((v = CaveNoise.sample(modifier, seed, px, pz)) > best)) continue;
                best = v;
                bestX = px;
                bestZ = pz;
            }
        }
        return new int[]{bestX, bestZ};
    }

    public static NoiseCave findConfig(Generator generator, CaveType type) {
        Registry<NoiseCave> registry = generator.getBiomeSource().getRegistries().registryOrThrow(TerraForged.CAVES.get());
        NoiseCave fallback = null;
        for (Holder<NoiseCave> holder : registry.holders().toList()) {
            NoiseCave cave = (NoiseCave)holder.value();
            if (cave.getType() != type) continue;
            if (fallback == null) {
                fallback = cave;
            }
            if (!holder.unwrapKey().map(k -> k.location().getPath().equals("giga")).orElse(false)) continue;
            return cave;
        }
        return fallback;
    }

    public record Result(int x, int z, int y, float strength, CaveType type, CaveSubtype subtype) {
    }
}
