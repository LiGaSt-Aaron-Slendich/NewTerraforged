package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

public class NoiseCaveCarver {
    private static final int MIN_GLOBAL_CAVERN = 1;
    /** Aligns with {@link CaveBreaches} — breach thins roof, does not pierce surface in column carve. */
    private static final float NATURAL_BREACH_THRESHOLD = 0.7f;
    /** Underground mega/giga: surface crust kept unless roof buffer is 0 or breach thins roof. */
    private static final int AGGRESSIVE_SURFACE_CRUST = 2;
    private static final int UNLIMITED_CEILING = Integer.MAX_VALUE / 4;
    /** Minimum vertical carve radius in mega/giga so low-noise columns still clear obstacles. */
    private static final int MIN_MEGA_GIGA_CAVERN = 3;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static void carve(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, boolean carve) {
        if (!carver.isColumnCacheReady()) {
            carver.prepareColumnCache(seed, chunk, generator);
        }
        CaveType configType = config.getType();
        CarverColumnCache columns = carver.columnCache();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = generator.getMinY();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        boolean megaGigaConfig = configType.isMegaOrGiga();
        if (megaGigaConfig && carve) {
            CaveEntranceCarver.tryCarveTunnelAnchors(seed, chunk, carver, generator, config, generator.getCaveEntranceClaims());
        }
        if (megaGigaConfig && !carver.hasTunnelRiver()) {
            int midX = startX + 8;
            int midZ = startZ + 8;
            CaveType systemType = CaveSystemGrid.dominantType(generator, seed, midX, midZ);
            CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(CaveSystemGrid.systemKey(midX, midZ, systemType));
            if (axis != null) {
                carver.restoreTunnel(axis);
            }
        }
        if (configType == CaveType.GLOBAL && !columns.anySynapseEligible()) {
            return;
        }
        CaveDensityBudget densityBudget = megaGigaConfig ? null : carver.densityBudget();
        CaveDensitySettings densitySettings = NoiseCaveCarver.resolveDensitySettings();
        if (!megaGigaConfig && !columns.anySynapseEligible() && !densitySettings.regionPassesSpatialThinning(seed, chunk.getPos().x, chunk.getPos().z)) {
            return;
        }
        int sea = generator.getSeaLevel();
        ChunkPos chunkPos = chunk.getPos();
        int[][] smoothedCenterY = null;
        int[][] smoothedCavern = null;
        if (megaGigaConfig) {
            smoothedCenterY = new int[16][16];
            smoothedCavern = new int[16][16];
            NoiseCaveCarver.prepareSmoothedMegaGigaColumns(seed, config, carver, columns, configType, startX, startZ, smoothedCenterY, smoothedCavern);
        }
        for (int n = 0; n < 256; ++n) {
            int i = densityBudget != null ? NoiseCaveCarver.permutedColumnIndex(seed, chunkPos, n) : n;
            int dx = i & 0xF;
            int dz = i >> 4;
            if (!NoiseCaveCarver.shouldCarveColumn(columns, configType, dx, dz)) {
                continue;
            }
            int x = startX + dx;
            int z = startZ + dz;
            int sampleX = x + columns.sampleShiftX(dx, dz);
            int sampleZ = z + columns.sampleShiftZ(dx, dz);
            boolean megaGiga = megaGigaConfig;
            float value;
            int cavern;
            int y;
            int surface;
            if (megaGiga) {
                surface = NoiseCaveCarver.resolveMegaGigaSurface(carver.cachedSurface(dx, dz), dx, dz, chunk, carver, seed, sampleX, sampleZ, sea);
                if (smoothedCavern[dx][dz] <= 0) {
                    continue;
                }
                y = smoothedCenterY[dx][dz];
                cavern = smoothedCavern[dx][dz];
                value = CaveNoise.sampleMerged(carver.modifier, seed, sampleX, sampleZ);
            } else {
                if (densityBudget != null && densityBudget.xyRemaining() <= 0) {
                    continue;
                }
                value = CaveNoise.sample(carver.modifier, seed, sampleX, sampleZ);
                cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
                if (cavern < MIN_GLOBAL_CAVERN) {
                    continue;
                }
                y = config.getHeight(seed, sampleX, sampleZ) - columns.extraCenterDrop(dx, dz);
                if (NoiseCaveCarver.isUnderwaterOcean(chunk, dx, dz, sea)) {
                    surface = NoiseCaveCarver.resolveColumnSurface(dx, dz, chunk, generator, carver, config, seed, x, z);
                } else {
                    surface = carver.cachedSurface(dx, dz);
                    float mask = carver.getCarvingMask(seed, sampleX, sampleZ, false);
                    if (surface > sea || surface < sea - 16) {
                        surface += 9;
                    }
                    surface -= NoiseUtil.floor(16.0f * mask);
                }
            }
            if (megaGiga && columns.oceanBlocked(dx, dz)) {
                continue;
            }
            if (megaGiga && cavern < MIN_MEGA_GIGA_CAVERN) {
                cavern = MIN_MEGA_GIGA_CAVERN;
            } else if (cavern == 0) {
                continue;
            }
            int floor = config.getFloorDepth(seed, x, z, cavern);
            float breachMask = carver.getCarvingMask(seed, sampleX, sampleZ, megaGiga);
            boolean underwaterOcean = !megaGiga && NoiseCaveCarver.isUnderwaterOcean(chunk, dx, dz, sea);
            int roofBuffer = NoiseCaveCarver.resolveRoofBuffer(breachMask, sampleX, sampleZ, chunk, generator, config, underwaterOcean, seed, columns, dx, dz);
            int ceiling = surface - roofBuffer;
            if (megaGiga && carve && !columns.suppressSurfaceBreach(dx, dz) && CaveEntranceCarver.isEntranceCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
                CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, generator.getCaveEntranceClaims(), config, seed, x, z, dx, dz, cavern, breachMask);
            } else if (megaGiga && carve && CaveEntranceCarver.isTunnelExitCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
                CaveEntranceCarver.carveTunnelExit(chunk, carver, generator, generator.getCaveEntranceClaims(), config, seed, x, z, dx, dz, cavern, breachMask);
            }
            if (ceiling <= minY) {
                continue;
            }
            int bottom;
            int top;
            boolean surfaceBreach;
            if (megaGiga) {
                int[] bounds = NoiseCaveCarver.computeMegaGigaBounds(y, cavern, surface, roofBuffer, minY, breachMask, columns, dx, dz);
                if (bounds == null) {
                    continue;
                }
                bottom = bounds[0];
                top = bounds[1];
                surfaceBreach = bounds[2] != 0;
            } else {
                int[] bounds = NoiseCaveCarver.computeGlobalBounds(y, cavern, floor, surface, roofBuffer, minY, breachMask);
                if (bounds == null) {
                    continue;
                }
                bottom = bounds[0];
                top = bounds[1];
                surfaceBreach = bounds[2] != 0;
            }
            if (top - bottom < 3) {
                continue;
            }
            int verticalSpan = top - bottom + 1;
            int midY = bottom + top >> 1;
            Holder<Biome> biome = carver.getBiome(x, z, midY, config, generator);
            if (biome == null || !carve) {
                continue;
            }
            if (densityBudget != null && !megaGiga && !densityBudget.canCarveSecondary(verticalSpan)) {
                continue;
            }
            boolean piercedSurface = NoiseCaveCarver.carveColumn(chunk, carver, config, generator, biome, x, z, dx, dz, bottom, top, surface, roofBuffer, megaGiga, pos);
            NoiseCaveCarver.noteSurfaceRisk(carver, megaGiga, dx, dz, top, surface, roofBuffer, surfaceBreach, piercedSurface);
            if (densityBudget != null && !megaGiga && !columns.isMegaGigaZone(dx, dz)) {
                densityBudget.consumeSecondary(verticalSpan);
            }
            if (columns.reserveEntrance(dx, dz) && top >= surface - 14 && !CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                carver.markEntranceColumn(dx, dz);
                if (columns.nearSea() && columns.riverHillside(dx, dz)) {
                    carver.markCoastalEntranceColumn(dx, dz);
                }
            } else if (surfaceBreach && top >= surface - 10 && !columns.suppressSurfaceBreach(dx, dz) && !CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                carver.markEntranceColumn(dx, dz);
                if (columns.nearSea() && columns.riverHillside(dx, dz)) {
                    carver.markCoastalEntranceColumn(dx, dz);
                }
            }
        }
        if (megaGigaConfig && carve) {
            NoiseCaveCarver.clearMegaGigaFloatingCrust(chunk, columns);
        }
    }

    /** Mega/giga: every column in an active chunk — original TF per-column model. */
    private static boolean shouldCarveColumn(CarverColumnCache columns, CaveType configType, int dx, int dz) {
        if (configType.isMegaOrGiga() && columns.anyMegaGiga()) {
            return !columns.oceanBlocked(dx, dz);
        }
        return columns.matches(configType, dx, dz);
    }

    /**
     * Flags columns where the cave volume encroached on the surface crust (breach-thinned roof,
     * pierce, or air within a few blocks of the heightmap). Only these chunks run surface guard.
     */
    private static void noteSurfaceRisk(CarverChunk carver, boolean megaGiga, int dx, int dz, int top, int surface, int roofBuffer, boolean surfaceBreach, boolean piercedSurface) {
        if (carver == null) {
            return;
        }
        int configured = megaGiga ? NoiseCaveCarver.configuredMegaGigaRoof() : NoiseCaveCarver.configuredSynapseRoof();
        if (configured <= 0) {
            carver.markSurfaceRiskColumn(dx, dz);
            return;
        }
        if (surfaceBreach || piercedSurface || roofBuffer < configured - 4) {
            carver.markSurfaceRiskColumn(dx, dz);
            return;
        }
        int crustLimit = Math.min(10, roofBuffer + 2);
        if (top >= surface - crustLimit) {
            carver.markSurfaceRiskColumn(dx, dz);
        }
    }

    private static int[] computeMegaGigaBounds(int centerY, int cavern, int surface, int roofBuffer, int minY, float breachMask, CarverColumnCache columns, int dx, int dz) {
        boolean reservedEntrance = columns.reserveEntrance(dx, dz);
        int vertRadius = Math.max(MIN_MEGA_GIGA_CAVERN, cavern);
        int ceiling = NoiseCaveCarver.resolveCeilingY(surface, roofBuffer);
        int top = roofBuffer <= 0 ? centerY + vertRadius : Math.min(centerY + vertRadius, ceiling);
        int bottom = Math.max(centerY - vertRadius, minY);
        if (top - bottom < 3) {
            top = roofBuffer <= 0 ? Math.max(minY + 3, bottom + 3) : Math.min(ceiling, Math.max(minY + 3, bottom + 3));
            bottom = Math.max(minY, top - 3);
        }
        boolean markEntrance = reservedEntrance && top >= surface - Math.max(1, roofBuffer);
        return new int[]{bottom, top, markEntrance ? 1 : 0};
    }

    private static int[] computeGlobalBounds(int centerY, int cavern, int floor, int surface, int roofBuffer, int minY, float breachMask) {
        int bottom = Math.max(centerY - floor, minY);
        int top = roofBuffer <= 0 ? centerY + cavern : Math.min(centerY + cavern, surface - roofBuffer);
        if (top - bottom < 3) {
            bottom = Math.max(minY, top - 3);
        }
        return new int[]{bottom, top, 0};
    }

    private static boolean carveColumn(ChunkAccess chunk, CarverChunk carver, NoiseCave config, Generator generator, Holder<Biome> defaultBiome, int x, int z, int dx, int dz, int bottom, int top, int surface, int roofBuffer, boolean megaGiga, BlockPos.MutableBlockPos pos) {
        int maxBiomeY = config.getType() == CaveType.GLOBAL ? config.getMaxY() >> 2 : surface - 12 >> 2;
        int topThird = config.getMaxY() - (config.getMaxY() - config.getMinY()) / 3;
        boolean patchPlacement = config.getPlacementType() == CavePlacementType.CEILING_PATCH || config.getPlacementType() == CavePlacementType.ISLAND_PATCH;
        boolean global = config.getType() == CaveType.GLOBAL;
        int surfaceBiomeSkip = global ? 3 : (megaGiga ? 10 : 8);
        Holder<Biome> patchBiome = defaultBiome;
        int patchY = bottom;
        if (patchPlacement) {
            patchY = config.getPlacementType() == CavePlacementType.ISLAND_PATCH ? bottom : top;
            patchBiome = carver.getBiome(x, z, patchY, config, generator);
            if (patchBiome == null) {
                patchBiome = defaultBiome;
            }
        }
        int roofCap = roofBuffer;
        int carveCap = roofBuffer <= 0 ? UNLIMITED_CEILING : surface - roofCap;
        boolean piercedSurface = false;
        for (int cy = bottom; cy <= top; ++cy) {
            if (cy > carveCap) {
                continue;
            }
            pos.set(dx, cy, dz);
            BlockState state = chunk.getBlockState((BlockPos)pos);
            boolean atOrAboveSurface = cy >= surface;
            if (!state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.isAir()) {
                continue;
            }
            if (atOrAboveSurface || cy >= surface - 1 && cy <= surface + 1) {
                piercedSurface = true;
            }
            chunk.setBlockState((BlockPos)pos, AIR, false);
            if (cy >> 2 >= maxBiomeY || cy >= surface - surfaceBiomeSkip) continue;
            Holder<Biome> biome = defaultBiome;
            if (patchPlacement) {
                boolean topSection = config.getPlacementType() == CavePlacementType.CEILING_PATCH ? cy >= topThird : cy < topThird;
                if (topSection) {
                    biome = patchBiome;
                }
            }
            NoiseCaveCarver.setBiomeQuart(chunk, carver, dx, cy, dz, biome);
        }
        carver.noteDecorateAnchor(defaultBiome, new BlockPos(x, bottom, z));
        if (patchPlacement && patchBiome != defaultBiome) {
            carver.noteDecorateAnchor(patchBiome, new BlockPos(x, patchY, z));
        } else if (megaGiga) {
            NoiseCaveCarver.paintFloorHalo(chunk, carver, defaultBiome, dx, dz, bottom, pos, x, z);
        }
        return piercedSurface;
    }

    private static void clearMegaGigaFloatingCrust(ChunkAccess chunk, CarverColumnCache columns) {
        int minY = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!columns.anyMegaGiga() && !columns.isMegaGigaZone(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int yStart = Math.max(minY, surface - 18);
                for (int y = surface; y >= yStart; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!NoiseCaveCarver.isFloatingCrustBlock(state)) {
                        continue;
                    }
                    if (y <= minY || !chunk.getBlockState(pos.setY(y - 1)).isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos.set(lx, y, lz), AIR, false);
                }
            }
        }
    }

    private static boolean isFloatingCrustBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS);
    }

    private static CaveDensitySettings resolveDensitySettings() {
        if (TFCaveSystemConfig.INSTANCE == null) {
            return CaveDensitySettings.DEFAULT;
        }
        return TFCaveSystemConfig.INSTANCE.caveDensity;
    }

    private static int permutedColumnIndex(int seed, ChunkPos chunkPos, int sequence) {
        return NoiseUtil.hash2D(seed ^ 0xC01A0505, chunkPos.x * 31 + sequence, chunkPos.z * 17 + sequence) & 0xFF;
    }

    private static void paintFloorHalo(ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, int dx, int dz, int cy, BlockPos.MutableBlockPos pos, int worldX, int worldZ) {
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                if (ox == 0 && oz == 0) continue;
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) continue;
                pos.set(px, cy, pz);
                BlockState state = chunk.getBlockState((BlockPos)pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || !state.isSolidRender((BlockGetter)chunk, (BlockPos)pos)) continue;
                NoiseCaveCarver.setBiomeQuart(chunk, carver, px, cy, pz, biome);
            }
        }
    }

    private static void setBiomeQuart(ChunkAccess chunk, CarverChunk carver, int dx, int cy, int dz, Holder<Biome> biome) {
        int biomeX = dx >> 2;
        int biomeZ = dz >> 2;
        int biomeY = (cy & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(cy);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
        carver.markBiomeRestoreColumn(dx, dz);
    }

    private static int resolveEffectiveSurface(int rawSurface, int localX, int localZ, ChunkAccess chunk, CarverChunk carver, int seed, int worldX, int worldZ, int sea) {
        if (NoiseCaveCarver.isUnderwaterOcean(chunk, localX, localZ, sea)) {
            float mask = carver.getCarvingMask(seed, worldX, worldZ, true);
            return sea - 1 - NoiseUtil.floor(16.0f * mask);
        }
        int surface = rawSurface;
        float mask = carver.getCarvingMask(seed, worldX, worldZ, true);
        if (surface > sea || surface < sea - 16) {
            surface += 9;
        }
        return surface - NoiseUtil.floor(16.0f * mask);
    }

    private static int resolveColumnSurface(int localX, int localZ, ChunkAccess chunk, Generator generator, CarverChunk carver, NoiseCave config, int seed, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        if (NoiseCaveCarver.isUnderwaterOcean(chunk, localX, localZ, sea)) {
            if (config.getType().isMegaOrGiga()) {
                float mask = carver.getCarvingMask(seed, worldX, worldZ, true);
                return sea - 1 - NoiseUtil.floor(16.0f * mask);
            }
            float mask = carver.getCarvingMask(seed, worldX, worldZ);
            return sea - 1 - NoiseUtil.floor(16.0f * mask);
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
        if (config.getType().isMegaOrGiga()) {
            return NoiseCaveCarver.resolveEffectiveSurface(surface, localX, localZ, chunk, carver, seed, worldX, worldZ, sea);
        }
        if (carver.terrainData != null) {
            surface = Math.max(surface, carver.terrainData.getHeight(localX, localZ));
        }
        float mask = carver.getCarvingMask(seed, worldX, worldZ);
        if (surface > sea || surface < sea - 16) {
            surface += 9;
        }
        return surface - NoiseUtil.floor(16.0f * mask);
    }

    private static int resolveMegaGigaSurface(int rawSurface, int localX, int localZ, ChunkAccess chunk, CarverChunk carver, int seed, int worldX, int worldZ, int sea) {
        if (NoiseCaveCarver.configuredMegaGigaRoof() <= 0) {
            if (NoiseCaveCarver.isUnderwaterOcean(chunk, localX, localZ, sea)) {
                return sea - 1;
            }
            return rawSurface;
        }
        return NoiseCaveCarver.resolveEffectiveSurface(rawSurface, localX, localZ, chunk, carver, seed, worldX, worldZ, sea);
    }

    private static int resolveCeilingY(int surface, int roofBuffer) {
        return roofBuffer <= 0 ? UNLIMITED_CEILING : surface - roofBuffer;
    }

    private static int configuredMegaGigaRoof() {
        return TFCaveSystemConfig.INSTANCE != null ? TFCaveSystemConfig.INSTANCE.surfaceRoofBufferMegaGiga : 26;
    }

    private static int configuredSynapseRoof() {
        return TFCaveSystemConfig.INSTANCE != null ? TFCaveSystemConfig.INSTANCE.surfaceRoofBufferSynapse : 0;
    }

    private static int resolveRoofBuffer(float breachMask, int worldX, int worldZ, ChunkAccess chunk, Generator generator, NoiseCave config, boolean underwaterOcean, int seed, CarverColumnCache columns, int dx, int dz) {
        boolean megaGiga = config.getType().isMegaOrGiga();
        int configured = megaGiga ? NoiseCaveCarver.configuredMegaGigaRoof() : NoiseCaveCarver.configuredSynapseRoof();
        if (configured <= 0) {
            return 0;
        }
        boolean massif;
        boolean riverHillside;
        boolean nearSea;
        if (megaGiga && columns != null && columns.zone(dx, dz) != CarverColumnCache.ZONE_NONE) {
            massif = columns.chunkMassif();
            riverHillside = columns.riverHillside(dx, dz);
            nearSea = columns.nearSea();
        } else {
            massif = megaGiga && CaveMassifCache.qualifiesMountainMassif(generator, seed, worldX, worldZ);
            riverHillside = megaGiga && CaveOceanFilter.qualifiesRiverEntranceVicinity(generator, worldX, worldZ);
            nearSea = megaGiga && CaveOceanFilter.isNearSea(generator, worldX, worldZ);
        }
        int base = configured;
        if (underwaterOcean) {
            return base;
        }
        if (megaGiga && !nearSea && breachMask < NATURAL_BREACH_THRESHOLD) {
            return base;
        }
        if (megaGiga && breachMask >= NATURAL_BREACH_THRESHOLD) {
            if (nearSea || CaveOceanFilter.isSurfaceWaterColumn(generator, worldX, worldZ)) {
                if (riverHillside || massif) {
                    return Math.max(4, base / 4);
                }
                return Math.max(8, base / 2);
            }
            float openThreshold = massif ? 0.62f : 0.78f;
            if (breachMask >= openThreshold && columns != null && columns.reserveEntrance(dx, dz)) {
                int relaxed = Math.max(12, base / 3);
                return columns.localSurfaceSlope(dx, dz, 3) ? Math.max(14, relaxed) : relaxed;
            }
            if (breachMask >= openThreshold) {
                return Math.max(6, base / 2);
            }
            return Math.max(8, (int)((float)base * (1.0f - breachMask * 0.75f)));
        }
        if (megaGiga && breachMask > 0.08f) {
            base = (int)Math.max(4.0f, (float)base * (1.0f - breachMask * 0.88f));
        }
        return base;
    }

    private static boolean isUnderwaterOcean(ChunkAccess chunk, int localX, int localZ, int seaLevel) {
        int oceanFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ) - 1;
        return oceanFloor < seaLevel - 4;
    }

    /** Per-column values for every non-ocean column when chunk has mega/giga. */
    private static void prepareSmoothedMegaGigaColumns(int seed, NoiseCave config, CarverChunk carver, CarverColumnCache columns, CaveType configType, int startX, int startZ, int[][] centerOut, int[][] cavernOut) {
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                if (columns.oceanBlocked(dx, dz)) {
                    continue;
                }
                int x = startX + dx;
                int z = startZ + dz;
                int sampleX = x + columns.sampleShiftX(dx, dz);
                int sampleZ = z + columns.sampleShiftZ(dx, dz);
                float value = CaveNoise.sampleMerged(carver.modifier, seed, sampleX, sampleZ);
                int cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
                if (cavern < MIN_MEGA_GIGA_CAVERN) {
                    cavern = MIN_MEGA_GIGA_CAVERN;
                }
                centerOut[dx][dz] = config.getHeight(seed, sampleX, sampleZ);
                cavernOut[dx][dz] = cavern;
            }
        }
    }

    /**
     * Force-carve one mega/giga column — used by {@link CaveFlatWallRepair} when a flat wall is detected.
     */
    public static boolean forceCarveMegaGigaColumn(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, Module modifier, int dx, int dz) {
        if (config == null || !config.getType().isMegaOrGiga() || modifier == null) {
            return false;
        }
        if (!carver.isColumnCacheReady()) {
            carver.prepareColumnCache(seed, chunk, generator);
        }
        CarverColumnCache columns = carver.columnCache();
        if (columns.oceanBlocked(dx, dz)) {
            return false;
        }
        carver.beginCavePass(config);
        int minY = generator.getMinY();
        int sea = generator.getSeaLevel();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int x = startX + dx;
        int z = startZ + dz;
        int sampleX = x + columns.sampleShiftX(dx, dz);
        int sampleZ = z + columns.sampleShiftZ(dx, dz);
        float value = CaveNoise.sampleMerged(modifier, seed, sampleX, sampleZ);
        int cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
        if (cavern < MIN_MEGA_GIGA_CAVERN) {
            cavern = MIN_MEGA_GIGA_CAVERN;
        }
        int y = config.getHeight(seed, sampleX, sampleZ);
        int surface = NoiseCaveCarver.resolveMegaGigaSurface(carver.cachedSurface(dx, dz), dx, dz, chunk, carver, seed, sampleX, sampleZ, sea);
        float breachMask = carver.getCarvingMask(seed, sampleX, sampleZ, true);
        int roofBuffer = NoiseCaveCarver.resolveRoofBuffer(breachMask, sampleX, sampleZ, chunk, generator, config, false, seed, columns, dx, dz);
        int[] bounds = NoiseCaveCarver.computeMegaGigaBounds(y, cavern, surface, roofBuffer, minY, breachMask, columns, dx, dz);
        int bottom;
        int top;
        if (bounds == null) {
            CaveColumnSimulator.Sample sample = CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, x, z);
            if (sample == null) {
                return false;
            }
            bottom = sample.floorY();
            top = sample.ceilingY();
        } else {
            bottom = bounds[0];
            top = bounds[1];
        }
        if (top - bottom < 3) {
            return false;
        }
        int midY = bottom + top >> 1;
        Holder<Biome> biome = carver.getBiome(x, z, midY, config, generator);
        if (biome == null) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        NoiseCaveCarver.carveColumn(chunk, carver, config, generator, biome, x, z, dx, dz, bottom, top, surface, roofBuffer, true, pos);
        return true;
    }
}
