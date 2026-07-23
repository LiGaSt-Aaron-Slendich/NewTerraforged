package com.terraforged.mod.worldgen.biome;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.map.WeightMap;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveRegionMap;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.cave.CaveTemperatureCalculator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.noise.util.Noise;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.biome.Biomes;

/**
 * Cave biome sampling with region/MEGA layout API + WeightMap fallback for UNIQUE/GLOBAL.
 */
public class CaveBiomeSampler {
    public static final int OFFSET = 124897;
    private static final int REGION_CACHE_LIMIT = 256;
    private final int seed;
    private final int scale;
    private final float frequency;
    private final CaveBiomeRegistry caveBiomeRegistry;
    private final Holder<Biome> fallbackBiome;
    private final Map<Long, CaveRegionMap> regionMapCache = new HashMap<>();
    private final CaveSystemConfig systemConfig;
    private final Registry<Biome> biomeRegistry;
    private final Map<CaveType, WeightMap<Holder<Biome>>> typeMap = new java.util.EnumMap<>(CaveType.class);
    private INoiseGenerator noiseGenerator;

    public CaveBiomeSampler(int scale, BiomeMapManager biomeMapManager, CaveBiomeRegistry caveBiomeRegistry, CaveSystemConfig systemConfig) {
        this(0L, scale, biomeMapManager, caveBiomeRegistry, systemConfig);
    }

    public CaveBiomeSampler(long seed, int scale, BiomeMapManager biomeMapManager, CaveBiomeRegistry caveBiomeRegistry, CaveSystemConfig systemConfig) {
        this.seed = (int) seed + OFFSET;
        this.scale = scale;
        this.frequency = 1.0f / (float) scale;
        this.caveBiomeRegistry = caveBiomeRegistry;
        this.systemConfig = systemConfig != null ? systemConfig : CaveSystemConfig.DEFAULT;
        this.biomeRegistry = biomeMapManager.getBiomes();
        this.fallbackBiome = this.resolveFallback(biomeMapManager);
        this.buildTypeMaps(biomeMapManager);
    }

    public CaveBiomeSampler(int scale, BiomeMapManager biomeMapManager) {
        this(scale, biomeMapManager, null, CaveSystemConfig.DEFAULT);
    }

    public CaveBiomeSampler(long seed, CaveBiomeSampler other) {
        this.seed = (int) seed + OFFSET;
        this.scale = other.scale;
        this.frequency = other.frequency;
        this.caveBiomeRegistry = other.caveBiomeRegistry;
        this.systemConfig = other.systemConfig;
        this.biomeRegistry = other.biomeRegistry;
        this.fallbackBiome = other.fallbackBiome;
        this.typeMap.putAll(other.typeMap);
        this.noiseGenerator = other.noiseGenerator;
    }

    public CaveBiomeSampler(long seed, int scale, BiomeMapManager biomeMapManager) {
        this(seed, scale, biomeMapManager, null, CaveSystemConfig.DEFAULT);
    }

    public void setNoiseGenerator(INoiseGenerator noise) {
        this.noiseGenerator = noise;
    }

    public INoiseGenerator getNoiseGenerator() {
        return this.noiseGenerator;
    }

    public CaveBiomeRegistry getRegistry() {
        return this.caveBiomeRegistry;
    }

    private void buildTypeMaps(BiomeMapManager biomeMapManager) {
        Holder<Biome>[] global = new Holder[]{this.fallbackBiome};
        Holder<Biome>[] unique;
        if (this.caveBiomeRegistry != null && !this.caveBiomeRegistry.isVanillaFallback()) {
            unique = this.caveBiomeRegistry.getPrimary().stream()
                .map(this::resolveHolder)
                .filter(h -> h != null)
                .distinct()
                .toArray(Holder[]::new);
            if (unique.length == 0) {
                unique = global;
            }
            // GLOBAL / synapse: primary + transition + fallback so paint is never a single stub biome.
            Holder<Biome>[] synapse = java.util.stream.Stream.concat(
                    this.caveBiomeRegistry.getPrimary().stream(),
                    this.caveBiomeRegistry.getTransition().stream())
                .map(this::resolveHolder)
                .filter(h -> h != null)
                .distinct()
                .toArray(Holder[]::new);
            if (synapse.length == 0) {
                synapse = global;
            }
            this.typeMap.put(CaveType.GLOBAL, create(synapse));
            this.typeMap.put(CaveType.UNIQUE, create(unique));
        } else {
            Holder<Biome>[] underground = biomeMapManager.getBiomes()
                .holders()
                .filter(b -> Biome.getBiomeCategory(b) == BiomeCategory.UNDERGROUND)
                .toArray(Holder[]::new);
            if (underground.length == 0) {
                underground = global;
            }
            this.typeMap.put(CaveType.GLOBAL, create(global));
            this.typeMap.put(CaveType.UNIQUE, create(underground));
        }
        this.typeMap.put(CaveType.MEGA, this.typeMap.get(CaveType.UNIQUE));
        this.typeMap.put(CaveType.GIGA, this.typeMap.get(CaveType.UNIQUE));
    }

