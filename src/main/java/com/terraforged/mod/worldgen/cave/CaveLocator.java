package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CaveBreaches;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveSiteTags;
import com.terraforged.mod.worldgen.cave.CaveSubtype;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class CaveLocator {
    private static final float GIGA_THRESHOLD = 0.12f;
    private static final float MEGA_THRESHOLD = 0.3f;
    private static final float GROTTO_GATE = 0.82f;
    private static final float ENTRANCE_GATE = 0.72f;
    private static final float ENTRANCE_BREACH = 0.55f;
    private static final float GROTTO_DRY_REGION = 0.92f;
    private static final float GROTTO_INLAND = 0.78f;
    public static final int DEFAULT_RADIUS_GIGA = 100000;
    public static final int DEFAULT_RADIUS_MEGA = 32000;
    public static final int DEFAULT_RADIUS_GROTTO = 16000;
    public static final int MAX_SEARCH_RADIUS = 500000;
    private static final int MAX_CANDIDATES = 24;
    private static final int MAX_ENTRANCE_CELL_CHECKS = 96;
    private static final int MAX_GROTTO_COARSE_RINGS = 96;
    private static final int GROTTO_REFINE_RADIUS = 64;
    private static final int GROTTO_REFINE_STEP = 8;

    private CaveLocator() {
    }

    public enum LocateMode {
        SYSTEM,
        ENTRANCE
    }

    public enum EntranceKind {
        SYSTEM("system center"),
        MASSIF("inland massif entrance"),
        COASTAL("coastal entrance"),
        TUNNEL_MOUTH("tunnel mouth"),
        TUNNEL_EXIT("tunnel exit"),
        GROTTO("river grotto entrance");

        private final String label;

        EntranceKind(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public static Result find(Generator generator, CaveType type, int originX, int originZ, int radius) {
        return CaveLocator.find(generator, type, CaveSubtype.ANY, LocateMode.SYSTEM, originX, originZ, radius);
    }

    public static Result find(Generator generator, CaveType type, CaveSubtype subtype, int originX, int originZ, int radius) {
        return CaveLocator.find(generator, type, subtype, LocateMode.SYSTEM, originX, originZ, radius);
    }

    public static Result find(Generator generator, CaveType type, CaveSubtype subtype, LocateMode mode, int originX, int originZ, int radius) {
        radius = Math.min(Math.max(radius, 256), MAX_SEARCH_RADIUS);
        if (mode == LocateMode.ENTRANCE) {
            return CaveLocator.findNearestEntrance(generator, type, subtype, originX, originZ, radius);
        }
        return CaveLocator.findSystemCenter(generator, type, subtype, originX, originZ, radius);
    }

    public static Result findGrotto(Generator generator, int originX, int originZ, int radius) {
        radius = Math.min(Math.max(radius, 256), MAX_SEARCH_RADIUS);
        int worldSeed = Seeds.get(generator.getSeed());
        int ringStep = Math.min(256, Math.max(96, radius / 200));
        long radiusSq = (long)radius * radius;
        int ringsChecked = 0;
        for (int ring = 0; ring <= radius; ring += ringStep) {
            if (++ringsChecked > MAX_GROTTO_COARSE_RINGS) {
                break;
            }
            EntranceHit coarseBest = CaveLocator.findBestCheapGrottoOnRing(generator, worldSeed, originX, originZ, ring, ringStep, radiusSq);
            if (coarseBest == null) {
                continue;
            }
            EntranceHit refined = CaveLocator.refineGrotto(generator, worldSeed, originX, originZ, coarseBest.x, coarseBest.z, radiusSq);
            if (refined != null) {
                return refined.toResult(CaveType.GLOBAL, CaveSubtype.ANY);
            }
        }
        return null;
    }

    private static EntranceHit findBestCheapGrottoOnRing(Generator generator, int worldSeed, int originX, int originZ, int ring, int step, long radiusSq) {
        if (ring == 0) {
            return CaveLocator.cheapGrottoPoint(generator, worldSeed, originX, originZ, originX, originZ, radiusSq);
        }
        EntranceHit best = null;
        for (int dx = -ring; dx <= ring; dx += step) {
            best = CaveLocator.pickCloserGrotto(best, CaveLocator.cheapGrottoPoint(generator, worldSeed, originX, originZ, originX + dx, originZ - ring, radiusSq));
            best = CaveLocator.pickCloserGrotto(best, CaveLocator.cheapGrottoPoint(generator, worldSeed, originX, originZ, originX + dx, originZ + ring, radiusSq));
        }
        for (int dz = -ring + step; dz <= ring - step; dz += step) {
            best = CaveLocator.pickCloserGrotto(best, CaveLocator.cheapGrottoPoint(generator, worldSeed, originX, originZ, originX - ring, originZ + dz, radiusSq));
            best = CaveLocator.pickCloserGrotto(best, CaveLocator.cheapGrottoPoint(generator, worldSeed, originX, originZ, originX + ring, originZ + dz, radiusSq));
        }
        return best;
    }

    private static EntranceHit cheapGrottoPoint(Generator generator, int worldSeed, int originX, int originZ, int x, int z, long radiusSq) {
        if (CaveLocator.distSq(originX, originZ, x, z) > radiusSq) {
            return null;
        }
        if (!CaveLocator.cheapGrottoNoise(worldSeed, x, z)) {
            return null;
        }
        return new EntranceHit(x, z, 0, 1.0f, EntranceKind.GROTTO, CaveLocator.distSq(originX, originZ, x, z));
    }

    private static EntranceHit pickCloserGrotto(EntranceHit best, EntranceHit candidate) {
        if (candidate == null) {
            return best;
        }
        if (best == null || candidate.distSq < best.distSq) {
            return candidate;
        }
        return best;
    }

    private static EntranceHit refineGrotto(Generator generator, int worldSeed, int originX, int originZ, int centerX, int centerZ, long radiusSq) {
        EntranceHit best = null;
        for (int dx = -GROTTO_REFINE_RADIUS; dx <= GROTTO_REFINE_RADIUS; dx += GROTTO_REFINE_STEP) {
            for (int dz = -GROTTO_REFINE_RADIUS; dz <= GROTTO_REFINE_RADIUS; dz += GROTTO_REFINE_STEP) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (CaveLocator.distSq(originX, originZ, x, z) > radiusSq) {
                    continue;
                }
                if (!CaveLocator.qualifiesGrottoSite(generator, worldSeed, x, z)) {
                    continue;
                }
                EntranceHit hit = CaveLocator.grottoHit(generator, worldSeed, x, z, originX, originZ);
                if (best == null || hit.distSq < best.distSq) {
                    best = hit;
                }
            }
        }
        return best;
    }

    private static boolean cheapGrottoNoise(int seed, int x, int z) {
        float gate = (NoiseUtil.valCoord2D(seed ^ 0x607770, x, z) + 1.0f) * 0.5f;
        if (gate <= GROTTO_GATE) {
            return false;
        }
        if (CaveNoise.sample(CaveModifiers.mega(), seed, x, z) > 0.22f) {
            return false;
        }
        return CaveNoise.sample(CaveModifiers.giga(), seed, x, z) <= 0.1f;
    }

    private static boolean cheapGrottoRegion(Generator generator, int x, int z) {
        NoiseSample sample = generator.getTerrainSample(x, z);
        if (sample.continentNoise >= GROTTO_INLAND) {
            return false;
        }
        return sample.riverNoise < GROTTO_DRY_REGION;
    }

    private static Result findSystemCenter(Generator generator, CaveType type, CaveSubtype subtype, int originX, int originZ, int radius) {
        int worldSeed = Seeds.get(generator.getSeed());
        Module modifier = CaveModifiers.get(type);
        float threshold = type == CaveType.GIGA ? GIGA_THRESHOLD : MEGA_THRESHOLD;
        int step = CaveLocator.searchStep(type, radius);
        int coarseStep = step * 2;
        List<Candidate> candidates = new ArrayList<Candidate>(MAX_CANDIDATES);
        for (int dx = -radius; dx <= radius; dx += coarseStep) {
            for (int dz = -radius; dz <= radius; dz += coarseStep) {
                int x = originX + dx;
                int z = originZ + dz;
                float strength = CaveNoise.sample(modifier, worldSeed, x, z);
                if (strength <= threshold) {
                    continue;
                }
                CaveLocator.addCandidate(candidates, x, z, strength, originX, originZ);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.distSq).thenComparing((a, b) -> Float.compare(b.strength, a.strength)));
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if (!CaveLocator.passesFilters(generator, type, subtype, worldSeed, candidate.x, candidate.z)) {
                continue;
            }
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
        return new Result(bestX, bestZ, y, strength, type, detected, LocateMode.SYSTEM, EntranceKind.SYSTEM);
    }

    private static Result findNearestEntrance(Generator generator, CaveType type, CaveSubtype subtype, int originX, int originZ, int radius) {
        int worldSeed = Seeds.get(generator.getSeed());
        int r = CaveSystemGrid.caveRadius(type);
        int cell = r * 2;
        long radiusSq = (long)radius * radius;
        Module modifier = CaveModifiers.get(type);
        float threshold = type == CaveType.GIGA ? GIGA_THRESHOLD : MEGA_THRESHOLD;
        NoiseCave config = CaveLocator.findConfig(generator, type);
        int startCx = Math.floorDiv(originX - radius - r, cell) * cell + r;
        int endCx = originX + radius;
        int startCz = Math.floorDiv(originZ - radius - r, cell) * cell + r;
        int endCz = originZ + radius;
        List<CellCandidate> cells = new ArrayList<>();
        for (int cx = startCx; cx <= endCx; cx += cell) {
            for (int cz = startCz; cz <= endCz; cz += cell) {
                double d = CaveLocator.distSq(originX, originZ, cx, cz);
                if (d > radiusSq) {
                    continue;
                }
                cells.add(new CellCandidate(cx, cz, d));
            }
        }
        cells.sort(Comparator.comparingDouble(c -> c.distSq));
        int checked = 0;
        for (CellCandidate cellCenter : cells) {
            if (++checked > MAX_ENTRANCE_CELL_CHECKS) {
                break;
            }
            if (!CaveLocator.cheapCellHasType(modifier, worldSeed, type, threshold, generator, cellCenter.cx, cellCenter.cz)) {
                continue;
            }
            int[] mouth = CaveSystemGrid.resolveTunnelMouthAnchor(worldSeed, type, cellCenter.cx, cellCenter.cz);
            Result hit = CaveLocator.tryEntranceAt(generator, type, subtype, worldSeed, originX, originZ, mouth[0], mouth[1], config, modifier);
            if (hit != null) {
                return hit;
            }
            if (subtype.isTunnel() || subtype == CaveSubtype.ANY) {
                CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(CaveSystemGrid.systemKey(cellCenter.cx, cellCenter.cz, type));
                if (axis == null) {
                    axis = CaveSiteTags.prospectiveTunnelAxis(worldSeed, type, cellCenter.cx, cellCenter.cz);
                }
                if (axis != null && CaveLocator.cheapTunnelAxis(worldSeed, axis) && CaveSiteTags.validatesTunnelAxis(generator, worldSeed, type, axis)) {
                    hit = CaveLocator.tryEntranceAt(generator, type, CaveSubtype.TUNNEL, worldSeed, originX, originZ, axis.exitX(), axis.exitZ(), config, modifier);
                    if (hit != null) {
                        return hit;
                    }
                }
            }
        }
        return null;
    }

    private static Result tryEntranceAt(Generator generator, CaveType type, CaveSubtype subtype, int worldSeed, int originX, int originZ, int x, int z, NoiseCave config, Module modifier) {
        if (!CaveLocator.cheapEntranceAnchorGate(worldSeed, x, z)) {
            return null;
        }
        EntranceKind kind = CaveLocator.classifyEntrance(generator, type, subtype, worldSeed, x, z, config, modifier);
        if (kind == null) {
            return null;
        }
        int y = generator.getOceanFloorHeight(x, z);
        float strength = CaveNoise.sample(modifier, worldSeed, x, z);
        CaveSubtype detected = switch (kind) {
            case COASTAL -> CaveSubtype.COASTAL;
            case TUNNEL_MOUTH, TUNNEL_EXIT -> CaveSubtype.TUNNEL;
            default -> subtype;
        };
        return new Result(x, z, y, strength, type, detected, LocateMode.ENTRANCE, kind);
    }

    private static boolean cheapCellHasType(Module modifier, int worldSeed, CaveType type, float threshold, Generator generator, int cx, int cz) {
        if (CaveNoise.sample(modifier, worldSeed, cx, cz) <= threshold) {
            return false;
        }
        return type != CaveType.GIGA || CaveReliefFilter.qualifiesGigaTerrain(generator, cx, cz);
    }

    private static boolean cheapEntranceAnchorGate(int worldSeed, int x, int z) {
        float gate = (NoiseUtil.valCoord2D(worldSeed ^ 0xE471A1, x, z) + 1.0f) * 0.5f;
        if (gate <= ENTRANCE_GATE) {
            return false;
        }
        return CaveBreaches.sample(worldSeed, x, z) >= ENTRANCE_BREACH;
    }

    private static boolean cheapTunnelAxis(int worldSeed, CaveEntranceClaims.TunnelAxis axis) {
        if (!CaveReliefFilter.validatesTunnelSpan(axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ())) {
            return false;
        }
        return CaveLocator.cheapEntranceAnchorGate(worldSeed, axis.mouthX(), axis.mouthZ())
                || CaveLocator.cheapEntranceAnchorGate(worldSeed, axis.exitX(), axis.exitZ());
    }

    private static EntranceKind classifyEntrance(Generator generator, CaveType type, CaveSubtype subtype, int worldSeed, int x, int z, NoiseCave config, Module modifier) {
        if (subtype.isCoastal()) {
            return CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, x, z) ? EntranceKind.COASTAL : null;
        }
        if (subtype.isTunnel()) {
            if (!CaveSiteTags.qualifiesProspectiveTunnel(generator, worldSeed, x, z)) {
                return null;
            }
            CaveEntranceClaims.TunnelAxis axis = CaveLocator.resolveAxis(generator, worldSeed, type, x, z);
            if (axis == null) {
                return null;
            }
            if (CaveLocator.isNear(x, z, axis.mouthX(), axis.mouthZ(), 4)) {
                return EntranceKind.TUNNEL_MOUTH;
            }
            if (CaveLocator.isNear(x, z, axis.exitX(), axis.exitZ(), 5)) {
                return EntranceKind.TUNNEL_EXIT;
            }
            return null;
        }
        if (CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, x, z)) {
            return EntranceKind.COASTAL;
        }
        CaveEntranceClaims.TunnelAxis axis = CaveLocator.resolveAxis(generator, worldSeed, type, x, z);
        if (axis != null && CaveSiteTags.qualifiesProspectiveTunnel(generator, worldSeed, x, z)) {
            if (CaveLocator.isNear(x, z, axis.mouthX(), axis.mouthZ(), 4)) {
                return EntranceKind.TUNNEL_MOUTH;
            }
            if (CaveLocator.isNear(x, z, axis.exitX(), axis.exitZ(), 5)) {
                return EntranceKind.TUNNEL_EXIT;
            }
        }
        if (CaveLocator.qualifiesMassifEntrance(generator, type, worldSeed, x, z, config, modifier)) {
            return EntranceKind.MASSIF;
        }
        return null;
    }

    private static CaveEntranceClaims.TunnelAxis resolveAxis(Generator generator, int worldSeed, CaveType type, int x, int z) {
        long key = CaveSystemGrid.systemKey(x, z, type);
        CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(key);
        if (axis == null) {
            axis = CaveSiteTags.prospectiveTunnelAxis(worldSeed, type, x, z);
        }
        return axis;
    }

    private static boolean qualifiesMassifEntrance(Generator generator, CaveType type, int worldSeed, int x, int z, NoiseCave config, Module modifier) {
        if (CaveSystemGrid.dominantType(generator, worldSeed, x, z) != type) {
            return false;
        }
        if (type == CaveType.GIGA && !CaveReliefFilter.qualifiesGigaTerrain(generator, x, z)) {
            return false;
        }
        if (!CaveSystemGrid.isEntranceAnchorColumn(worldSeed, x, z, type)) {
            return false;
        }
        if (!CaveMassifCache.qualifiesMountainMassif(generator, worldSeed, x, z)) {
            return false;
        }
        int sea = generator.getSeaLevel();
        if (generator.getOceanFloorHeight(x, z) <= sea + 6) {
            return false;
        }
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
            return false;
        }
        if (CaveOceanFilter.isNearSea(generator, x, z)) {
            return false;
        }
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, x, z)) {
            return false;
        }
        if (config == null) {
            return false;
        }
        float value = CaveNoise.sample(modifier, worldSeed, x, z);
        int cavern = config.getCavernSize(worldSeed, x, z, value);
        if (cavern < CaveEntranceCarver.minCavernForEntrance()) {
            return false;
        }
        return true;
    }

    private static boolean qualifiesGrottoSite(Generator generator, int seed, int x, int z) {
        if (!CaveLocator.cheapGrottoNoise(seed, x, z) || !CaveLocator.cheapGrottoRegion(generator, x, z)) {
            return false;
        }
        int sea = generator.getSeaLevel();
        if (generator.getOceanFloorHeight(x, z) <= sea + 8) {
            return false;
        }
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
            return false;
        }
        if (!CaveOceanFilter.isNearSea(generator, x, z)) {
            return false;
        }
        if (!CaveRiverProximityCache.columnNearRiver(generator, x, z)) {
            return false;
        }
        if (CaveOceanFilter.sampleHeightGradient(generator, x, z) < 0.35f) {
            return false;
        }
        float giga = CaveNoise.sample(CaveModifiers.giga(), seed, x, z);
        return giga <= 0.1f || !CaveReliefFilter.qualifiesGigaTerrain(generator, x, z);
    }

    private static EntranceHit grottoHit(Generator generator, int seed, int x, int z, int originX, int originZ) {
        int y = generator.getOceanFloorHeight(x, z);
        return new EntranceHit(x, z, y, 1.0f, EntranceKind.GROTTO, CaveLocator.distSq(originX, originZ, x, z));
    }

    private static void addCandidate(List<Candidate> candidates, int x, int z, float strength, int originX, int originZ) {
        if (candidates.size() < MAX_CANDIDATES) {
            candidates.add(new Candidate(x, z, strength, originX, originZ));
            return;
        }
        Candidate weakest = candidates.get(0);
        for (Candidate candidate : candidates) {
            if (candidate.strength >= weakest.strength) {
                continue;
            }
            weakest = candidate;
        }
        if (strength <= weakest.strength) {
            return;
        }
        weakest.x = x;
        weakest.z = z;
        weakest.strength = strength;
        weakest.distSq = CaveLocator.distSq(originX, originZ, x, z);
    }

    private static boolean passesFilters(Generator generator, CaveType type, CaveSubtype subtype, int worldSeed, int x, int z) {
        if (type == CaveType.GIGA && !CaveReliefFilter.qualifiesGigaTerrain(generator, x, z)) {
            return false;
        }
        if (CaveOceanFilter.isBlockedForMegaGiga(generator, type, x, z)) {
            return false;
        }
        if (subtype.isCoastal() && !CaveSiteTags.qualifiesCoastalMegaGiga(generator, type, worldSeed, x, z)) {
            return false;
        }
        return !subtype.isTunnel() || CaveSiteTags.qualifiesProspectiveTunnel(generator, worldSeed, x, z);
    }

    private static int searchStep(CaveType type, int radius) {
        if (type == CaveType.GIGA) {
            return Math.min(768, Math.max(96, radius / 300));
        }
        return Math.min(384, Math.max(48, radius / 300));
    }

    public static int defaultRadius(CaveType type) {
        return type == CaveType.GIGA ? DEFAULT_RADIUS_GIGA : DEFAULT_RADIUS_MEGA;
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
                if (!(v > best)) {
                    continue;
                }
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
                    if (!CaveLocator.passesFilters(generator, type, subtype, seed, px, pz) || !((v = CaveNoise.sample(modifier, seed, px, pz)) > best)) {
                        continue;
                    }
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
            if (cave.getType() != type) {
                continue;
            }
            if (fallback == null) {
                fallback = cave;
            }
            if (!holder.unwrapKey().map(k -> k.location().getPath().equals("giga")).orElse(false)) {
                continue;
            }
            return cave;
        }
        return fallback;
    }

    private static double distSq(int ax, int az, int bx, int bz) {
        long dx = (long)bx - ax;
        long dz = (long)bz - az;
        return dx * dx + dz * dz;
    }

    private static boolean isNear(int x, int z, int ax, int az, int radius) {
        int dx = x - ax;
        int dz = z - az;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static final class CellCandidate {
        final int cx;
        final int cz;
        final double distSq;

        CellCandidate(int cx, int cz, double distSq) {
            this.cx = cx;
            this.cz = cz;
            this.distSq = distSq;
        }
    }

    private static final class Candidate {
        int x;
        int z;
        float strength;
        double distSq;

        Candidate(int x, int z, float strength, int originX, int originZ) {
            this.x = x;
            this.z = z;
            this.strength = strength;
            this.distSq = CaveLocator.distSq(originX, originZ, x, z);
        }
    }

    private static final class EntranceHit {
        final int x;
        final int z;
        final int y;
        final float strength;
        final EntranceKind kind;
        final double distSq;

        EntranceHit(int x, int z, int y, float strength, EntranceKind kind, double distSq) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.strength = strength;
            this.kind = kind;
            this.distSq = distSq;
        }

        Result toResult(CaveType type, CaveSubtype subtype) {
            CaveSubtype detected = switch (this.kind) {
                case COASTAL -> CaveSubtype.COASTAL;
                case TUNNEL_MOUTH, TUNNEL_EXIT -> CaveSubtype.TUNNEL;
                default -> subtype;
            };
            return new Result(this.x, this.z, this.y, this.strength, type, detected, LocateMode.ENTRANCE, this.kind);
        }
    }

    public record Result(int x, int z, int y, float strength, CaveType type, CaveSubtype subtype, LocateMode mode, EntranceKind entranceKind) {
    }
}
