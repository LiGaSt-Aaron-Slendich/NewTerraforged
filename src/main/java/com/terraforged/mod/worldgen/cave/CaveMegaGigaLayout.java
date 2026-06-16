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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class CaveMegaGigaLayout {
    private static final int MIN_UNIQUE_REGIONS = 7;
    private static final float MAX_BIOME_SECTOR_FRACTION = 0.16f;
    private static final float MAX_BIOME_CELL_FRACTION = 0.28f;
    private static final float MAX_HEAT_SHELL_CELL_FRACTION = 0.015f;

    static float conditionRelax() {
        com.terraforged.mod.platform.forge.TFCaveBiomeConfig cfg = com.terraforged.mod.platform.forge.TFCaveBiomeConfig.INSTANCE;
        return cfg != null ? cfg.conditionRelax : 0.0f;
    }

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

    public float centerX() {
        return this.centerX;
    }

    public float centerZ() {
        return this.centerZ;
    }

    public CaveBiomeEntry getBiomeAt(int x, int z) {
        if (!this.isCentralOpenHall(x, z)) {
            for (GeneratorNode generator : this.generators) {
                float gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z());
                if (gDist <= this.generatorCoreRadius(generator)) {
                    return generator.biome();
                }
            }
        }
        if (this.regionGrid != null) {
            CaveBiomeEntry gridBiome = this.regionGrid.biomeAt(x, z);
            if (gridBiome != null) {
                return gridBiome;
            }
        }
        return this.resolveRegionalBiome(x, z);
    }

    public CaveStatVector statsAt(int x, int z) {
        CaveStatVector stats = this.approximateStatsAt(x, z);
        float dx = (float)x - this.centerX;
        float dz = (float)z - this.centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        float edgeFade = MathUtil.clamp(dist / Math.max(1.0f, this.blurRadius * 4.2f), 0.0f, 1.0f);
        return stats.add(new CaveStatVector(-edgeFade * 0.8f, -edgeFade * 0.35f, -edgeFade * 0.65f)).clamped();
    }

    private float centerProximity(int x, int z) {
        float maxDist = Math.max(48.0f, this.blurRadius * 3.8f);
        return 1.0f - MathUtil.clamp(CaveMegaGigaLayout.dist(x, z, this.centerX, this.centerZ) / maxDist, 0.0f, 1.0f);
    }

    private float vegetationWeightBias(int x, int z, CaveBiomeEntry entry, float baseWeight) {
        float center = this.centerProximity(x, z);
        float veg = entry.vegetationDensity();
        float w = baseWeight;
        w *= 0.35f + veg * (0.45f + center * 0.85f);
        if (center > 0.55f && veg >= 0.45f) {
            w *= 1.0f + center * 0.9f;
        }
        if (center > 0.45f && CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
            w *= 0.12f + (1.0f - center) * 0.35f;
        }
        if (center < 0.25f && veg >= 0.6f) {
            w *= 0.55f;
        }
        return w;
    }

    private CaveBiomeEntry resolveRegionalBiome(int x, int z) {
        CaveBiomeEntry warm;
        if (this.sectors.isEmpty() && this.shellPool.isEmpty()) {
            return null;
        }
        CaveStatVector localStats = this.approximateStatsAt(x, z);
        boolean thermalOasis = this.isThermalOasis(x, z, localStats);
        for (GeneratorNode generator : this.generators) {
            float gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z());
            if (this.isCentralOpenHall(x, z) || !(gDist <= this.generatorCoreRadius(generator))) continue;
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
            float score = CaveMegaGigaLayout.sectorMatchScore(this.layoutSeed, x, z, dist, angle, sector, this.blurRadius);
            if (!(score < bestScore)) continue;
            bestScore = score;
            best = sector;
        }
        CaveBiomeEntry sectorBiome = best == null ? this.pickBiomeForPosition(x, z, localStats, false) : best.biome();
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
            float heatRadius = this.blurRadius * 1.55f;
            factor = CaveMegaGigaLayout.smoothFalloff(gDist, heatRadius);
            if (this.isCentralOpenHall(x, z)) {
                factor *= 0.2f;
            }
            factor *= factor;
            stats = stats.add(genStats.local().scale(factor));
            CaveStatVector heat = genStats.globalForClimate(this.climateType);
            float tempBoost = Math.max(heat.temperature(), 4.0f) * factor * 1.05f;
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
            float tempBoost = Math.max(heat.temperature(), 4.0f) * 1.05f;
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
            if (this.isCentralOpenHall(cx, cz)) {
                return;
            }
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

    private void balanceRegionGrid(int seed, CaveBiomeRegistry registry) {
        if (this.regionGrid == null) {
            return;
        }
        this.regionGrid.balanceBiomeFootprint(seed, MAX_BIOME_CELL_FRACTION, MAX_HEAT_SHELL_CELL_FRACTION, (x, z, excluded) -> this.pickBiomeForPositionExcluding(x, z, this.statsAt(x, z), false, excluded));
        this.regionGrid.reinforceAggressiveTransitionRing(registry, seed);
    }

    private CaveBiomeEntry pickBiomeForPositionExcluding(int x, int z, CaveStatVector stats, boolean thermalOasis, Set<ResourceLocation> excluded) {
        CaveBiomeEntry best = null;
        float bestWeight = -1.0f;
        for (CaveBiomeEntry entry : this.shellPool) {
            float score;
            if (entry.statGenerator() || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || excluded != null && excluded.contains(entry.biome()) || !CaveBiomeClimateAffinity.matches(entry.biome(), this.climateType, stats.temperature(), thermalOasis) || !entry.stats().matches(stats)) continue;
            float w = this.vegetationWeightBias(x, z, entry, entry.weight());
            if (CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.22f;
            }
            if (CaveBiomeIds.isHeatShellCaveBiome(entry.biome())) {
                w *= 0.18f;
            }
            if (CaveBiomeIds.isScorchingCaveBiome(entry.biome())) {
                w *= 0.12f;
            }
            if (this.isCentralOpenHall(x, z)) {
                if (CaveBiomeIds.isScorchingCaveBiome(entry.biome()) || CaveBiomeIds.isVolcanicCaveBiome(entry.biome())) {
                    w *= 0.1f;
                } else if (CaveBiomeIds.isHeatShellCaveBiome(entry.biome())) {
                    w *= 0.2f;
                }
            }
            if (thermalOasis && CaveBiomeClimateAffinity.isWarmOasisBiome(entry.biome().getPath())) {
                w *= 1.4f + stats.temperature() * 0.12f;
            }
            float noise = CaveMegaGigaLayout.noise01(this.layoutSeed, x * 7919 + z * 104729);
            float detail = CaveMegaGigaLayout.noise01((int)((long)this.layoutSeed ^ 0x517CC1L), x * 31 + z * 17 + entry.biome().hashCode());
            if (!((score = w * (0.72f + noise * 0.12f + detail * 0.06f)) > bestWeight)) continue;
            bestWeight = score;
            best = entry;
        }
        if (best != null) {
            return best;
        }
        return this.shellPool.stream().filter(e -> !e.statGenerator()).filter(e -> excluded == null || !excluded.contains(e.biome())).filter(e -> CaveBiomeClimateAffinity.matches(e.biome(), this.climateType, stats.temperature(), thermalOasis)).max(Comparator.comparingDouble(CaveBiomeEntry::weight)).orElse(null);
    }

    private CaveBiomeEntry pickBiomeForPosition(int x, int z, CaveStatVector stats, boolean thermalOasis) {
        return this.pickBiomeForPositionExcluding(x, z, stats, thermalOasis, null);
    }

    private boolean isCentralOpenHall(int x, int z) {
        return CaveMegaGigaLayout.dist(x, z, this.centerX, this.centerZ) < this.blurRadius * 0.52f;
    }

    private boolean isThermalOasis(int x, int z, CaveStatVector local) {
        if (this.isCentralOpenHall(x, z)) {
            return false;
        }
        if (local.temperature() < 1.5f) {
            return false;
        }
        for (GeneratorNode generator : this.generators) {
            float gDist;
            if (!CaveBiomeClimateAffinity.isWarmOasisGenerator(generator.biome().biome()) || !((gDist = CaveMegaGigaLayout.dist(x, z, generator.x(), generator.z())) <= this.blurRadius * 2.8f)) continue;
            return true;
        }
        return false;
    }

    private float generatorCoreRadius(GeneratorNode generator) {
        ResourceLocation genId = generator.biome().biome();
        if (CaveBiomeClimateAffinity.isHeatGenerator(genId) || CaveBiomeClimateAffinity.isWarmOasisGenerator(genId)) {
            return Math.max(12.0f, this.blurRadius * 0.1f);
        }
        if (CaveBiomeClimateAffinity.isColdGenerator(genId)) {
            return Math.max(22.0f, this.blurRadius * 0.28f);
        }
        return Math.max(28.0f, this.blurRadius * 0.38f);
    }

    private float generatorCoreRadius() {
        return Math.max(28.0f, this.blurRadius * 0.38f);
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
        List<GeneratorNode> generators = CaveMegaGigaLayout.pickGenerators(seed, centerX, centerZ, generatorPool, poolStats, climate, isMega, blur);
        for (GeneratorNode generator : generators) {
            if (CaveBiomeClimateAffinity.isSpringGenerator(generator.biome().biome())) continue;
            poolStats = poolStats.add(generator.biome().stats().globalForClimate(climate));
        }
        int shellMin = isMega ? config.megaRegionCountMin() : config.gigaRegionCountMin();
        int n = shellMax = isMega ? config.megaRegionCountMax() : config.gigaRegionCountMax();
        if (shellMin > shellMax) {
            shellMin = shellMax;
        }
        int regionCount = shellMin + Math.abs(NoiseUtil.hash2D(seed ^ (int)(centerX * 31L) ^ (int)(centerZ * 17L) ^ 0x5EEDCAFE, (int)centerX, (int)centerZ)) % Math.max(1, shellMax - shellMin + 1);
        regionCount = Math.max(MIN_UNIQUE_REGIONS, regionCount);
        List<Sector> sectors = CaveMegaGigaLayout.buildUniqueRegions(seed, radius, shellPool, poolStats, climate, generators, regionCount, blur, registry, config);
        CaveMegaGigaLayout layout = new CaveMegaGigaLayout(centerX, centerZ, blur, poolStats.clamped(), climate, generators, sectors, shellPool, seed, null);
        layout.regionGrid = CaveLayoutRegionGrid.build(centerX, centerZ, radius, isMega, seed, poolStats.clamped(), layout::resolveRegionalBiome, generators, layout::generatorStatSource);
        layout.balanceRegionGrid(seed, registry);
        layout.refreshWarmOasisBiomes();
        return layout;
    }

    private static CaveMegaGigaLayout empty(float centerX, float centerZ, CaveStatInitializer.CaveStatSnapshot snapshot) {
        return new CaveMegaGigaLayout(centerX, centerZ, 16.0f, snapshot.initial(), snapshot.climateType(), List.of(), List.of(), List.of(), 0, null);
    }

    private static List<GeneratorNode> pickGenerators(int seed, float centerX, float centerZ, List<CaveBiomeEntry> pool, CaveStatVector stats, CaveClimateType climate, boolean isMega, float blurRadius) {
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
        CaveMegaGigaLayout.ensureThermalGenerator(result, pool, centerX, centerZ, seed, blurRadius);
        return List.copyOf(result);
    }

    private static void ensureThermalGenerator(List<GeneratorNode> nodes, List<CaveBiomeEntry> pool, float centerX, float centerZ, int seed, float blurRadius) {
        boolean hasWarm = nodes.stream().anyMatch(node -> CaveBiomeClimateAffinity.isWarmOasisGenerator(node.biome().biome()));
        if (hasWarm) {
            return;
        }
        CaveBiomeEntry warm = pool.stream().filter(e -> CaveBiomeClimateAffinity.isSpringGenerator(e.biome()) || CaveBiomeClimateAffinity.isHeatGenerator(e.biome())).findFirst().orElse(null);
        if (warm == null) {
            return;
        }
        float angle = NoiseUtil.valCoord2D(seed ^ 0x7A11CE, 0, 1) * (float)Math.PI * 2.0f;
        float dist = Math.max(36.0f, blurRadius * 0.42f);
        float gx = centerX + (float)Math.cos(angle) * dist;
        float gz = centerZ + (float)Math.sin(angle) * dist;
        if (nodes.isEmpty()) {
            nodes.add(new GeneratorNode(gx, gz, warm));
            return;
        }
        nodes.set(0, new GeneratorNode(gx, gz, warm));
    }

    private static List<Sector> buildUniqueRegions(int seed, int radius, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, int regionCount, float blur, CaveBiomeRegistry registry, CaveSystemConfig config) {
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
        int target = Math.max(MIN_UNIQUE_REGIONS, regionCount);
        for (int r = 0; r < target; ++r) {
            float inner = coreRadius + (maxRadius - coreRadius) * ((float)r / (float)target);
            float outer = coreRadius + (maxRadius - coreRadius) * (((float)r + 1.0f) / (float)target);
            inner = CaveMegaGigaLayout.blurRadius(inner, seed, r + 1);
            outer = CaveMegaGigaLayout.blurRadius(outer, seed, r + 21);
            float span = fullCircle / (float)target;
            float startJitter = (CaveMegaGigaLayout.noise01(seed, r + 500) - 0.5f) * span * 0.12f;
            float endJitter = (CaveMegaGigaLayout.noise01(seed, r + 501) - 0.5f) * span * 0.12f;
            float start = CaveMegaGigaLayout.normalizeAngle(angleOffset + span * (float)r + startJitter);
            float end = CaveMegaGigaLayout.normalizeAngle(angleOffset + span * (float)(r + 1) + endJitter);
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
        CaveMegaGigaLayout.enforceRegionBiomeCap(sectors, seed, pool, globalPool, climate, generators, usedBiomes);
        CaveMegaGigaLayout.insertTransitionSectors(sectors, seed, registry, config, blur);
        return List.copyOf(sectors);
    }

    private static void insertTransitionSectors(List<Sector> sectors, int seed, CaveBiomeRegistry registry, CaveSystemConfig config, float blur) {
        if (sectors.size() < 2 || registry == null) {
            return;
        }
        int transitionsPerRegion = Math.max(1, config.megaTransitionPerRegion());
        float angularWidth = (float)config.transitionMaxWidthBlocks() / Math.max(48.0f, blur) * 0.32f;
        ArrayList<Sector> expanded = new ArrayList<Sector>(sectors.size() + sectors.size() / 2);
        for (int i = 0; i < sectors.size(); ++i) {
            Sector sector = sectors.get(i);
            expanded.add(sector);
            if (sector.hopFromCore() == 0) {
                continue;
            }
            Sector next = sectors.get((i + 1) % sectors.size());
            if (next.hopFromCore() == 0 || !registry.needsTransitionBuffer(sector.biome(), next.biome())) {
                continue;
            }
            CaveBiomeEntry transition = registry.findTransitionBetween(sector.biome().caveTemperature(), next.biome().caveTemperature(), sector.biome().biome(), next.biome().biome());
            if (transition == null || CaveBiomeIds.isBlockedCaveBiome(transition.biome())) {
                continue;
            }
            float wobble = angularWidth * (0.75f + CaveMegaGigaLayout.noise01(seed, i + 900) * 0.5f);
            float boundary = sector.angleEnd();
            float inner = Math.max(sector.innerRadius() * 0.92f, next.innerRadius() * 0.92f);
            float outer = Math.max(sector.outerRadius(), next.outerRadius()) * 1.02f;
            for (int t = 0; t < transitionsPerRegion; ++t) {
                float slice = wobble / (float)transitionsPerRegion;
                float start = CaveMegaGigaLayout.normalizeAngle(boundary - wobble * 0.5f + slice * (float)t);
                float end = CaveMegaGigaLayout.normalizeAngle(start + slice * 0.92f);
                expanded.add(new Sector(start, end, inner, outer, Math.max(sector.hopFromCore(), next.hopFromCore()), transition));
            }
        }
        sectors.clear();
        sectors.addAll(expanded);
    }

    private static void enforceRegionBiomeCap(List<Sector> sectors, int seed, List<CaveBiomeEntry> pool, CaveStatVector globalPool, CaveClimateType climate, List<GeneratorNode> generators, Set<ResourceLocation> usedBiomes) {
        if (sectors.isEmpty()) {
            return;
        }
        int maxPerBiome = Math.max(1, (int)Math.floor((float)sectors.size() * MAX_BIOME_SECTOR_FRACTION));
        HashMap<ResourceLocation, Integer> counts = new HashMap<ResourceLocation, Integer>();
        for (int i = 0; i < sectors.size(); ++i) {
            Sector sector = sectors.get(i);
            ResourceLocation id = sector.biome().biome();
            int count = counts.merge(id, 1, Integer::sum);
            if (count <= maxPerBiome) continue;
            counts.merge(id, -1, Integer::sum);
            CaveStatVector shellStats = CaveMegaGigaLayout.statsForShell(globalPool, generators, sector.hopFromCore());
            CaveBiomeEntry replacement = CaveMegaGigaLayout.pickBiome(seed, i + 400, 0, pool, shellStats, climate, usedBiomes, true);
            if (replacement == null) {
                replacement = CaveMegaGigaLayout.pickBiomeUniqueRelaxed(seed, i + 400, pool, climate, usedBiomes);
            }
            if (replacement == null || replacement.biome().equals(id)) continue;
            usedBiomes.add(replacement.biome());
            sectors.set(i, new Sector(sector.angleStart(), sector.angleEnd(), sector.innerRadius(), sector.outerRadius(), sector.hopFromCore(), replacement));
            counts.merge(replacement.biome(), 1, Integer::sum);
        }
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
        if (distinct.size() >= MIN_UNIQUE_REGIONS) {
            return;
        }
        float maxRadius = (float)radius * 0.92f;
        float coreRadius = Math.max(24.0f, maxRadius * 0.12f);
        float fullCircle = (float)Math.PI * 2;
        int attempt = 0;
        while (distinct.size() < MIN_UNIQUE_REGIONS && attempt < pool.size() * 2) {
            ++attempt;
            int slot = sectors.size();
            float inner = coreRadius + (maxRadius - coreRadius) * 0.55f;
            float outer = maxRadius;
            inner = CaveMegaGigaLayout.blurRadius(inner, seed, slot + 40);
            outer = CaveMegaGigaLayout.blurRadius(outer, seed, slot + 50);
            float span = fullCircle / (float)MIN_UNIQUE_REGIONS;
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
        float centerBias = 1.0f - MathUtil.clamp((float)shell / 7.0f, 0.0f, 1.0f);
        for (CaveBiomeEntry entry : pool) {
            float noise;
            float score;
            if (entry.statGenerator() || CaveBiomeIds.isEmptyStoneCave(entry.biome()) || !CaveBiomeClimateAffinity.matches(entry.biome(), climate, stats.temperature(), false) || !entry.stats().matches(stats, CaveMegaGigaLayout.conditionRelax()) || strictUnique && usedBiomes.contains(entry.biome())) continue;
            float w = entry.weight();
            w *= 0.35f + entry.vegetationDensity() * (0.45f + centerBias * 0.85f);
            if (centerBias > 0.5f && entry.vegetationDensity() >= 0.45f) {
                w *= 1.0f + centerBias * 0.75f;
            }
            if (centerBias > 0.45f && CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.15f;
            }
            if (CaveBiomeIds.isSparseCaveBiome(entry.biome())) {
                w *= 0.22f;
            }
            if (CaveBiomeIds.isHeatShellCaveBiome(entry.biome())) {
                w *= 0.35f;
            }
            if (!strictUnique && usedBiomes.contains(entry.biome())) {
                w *= 0.35f;
            }
            if (!strictUnique && CaveBiomeIds.isFungalCaveBiome(entry.biome()) && usedBiomes.size() >= 2) {
                w *= 0.55f;
            }
            if (!((score = w * (0.78f + (noise = CaveMegaGigaLayout.noise01(seed, shell * 100 + sector * 17 + entry.biome().hashCode())) * 0.18f)) > bestWeight)) continue;
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

    private static float sectorMatchScore(int layoutSeed, int x, int z, float dist, float angle, Sector sector, float blurRadius) {
        float radialWobble = CaveMegaGigaLayout.boundaryNoise(layoutSeed, x, z) * blurRadius * 0.11f;
        dist += radialWobble;
        float angleWobble = CaveMegaGigaLayout.boundaryNoise(layoutSeed ^ 0xBAD5EED, x * 3 + 7, z * 5 + 11) * 0.24f;
        angle = CaveMegaGigaLayout.normalizeAngle(angle + angleWobble);
        float radial = 0.0f;
        if (dist < sector.innerRadius()) {
            radial = sector.innerRadius() - dist;
        } else if (dist > sector.outerRadius()) {
            radial = dist - sector.outerRadius();
        }
        float centerAngle = (sector.angleStart() + sector.angleEnd()) * 0.5f;
        float angular = CaveMegaGigaLayout.shortestAngleDelta(angle, centerAngle);
        float angularScale = sector.outerRadius() + 24.0f + CaveMegaGigaLayout.boundaryNoise(layoutSeed ^ 0x51A7E1, x + z, x - z) * 18.0f;
        return radial + angular * angularScale;
    }

    private static float boundaryNoise(int seed, int x, int z) {
        return NoiseUtil.valCoord2D(seed, x, z);
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
