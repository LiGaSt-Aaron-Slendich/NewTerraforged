package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.util.storage.ObjectPool;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.decorator.FeatureDensityBudget;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeVolumeDecorator;
import com.terraforged.mod.worldgen.cave.CaveBreaches;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CaveEntranceSurfaceDecorator;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlan;
import com.terraforged.mod.worldgen.cave.CaveGrottoEntranceDecorator;
import com.terraforged.mod.worldgen.cave.CaveMegaAccentDecorator;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CavePatchPlacer;
import com.terraforged.mod.worldgen.cave.CaveSurfaceBiomeRestorer;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveThermalSpringsDecorator;
import com.terraforged.mod.worldgen.cave.CaveTunnelRiverDecorator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.CaveUndergroundJungleDecorator;
import com.terraforged.mod.worldgen.cave.NoiseCaveCarver;
import com.terraforged.mod.worldgen.cave.NoiseCaveDecorator;
import com.terraforged.mod.worldgen.util.ChunkScopedWorldGenLevel;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public class NoiseCaveGenerator {
    protected static final int POOL_SIZE = 32;
    protected static final float DENSITY = 0.05f;
    protected static final int GLOBAL_CAVE_REPS = 1;
    protected final NoiseCave[] caves;
    protected final Module megaCaveNoise;
    protected final Module gigaCaveNoise;
    protected final Module uniqueCaveNoise;
    protected final Module caveBreachNoise;
    protected final ObjectPool<CarverChunk> pool;
    protected final Map<ChunkPos, CarverChunk> cache = new ConcurrentHashMap<ChunkPos, CarverChunk>();
    private final CaveEntranceClaims entranceClaims = new CaveEntranceClaims();

    public CaveEntranceClaims getEntranceClaims() {
        return this.entranceClaims;
    }

    public NoiseCaveGenerator(long seed, RegistryAccess access) {
        this.megaCaveNoise = CaveModifiers.mega();
        this.gigaCaveNoise = CaveModifiers.giga();
        this.uniqueCaveNoise = CaveModifiers.unique();
        this.caveBreachNoise = CaveBreaches.mask();
        this.caves = NoiseCaveGenerator.createArray((Iterable<NoiseCave>)access.registryOrThrow(TerraForged.CAVES.get()));
        this.pool = new ObjectPool<CarverChunk>(32, this::createCarverChunk);
    }

    public NoiseCaveGenerator(long seed, NoiseCaveGenerator other) {
        this.caves = NoiseCaveGenerator.copyOf(seed, other.caves);
        this.megaCaveNoise = CaveModifiers.mega();
        this.gigaCaveNoise = CaveModifiers.giga();
        this.uniqueCaveNoise = CaveModifiers.unique();
        this.caveBreachNoise = CaveBreaches.mask();
        this.pool = new ObjectPool<CarverChunk>(32, this::createCarverChunk);
    }

    public void carve(ChunkAccess chunk, Generator generator) {
        int seed = (int)generator.getSeed();
        CarverChunk carver = this.getPreCarveChunk(chunk);
        carver.terrainData = generator.getChunkData(chunk.getPos());
        carver.mask = this.caveBreachNoise;
        carver.megaModifier = this.megaCaveNoise;
        carver.gigaModifier = this.gigaCaveNoise;
        for (NoiseCave config : this.caves) {
            CaveBiomeRegistry registry;
            if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
            carver.beginCavePass(config);
            carver.modifier = this.getModifier(config);
            NoiseCaveCarver.carve(seed, chunk, carver, generator, config, true);
            if (!config.getType().isMegaOrGiga() || (registry = NoiseCaveGenerator.resolveRegistry(generator)) == null) continue;
            CavePatchPlacer.apply(seed, chunk, generator, config, carver, registry);
        }
        ChunkUtil.refreshHeightmaps(chunk);
        CaveSurfaceBiomeRestorer.restore(chunk, generator);
    }

    public void decorate(ChunkAccess chunk, WorldGenLevel region, Generator generator) {
        int seed = (int)generator.getSeed();
        CarverChunk carver = this.getPostCarveChunk(seed, chunk, generator);
        HashSet<Long> decoratedColumns = new HashSet<Long>(128);
        CaveFeaturePlan.Cache planCache = new CaveFeaturePlan.Cache();
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk);
        CaveEntranceSurfaceDecorator.decorate(chunk, carver, guarded, generator);
        CaveBiomeVolumeDecorator.decorateChunk(chunk, carver, guarded, generator);
        CaveTunnelRiverDecorator.decorate(chunk, carver, guarded, generator);
        CaveThermalSpringsDecorator.decorate(chunk, carver, guarded, generator);
        CaveUndergroundJungleDecorator.decorate(chunk, carver, guarded, generator);
        CaveMegaAccentDecorator.decorate(chunk, carver, guarded, generator);
        CaveGrottoEntranceDecorator.decorate(chunk, carver, guarded, generator);
        for (NoiseCave config : this.caves) {
            if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
            FeatureDensityBudget featureBudget = FeatureDensityBudget.forCaves();
            NoiseCaveDecorator.decorate(chunk, carver, guarded, generator, config, decoratedColumns, featureBudget, planCache);
        }
        planCache.clear();
        this.pool.restore(carver);
    }

    private CarverChunk getPreCarveChunk(ChunkAccess chunk) {
        return this.cache.computeIfAbsent(chunk.getPos(), p -> this.pool.take().reset());
    }

    private CarverChunk getPostCarveChunk(int seed, ChunkAccess chunk, Generator generator) {
        CarverChunk carver = this.cache.remove(chunk.getPos());
        if (carver != null) {
            return carver;
        }
        carver = this.pool.take().reset();
        carver.mask = this.caveBreachNoise;
        carver.megaModifier = this.megaCaveNoise;
        carver.gigaModifier = this.gigaCaveNoise;
        carver.terrainData = generator.getChunkData(chunk.getPos());
        int chunkX = chunk.getPos().getMinBlockX() + 8;
        int chunkZ = chunk.getPos().getMinBlockZ() + 8;
        CaveType systemType = CaveSystemGrid.dominantType(seed, chunkX, chunkZ);
        CaveEntranceCarver.ensureMassifTunnelAxis(generator, generator.getCaveEntranceClaims(), seed, chunkX, chunkZ);
        CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(CaveSystemGrid.systemKey(chunkX, chunkZ, systemType));
        if (axis != null) {
            carver.restoreTunnel(axis);
        }
        for (NoiseCave config : this.caves) {
            if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
            carver.beginCavePass(config);
            carver.modifier = this.getModifier(config);
            NoiseCaveCarver.carve(seed, chunk, carver, generator, config, false);
        }
        return carver;
    }

    private Module getModifier(NoiseCave cave) {
        return switch (cave.getType()) {
            default -> throw new IncompatibleClassChangeError();
            case GLOBAL -> Source.ONE;
            case MEGA -> this.megaCaveNoise;
            case GIGA -> this.gigaCaveNoise;
            case UNIQUE -> this.uniqueCaveNoise;
        };
    }

    private CarverChunk createCarverChunk() {
        return new CarverChunk(this.caves.length);
    }

    private static NoiseCave[] copyOf(long seed, NoiseCave[] other) {
        NoiseCave[] array = Arrays.copyOf(other, other.length);
        for (int i = 0; i < array.length; ++i) {
            array[i] = array[i].withSeed(seed);
        }
        return array;
    }

    private static NoiseCave[] createArray(Iterable<NoiseCave> source) {
        int length = 0;
        for (NoiseCave cave : source) {
            length += NoiseCaveGenerator.getCount(cave);
        }
        NoiseCave[] array = new NoiseCave[length];
        int i = 0;
        for (NoiseCave cave : source) {
            int count = NoiseCaveGenerator.getCount(cave);
            for (int j = 0; j < count; ++j) {
                array[i++] = cave.withSeed((long)j * 16421058L);
            }
        }
        return array;
    }

    private static int getCount(NoiseCave cave) {
        return cave.getType() == CaveType.GLOBAL ? 1 : 1;
    }

    private static boolean isCaveEnabled(NoiseCave cave) {
        if (cave.getType() != CaveType.GLOBAL) {
            return true;
        }
        if (TFCaveSystemConfig.INSTANCE == null) {
            return false;
        }
        return TFCaveSystemConfig.INSTANCE.enableSynapseCaves;
    }

    private static CaveBiomeRegistry resolveRegistry(Generator generator) {
        return generator.getBiomeSource().getCaveBiomeRegistry();
    }
}
