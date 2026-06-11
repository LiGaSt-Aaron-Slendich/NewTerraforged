package com.terraforged.mod.worldgen.biome;

import com.mojang.serialization.Codec;
import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.mod.util.storage.LongCache;
import com.terraforged.mod.util.storage.LossyCache;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.BiomeSampler;
import com.terraforged.mod.worldgen.biome.CaveBiomeSampler;
import com.terraforged.mod.worldgen.biome.SourceCodec;
import com.terraforged.mod.worldgen.biome.util.BiomeMapManager;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

public class Source
extends BiomeSource {
    public static final Codec<Source> CODEC = new SourceCodec();
    public static final Climate.Sampler NOOP_CLIMATE_SAMPLER = Climate.empty();
    private static List<Holder<Biome>> possibleBiomesCache;
    private static int possibleBiomesCacheSize = -1;
    protected final long seed;
    protected final RegistryAccess registries;
    protected final BiomeSampler biomeSampler;
    protected final BiomeMapManager biomeMapManager;
    protected final CaveBiomeSampler caveBiomeSampler;
    protected final LongCache<Holder<Biome>> cache = LossyCache.concurrent(2048, Holder[]::new);

    public Source(long seed, INoiseGenerator noise, Source other) {
        super(new ArrayList(other.possibleBiomes()));
        this.seed = seed;
        this.registries = other.registries;
        this.biomeMapManager = other.biomeMapManager;
        this.biomeSampler = new BiomeSampler(noise, other.biomeMapManager);
        this.caveBiomeSampler = new CaveBiomeSampler(seed, other.caveBiomeSampler);
    }

    public Source(long seed, INoiseGenerator noise, RegistryAccess access) {
        super(Source.possibleBiomes(access));
        this.seed = seed;
        this.registries = access;
        this.biomeMapManager = BiomeMapManager.getOrCreate(access);
        this.biomeSampler = new BiomeSampler(noise, this.biomeMapManager);
        this.caveBiomeSampler = new CaveBiomeSampler(seed, 800, this.biomeMapManager);
    }

    public Source(long seed, INoiseGenerator noise, RegistryAccess access, CaveBiomeRegistry caveBiomeRegistry, CaveSystemConfig systemConfig) {
        super(Source.possibleBiomes(access));
        this.seed = seed;
        this.registries = access;
        this.biomeMapManager = BiomeMapManager.getOrCreate(access);
        this.biomeSampler = new BiomeSampler(noise, this.biomeMapManager);
        this.caveBiomeSampler = new CaveBiomeSampler(seed, 800, this.biomeMapManager, caveBiomeRegistry, systemConfig);
    }

    private static List<Holder<Biome>> possibleBiomes(RegistryAccess access) {
        Registry biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        int biomeCount = biomes.size();
        if (possibleBiomesCache != null && possibleBiomesCacheSize == biomeCount) {
            return possibleBiomesCache;
        }
        possibleBiomesCache = BiomeMapManager.getOrCreate(access).getPossibleBiomeSourceBiomes();
        possibleBiomesCacheSize = biomeCount;
        return possibleBiomesCache;
    }

    public BiomeSource withSeed(long seed) {
        return this;
    }

    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return this.cache.computeIfAbsent(Seeds.get(this.seed), PosUtil.pack(x, z), this::compute);
    }

    public RegistryAccess getRegistries() {
        return this.registries;
    }

    public BiomeSampler getBiomeSampler() {
        return this.biomeSampler;
    }

    public CaveBiomeSampler getCaveBiomeSampler() {
        return this.caveBiomeSampler;
    }

    public CaveBiomeRegistry getCaveBiomeRegistry() {
        return this.caveBiomeSampler.getRegistry();
    }

    public Holder<Biome> getUnderGroundBiome(int seed, int x, int z, CaveType type, Holder<Biome> surfaceBiome, int blockY, int surfaceY, int caveCenterX, int caveCenterZ, int caveRadius) {
        return this.caveBiomeSampler.getUnderGroundBiome(seed, x, z, type, surfaceBiome, blockY, surfaceY, caveCenterX, caveCenterZ, caveRadius);
    }

    public Optional<Holder<Biome>> getCoastalEntranceBiome(int seed, int x, int z, Generator generator, Holder<Biome> surfaceBiome, int blockY, int surfaceY) {
        return this.caveBiomeSampler.getCoastalEntranceBiome(seed, x, z, generator, surfaceBiome, blockY, surfaceY);
    }

    public Registry<Biome> getRegistry() {
        return this.biomeMapManager.getBiomes();
    }

    protected Holder<Biome> compute(int seed, long index) {
        int x = PosUtil.unpackLeft(index) << 2;
        int z = PosUtil.unpackRight(index) << 2;
        return this.biomeSampler.sampleBiome(seed, x, z);
    }
}
