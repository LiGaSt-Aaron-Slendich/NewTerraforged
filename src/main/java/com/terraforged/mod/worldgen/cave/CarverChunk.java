package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.util.BiomeList;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveSurfaceBiomeRestorer;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

public class CarverChunk {
    private static final int SECTION_BOTTOM = 0;
    private static final int SECTION_TOP = 1;
    private Holder<Biome> cachedFull;
    private int cachedFullX;
    private int cachedFullZ;
    private NoiseCave cachedFullConfig;
    private final Map<Long, Holder<Biome>> fullRegionCache = new HashMap<Long, Holder<Biome>>();
    private final Map<Long, Holder<Biome>> patchCache = new HashMap<Long, Holder<Biome>>();
    private int biomeListIndex = -1;
    private final BiomeList[] biomeLists;
    private final Map<NoiseCave, BiomeList> biomes = new IdentityHashMap<NoiseCave, BiomeList>();
    private final Map<Holder<Biome>, BlockPos> decorateAnchors = new IdentityHashMap<Holder<Biome>, BlockPos>();
    private final Map<Long, Holder<Biome>> patchBiomeOverrides = new HashMap<Long, Holder<Biome>>();
    private final boolean[] entranceColumns = new boolean[256];
    private final boolean[] coastalEntranceColumns = new boolean[256];
    private final boolean[] biomeRestoreColumns = new boolean[256];
    private boolean hasEntranceColumns;
    private int tunnelMouthX;
    private int tunnelMouthZ;
    private int tunnelExitX;
    private int tunnelExitZ;
    private float tunnelFlowX;
    private float tunnelFlowZ;
    private boolean tunnelRiver;
    public Module mask;
    public Module modifier;
    public Module megaModifier;
    public Module gigaModifier;
    public TerrainData terrainData;
    private final CarverColumnCache columns = new CarverColumnCache();
    private CaveDensityBudget densityBudget;
    private boolean columnsReady;

    public void beginCavePass(NoiseCave config) {
        if (this.cachedFullConfig != config) {
            this.cachedFull = null;
            this.fullRegionCache.clear();
            this.cachedFullConfig = config;
        }
    }

    public CarverChunk(int size) {
        this.biomeLists = new BiomeList[size];
        for (int i = 0; i < this.biomeLists.length; ++i) {
            this.biomeLists[i] = new BiomeList();
        }
    }

    public CarverChunk reset() {
        this.cachedFull = null;
        this.cachedFullConfig = null;
        this.fullRegionCache.clear();
        this.patchCache.clear();
        this.patchBiomeOverrides.clear();
        this.biomes.clear();
        this.decorateAnchors.clear();
        Arrays.fill(this.entranceColumns, false);
        Arrays.fill(this.coastalEntranceColumns, false);
        Arrays.fill(this.biomeRestoreColumns, false);
        this.hasEntranceColumns = false;
        this.tunnelMouthX = 0;
        this.tunnelMouthZ = 0;
        this.tunnelExitX = 0;
        this.tunnelExitZ = 0;
        this.tunnelFlowX = 0.0f;
        this.tunnelFlowZ = 0.0f;
        this.tunnelRiver = false;
        this.biomeListIndex = -1;
        this.densityBudget = null;
        this.clearColumnCache();
        return this;
    }

    void setDensityBudget(CaveDensityBudget budget) {
        this.densityBudget = budget;
    }

    CaveDensityBudget densityBudget() {
        return this.densityBudget;
    }

    public void prepareColumnCache(int seed, ChunkAccess chunk, Generator generator) {
        if (!this.columnsReady) {
            this.columns.build(seed, chunk, this, generator);
            this.columnsReady = true;
        }
    }

    public boolean isColumnCacheReady() {
        return this.columnsReady;
    }

    CarverColumnCache columnCache() {
        return this.columns;
    }

    public void clearColumnCache() {
        this.columnsReady = false;
    }

    public int cachedSurface(int dx, int dz) {
        return this.columns.surfaceY(dx, dz);
    }

    public void noteTunnelRiver(int mouthWx, int mouthWz, int chamberWx, int chamberWz, CaveType type) {
        this.tunnelMouthX = mouthWx;
        this.tunnelMouthZ = mouthWz;
        int cx = CaveSystemGrid.snapCenter(mouthWx, type);
        int cz = CaveSystemGrid.snapCenter(mouthWz, type);
        this.tunnelExitX = cx * 2 - mouthWx;
        this.tunnelExitZ = cz * 2 - mouthWz;
        float dx = chamberWx - mouthWx;
        float dz = chamberWz - mouthWz;
        float len = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (len >= 1.0f) {
            this.tunnelFlowX = dx / len;
            this.tunnelFlowZ = dz / len;
            this.tunnelRiver = true;
        }
    }

