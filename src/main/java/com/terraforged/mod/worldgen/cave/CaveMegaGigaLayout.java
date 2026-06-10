package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.cave.CaveBiomeClimateAffinity;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeStats;
import com.terraforged.mod.worldgen.cave.CaveClimateType;
import com.terraforged.mod.worldgen.cave.CaveLayoutRegionGrid;
import com.terraforged.mod.worldgen.cave.CaveStatInitializer;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class CaveMegaGigaLayout {
    private static final int MIN_UNIQUE_REGIONS = 5;
    private final float centerX;
    private final float centerZ;
    private final float blurRadius;
    private final CaveStatVector globalPool;
    private final CaveClimateType climateType;
    private final List<GeneratorNode> generators;
    private final List<Sector> sectors;
    private final List<CaveBiomeEntry> shellPool;
    private final int layoutSeed;
    private CaveLayoutRegionGrid regionGrid;

    private CaveMegaGigaLayout(float centerX, float centerZ, float blurRadius, CaveStatVector globalPool, CaveClimateType climateType, List<GeneratorNode> generators, List<Sector> sectors, List<CaveBiomeEntry> shellPool, int layoutSeed, CaveLayoutRegionGrid regionGrid) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.blurRadius = blurRadius;
        this.globalPool = globalPool;
        this.climateType = climateType;
        this.generators = generators;
        this.sectors = sectors;
        this.shellPool = shellPool;
        this.layoutSeed = layoutSeed;
        this.regionGrid = regionGrid;
    }

    public List<GeneratorNode> generators() {
        return this.generators;
    }

    public CaveStatVector globalPool() {
        return this.globalPool;
    }

    public CaveClimateType climateType() {
        return this.climateType;
    }

    public CaveLayoutRegionGrid regionGrid() {
        return this.regionGrid;
    }

    public CaveBiomeEntry getBiomeAt(int x, int z) {
        return this.resolveRegionalBiome(x, z);
    }

    public CaveStatVector statsAt(int x, int z) {
        CaveStatVector stats = this.regionGrid != null ? this.regionGrid.statsAt(x, z) : this.globalPool;
        float dx = (float)x - this.centerX;
        float dz = (float)z - this.centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        float edgeFade = MathUtil.clamp(dist / Math.max(1.0f, this.blurRadius * 4.0f), 0.0f, 1.0f);
        return stats.add(new CaveStatVector(-edgeFade, -edgeFade * 0.25f, -edgeFade * 0.5f)).clamped();
    }

    private CaveBiomeEntry resolveRegionalBiome(int x, int z) {
        CaveBiomeEntry sectorBiome;
        CaveBiomeEntry warm;
        if (this.sectors.isEmpty() && this.shellPool.isEmpty()) {
            return null;
        }
        CaveStatVector localStats = this.approximateStatsAt(x, z);
        boolean thermalOasis = this.isThermalOasis(x, z, localStats);
        for (GeneratorNode generator : this.generators) {
            float gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z());
            if (!(gDist <= this.generatorCoreRadius())) continue;
            return generator.biome();
        }
        if (thermalOasis && !this.shellPool.isEmpty() && (warm = this.pickBiomeForPosition(x, z, localStats, true)) != null) {
            return warm;
        }
        if (this.sectors.isEmpty()) {
            return this.pickBiomeForPosition(x, z, localStats, false);
        }
        float dx = (float)x - this.centerX;
        float dz = (float)z - this.centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        float angle = CaveMegaGigaLayout.normalizeAngle((float)Math.atan2(dz, dx));
        Sector best = null;
        float bestScore = Float.MAX_VALUE;
        for (Sector sector : this.sectors) {
            if (!CaveMegaGigaLayout.inSectorStrict(dist, angle, sector)) continue;
            float centerDist = (sector.innerRadius() + sector.outerRadius()) * 0.5f;
            float centerAngle = (sector.angleStart() + sector.angleEnd()) * 0.5f;
            float score = Math.abs(dist - centerDist) + Math.abs(CaveMegaGigaLayout.shortestAngleDelta(angle, centerAngle)) * 8.0f;
            if (!(score < bestScore)) continue;
            bestScore = score;
            best = sector;
        }
        CaveBiomeEntry caveBiomeEntry = sectorBiome = best == null ? this.sectors.get(this.sectors.size() - 1).biome() : best.biome();
        if (!thermalOasis && localStats.temperature() <= -1.0f && sectorBiome != null && CaveBiomeClimateAffinity.isWarmOasisBiome(sectorBiome.biome().getPath())) {
            CaveBiomeEntry cold = this.pickBiomeForPosition(x, z, localStats, false);
            return cold != null ? cold : sectorBiome;
        }
        return sectorBiome;
    }

    private CaveStatVector approximateStatsAt(int x, int z) {
        CaveStatVector stats = this.globalPool;
        for (GeneratorNode generator : this.generators) {
            float factor;
            float gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z());
            ResourceLocation genId = generator.biome().biome();
            CaveBiomeStats genStats = generator.biome().stats();
            if (CaveBiomeClimateAffinity.isColdGenerator(genId)) {
                float coldRadius = this.blurRadius * 3.5f;
                factor = CaveMegaGigaLayout.smoothFalloff(gDist, coldRadius);
                factor *= factor;
                stats = stats.add(new CaveStatVector(-1.0f * factor, -Math.max(3.0f, Math.abs(genStats.global().temperature())) * factor * 1.5f, 0.0f));
                continue;
            }
            if (CaveBiomeClimateAffinity.isSpringGenerator(genId)) {
                float springRadius = this.blurRadius * 4.5f;
                factor = CaveMegaGigaLayout.smoothFalloff(gDist, springRadius);
                factor *= factor;
                stats = stats.add(genStats.local().scale(factor));
                continue;
            }
            if (!CaveBiomeClimateAffinity.isHeatGenerator(genId)) continue;
            float heatRadius = this.blurRadius * 4.5f;
            factor = CaveMegaGigaLayout.smoothFalloff(gDist, heatRadius);
            factor *= factor;
            stats = stats.add(genStats.local().scale(factor));
            CaveStatVector heat = genStats.globalForClimate(this.climateType);
            float tempBoost = Math.max(heat.temperature(), 4.0f) * factor * 1.8f;
            stats = stats.add(new CaveStatVector(heat.moisture() * factor * 0.6f, tempBoost, heat.fertility() * factor * 0.5f));
        }
        return stats.clamped();
    }

    private CaveStatVector generatorStatSource(GeneratorNode generator) {
        CaveBiomeStats genStats = generator.biome().stats();
        ResourceLocation genId = generator.biome().biome();
        if (CaveBiomeClimateAffinity.isColdGenerator(genId)) {
            return genStats.local().add(new CaveStatVector(-1.0f, -4.0f, 0.0f));
        }
        if (CaveBiomeClimateAffinity.isSpringGenerator(genId)) {
            return genStats.local();
        }
        if (CaveBiomeClimateAffinity.isHeatGenerator(genId)) {
            CaveStatVector heat = genStats.globalForClimate(this.climateType);
            float tempBoost = Math.max(heat.temperature(), 4.0f) * 1.8f;
            CaveStatVector pulse = new CaveStatVector(heat.moisture() * 0.6f, tempBoost, heat.fertility() * 0.5f);
            if (this.climateType == CaveClimateType.FROST) {
                pulse = pulse.add(new CaveStatVector(4.0f, 3.0f, 1.0f));
            }
            return genStats.local().add(pulse);
        }
        return genStats.local();
    }

    private void refreshWarmOasisBiomes() {
        if (this.regionGrid == null || this.shellPool.isEmpty()) {
            return;
        }
        this.regionGrid.forEachCell((center, biome) -> {
            int cx = center[0];
            int cz = center[1];
            CaveStatVector local = this.statsAt(cx, cz);
            if (!this.isThermalOasis(cx, cz, local)) {
                return;
            }
            CaveBiomeEntry warm = this.pickBiomeForPosition(cx, cz, local, true);
            if (warm != null) {
                this.regionGrid.overrideBiome(cx, cz, warm);
            }
        });
    }

    private CaveBiomeEntry pickBiomeForPosition(int x, int z, CaveStatVector stats, boolean thermalOasis) {
        CaveBiomeEntry best = null;
        float bestWeight = -1.0f;
        for (CaveBiomeEntry entry : this.shellPool) {
            float detail;
            float noise;
            float score;
            if (entry.statGenerator() || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || !CaveBiomeClimateAffinity.matches(entry.biome(), this.climateType, stats.temperature(), thermalOasis) || !entry.stats().matches(stats)) continue;
            float w = entry.weight();
            if (CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.22f;
            }
            if (thermalOasis && CaveBiomeClimateAffinity.isWarmOasisBiome(entry.biome().getPath())) {
                w *= 1.4f + stats.temperature() * 0.12f;
            }
            if (!((score = w * (0.55f + (noise = CaveMegaGigaLayout.noise01(this.layoutSeed, x * 7919 + z * 104729)) * 0.3f + (detail = CaveMegaGigaLayout.noise01((int)((long)this.layoutSeed ^ 0x517CC1L), x * 31 + z * 17 + entry.biome().hashCode())) * 0.15f)) > bestWeight)) continue;
            bestWeight = score;
            best = entry;
        }
        if (best != null) {
            return best;
        }
        return this.shellPool.stream().filter(e -> !e.statGenerator()).filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), this.climateType, stats.temperature(), thermalOasis)).max(Comparator.comparingDouble(CaveBiomeEntry::weight)).orElse(null);
    }

    private boolean isThermalOasis(int x, int z, CaveStatVector local) {
        if (local.temperature() < 1.5f) {
            return false;
        }
        for (GeneratorNode generator : this.generators) {
            float gDist;
            if (!CaveBiomeClimateAffinity.isWarmOasisGenerator(generator.biome().biome()) || !((gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z())) <= this.blurRadius * 4.5f)) continue;
            return true;
        }
        return false;
    }

    private float generatorCoreRadius() {
        return Math.max(28.0f, this.blurRadius * 0.45f);
    }

    private static float dist(int x, int z, float gx, float gz) {
        float dx = (float)x - gx;
        float dz = (float)z - gz;
        return NoiseUtil.sqrt(dx * dx + dz * dz);
    }

    private static float smoothFalloff(float distance, float radius) {
        if (radius <= 0.0f) {
            return 0.0f;
        }
        return MathUtil.clamp(1.0f - distance / radius, 0.0f, 1.0f);
    }

    public static CaveMegaGigaLayout build(int seed, float centerX, float centerZ, int radius, CaveBiomeRegistry registry, CaveSystemConfig config, boolean isMega, CaveStatInitializer.CaveStatSnapshot snapshot) {
        int shellMax;
        List<CaveBiomeEntry> generatorPool = registry.getMegaGigaGeneratorPool();
        List<CaveBiomeEntry> shellPool = registry.getMegaGigaShellPool();
        if (generatorPool.isEmpty() && shellPool.isEmpty()) {
            return CaveMegaGigaLayout.empty(centerX, centerZ, snapshot);
        }
        float blur = (float)config.transitionMaxWidthBlocks() * (isMega ? 1.5f : 2.5f);
        CaveStatVector poolStats = snapshot.initial();
        CaveClimateType climate = snapshot.climateType();
        List<GeneratorNode> generators = CaveMegaGigaLayout.pickGenerators(seed, centerX, centerZ, generatorPool, poolStats, climate, isMega);
        for (GeneratorNode generator : generators) {
            if (CaveBiomeClimateAffinity.isSpringGenerator(generator.biome().biome())) continue;
            poolStats = poolStats.add(generator.biome().stats().globalForClimate(climate));
        }
        int shellMin = isMega ? config.megaRegionCountMin() : config.gigaRegionCountMin();
        int n = shellMax = isMega ? config.megaRegionCountMax() : config.gigaRegionCountMax();
        if (shellMin > shellMax) {
            shellMin = shellMax;
        }
        int regionCount = shellMin + Math.abs(NoiseUtil.hash2D(isMega ? seed : seed ^ 0x9E3779B9, (int)centerX, (int)centerZ)) % Math.max(1, shellMax - shellMin + 1);
        regionCount = Math.max(5, regionCount);
        List<Sector> sectors = CaveMegaGigaLayout.buildUniqueRegions(seed, radius, shellPool, poolStats, climate, generators, regionCount, blur);
        CaveMegaGigaLayout layout = new CaveMegaGigaLayout(centerX, centerZ, blur, poolStats.clamped(), climate, generators, sectors, shellPool, seed, null);
        layout.regionGrid = CaveLayoutRegionGrid.build(centerX, centerZ, radius, isMega, poolStats.clamped(), layout::resolveRegionalBiome, generators, layout::generatorStatSource);
        layout.refreshWarmOasisBiomes();
        return layout;
    }

    private static CaveMegaGigaLayout empty(float centerX, float centerZ, CaveStatInitializer.CaveStatSnapshot snapshot) {
        return new CaveMegaGigaLayout(centerX, centerZ, 16.0f, snapshot.initial(), snapshot.climateType(), List.of(), List.of(), List.of(), 0, null);
    }

    private static List<GeneratorNode> pickGenerators(int seed, float centerX, float centerZ, List<CaveBiomeEntry> pool, CaveStatVector stats, CaveClimateType climate, boolean isMega) {
        List<CaveBiomeEntry> candidates = pool.stream().filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), climate) || CaveBiomeClimateAffinity.isHeatGenerator(e.biome()) || CaveBiomeClimateAffinity.isSpringGenerator(e.biome()) || CaveBiomeClimateAffinity.isColdGenerator(e.biome())).filter(e -> e.stats().matches(stats)).toList();
        if (candidates.isEmpty()) {
            candidates = pool.stream().filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), climate) || CaveBiomeClimateAffinity.isHeatGenerator(e.biome()) || CaveBiomeClimateAffinity.isSpringGenerator(e.biome()) || CaveBiomeClimateAffinity.isColdGenerator(e.biome())).toList();
        }
        if (candidates.isEmpty()) {
            candidates = pool;
        }
        int max = isMega ? 2 : 3;
        int count = 1 + Math.abs(NoiseUtil.hash2D(seed ^ 0xCAFE, (int)centerX, (int)centerZ)) % max;
        count = Math.min(count, candidates.size());
        Random rng = new Random(seed);
        ArrayList<CaveBiomeEntry> shuffled = new ArrayList<CaveBiomeEntry>(candidates);
        Collections.shuffle(shuffled, rng);
        ArrayList<GeneratorNode> result = new ArrayList<GeneratorNode>(count);
        for (int i = 0; i < count; ++i) {
            CaveBiomeEntry entry = shuffled.get(i);
            float jitterX = NoiseUtil.valCoord2D(seed, i, 1) * 0.5f * 24.0f;
            float jitterZ = NoiseUtil.valCoord2D(seed, i, 2) * 0.5f * 24.0f;
            result.add(new GeneratorNode(centerX + jitterX, centerZ + jitterZ, entry));
        }
        CaveMegaGigaLayout.ensureThermalGenerator(result, pool, centerX, centerZ);
        return List.copyOf(result);
    }

    private static void ensureThermalGenerator(List<GeneratorNode> nodes, List<CaveBiomeEntry> pool, float centerX, float centerZ) {
        boolean hasWarm = nodes.stream().anyMatch(node -> CaveBiomeClimateAffinity.isWarmOasisGenerator(node.biome().biome()));
        if (hasWarm) {
            return;
        }
        CaveBiomeEntry warm = pool.stream().filter(e -> CaveBiomeClimateAffinity.isSpringGenerator(e.biome()) || CaveBiomeClimateAffinity.isHeatGenerator(e.biome())).findFirst().orElse(null);
        if (warm == null) {
            return;
        }
        if (nodes.isEmpty()) {
            nodes.add(new GeneratorNode(centerX, centerZ, warm));
            return;
        }
        nodes.set(0, new GeneratorNode(centerX, centerZ, warm));
    }

    private static List<Sector> buildUniqueRegions(int seed, int radius, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, int regionCount, float blur) {
        ArrayList<Sector> sectors = new ArrayList<Sector>();
        HashSet<ResourceLocation> usedBiomes = new HashSet<ResourceLocation>();
        float maxRadius = (float)radius * 0.92f;
        float coreRadius = Math.max(24.0f, maxRadius * 0.12f);
        if (!generators.isEmpty()) {
            for (int i = 0; i < generators.size(); ++i) {
                GeneratorNode generator = generators.get(i);
                usedBiomes.add(generator.biome().biome());
                float genRadius = coreRadius * (0.8f + 0.25f * (float)i);
                float width = (float)(Math.PI * 2 / (double)Math.max(1, generators.size()));
                sectors.add(new Sector(CaveMegaGigaLayout.normalizeAngle(width * (float)i - 0.2f), CaveMegaGigaLayout.normalizeAngle(width * (float)(i + 1) + 0.2f), 0.0f, genRadius, 0, generator.biome()));
            }
        }
        float fullCircle = (float)Math.PI * 2;
        float angleOffset = CaveMegaGigaLayout.normalizeAngle(NoiseUtil.valCoord2D(seed, 3, 9) * (float)Math.PI);
        int target = Math.max(5, regionCount);
        for (int r = 0; r < target; ++r) {
            float inner = coreRadius + (maxRadius - coreRadius) * ((float)r / (float)target);
            float outer = coreRadius + (maxRadius - coreRadius) * (((float)r + 1.0f) / (float)target);
            inner = CaveMegaGigaLayout.blurRadius(inner, seed, r + 1);
            outer = CaveMegaGigaLayout.blurRadius(outer, seed, r + 21);
            float span = fullCircle / (float)target;
            float start = CaveMegaGigaLayout.normalizeAngle(angleOffset + span * (float)r);
            float end = CaveMegaGigaLayout.normalizeAngle(angleOffset + span * (float)(r + 1));
            CaveStatVector shellStats = CaveMegaGigaLayout.statsForShell(globalPool, generators, r + 1);
            CaveBiomeEntry picked = CaveMegaGigaLayout.pickBiome(seed, r + 1, 0, pool, shellStats, climate, usedBiomes, true);
            if (picked == null) {
                picked = CaveMegaGigaLayout.pickBiomeUniqueRelaxed(seed, r + 1, pool, climate, usedBiomes);
            }
            if (picked == null) continue;
            usedBiomes.add(picked.biome());
            sectors.add(new Sector(start, end, inner, outer, r + 1, picked));
        }
        CaveMegaGigaLayout.dedupeMegaPrimaryRegions(sectors, seed, pool, globalPool, climate, generators, usedBiomes);
        CaveMegaGigaLayout.ensureMinimumUniqueRegions(sectors, seed, radius, pool, globalPool, climate, generators, blur, usedBiomes);
        return List.copyOf(sectors);
    }

    private static void dedupeMegaPrimaryRegions(List<Sector> sectors, int seed, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, Set<ResourceLocation> usedBiomes) {
        HashSet<ResourceLocation> seen = new HashSet<ResourceLocation>();
        for (int i = 0; i < sectors.size(); ++i) {
            Sector sector = sectors.get(i);
            ResourceLocation id = sector.biome().biome();
            if (seen.add(id)) continue;
            CaveStatVector shellStats = CaveMegaGigaLayout.statsForShell(globalPool, generators, sector.hopFromCore());
            CaveBiomeEntry replacement = CaveMegaGigaLayout.pickBiome(seed, i + 97, 0, pool, shellStats, climate, usedBiomes, true);
            if (replacement == null) {
                replacement = CaveMegaGigaLayout.pickBiomeUniqueRelaxed(seed, i + 97, pool, climate, usedBiomes);
            }
            if (replacement == null) continue;
            usedBiomes.add(replacement.biome());
            sectors.set(i, new Sector(sector.angleStart(), sector.angleEnd(), sector.innerRadius(), sector.outerRadius(), sector.hopFromCore(), replacement));
            seen.add(replacement.biome());
        }
    }

    private static void ensureMinimumUniqueRegions(List<Sector> sectors, int seed, int radius, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, float blur, Set<ResourceLocation> usedBiomes) {
        HashSet<ResourceLocation> distinct = new HashSet<ResourceLocation>();
        for (Sector sector : sectors) {
            distinct.add(sector.biome().biome());
        }
        if (distinct.size() >= 5) {
            return;
        }
        float maxRadius = (float)radius * 0.92f;
        float coreRadius = Math.max(24.0f, maxRadius * 0.12f);
        float fullCircle = (float)Math.PI * 2;
        int attempt = 0;
        while (distinct.size() < 5 && attempt < pool.size() * 2) {
            ++attempt;
            int slot = sectors.size();
            float inner = coreRadius + (maxRadius - coreRadius) * 0.55f;
            float outer = maxRadius;
            inner = CaveMegaGigaLayout.blurRadius(inner, seed, slot + 40);
            outer = CaveMegaGigaLayout.blurRadius(outer, seed, slot + 50);
            float span = fullCircle / 5.0f;
            float start = CaveMegaGigaLayout.normalizeAngle(span * (float)slot + CaveMegaGigaLayout.noise01(seed, slot) * 0.15f);
            float end = CaveMegaGigaLayout.normalizeAngle(start + span * 0.9f);
            CaveStatVector shellStats = CaveMegaGigaLayout.statsForShell(globalPool, generators, slot + 1);
            CaveBiomeEntry picked = CaveMegaGigaLayout.pickBiome(seed, slot + 200, 0, pool, shellStats, climate, usedBiomes, true);
            if (picked == null) {
                picked = CaveMegaGigaLayout.pickBiomeUniqueRelaxed(seed, slot + 200, pool, climate, usedBiomes);
            }
            if (picked == null) break;
            usedBiomes.add(picked.biome());
            distinct.add(picked.biome());
            sectors.add(new Sector(start, end, inner, outer, slot + 1, picked));
        }
    }

    private static CaveBiomeEntry pickBiomeUniqueRelaxed(int seed, int salt, List<CaveBiomeEntry> pool, CaveClimateType climate, Set<ResourceLocation> usedBiomes) {
        CaveBiomeEntry best = null;
        float bestWeight = -1.0f;
        for (CaveBiomeEntry entry : pool) {
            float noise;
            float score;
            if (entry.statGenerator() || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || usedBiomes.contains(entry.biome()) || !CaveBiomeClimateAffinity.matches(entry.biome(), climate, 0.0f, false)) continue;
            float w = entry.weight();
            if (CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.22f;
            }
            if (!((score = w * (0.75f + (noise = CaveMegaGigaLayout.noise01(seed, salt * 31 + entry.biome().hashCode())) * 0.5f)) > bestWeight)) continue;
            bestWeight = score;
            best = entry;
        }
        return best;
    }

    private static List<Sector> buildSectors(int seed, float centerX, float centerZ, int radius, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, int shellCount, float blur, boolean strictUnique) {
        ArrayList<Sector> sectors = new ArrayList<Sector>();
        HashSet<ResourceLocation> usedShellBiomes = new HashSet<ResourceLocation>();
        float maxRadius = (float)radius * 0.92f;
        float coreRadius = Math.max(24.0f, maxRadius * 0.12f);
        if (!generators.isEmpty()) {
            for (int i = 0; i < generators.size(); ++i) {
                GeneratorNode generator = generators.get(i);
                if (strictUnique) {
                    usedShellBiomes.add(generator.biome().biome());
                }
                float genRadius = coreRadius * (0.8f + 0.25f * (float)i);
                float width = (float)(Math.PI * 2 / (double)Math.max(1, generators.size()));
                sectors.add(new Sector(CaveMegaGigaLayout.normalizeAngle(width * (float)i - 0.2f), CaveMegaGigaLayout.normalizeAngle(width * (float)(i + 1) + 0.2f), 0.0f, genRadius, 0, generator.biome()));
            }
        }
        for (int shell = 1; shell <= shellCount; ++shell) {
            float inner = coreRadius + (maxRadius - coreRadius) * (((float)shell - 1.0f) / (float)shellCount);
            float outer = coreRadius + (maxRadius - coreRadius) * ((float)shell / (float)shellCount);
            inner = CaveMegaGigaLayout.blurRadius(inner, seed, shell);
            outer = CaveMegaGigaLayout.blurRadius(outer, seed, shell + 11);
            int sectorCount = 3 + Math.abs(NoiseUtil.hash2D(seed, shell, 7)) % 4;
            float cursor = CaveMegaGigaLayout.normalizeAngle(NoiseUtil.valCoord2D(seed, shell, 3) * (float)Math.PI);
            float fullCircle = (float)Math.PI * 2;
            for (int s = 0; s < sectorCount; ++s) {
                float end;
                float span = fullCircle / (float)sectorCount;
                float start = cursor;
                cursor = end = CaveMegaGigaLayout.normalizeAngle(cursor + (span *= 0.94f + 0.06f * CaveMegaGigaLayout.noise01(seed, shell * 17 + s)));
                CaveStatVector shellStats = CaveMegaGigaLayout.statsForShell(globalPool, generators, shell);
                CaveBiomeEntry picked = CaveMegaGigaLayout.pickBiome(seed, shell, s, pool, shellStats, climate, usedShellBiomes, strictUnique);
                if (picked == null) continue;
                usedShellBiomes.add(picked.biome());
                sectors.add(new Sector(start, end, inner, outer, shell, picked));
            }
        }
        return List.copyOf(sectors);
    }

    private static CaveStatVector statsForShell(CaveStatVector globalPool, List<GeneratorNode> generators, int shell) {
        CaveStatVector stats = globalPool;
        for (GeneratorNode generator : generators) {
            float factor = Math.max(0.0f, 1.0f - (float)shell * generator.biome().stats().localFalloffPerHop() * 0.2f);
            stats = stats.add(generator.biome().stats().local().scale(factor));
        }
        return stats.clamped();
    }

    private static CaveBiomeEntry pickBiome(int seed, int shell, int sector, List<CaveBiomeEntry> pool, CaveStatVector stats, CaveClimateType climate, Set<ResourceLocation> usedBiomes, boolean strictUnique) {
        CaveBiomeEntry best = null;
        float bestWeight = -1.0f;
        for (CaveBiomeEntry entry : pool) {
            float noise;
            float score;
            if (entry.statGenerator() || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || !CaveBiomeClimateAffinity.matches(entry.biome(), climate, stats.temperature(), false) || !entry.stats().matches(stats) || strictUnique && usedBiomes.contains(entry.biome())) continue;
            float w = entry.weight();
            if (CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.22f;
            }
            if (!strictUnique && usedBiomes.contains(entry.biome())) {
                w *= 0.35f;
            }
            if (!strictUnique && CaveBiomeIds.isFungalCaveBiome(entry.biome()) && usedBiomes.size() >= 2) {
                w *= 0.55f;
            }
            if (!((score = w * (0.75f + (noise = CaveMegaGigaLayout.noise01(seed, shell * 100 + sector * 17 + entry.biome().hashCode())) * 0.5f)) > bestWeight)) continue;
            bestWeight = score;
            best = entry;
        }
        if (best != null) {
            return best;
        }
        if (strictUnique) {
            return CaveMegaGigaLayout.pickBiomeUniqueRelaxed(seed, shell * 100 + sector, pool, climate, usedBiomes);
        }
        return pool.stream().filter(e -> !e.statGenerator()).filter(e -> !CaveBiomeIds.isSparseCaveBiome(e.biome())).filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), climate, stats.temperature(), false)).max(Comparator.comparingDouble(CaveBiomeEntry::weight)).orElseGet(() -> pool.stream().filter(e -> !e.statGenerator()).filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), climate, stats.temperature(), false)).max(Comparator.comparingDouble(CaveBiomeEntry::weight)).orElseGet(() -> pool.stream().filter(e -> !e.statGenerator()).max(Comparator.comparingDouble(CaveBiomeEntry::weight)).orElse(pool.isEmpty() ? null : (CaveBiomeEntry)pool.get(0))));
    }

    private static float blurRadius(float radius, int seed, int salt) {
        return radius + CaveMegaGigaLayout.noise01(seed, salt) * 12.0f - 6.0f;
    }

    private static float noise01(int seed, int salt) {
        return (NoiseUtil.valCoord2D(seed, salt, salt ^ 0x5A) + 1.0f) * 0.5f;
    }

    private static boolean inSectorStrict(float dist, float angle, Sector sector) {
        if (dist < sector.innerRadius() || dist > sector.outerRadius()) {
            return false;
        }
        return CaveMegaGigaLayout.angleInRange(angle, sector.angleStart(), sector.angleEnd());
    }

    private static boolean angleInRange(float angle, float start, float end) {
        angle = CaveMegaGigaLayout.normalizeAngle(angle);
        if ((start = CaveMegaGigaLayout.normalizeAngle(start)) <= (end = CaveMegaGigaLayout.normalizeAngle(end))) {
            return angle >= start && angle <= end;
        }
        return angle >= start || angle <= end;
    }

    private static float normalizeAngle(float angle) {
        float twoPi = (float)Math.PI * 2;
        if ((angle %= twoPi) < 0.0f) {
            angle += twoPi;
        }
        return angle;
    }

    private static float shortestAngleDelta(float a, float b) {
        float delta = CaveMegaGigaLayout.normalizeAngle(a - b);
        if ((double)delta > Math.PI) {
            delta -= (float)Math.PI * 2;
        }
        return Math.abs(delta);
    }

    public record GeneratorNode(float x, float z, CaveBiomeEntry biome) {
    }

    public record Sector(float angleStart, float angleEnd, float innerRadius, float outerRadius, int hopFromCore, CaveBiomeEntry biome) {
    }
}
