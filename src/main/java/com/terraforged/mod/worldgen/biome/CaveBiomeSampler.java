package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveRegionMap;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.cave.CaveTemperatureCalculator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class CaveBiomeSampler {
    public static final int OFFSET = 124897;
    private final int scale;
    private final float frequency;
    private final CaveBiomeRegistry caveBiomeRegistry;
    private final Holder<Biome> fallbackBiome;
    private final Map<Long, CaveRegionMap> regionMapCache = new HashMap<Long, CaveRegionMap>();
    private final CaveSystemConfig systemConfig;
    private final Registry<Biome> biomeRegistry;

    public CaveBiomeSampler(int scale, BiomeMapManager biomeMapManager, CaveBiomeRegistry caveBiomeRegistry, CaveSystemConfig systemConfig) {
        this.scale = scale;
        this.frequency = 1.0f / (float)scale;
        this.caveBiomeRegistry = caveBiomeRegistry;
        this.systemConfig = systemConfig;
        this.biomeRegistry = biomeMapManager.getBiomes();
        this.fallbackBiome = this.resolveFallback(biomeMapManager);
    }

    public CaveBiomeSampler(int scale, BiomeMapManager biomeMapManager) {
        this(scale, biomeMapManager, null, CaveSystemConfig.DEFAULT);
    }

    public CaveBiomeSampler(long seed, CaveBiomeSampler other) {
        this.scale = other.scale;
        this.frequency = other.frequency;
        this.caveBiomeRegistry = other.caveBiomeRegistry;
        this.systemConfig = other.systemConfig;
        this.biomeRegistry = other.biomeRegistry;
        this.fallbackBiome = other.fallbackBiome;
    }

    public CaveBiomeRegistry getRegistry() {
        return this.caveBiomeRegistry;
    }

    public ResourceLocation getPrimaryRegionBiomeId(int seed, int x, int z, CaveType type) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback() || !type.isMegaOrGiga()) {
            return null;
        }
        int cx = CaveBiomeSampler.snapToCaveGrid(x, type);
        int cz = CaveBiomeSampler.snapToCaveGrid(z, type);
        int radius = CaveBiomeSampler.estimateCaveRadius(type);
        long key = CaveBiomeSampler.packRegionKey(cx, cz);
        CaveRegionMap regionMap = this.regionMapCache.computeIfAbsent(key, k -> this.createRegionMap(seed, cx, cz, radius, type == CaveType.MEGA, null, 32, 64));
        CaveBiomeEntry entry = regionMap.getBiomeAt(x, z);
        return entry == null ? null : entry.biome();
    }

    public CaveRegionMap getRegionMap(int seed, int x, int z, CaveType type) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback() || !type.isMegaOrGiga()) {
            return null;
        }
        int cx = CaveBiomeSampler.snapToCaveGrid(x, type);
        int cz = CaveBiomeSampler.snapToCaveGrid(z, type);
        int radius = CaveBiomeSampler.estimateCaveRadius(type);
        long key = CaveBiomeSampler.packRegionKey(cx, cz);
        return this.regionMapCache.computeIfAbsent(key, k -> this.createRegionMap(seed, cx, cz, radius, type == CaveType.MEGA, null, 32, 64));
    }

    private static int estimateCaveRadius(CaveType type) {
        return switch (type) {
            case GIGA -> 400;
            case MEGA -> 250;
            default -> 256;
        };
    }

    private static int snapToCaveGrid(int coord, CaveType type) {
        int radius = CaveBiomeSampler.estimateCaveRadius(type);
        int cell = radius * 2;
        return Math.floorDiv(coord, cell) * cell + radius;
    }

    public static int snapRegionCoord(int coord, int caveCenter, int cellSize) {
        int half = cellSize / 2;
        return caveCenter + Math.floorDiv(coord - caveCenter, cellSize) * cellSize + half;
    }

    public CaveBiomeSampler(long seed, int scale, BiomeMapManager biomeMapManager) {
        this(scale, biomeMapManager);
    }

    public CaveBiomeSampler(long seed, int scale, BiomeMapManager biomeMapManager, CaveBiomeRegistry caveBiomeRegistry, CaveSystemConfig systemConfig) {
        this(scale, biomeMapManager, caveBiomeRegistry, systemConfig);
    }

    public Holder<Biome> getUnderGroundBiome(int seed, int x, int z, CaveType type, Holder<Biome> surfaceBiome, int blockY, int surfaceY, int caveCenterX, int caveCenterZ, int caveRadius) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback()) {
            return this.fallbackBiome;
        }
        if (type.isMegaOrGiga()) {
            return this.getMegaRegionBiome(seed, x, z, type, caveCenterX, caveCenterZ, caveRadius, surfaceBiome, blockY, surfaceY);
        }
        if (type == CaveType.GLOBAL ? blockY >= surfaceY - 8 : blockY >= surfaceY - 32) {
            return this.fallbackBiome;
        }
        float caveTemp = CaveTemperatureCalculator.calculate(surfaceBiome, blockY, surfaceY);
        return this.getNormalCaveBiome(seed, x, z, caveTemp);
    }

    public Optional<Holder<Biome>> getCoastalEntranceBiome(int seed, int x, int z, Generator generator, Holder<Biome> surfaceBiome, int blockY, int surfaceY) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback()) {
            return Optional.empty();
        }
        if (this.caveBiomeRegistry.getCoastal().isEmpty()) {
            return Optional.empty();
        }
        if (!CaveOceanFilter.isNearSea(generator, x, z)) {
            return Optional.empty();
        }
        int sea = generator.getSeaLevel();
        if (surfaceY <= sea + 2) {
            return Optional.empty();
        }
        float caveTemp = CaveTemperatureCalculator.calculate(surfaceBiome, blockY, surfaceY);
        CaveBiomeEntry entry = this.caveBiomeRegistry.pickCoastalEntrance(caveTemp, seed, x, z);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(this.resolveHolder(entry));
    }

    private Holder<Biome> getMegaRegionBiome(int seed, int x, int z, CaveType type, int cx, int cz, int radius, Holder<Biome> surfaceBiome, int blockY, int surfaceY) {
        long key = CaveBiomeSampler.packRegionKey(cx, cz);
        CaveRegionMap regionMap = this.regionMapCache.computeIfAbsent(key, k -> this.createRegionMap(seed, cx, cz, radius, !type.isGiga(), surfaceBiome, blockY, surfaceY));
        CaveBiomeEntry entry = regionMap.getBiomeAt(x, z);
        if (entry == null) {
            return this.fallbackBiome;
        }
        return this.resolveHolder(entry);
    }

    public CaveMegaGigaLayout getMegaGigaLayout(int seed, int cx, int cz, int radius, CaveType type, Holder<Biome> surfaceBiome, int blockY, int surfaceY) {
        long key = CaveBiomeSampler.packRegionKey(cx, cz);
        CaveRegionMap regionMap = this.regionMapCache.computeIfAbsent(key, k -> this.createRegionMap(seed, cx, cz, radius, !type.isGiga(), surfaceBiome, blockY, surfaceY));
        return regionMap.layout();
    }

    private CaveRegionMap createRegionMap(int seed, int cx, int cz, int radius, boolean isMega, Holder<Biome> surfaceBiome, int sampleY, int surfaceY) {
        Holder<Biome> surface = surfaceBiome != null ? surfaceBiome : this.fallbackBiome;
        return new CaveRegionMap(seed, cx, cz, radius, this.caveBiomeRegistry, this.systemConfig, isMega, surface, sampleY, surfaceY);
    }

    private Holder<Biome> getNormalCaveBiome(int seed, int x, int z, float caveTemp) {
        float d;
        CaveBiomeEntry best = null;
        float bestDist = Float.MAX_VALUE;
        for (CaveBiomeEntry entry : this.caveBiomeRegistry.getPrimary()) {
            if (CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || CaveBiomeIds.isSparseCaveBiome(entry.biome()) || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) continue;
            bestDist = d;
            best = entry;
        }
        if (best == null) {
            for (CaveBiomeEntry entry : this.caveBiomeRegistry.getPrimary()) {
                if (CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) continue;
                bestDist = d;
                best = entry;
            }
        }
        for (CaveBiomeEntry entry : this.caveBiomeRegistry.getTransition()) {
            if (CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) continue;
            bestDist = d;
            best = entry;
        }
        return best != null ? this.resolveHolder(best) : this.fallbackBiome;
    }

    private Holder<Biome> resolveHolder(CaveBiomeEntry entry) {
        if (entry == null) {
            return this.fallbackBiome;
        }
        return this.caveBiomeRegistry.getHolder(entry).orElse(this.fallbackBiome);
    }

    private static float sample(int seed, int x, int z, float frequency) {
        float nx = (float)x * frequency;
        float nz = (float)z * frequency;
        float n = (1.0f + Noise.singleSimplex(nx, nz, seed)) * 0.5f;
        return NoiseUtil.clamp(n, 0.0f, 1.0f);
    }

    private static long packRegionKey(int cx, int cz) {
        return (long)cx << 32 | (long)cz & 0xFFFFFFFFL;
    }

    private Holder<Biome> resolveFallback(BiomeMapManager manager) {
        Registry<Biome> registry = manager.getBiomes();
        for (ResourceLocation id : new ResourceLocation[]{TerraForged.location("cave"), new ResourceLocation("terraforged", "cave"), new ResourceLocation("minecraft", "dripstone_caves")}) {
            Optional holder = registry.getHolder(ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)id));
            if (!holder.isPresent()) continue;
            return (Holder)holder.get();
        }
        return manager.get((ResourceKey<Biome>)Biomes.PLAINS);
    }
}
