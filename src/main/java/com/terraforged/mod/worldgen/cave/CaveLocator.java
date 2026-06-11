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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class CaveLocator {
    private static final float GIGA_THRESHOLD = 0.12f;
    private static final float MEGA_THRESHOLD = 0.3f;
    public static final int DEFAULT_RADIUS_GIGA = 50000;
    public static final int DEFAULT_RADIUS_MEGA = 8000;
    public static final int MAX_SEARCH_RADIUS = 100000;
    private static final int MAX_CANDIDATES = 16;

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
        int coarseStep = step * 2;
        List<Candidate> candidates = new ArrayList<Candidate>(MAX_CANDIDATES);
        for (int dx = -radius; dx <= radius; dx += coarseStep) {
            for (int dz = -radius; dz <= radius; dz += coarseStep) {
                int x = originX + dx;
                int z = originZ + dz;
                float strength = CaveNoise.sample(modifier, worldSeed, x, z);
                if (strength <= threshold) continue;
                CaveLocator.addCandidate(candidates, x, z, strength);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((a, b) -> Float.compare(b.strength, a.strength));
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if (!CaveLocator.passesFilters(generator, type, subtype, worldSeed, candidate.x, candidate.z)) continue;
            best = candidate;
            break;
        }
        if (best == null) {
            return null;
        }
        int[] refined = CaveLocator.refine(modifier, worldSeed, best.x, best.z, step / 2, generator, type, subtype);
        int bestX = refined[0];
        int bestZ = refined[1];
        float strength = CaveNoise.sample(modifier, worldSeed, bestX, bestZ);
        if (strength <= threshold || !CaveLocator.passesFilters(generator, type, subtype, worldSeed, bestX, bestZ)) {
            return null;
        }
        NoiseCave cave = CaveLocator.findConfig(generator, type);
        int y = cave != null ? cave.getHeight(worldSeed, bestX, bestZ) : 32;
        CaveSubtype detected = CaveSiteTags.detectSubtype(generator, type, worldSeed, bestX, bestZ);
        return new Result(bestX, bestZ, y, strength, type, detected);
    }

    private static void addCandidate(List<Candidate> candidates, int x, int z, float strength) {
        if (candidates.size() < MAX_CANDIDATES) {
            candidates.add(new Candidate(x, z, strength));
            return;
        }
        Candidate weakest = candidates.get(0);
        for (Candidate candidate : candidates) {
            if (candidate.strength >= weakest.strength) continue;
            weakest = candidate;
        }
        if (strength <= weakest.strength) {
            return;
        }
        weakest.x = x;
        weakest.z = z;
        weakest.strength = strength;
    }

    private static boolean passesFilters(Generator generator, CaveType type, CaveSubtype subtype, int worldSeed, int x, int z) {
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, x, z)) {
            return false;
        }
        if (subtype.isCoastal() && !CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, x, z)) {
            return false;
        }
        return !subtype.isTunnel() || CaveSiteTags.qualifiesTunnelMegaGiga(generator, worldSeed, x, z);
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
                int px = x + dx;
                int pz = z + dz;
                float v = CaveNoise.sample(modifier, seed, px, pz);
                if (!(v > best)) continue;
                best = v;
                bestX = px;
                bestZ = pz;
            }
        }
        if (!CaveLocator.passesFilters(generator, type, subtype, seed, bestX, bestZ)) {
            for (int dx = -range; dx <= range; dx += 8) {
                for (int dz = -range; dz <= range; dz += 8) {
                    float v;
                    int px = x + dx;
                    int pz = z + dz;
                    if (!CaveLocator.passesFilters(generator, type, subtype, seed, px, pz) || !((v = CaveNoise.sample(modifier, seed, px, pz)) > best)) continue;
                    best = v;
                    bestX = px;
                    bestZ = pz;
                }
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

    private static final class Candidate {
        int x;
        int z;
        float strength;

        Candidate(int x, int z, float strength) {
            this.x = x;
            this.z = z;
            this.strength = strength;
        }
    }

    public record Result(int x, int z, int y, float strength, CaveType type, CaveSubtype subtype) {
    }
}