    public void restoreTunnel(CaveEntranceClaims.TunnelAxis axis) {
        float dz;
        this.tunnelMouthX = axis.mouthX();
        this.tunnelMouthZ = axis.mouthZ();
        this.tunnelExitX = axis.exitX();
        this.tunnelExitZ = axis.exitZ();
        float dx = axis.exitX() - axis.mouthX();
        float len = NoiseUtil.sqrt(dx * dx + (dz = (float)(axis.exitZ() - axis.mouthZ())) * dz);
        if (len >= 1.0f) {
            this.tunnelFlowX = dx / len;
            this.tunnelFlowZ = dz / len;
            this.tunnelRiver = true;
        }
    }

    public int tunnelExitX() {
        return this.tunnelExitX;
    }

    public int tunnelExitZ() {
        return this.tunnelExitZ;
    }

    public boolean hasTunnelRiver() {
        return this.tunnelRiver;
    }

    public int tunnelMouthX() {
        return this.tunnelMouthX;
    }

    public int tunnelMouthZ() {
        return this.tunnelMouthZ;
    }

    public float tunnelFlowX() {
        return this.tunnelFlowX;
    }

    public float tunnelFlowZ() {
        return this.tunnelFlowZ;
    }

    public void markEntranceColumn(int dx, int dz) {
        if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16) {
            this.entranceColumns[dz << 4 | dx] = true;
            this.hasEntranceColumns = true;
        }
    }

    public boolean hasAnyEntranceColumn() {
        return this.hasEntranceColumns;
    }

    public void noteDecorateAnchor(Holder<Biome> biome, BlockPos worldPos) {
        this.decorateAnchors.putIfAbsent(biome, worldPos);
    }

    public boolean isEntranceColumn(int dx, int dz) {
        if (dx < 0 || dx > 15 || dz < 0 || dz > 15) {
            return false;
        }
        return this.entranceColumns[dz << 4 | dx];
    }

    void restoreEntranceColumns(boolean[] snapshot) {
        if (snapshot == null) {
            return;
        }
        System.arraycopy(snapshot, 0, this.entranceColumns, 0, 256);
        this.hasEntranceColumns = false;
        for (boolean entrance : this.entranceColumns) {
            if (!entrance) {
                continue;
            }
            this.hasEntranceColumns = true;
            break;
        }
    }

    boolean[] snapshotEntranceColumns() {
        return (boolean[])this.entranceColumns.clone();
    }

    void markBiomeRestoreColumn(int dx, int dz) {
        if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16) {
            this.biomeRestoreColumns[dz << 4 | dx] = true;
        }
    }

    boolean needsBiomeRestore(int dx, int dz) {
        if (dx < 0 || dx > 15 || dz < 0 || dz > 15) {
            return false;
        }
        return this.biomeRestoreColumns[dz << 4 | dx];
    }

    boolean hasAnyBiomeRestoreColumn() {
        for (boolean marked : this.biomeRestoreColumns) {
            if (marked) {
                return true;
            }
        }
        return false;
    }

    public void markCoastalEntranceColumn(int dx, int dz) {
        this.markEntranceColumn(dx, dz);
        if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16) {
            this.coastalEntranceColumns[dz << 4 | dx] = true;
        }
    }

    public void expandEntranceZone(int radius) {
        if (radius <= 0) {
            return;
        }
        boolean[] snapshot = (boolean[])this.entranceColumns.clone();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                if (!snapshot[dz << 4 | dx]) continue;
                for (int ox = -radius; ox <= radius; ++ox) {
                    for (int oz = -radius; oz <= radius; ++oz) {
                        if (ox * ox + oz * oz > radius * radius) continue;
                        this.markEntranceColumn(dx + ox, dz + oz);
                    }
                }
            }
        }
    }

    public boolean isCoastalEntranceColumn(int dx, int dz) {
        if (dx < 0 || dx > 15 || dz < 0 || dz > 15) {
            return false;
        }
        return this.coastalEntranceColumns[dz << 4 | dx];
    }

    public void forEachDecorateAnchor(BiConsumer<Holder<Biome>, BlockPos> consumer) {
        this.decorateAnchors.forEach(consumer);
    }

    public boolean hasDecorateAnchors() {
        return !this.decorateAnchors.isEmpty();
    }

    public void setPatchBiome(int lx, int ly, int lz, Holder<Biome> biome, int chunkMinX, int chunkMinZ) {
        this.patchBiomeOverrides.put(CarverChunk.packQuartKey(lx >> 2, ly, lz >> 2), biome);
        this.decorateAnchors.putIfAbsent(biome, new BlockPos(chunkMinX + lx, ly, chunkMinZ + lz));
    }

    public Holder<Biome> resolveBiome(ChunkAccess chunk, int lx, int ly, int lz) {
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, ly, lz);
        if (painted != null && CaveBiomeIds.isUndergroundBiome(painted)) {
            return painted;
        }
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        if (ly >= surface - 8) {
            return chunk.getNoiseBiome(lx >> 2, QuartPos.fromBlock((int)ly), lz >> 2);
        }
        Holder<Biome> override = this.patchBiomeOverrides.get(CarverChunk.packQuartKey(lx >> 2, ly, lz >> 2));
        if (override != null) {
            return override;
        }
        if (painted != null) {
            return painted;
        }
        return chunk.getNoiseBiome(lx >> 2, QuartPos.fromBlock((int)ly), lz >> 2);
    }

    public static Holder<Biome> readPaintedBiomeAt(ChunkAccess chunk, int lx, int ly, int lz) {
        return CarverChunk.readPaintedBiome(chunk, lx, ly, lz);
    }

    public static void writeBiomeAt(ChunkAccess chunk, int lx, int ly, int lz, Holder<Biome> biome) {
        CaveSurfaceBiomeRestorer.setBiomeQuart(chunk, lx, ly, lz, biome);
    }

    private static Holder<Biome> readPaintedBiome(ChunkAccess chunk, int lx, int ly, int lz) {
        int sectionIndex = chunk.getSectionIndex(ly);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
            return null;
        }
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        return (Holder)container.get(lx >> 2, (ly & 0xF) >> 2, lz >> 2);
    }

    private static long packQuartKey(int biomeX, int blockY, int biomeZ) {
        return (long)(blockY & 0xFFFF) << 32 | ((long)biomeX & 0xFFFFL) << 16 | (long)biomeZ & 0xFFFFL;
    }

    public BiomeList getBiomes(NoiseCave config) {
        return this.biomes.get(config);
    }

    public Holder<Biome> getBiome(int x, int z, int blockY, NoiseCave config, Generator generator) {
        CavePlacementType placement = config.getPlacementType();
        return switch (placement) {
            default -> throw new IncompatibleClassChangeError();
            case FULL_REGION -> this.getFullRegionBiome(x, z, blockY, config, generator);
            case CEILING_PATCH, ISLAND_PATCH -> this.getPatchBiome(x, z, blockY, config, generator);
        };
    }

    public float getCarvingMask(int seed, int x, int z) {
        return this.getCarvingMask(seed, x, z, false);
    }

    public float getCarvingMask(int seed, int x, int z, boolean megaGiga) {
        float noise = CaveNoise.sample(this.mask, seed, x, z);
        if (megaGiga || this.terrainData == null) {
            return 1.0f - noise;
        }
        int localX = x & 0xF;
        int localZ = z & 0xF;
        float river = this.terrainData.getRiver().get(localX, localZ);
        return 1.0f - noise * river * 0.45f;
    }

    private Holder<Biome> getFullRegionBiome(int x, int z, int blockY, NoiseCave config, Generator generator) {
        if (config.getType().isMegaOrGiga()) {
            long key = CarverChunk.packFullRegionKey(x, z, config);
            Holder<Biome> cached = this.fullRegionCache.get(key);
            if (cached != null) {
                return cached;
            }
            int surfaceY = this.getSurfaceY(x, z);
            Holder<Biome> surfaceBiome = this.getSurfaceBiome(x, z, surfaceY, generator);
            int sampleY = blockY;
            cached = generator.getBiomeSource().getUnderGroundBiome(config.getSeed(), x, z, config.getType(), surfaceBiome, sampleY, surfaceY, CarverChunk.snapToCaveGrid(x, config), CarverChunk.snapToCaveGrid(z, config), CarverChunk.estimateCaveRadius(config));
            this.fullRegionCache.put(key, cached);
            this.biomes.computeIfAbsent(config, c -> this.nextList()).add(cached);
            this.decorateAnchors.putIfAbsent(cached, new BlockPos(x, blockY, z));
            return cached;
        }
        int cacheX = x >> 2;
        int cacheZ = z >> 2;
        if (this.cachedFull == null || cacheX != this.cachedFullX || cacheZ != this.cachedFullZ || this.cachedFullConfig != config) {
            int surfaceY = this.getSurfaceY(x, z);
            Holder<Biome> surfaceBiome = this.getSurfaceBiome(x, z, surfaceY, generator);
            int sampleY = config.getType() == CaveType.GLOBAL ? (config.getMinY() + config.getMaxY()) / 2 : blockY;
            this.cachedFull = generator.getBiomeSource().getUnderGroundBiome(config.getSeed(), x, z, config.getType(), surfaceBiome, sampleY, surfaceY, CarverChunk.snapToCaveGrid(x, config), CarverChunk.snapToCaveGrid(z, config), CarverChunk.estimateCaveRadius(config));
            this.cachedFullX = cacheX;
            this.cachedFullZ = cacheZ;
            this.biomes.computeIfAbsent(config, c -> this.nextList()).add(this.cachedFull);
            this.decorateAnchors.putIfAbsent(this.cachedFull, new BlockPos(x, blockY, z));
        }
        return this.cachedFull;
    }

    private static long packFullRegionKey(int biomeX, int biomeZ, NoiseCave config) {
        return (long)System.identityHashCode(config) << 32 | (long)(biomeX & 0xFFFF) << 16 | (long)biomeZ & 0xFFFFL;
    }

    private Holder<Biome> getPatchBiome(int x, int z, int blockY, NoiseCave config, Generator generator) {
        boolean patchSection;
        int caveBottom;
        int biomeX = x >> 2;
        int biomeZ = z >> 2;
        int caveTop = config.getMaxY();
        int topThird = caveTop - (caveTop - (caveBottom = config.getMinY())) / 3;
        int section = blockY >= topThird ? 1 : 0;
        patchSection = switch (config.getPlacementType()) {
            case CEILING_PATCH -> section == 1;
            case ISLAND_PATCH -> section == 0;
            default -> false;
        };
        if (!patchSection) {
            return this.getFullRegionBiome(x, z, blockY, config, generator);
        }
        long cacheKey = CarverChunk.packPatchKey(biomeX, biomeZ, section);
        Holder<Biome> cached = this.patchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int patchY = config.getPlacementType() == CavePlacementType.ISLAND_PATCH ? caveBottom : caveTop;
        int surfaceY = this.getSurfaceY(x, z);
        Holder<Biome> surfaceBiome = this.getSurfaceBiome(x, z, surfaceY, generator);
        Holder<Biome> result = generator.getBiomeSource().getUnderGroundBiome(config.getSeed(), x, z, config.getType(), surfaceBiome, patchY, surfaceY, CarverChunk.snapToCaveGrid(x, config), CarverChunk.snapToCaveGrid(z, config), CarverChunk.estimateCaveRadius(config));
        this.patchCache.put(cacheKey, result);
        this.biomes.computeIfAbsent(config, c -> this.nextList()).add(result);
        this.decorateAnchors.putIfAbsent(result, new BlockPos(x, patchY, z));
        return result;
    }

    private int getSurfaceY(int x, int z) {
        if (this.columnsReady) {
            return this.columns.surfaceY(x & 0xF, z & 0xF);
        }
        if (this.terrainData == null) {
            return 64;
        }
        return this.terrainData.getHeight(x & 0xF, z & 0xF);
    }

    private Holder<Biome> getSurfaceBiome(int x, int z, int surfaceY, Generator generator) {
        return generator.getBiomeSource().getNoiseBiome(x >> 2, surfaceY >> 2, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
    }

    private static int estimateCaveRadius(NoiseCave config) {
        return switch (config.getType()) {
            case GIGA -> 400;
            case MEGA -> 250;
            default -> config.getMaxY() - config.getMinY();
        };
    }

    private static int snapToCaveGrid(int coord, NoiseCave config) {
        int radius = CarverChunk.estimateCaveRadius(config);
        int cell = radius * 2;
        return Math.floorDiv(coord, cell) * cell + radius;
    }

    private static long packPatchKey(int biomeX, int biomeZ, int section) {
        return (long)section << 32 | ((long)biomeX & 0xFFFFL) << 16 | (long)biomeZ & 0xFFFFL;
    }

    private BiomeList nextList() {
        int i = this.biomeListIndex + 1;
        if (i < this.biomeLists.length) {
            this.biomeListIndex = i;
            return this.biomeLists[i].reset();
        }
        return new BiomeList();
    }
}