    public ResourceLocation getPrimaryRegionBiomeId(int seed, int x, int z, CaveType type) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback() || !type.isMegaOrGiga()) {
            return null;
        }
        int cx = snapToCaveGrid(x, type);
        int cz = snapToCaveGrid(z, type);
        int radius = estimateCaveRadius(type);
        long key = packRegionKey(seed, cx, cz);
        CaveRegionMap regionMap = this.getOrCreateRegionMap(key, seed, cx, cz, radius, type == CaveType.MEGA, null, 32, 64);
        CaveBiomeEntry entry = regionMap.getBiomeAt(x, z);
        return entry == null ? null : entry.biome();
    }

    public CaveRegionMap getRegionMap(int seed, int x, int z, CaveType type) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback() || !type.isMegaOrGiga()) {
            return null;
        }
        int cx = snapToCaveGrid(x, type);
        int cz = snapToCaveGrid(z, type);
        int radius = estimateCaveRadius(type);
        long key = packRegionKey(seed, cx, cz);
        return this.getOrCreateRegionMap(key, seed, cx, cz, radius, type == CaveType.MEGA, null, 32, 64);
    }

    private static int estimateCaveRadius(CaveType type) {
        return switch (type) {
            case GIGA -> 400;
            case MEGA -> 250;
            default -> 256;
        };
    }

    private static int snapToCaveGrid(int coord, CaveType type) {
        int radius = estimateCaveRadius(type);
        int cell = radius * 2;
        return Math.floorDiv(coord, cell) * cell + radius;
    }

    public static int snapRegionCoord(int coord, int caveCenter, int cellSize) {
        int half = cellSize / 2;
        return caveCenter + Math.floorDiv(coord - caveCenter, cellSize) * cellSize + half;
    }

    /** TF118-compatible entry: noise WeightMap for UNIQUE/GLOBAL; mega uses layout when registry present. */
    public Holder<Biome> getUnderGroundBiome(int seed, int x, int z, CaveType type) {
        if (type != null && type.isMegaOrGiga() && this.caveBiomeRegistry != null && !this.caveBiomeRegistry.isVanillaFallback()) {
            return this.getUnderGroundBiome(seed, x, z, type, this.fallbackBiome, 32, 64, snapToCaveGrid(x, type), snapToCaveGrid(z, type), estimateCaveRadius(type));
        }
        return this.sampleWeightMap(seed, x, z, type);
    }

    public Holder<Biome> getUnderGroundBiome(
        int seed,
        int x,
        int z,
        CaveType type,
        Holder<Biome> surfaceBiome,
        int blockY,
        int surfaceY,
        int caveCenterX,
        int caveCenterZ,
        int caveRadius
    ) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback()) {
            Holder<Biome> fromMap = this.sampleWeightMap(seed, x, z, type);
            return fromMap != null ? fromMap : this.fallbackBiome;
        }
        if (type != null && type.isMegaOrGiga()) {
            return this.getMegaRegionBiome(seed, x, z, type, caveCenterX, caveCenterZ, caveRadius, surfaceBiome, blockY, surfaceY);
        }
        if (type == CaveType.GLOBAL ? blockY >= surfaceY - 8 : blockY >= surfaceY - 32) {
            // Near surface: still prefer a configured cave biome over leaking surface biomes into carved volume.
            Holder<Biome> near = this.sampleWeightMap(seed, x, z, type);
            return near != null ? near : this.fallbackBiome;
        }
        float caveTemp = CaveTemperatureCalculator.calculate(surfaceBiome, blockY, surfaceY);
        Holder<Biome> matched = this.getNormalCaveBiome(seed, x, z, caveTemp);
        if (matched != null) {
            return matched;
        }
        return this.sampleWeightMap(seed, x, z, type);
    }

    public CaveMegaGigaLayout getMegaGigaLayout(
        int seed, int cx, int cz, int radius, CaveType type, Holder<Biome> surfaceBiome, int blockY, int surfaceY
    ) {
        if (this.caveBiomeRegistry == null || this.caveBiomeRegistry.isVanillaFallback()) {
            return null;
        }
        long key = packRegionKey(seed, cx, cz);
        CaveRegionMap regionMap = this.getOrCreateRegionMap(key, seed, cx, cz, radius, type == null || !type.isGiga(), surfaceBiome, blockY, surfaceY);
        return regionMap.layout();
    }

    private Holder<Biome> getMegaRegionBiome(
        int seed, int x, int z, CaveType type, int cx, int cz, int radius, Holder<Biome> surfaceBiome, int blockY, int surfaceY
    ) {
        long key = packRegionKey(seed, cx, cz);
        CaveRegionMap regionMap = this.getOrCreateRegionMap(key, seed, cx, cz, radius, !type.isGiga(), surfaceBiome, blockY, surfaceY);
        CaveBiomeEntry entry = regionMap.getBiomeAt(x, z);
        if (entry == null) {
            return this.fallbackBiome;
        }
        return this.resolveHolder(entry);
    }

    private CaveRegionMap getOrCreateRegionMap(
        long key, int seed, int cx, int cz, int radius, boolean isMega, Holder<Biome> surfaceBiome, int blockY, int surfaceY
    ) {
        CaveRegionMap cached = this.regionMapCache.get(key);
        if (cached != null) {
            return cached;
        }
        if (this.regionMapCache.size() >= REGION_CACHE_LIMIT) {
            Iterator<Long> it = this.regionMapCache.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        CaveRegionMap created = this.createRegionMap(seed, cx, cz, radius, isMega, surfaceBiome, blockY, surfaceY);
        this.regionMapCache.put(key, created);
        return created;
    }

    private CaveRegionMap createRegionMap(
        int seed, int cx, int cz, int radius, boolean isMega, Holder<Biome> surfaceBiome, int sampleY, int surfaceY
    ) {
        Holder<Biome> surface = surfaceBiome != null ? surfaceBiome : this.fallbackBiome;
        return new CaveRegionMap(seed, cx, cz, radius, this.caveBiomeRegistry, this.systemConfig, isMega, surface, sampleY, surfaceY, this.noiseGenerator);
    }

    private Holder<Biome> getNormalCaveBiome(int seed, int x, int z, float caveTemp) {
        float d;
        CaveBiomeEntry best = null;
        float bestDist = Float.MAX_VALUE;
        for (CaveBiomeEntry entry : this.caveBiomeRegistry.getPrimary()) {
            if (CaveBiomeIds.isBlockedCaveBiome(entry.biome())
                || CaveBiomeIds.isSynapseExcluded(entry.biome())
                || CaveBiomeIds.isSparseCaveBiome(entry.biome())
                || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) {
                continue;
            }
            bestDist = d;
            best = entry;
        }
        if (best == null) {
            for (CaveBiomeEntry entry : this.caveBiomeRegistry.getPrimary()) {
                if (CaveBiomeIds.isBlockedCaveBiome(entry.biome())
                    || CaveBiomeIds.isSynapseExcluded(entry.biome())
                    || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) {
                    continue;
                }
                bestDist = d;
                best = entry;
            }
        }
        for (CaveBiomeEntry entry : this.caveBiomeRegistry.getTransition()) {
            if (CaveBiomeIds.isBlockedCaveBiome(entry.biome())
                || CaveBiomeIds.isSynapseExcluded(entry.biome())
                || !((d = Math.abs(entry.caveTemperature() - caveTemp)) < bestDist)) {
                continue;
            }
            bestDist = d;
            best = entry;
        }
        return best != null ? this.resolveHolder(best) : null;
    }

    private Holder<Biome> sampleWeightMap(int seed, int x, int z, CaveType type) {
        WeightMap<Holder<Biome>> map = this.typeMap.get(type == null ? CaveType.GLOBAL : type);
        if (map == null || map.isEmpty()) {
            return this.fallbackBiome;
        }
        float noise = sample(seed + this.seed, x, z, this.frequency);
        Holder<Biome> value = map.getValue(noise);
        return value != null ? value : this.fallbackBiome;
    }

    private Holder<Biome> resolveHolder(CaveBiomeEntry entry) {
        if (entry == null || CaveBiomeIds.isBlockedCaveBiome(entry.biome()) || CaveBiomeIds.isNetherThemedBiome(entry.biome())) {
            return this.fallbackBiome;
        }
        return this.caveBiomeRegistry.getHolder(entry).orElse(this.fallbackBiome);
    }

    private static float sample(int seed, int x, int z, float frequency) {
        float nx = (float) x * frequency;
        float nz = (float) z * frequency;
        float n = (1.0f + Noise.singleSimplex(nx, nz, seed)) * 0.5f;
        return NoiseUtil.clamp(n, 0.0f, 1.0f);
    }

    private static long packRegionKey(int seed, int cx, int cz) {
        return (long) Integer.rotateLeft(seed, 13) ^ (long) cx << 32 | (long) cz & 0xFFFFFFFFL;
    }

    private Holder<Biome> resolveFallback(BiomeMapManager manager) {
        Registry<Biome> registry = manager.getBiomes();
        for (ResourceLocation id : new ResourceLocation[]{
            TerraForged.location("cave"),
            new ResourceLocation("newterraforged", "cave"),
            new ResourceLocation("terraforged", "cave"),
            new ResourceLocation("minecraft", "dripstone_caves"),
            new ResourceLocation("minecraft", "lush_caves")
        }) {
            Optional<Holder<Biome>> holder = registry.getHolder(ResourceKey.create(Registry.BIOME_REGISTRY, id));
            if (holder.isPresent()) {
                return holder.get();
            }
        }
        return manager.get(Biomes.PLAINS);
    }

    protected static WeightMap<Holder<Biome>> create(Holder<Biome>[] biomes) {
        if (biomes == null || biomes.length == 0) {
            return new WeightMap<>(new Holder[0], new float[0]);
        }
        float[] weights = new float[biomes.length];
        Arrays.fill(weights, 1.0F);
        return new WeightMap<>(biomes, weights);
    }
}
