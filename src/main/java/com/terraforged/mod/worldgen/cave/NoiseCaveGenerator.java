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
import com.terraforged.mod.worldgen.cave.CaveBiomeVanillaPass;
import com.terraforged.mod.worldgen.cave.CaveDecorationSettings;
import com.terraforged.mod.worldgen.cave.CaveEntranceSurfaceDecorator;
import com.terraforged.mod.worldgen.cave.CaveEntranceVanillaDecorator;
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
    private final NoiseCave[] carveOrderCaves;
    protected final Module megaCaveNoise;
    protected final Module gigaCaveNoise;
    protected final Module uniqueCaveNoise;
    protected final Module caveBreachNoise;
    protected final ObjectPool<CarverChunk> pool;
    protected final Map<ChunkPos, CarverChunk> cache = new ConcurrentHashMap<ChunkPos, CarverChunk>();
    private final Map<ChunkPos, boolean[]> entranceSnapshots = new ConcurrentHashMap<ChunkPos, boolean[]>();
    private final CaveEntranceClaims entranceClaims = new CaveEntranceClaims();

    public CaveEntranceClaims getCaveEntranceClaims() {
        return this.entranceClaims;
    }

    public CaveEntranceClaims getEntranceClaims() {
        return this.entranceClaims;
    }

    public CarverChunk peekCarver(ChunkPos pos) {
        return this.cache.get(pos);
    }

    public NoiseCave[] caveConfigs() {
        return this.caves;
    }

    public Module modifierFor(NoiseCave cave) {
        return this.getModifier(cave);
    }

    public void decorateVolume(ChunkAccess chunk, WorldGenLevel region, Generator generator) {
        CarverChunk carver = this.prepareDecorateCarver((int)generator.getSeed(), chunk, generator);
        if (carver == null) {
            return;
        }
        carver.columnCache().buildDecorationFlags(carver, chunk);
        CarverColumnCache columns = carver.columnCache();
        boolean megaGiga = columns.anyMegaGiga();
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        if (CaveDecorationSettings.usePerBiomeDecorators()) {
            CaveHybridBiomeDecorator.decorateVolume(chunk, carver, guarded, generator);
        } else if (CaveDecorationSettings.useOfficialTfDecorator()) {
            for (NoiseCave config : this.caves) {
                if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
                TerraForgedOfficialCaveDecorator.decorate(chunk, carver, guarded, generator, config);
            }
        } else if (CaveDecorationSettings.useLegacyDecorators()) {
            CaveBiomeVolumeDecorator.decorateChunk(chunk, carver, guarded, generator);
            if (megaGiga) {
                if (carver.hasTunnelRiver()) {
                    CaveTunnelRiverDecorator.decorate(chunk, carver, guarded, generator);
                }
                CaveThermalSpringsDecorator.decorate(chunk, carver, guarded, generator);
                CaveMegaAccentDecorator.decorate(chunk, carver, guarded, generator);
            }
        } else if (CaveDecorationSettings.useCompromiseDecorator()) {
            CaveBiomeCompromiseDecorator.decorateVolume(chunk, carver, guarded, generator);
        } else if (CaveDecorationSettings.useVanillaPass()) {
            CaveBiomeVanillaPass.decorateChunk(chunk, carver, guarded, generator);
            if (megaGiga && carver.hasTunnelRiver()) {
                CaveTunnelRiverDecorator.decorate(chunk, carver, guarded, generator);
            }
        }
    }

    public void decorateEntrances(ChunkAccess chunk, WorldGenLevel region, Generator generator) {
        CarverChunk carver = this.cache.get(chunk.getPos());
        if (carver == null) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        boolean megaGiga = columns.anyMegaGiga();
        HashSet<Long> decoratedColumns = new HashSet<Long>(128);
        CaveFeaturePlan.Cache planCache = new CaveFeaturePlan.Cache();
        WorldGenLevel guarded = ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(region, chunk, carver);
        if (CaveDecorationSettings.usePerBiomeDecorators()) {
            CaveHybridBiomeDecorator.decorateEntrances(chunk, carver, guarded, generator);
        } else if (CaveDecorationSettings.useLegacyDecorators()) {
            if (carver.hasAnyEntranceColumn()) {
                CaveEntranceSurfaceDecorator.decorate(chunk, carver, guarded, generator);
            }
            CaveGrottoEntranceDecorator.decorate(chunk, carver, guarded, generator);
            for (NoiseCave config : this.caves) {
                if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
                FeatureDensityBudget featureBudget = FeatureDensityBudget.forCaves();
                NoiseCaveDecorator.decorate(chunk, carver, guarded, generator, config, decoratedColumns, featureBudget, planCache);
            }
        } else if (CaveDecorationSettings.useCompromiseDecorator()) {
            CaveBiomeCompromiseDecorator.decorateEntrances(chunk, carver, guarded, generator);
            HashSet<Long> globalColumns = new HashSet<Long>(64);
            FeatureDensityBudget globalBudget = FeatureDensityBudget.forCaves();
            for (NoiseCave config : this.caves) {
                if (!NoiseCaveGenerator.isCaveEnabled(config) || config.getType() != CaveType.GLOBAL) continue;
                NoiseCaveDecorator.decorate(chunk, carver, guarded, generator, config, globalColumns, globalBudget, planCache);
            }
        } else if (CaveDecorationSettings.useVanillaPass() && carver.hasAnyEntranceColumn()) {
            CaveEntranceVanillaDecorator.decorate(chunk, carver, guarded, generator);
        }
        planCache.clear();
    }

    public void finishDecorate(ChunkAccess chunk, Generator generator) {
        this.finishDecorate(chunk);
    }

    public void finishDecorate(ChunkAccess chunk) {
        CarverChunk carver = this.cache.remove(chunk.getPos());
        this.entranceSnapshots.remove(chunk.getPos());
        if (carver != null) {
            carver.clearColumnCache();
            this.pool.restore(carver);
        }
    }

    public NoiseCaveGenerator(long seed, RegistryAccess access) {
        this.megaCaveNoise = CaveModifiers.mega();
        this.gigaCaveNoise = CaveModifiers.giga();
        this.uniqueCaveNoise = CaveModifiers.unique();
        this.caveBreachNoise = CaveBreaches.mask();
        this.caves = NoiseCaveGenerator.createArray((Iterable<NoiseCave>)access.registryOrThrow(TerraForged.CAVES.get()));
        this.carveOrderCaves = NoiseCaveGenerator.orderByCarvePriority(this.caves);
        this.pool = new ObjectPool<CarverChunk>(32, this::createCarverChunk);
    }

    public NoiseCaveGenerator(long seed, NoiseCaveGenerator other) {
        this.caves = NoiseCaveGenerator.copyOf(seed, other.caves);
        this.carveOrderCaves = NoiseCaveGenerator.orderByCarvePriority(this.caves);
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
        carver.prepareColumnCache(seed, chunk, generator);
        CarverColumnCache columns = carver.columnCache();
        carver.setDensityBudget(NoiseCaveGenerator.createDensityBudget());
        CaveDensitySettings densitySettings = NoiseCaveGenerator.resolveDensitySettings();
        if (columns.anyMegaGiga()) {
            int chunkX = chunk.getPos().getMinBlockX() + 8;
            int chunkZ = chunk.getPos().getMinBlockZ() + 8;
            CaveEntranceCarver.ensureMassifTunnelAxis(generator, this.entranceClaims, seed, chunkX, chunkZ);
        }
        NoiseCave envelopeConfig = NoiseCaveGenerator.representativeMegaGiga(columns, this.caves);
        if (envelopeConfig != null) {
            carver.beginCavePass(envelopeConfig);
            carver.modifier = this.getModifier(envelopeConfig);
            CaveParallelExposureFilter.build(columns, seed, chunk, carver, generator, envelopeConfig);
        }
        NoiseCave synapseProbe = NoiseCaveGenerator.findPrimarySynapseConfig(this.caves);
        if (NoiseCaveGenerator.isCaveEnabled(synapseProbe)) {
            columns.ensureSynapseEligibility(synapseProbe, seed);
        }
        for (NoiseCave config : this.carveOrderCaves) {
            CaveBiomeRegistry registry;
            if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
            CaveType type = config.getType();
            if (type == CaveType.GLOBAL && !columns.anySynapseEligible()) continue;
            carver.beginCavePass(config);
            carver.modifier = this.getModifier(config);
            NoiseCaveCarver.carve(seed, chunk, carver, generator, config, true);
            if (!type.isMegaOrGiga() || (registry = NoiseCaveGenerator.resolveRegistry(generator)) == null) continue;
            CavePatchPlacer.apply(seed, chunk, generator, config, carver, registry);
        }
        if (NoiseCaveGenerator.isSynapseEnabled()) {
            CaveGrottoCarver.tryCarveChunk(seed, chunk, carver, generator, NoiseCaveGenerator.findSynapseConfig(this.caves), generator.getCaveEntranceClaims());
        }
        CaveRiverEntranceHydrator.hydrate(chunk, carver, generator);
        CaveFlatWallRepair.afterCarve(seed, chunk, carver, generator, this.caves, this::getModifier);
        this.entranceSnapshots.put(chunk.getPos(), carver.snapshotEntranceColumns());
        ChunkUtil.refreshHeightmaps(chunk);
        CaveSurfaceBiomeRestorer.restore(chunk, generator, carver);
    }

    private CarverChunk prepareDecorateCarver(int seed, ChunkAccess chunk, Generator generator) {
        CarverChunk carver = this.cache.get(chunk.getPos());
        if (carver == null) {
            carver = this.rebuildDecorateCarver(seed, chunk, generator);
            if (carver != null) {
                this.cache.put(chunk.getPos(), carver);
            }
        }
        return carver;
    }

    private CarverChunk rebuildDecorateCarver(int seed, ChunkAccess chunk, Generator generator) {
        CarverChunk carver = this.pool.take().reset();
        carver.mask = this.caveBreachNoise;
        carver.megaModifier = this.megaCaveNoise;
        carver.gigaModifier = this.gigaCaveNoise;
        carver.terrainData = generator.getChunkData(chunk.getPos());
        carver.prepareColumnCache(seed, chunk, generator);
        CarverColumnCache columns = carver.columnCache();
        int chunkX = chunk.getPos().getMinBlockX() + 8;
        int chunkZ = chunk.getPos().getMinBlockZ() + 8;
        if (columns.anyMegaGiga()) {
            CaveEntranceCarver.ensureMassifTunnelAxis(generator, generator.getCaveEntranceClaims(), seed, chunkX, chunkZ);
        }
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, chunkX, chunkZ);
        CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(CaveSystemGrid.systemKey(chunkX, chunkZ, systemType));
        if (axis != null) {
            carver.restoreTunnel(axis);
        }
        boolean[] savedEntrances = this.entranceSnapshots.get(chunk.getPos());
        if (savedEntrances != null) {
            carver.restoreEntranceColumns(savedEntrances);
            if (CaveDecorationSettings.useOfficialTfDecorator()) {
                this.replayCarveForBiomes(seed, chunk, carver, generator);
            }
            carver.columnCache().buildDecorationFlags(carver, chunk);
            return carver;
        }
        NoiseCave envelopeConfig = NoiseCaveGenerator.representativeMegaGiga(columns, this.caves);
        if (envelopeConfig != null) {
            carver.beginCavePass(envelopeConfig);
            carver.modifier = this.getModifier(envelopeConfig);
            CaveParallelExposureFilter.build(columns, seed, chunk, carver, generator, envelopeConfig);
        }
        if (CaveDecorationSettings.useOfficialTfDecorator()) {
            this.replayCarveForBiomes(seed, chunk, carver, generator);
        } else {
            for (NoiseCave config : this.carveOrderCaves) {
                if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
                CaveType type = config.getType();
                if (!type.isMegaOrGiga()) continue;
                carver.beginCavePass(config);
                carver.modifier = this.getModifier(config);
                NoiseCaveCarver.carve(seed, chunk, carver, generator, config, false);
            }
        }
        return carver;
    }

    private void replayCarveForBiomes(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator) {
        for (NoiseCave config : this.carveOrderCaves) {
            if (!NoiseCaveGenerator.isCaveEnabled(config)) continue;
            carver.beginCavePass(config);
            carver.modifier = this.getModifier(config);
            NoiseCaveCarver.carve(seed, chunk, carver, generator, config, false);
        }
    }

    private static CaveDensitySettings resolveDensitySettings() {
        if (TFCaveSystemConfig.INSTANCE == null) {
            return CaveDensitySettings.DEFAULT;
        }
        return TFCaveSystemConfig.INSTANCE.caveDensity;
    }

    private static CaveDensityBudget createDensityBudget() {
        if (TFCaveSystemConfig.INSTANCE == null) {
            return null;
        }
        CaveDensitySettings settings = TFCaveSystemConfig.INSTANCE.caveDensity;
        if (settings.useSpatialThinning()) {
            return null;
        }
        if (settings.xyLimit() == null && settings.yzLimit() == null) {
            return null;
        }
        return new CaveDensityBudget(settings);
    }

    private static NoiseCave[] orderByCarvePriority(NoiseCave[] caves) {
        NoiseCave[] ordered = Arrays.copyOf(caves, caves.length);
        Arrays.sort(ordered, (a, b) -> Integer.compare(NoiseCaveGenerator.carvePriority(a.getType()), NoiseCaveGenerator.carvePriority(b.getType())));
        return ordered;
    }

    private static int carvePriority(CaveType type) {
        return switch (type) {
            case GIGA -> 0;
            case MEGA -> 1;
            case UNIQUE -> 2;
            case GLOBAL -> 3;
        };
    }

    private CarverChunk getPreCarveChunk(ChunkAccess chunk) {
        return this.cache.computeIfAbsent(chunk.getPos(), p -> this.pool.take().reset());
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

    private static boolean isSynapseEnabled() {
        if (TFCaveSystemConfig.INSTANCE == null) {
            return false;
        }
        return TFCaveSystemConfig.INSTANCE.enableSynapseCaves;
    }

    private static NoiseCave findSynapseConfig(NoiseCave[] caves) {
        return NoiseCaveGenerator.findPrimarySynapseConfig(caves);
    }

    /** Main synapse layer (mid elevation band) — used for prefilter and density-limited carving. */
    private static NoiseCave findPrimarySynapseConfig(NoiseCave[] caves) {
        NoiseCave fallback = null;
        for (NoiseCave cave : caves) {
            if (cave.getType() != CaveType.GLOBAL || !NoiseCaveGenerator.isCaveEnabled(cave)) {
                continue;
            }
            if (fallback == null) {
                fallback = cave;
            }
            if (cave.getMinY() == 0 && cave.getMaxY() == 256) {
                return cave;
            }
        }
        return fallback;
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

    static NoiseCave representativeMegaGiga(CarverColumnCache columns, NoiseCave[] caves) {
        if (!columns.anyMegaGiga()) {
            return null;
        }
        for (NoiseCave cave : caves) {
            if (!NoiseCaveGenerator.isCaveEnabled(cave)) {
                continue;
            }
            CaveType type = cave.getType();
            if (type == CaveType.GIGA && columns.hasGiga()) {
                return cave;
            }
        }
        for (NoiseCave cave : caves) {
            if (!NoiseCaveGenerator.isCaveEnabled(cave)) {
                continue;
            }
            if (cave.getType() == CaveType.MEGA && columns.hasMega()) {
                return cave;
            }
        }
        return null;
    }

    private static CaveBiomeRegistry resolveRegistry(Generator generator) {
        return generator.getBiomeSource().getCaveBiomeRegistry();
    }
}
