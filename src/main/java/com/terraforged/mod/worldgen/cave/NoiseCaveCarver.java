package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
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
    /** Max excess of terrain-data surface over heightmap — river/lake banks inflate far higher. */
    private static final int MAX_TERRAIN_INFLATION = 12;
    /** Breach mask at/above this: no roof cap тАФ noise carve may open to sky; decorators finish the mouth. */
    private static final float NATURAL_BREACH_THRESHOLD = 0.25f;
    /** Breach strong enough to pierce solid rock at the surface and form a decorated entrance. */
    private static final float SURFACE_PIERCE_BREACH = 0.35f;
    /** Air volume above surface when a breach entrance opens (grotto lip / adit mouth). */
    private static final int ENTRANCE_AIR_LIFT = 4;
    /** Underground mega/giga: only this many blocks of surface crust remain unless breach opens. */
    private static final int AGGRESSIVE_SURFACE_CRUST = 2;
    /** Minimum vertical carve radius in mega/giga so low-noise columns still clear obstacles. */
    private static final int MIN_MEGA_GIGA_CAVERN = 3;
    /** Non-reserved surface pierce: only carve this deep below surface (mouth), not the full column. */
    private static final int SURFACE_MOUTH_DEPTH = 16;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static void carve(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, boolean carve) {
        if (!CaveCarvingGate.isEnabled()) {
            return;
        }
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
        CaveDensityBudget densityBudget = carver.densityBudget();
        CaveDensitySettings densitySettings = NoiseCaveCarver.resolveDensitySettings();
        if (!megaGigaConfig && !densitySettings.regionPassesSpatialThinning(seed, chunk.getPos().x, chunk.getPos().z)) {
            return;
        }
        int sea = generator.getSeaLevel();
        ChunkPos chunkPos = chunk.getPos();
        for (int n = 0; n < 256; ++n) {
            int i = densityBudget != null ? NoiseCaveCarver.permutedColumnIndex(seed, chunkPos, n) : n;
            int dx = i & 0xF;
            int dz = i >> 4;
            if (!columns.matches(configType, dx, dz)) {
                continue;
            }
            if (columns.riverCarveBlocked(dx, dz)) {
                continue;
            }
            if (carver.terrainData != null && CaveChunkSurfaceRepair.isRiverBedColumn(carver.terrainData, dx, dz)) {
                continue;
            }
            if (CaveOceanFilter.findWaterSurfaceY(chunk, dx, dz) >= 0) {
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
                if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)
                        || CaveOceanFilter.findWaterSurfaceY(chunk, dx, dz) >= 0) {
                    continue;
                }
                surface = carver.cachedSurface(dx, dz);
                int actualSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (surface > actualSurface + MAX_TERRAIN_INFLATION) {
                    surface = actualSurface + MAX_TERRAIN_INFLATION;
                }
                value = CaveNoise.sampleMerged(carver.modifier, seed, sampleX, sampleZ);
                cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
                y = config.getHeight(seed, sampleX, sampleZ) - columns.extraCenterDrop(dx, dz);
            } else {
                if (densityBudget != null && densityBudget.xyRemaining() <= 0) {
                    continue;
                }
                CarverColumnCache.SynapseSample synapseSample = columns.resolveSynapseSample(config, carver.modifier, seed, x, z, dx, dz);
                value = synapseSample.modifierValue;
                cavern = synapseSample.cavern;
                sampleX = synapseSample.sampleX;
                sampleZ = synapseSample.sampleZ;
                if (cavern < MIN_GLOBAL_CAVERN) {
                    continue;
                }
                y = config.getHeight(seed, sampleX, sampleZ) - columns.extraCenterDrop(dx, dz);
                if (NoiseCaveCarver.isUnderwaterOcean(chunk, dx, dz, sea)) {
                    surface = NoiseCaveCarver.resolveColumnSurface(dx, dz, chunk, generator, carver, config, seed, x, z);
                } else {
                    surface = carver.cachedSurface(dx, dz);
                    int actualSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                    if (surface > actualSurface + MAX_TERRAIN_INFLATION) {
                        surface = actualSurface + MAX_TERRAIN_INFLATION;
                    }
                    if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                        surface = Math.min(surface, chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz));
                    }
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
            boolean naturalBreach = megaGiga && roofBuffer == 0;
            if (megaGiga && carve && !columns.riverSurfaceSuppressed(dx, dz) && !naturalBreach && !columns.suppressSurfaceBreach(dx, dz) && CaveEntranceCarver.isEntranceCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
                CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, generator.getCaveEntranceClaims(), config, seed, x, z, dx, dz, cavern, breachMask);
            } else if (megaGiga && carve && !columns.riverSurfaceSuppressed(dx, dz) && !naturalBreach && CaveEntranceCarver.isTunnelExitCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
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
            // For shallow water synapse columns: use the actual stone bed as the carve ceiling
            // guard instead of the noise-inflated cachedSurface. This prevents carving through
            // river/pond floors while keeping bounds (and cave depth) unchanged.
            int floorBed = surface;
            if (!megaGiga) {
                int stoneFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz) - 1;
                int blockTop   = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz) - 1;
                if (stoneFloor < blockTop && stoneFloor >= sea - 4) {
                    floorBed = Math.min(surface, stoneFloor);
                }
            }
            boolean piercedSurface = NoiseCaveCarver.carveColumn(chunk, carver, config, generator, biome, x, z, dx, dz, bottom, top, surface, floorBed, roofBuffer, megaGiga, surfaceBreach, cavern, columns, pos);
            if (densityBudget != null) {
                if (megaGiga) {
                    densityBudget.consumeMegaGiga(1, verticalSpan);
                } else if (!columns.isMegaGigaZone(dx, dz)) {
                    densityBudget.consumeSecondary(verticalSpan);
                }
            }
            if (columns.reserveEntrance(dx, dz) && top >= surface - 6 && !columns.riverSurfaceSuppressed(dx, dz) && !CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                carver.markEntranceColumn(dx, dz);
                if (columns.nearSea() && columns.riverHillside(dx, dz)) {
                    carver.markCoastalEntranceColumn(dx, dz);
                }
            } else if (NoiseCaveCarver.shouldMarkEntranceColumn(naturalBreach, surfaceBreach, piercedSurface, breachMask, top, surface, columns, dx, dz) && !columns.riverSurfaceSuppressed(dx, dz) && !columns.suppressSurfaceBreach(dx, dz) && !CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
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

    /**
     * When local headroom is smaller than the cave radius under a river bed, shift the column
     * center down instead of clipping the ceiling тАФ avoids the sharp "U" under watercourses.
     */
    private static int shiftCenterForHeadroom(int centerY, int radius, int minY, int surface, int roofBuffer) {
        int ceiling = surface - roofBuffer;
        int headroom = ceiling - centerY;
        if (headroom >= radius) {
            return centerY;
        }
        int shifted = centerY - (radius - headroom);
        return Math.max(minY + radius, Math.min(centerY, shifted));
    }

    private static boolean allowsSurfacePierce(float breachMask, int top, int surface, int roofBuffer, boolean megaGiga) {
        if (!megaGiga) {
            return roofBuffer == 0;
        }
        if (roofBuffer == 0) {
            return true;
        }
        if (top >= surface - 2 && breachMask >= SURFACE_PIERCE_BREACH) {
            return true;
        }
        return top >= surface - roofBuffer && breachMask >= NATURAL_BREACH_THRESHOLD;
    }

    private static boolean shouldMarkEntranceColumn(boolean naturalBreach, boolean surfaceBreach, boolean piercedSurface, float breachMask, int top, int surface, CarverColumnCache columns, int dx, int dz) {
        if (columns.riverSurfaceSuppressed(dx, dz) || columns.reserveEntrance(dx, dz)) {
            return false;
        }
        if (naturalBreach && top >= surface - 6) {
            return true;
        }
        boolean hillsidePierce = breachMask >= SURFACE_PIERCE_BREACH && (columns.riverHillside(dx, dz) || columns.chunkMassif());
        if (surfaceBreach && hillsidePierce && top >= surface - 6) {
            return true;
        }
        return piercedSurface && hillsidePierce && top >= surface - 4;
    }

    private static int[] computeMegaGigaBounds(int centerY, int cavern, int surface, int roofBuffer, int minY, float breachMask, CarverColumnCache columns, int dx, int dz) {
        boolean riverColumn = columns.riverSurfaceSuppressed(dx, dz) || columns.nearRiver(dx, dz);
        boolean reservedEntrance = columns.reserveEntrance(dx, dz);
        int vertRadius = Math.max(MIN_MEGA_GIGA_CAVERN, cavern);
        int shiftedY;
        if (riverColumn) {
            int roofCap = reservedEntrance ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
            shiftedY = NoiseCaveCarver.shiftCenterForHeadroom(centerY, vertRadius, minY, surface, roofCap);
        } else {
            shiftedY = centerY;
        }
        int bottom = shiftedY - vertRadius;
        int top = shiftedY + vertRadius;
        boolean hillsidePierce = breachMask >= SURFACE_PIERCE_BREACH && (columns.riverHillside(dx, dz) || columns.chunkMassif());
        boolean allowSurfaceExit = !columns.suppressSurfaceBreach(dx, dz) && (reservedEntrance || !riverColumn && (hillsidePierce || breachMask >= NATURAL_BREACH_THRESHOLD));
        boolean surfaceBreach = allowSurfaceExit && (reservedEntrance || !riverColumn && (hillsidePierce || breachMask >= NATURAL_BREACH_THRESHOLD));
        int roofCap = reservedEntrance || surfaceBreach ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
        int ceiling = surface - roofCap;
        if (!riverColumn) {
            shiftedY = Math.max(minY + vertRadius, Math.min(centerY, ceiling - vertRadius));
            bottom = shiftedY - vertRadius;
            top = shiftedY + vertRadius;
        }
        if (top > ceiling && !surfaceBreach) {
            shiftedY = Math.max(minY + vertRadius, ceiling - vertRadius);
            bottom = shiftedY - vertRadius;
            top = shiftedY + vertRadius;
        }
        if (surfaceBreach && allowSurfaceExit) {
            top = Math.max(top, Math.min(shiftedY + cavern, surface + ENTRANCE_AIR_LIFT));
            if (!reservedEntrance) {
                int mouthDepth = Math.min(cavern, SURFACE_MOUTH_DEPTH) + AGGRESSIVE_SURFACE_CRUST + ENTRANCE_AIR_LIFT;
                bottom = Math.max(bottom, surface - mouthDepth);
            }
        }
        if (top - bottom < MIN_MEGA_GIGA_CAVERN) {
            bottom = Math.max(minY, top - Math.max(MIN_MEGA_GIGA_CAVERN, cavern));
        }
        if (top - bottom < 3) {
            bottom = Math.max(minY, top - 3);
        }
        return new int[]{bottom, top, surfaceBreach && allowSurfaceExit ? 1 : 0};
    }

    private static int[] computeGlobalBounds(int centerY, int cavern, int floor, int surface, int roofBuffer, int minY, float breachMask) {
        int roofCap = breachMask >= NATURAL_BREACH_THRESHOLD ? Math.min(roofBuffer, 4) : roofBuffer;
        int shiftedY = NoiseCaveCarver.shiftCenterForHeadroom(centerY, cavern, minY, surface, roofCap);
        int ceiling = surface - roofCap;
        int top = Math.min(shiftedY + cavern, ceiling);
        int bottom = Math.max(shiftedY - floor, minY);
        if (top - bottom < 3) {
            shiftedY = NoiseCaveCarver.shiftCenterForHeadroom(centerY, cavern, minY, surface, Math.max(2, roofCap / 2));
            top = Math.min(shiftedY + cavern, ceiling);
            bottom = Math.max(shiftedY - floor, minY);
        }
        if (top - bottom < 3) {
            top = Math.min(ceiling, bottom + Math.max(3, cavern / 2));
            bottom = Math.max(minY, top - Math.max(3, cavern / 2));
        }
        if (top - bottom < 3) {
            bottom = Math.max(minY, top - 3);
        }
        boolean surfaceBreach = breachMask >= SURFACE_PIERCE_BREACH && top >= surface - 2;
        if (surfaceBreach) {
            top = Math.max(top, Math.min(shiftedY + cavern, surface + ENTRANCE_AIR_LIFT));
            if (top - bottom < 3) {
                bottom = Math.max(minY, top - 3);
            }
        }
        return new int[]{bottom, top, surfaceBreach ? 1 : 0};
    }

    private static boolean carveColumn(ChunkAccess chunk, CarverChunk carver, NoiseCave config, Generator generator, Holder<Biome> defaultBiome, int x, int z, int dx, int dz, int bottom, int top, int surface, int floorBed, int roofBuffer, boolean megaGiga, boolean surfaceBreach, int cavern, CarverColumnCache columns, BlockPos.MutableBlockPos pos) {
        int maxBiomeY = config.getType() == CaveType.GLOBAL ? config.getMaxY() >> 2 : surface - 12 >> 2;
        int topThird = config.getMaxY() - (config.getMaxY() - config.getMinY()) / 3;
        boolean patchPlacement = config.getPlacementType() == CavePlacementType.CEILING_PATCH || config.getPlacementType() == CavePlacementType.ISLAND_PATCH;
        boolean global = config.getType() == CaveType.GLOBAL;
        int surfaceBiomeSkip = global ? 3 : (megaGiga ? 6 : 8);
        Holder<Biome> patchBiome = defaultBiome;
        int patchY = bottom;
        if (patchPlacement) {
            patchY = config.getPlacementType() == CavePlacementType.ISLAND_PATCH ? bottom : top;
            patchBiome = carver.getBiome(x, z, patchY, config, generator);
            if (patchBiome == null) {
                patchBiome = defaultBiome;
            }
        }
        int actualSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
        int solidCap;
        int carveCap;
        if (megaGiga) {
            solidCap = surface - (surfaceBreach ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST));
            carveCap = surfaceBreach ? surface + ENTRANCE_AIR_LIFT : solidCap;
        } else {
            solidCap = floorBed - (surfaceBreach ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST));
            carveCap = surfaceBreach ? surface + ENTRANCE_AIR_LIFT : solidCap;
        }
        if (!surfaceBreach) {
            int heightCap = actualSurface - Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
            solidCap = Math.min(solidCap, heightCap);
            carveCap = Math.min(carveCap, heightCap);
            int waterTop = CaveOceanFilter.findWaterSurfaceY(chunk, dx, dz);
            if (waterTop >= 0) {
                int waterCap = waterTop - 1 - Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
                solidCap = Math.min(solidCap, waterCap);
                carveCap = Math.min(carveCap, waterCap);
            }
        }
        boolean piercedSurface = false;
        for (int cy = bottom; cy <= top; ++cy) {
            if (cy > carveCap) {
                continue;
            }
            pos.set(dx, cy, dz);
            BlockState state = chunk.getBlockState((BlockPos)pos);
            boolean atOrAboveSurface = cy >= actualSurface;
            if (!state.getFluidState().isEmpty()) {
                if (!megaGiga || atOrAboveSurface) {
                    continue;
                }
            }
            if (state.isAir()) {
                if (atOrAboveSurface && !surfaceBreach) {
                    continue;
                }
                continue;
            }
            if (!megaGiga && cy >= solidCap && !surfaceBreach) {
                continue;
            }
            if (megaGiga && cy >= solidCap && !surfaceBreach) {
                continue;
            }
            if (atOrAboveSurface || cy >= actualSurface - 1 && cy <= actualSurface + 1) {
                piercedSurface = true;
            }
            chunk.setBlockState((BlockPos)pos, AIR, false);
            if (cy >> 2 >= maxBiomeY || cy >= actualSurface - surfaceBiomeSkip) {
                continue;
            }
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
            if (megaGiga) {
                NoiseCaveCarver.paintFloorHalo(chunk, carver, patchBiome, dx, dz, patchY, pos, x, z);
            }
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
                if (!columns.isMegaGigaZone(lx, lz)) {
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

    private static int resolveColumnSurface(int localX, int localZ, ChunkAccess chunk, Generator generator, CarverChunk carver, NoiseCave config, int seed, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        if (NoiseCaveCarver.isUnderwaterOcean(chunk, localX, localZ, sea)) {
            if (config.getType().isMegaOrGiga()) {
                return sea - 1;
            }
            float mask = carver.getCarvingMask(seed, worldX, worldZ);
            return sea - 1 - NoiseUtil.floor(16.0f * mask);
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ);
        if (config.getType().isMegaOrGiga()) {
            return surface;
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

    private static int resolveRoofBuffer(float breachMask, int worldX, int worldZ, ChunkAccess chunk, Generator generator, NoiseCave config, boolean underwaterOcean, int seed, CarverColumnCache columns, int dx, int dz) {
        boolean megaGiga = config.getType().isMegaOrGiga();
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
        int base = megaGiga ? AGGRESSIVE_SURFACE_CRUST : 18;
        if (underwaterOcean) {
            return megaGiga ? Math.max(AGGRESSIVE_SURFACE_CRUST, base) : base;
        }
        // Low land-factor mask (rivers): never leave a 2-block crust — thick roof or skip path should win.
        if (megaGiga && breachMask < 0.15f) {
            return Math.max(24, base * 8);
        }
        if (megaGiga && !nearSea && breachMask < NATURAL_BREACH_THRESHOLD) {
            return AGGRESSIVE_SURFACE_CRUST;
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
                return 0;
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

    public static final class ColumnProbeResult {
        private final boolean carvesAtProbe;
        private final String skipReason;
        private final int bottom;
        private final int top;
        private final int surface;
        private final int cavern;
        private final int centerY;
        private final int roofBuffer;
        private final int carveCap;
        private final float noiseValue;
        private final float breachMask;

        private ColumnProbeResult(boolean carvesAtProbe, String skipReason, int bottom, int top, int surface,
                int cavern, int centerY, int roofBuffer, int carveCap, float noiseValue, float breachMask) {
            this.carvesAtProbe = carvesAtProbe;
            this.skipReason = skipReason;
            this.bottom = bottom;
            this.top = top;
            this.surface = surface;
            this.cavern = cavern;
            this.centerY = centerY;
            this.roofBuffer = roofBuffer;
            this.carveCap = carveCap;
            this.noiseValue = noiseValue;
            this.breachMask = breachMask;
        }

        public boolean carvesAtProbe() {
            return this.carvesAtProbe;
        }

        public String skipReason() {
            return this.skipReason;
        }

        public int bottom() {
            return this.bottom;
        }

        public int top() {
            return this.top;
        }

        public int centerY() {
            return this.centerY;
        }

        public int cavern() {
            return this.cavern;
        }

        public String detailLine() {
            return String.format(java.util.Locale.ROOT,
                    "Y=%d..%d carveCap=%d centerY=%d cavern=%d surface=%d roofBuffer=%d noise=%.3f breach=%.3f",
                    this.bottom, this.top, this.carveCap, this.centerY, this.cavern, this.surface, this.roofBuffer,
                    this.noiseValue, this.breachMask);
        }

        private static ColumnProbeResult skip(String reason) {
            return new ColumnProbeResult(false, reason, 0, 0, 0, 0, 0, 0, 0, 0.0f, 0.0f);
        }
    }

    public static ColumnProbeResult probeColumn(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator,
            NoiseCave config, int dx, int dz, int probeY) {
        CaveType configType = config.getType();
        CarverColumnCache columns = carver.columnCache();
        if (!columns.matches(configType, dx, dz)) {
            return ColumnProbeResult.skip("SKIP — zone mismatch for " + configType + " (column zone="
                    + columns.megaGigaFlag(dx, dz) + ")");
        }
        if (columns.riverCarveBlocked(dx, dz)) {
            return ColumnProbeResult.skip("SKIP — river/lake corridor (riverCarveBlocked)");
        }
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int x = startX + dx;
        int z = startZ + dz;
        int sampleX = x + columns.sampleShiftX(dx, dz);
        int sampleZ = z + columns.sampleShiftZ(dx, dz);
        boolean megaGiga = configType.isMegaOrGiga();
        int minY = generator.getMinY();
        int sea = generator.getSeaLevel();
        float value;
        int cavern;
        int centerY;
        int surface;
        if (megaGiga) {
            if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                return ColumnProbeResult.skip("SKIP — mega/giga skips surface water column");
            }
            surface = carver.cachedSurface(dx, dz);
            int actualSurfaceClamp = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
            if (surface > actualSurfaceClamp + MAX_TERRAIN_INFLATION) {
                surface = actualSurfaceClamp + MAX_TERRAIN_INFLATION;
            }
            value = CaveNoise.sampleMerged(carver.modifier, seed, sampleX, sampleZ);
            cavern = config.getCavernSize(seed, sampleX, sampleZ, value);
            if (cavern < MIN_MEGA_GIGA_CAVERN) {
                cavern = MIN_MEGA_GIGA_CAVERN;
            }
            centerY = config.getHeight(seed, sampleX, sampleZ) - columns.extraCenterDrop(dx, dz);
        } else {
            CarverColumnCache.SynapseSample synapseSample = columns.resolveSynapseSample(config, carver.modifier, seed, x, z, dx, dz);
            value = synapseSample.modifierValue;
            cavern = synapseSample.cavern;
            sampleX = synapseSample.sampleX;
            sampleZ = synapseSample.sampleZ;
            if (cavern < MIN_GLOBAL_CAVERN) {
                String stitch = synapseSample.stitchedFromNeighbor ? " (border stitch tried)" : "";
                return ColumnProbeResult.skip(String.format(java.util.Locale.ROOT,
                        "SKIP — cavern=%d < min %d (noise=%.3f)%s", cavern, MIN_GLOBAL_CAVERN, value, stitch));
            }
            centerY = config.getHeight(seed, sampleX, sampleZ) - columns.extraCenterDrop(dx, dz);
            if (NoiseCaveCarver.isUnderwaterOcean(chunk, dx, dz, sea)) {
                surface = NoiseCaveCarver.resolveColumnSurface(dx, dz, chunk, generator, carver, config, seed, x, z);
            } else {
                surface = carver.cachedSurface(dx, dz);
                int actualSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (surface > actualSurface + MAX_TERRAIN_INFLATION) {
                    surface = actualSurface + MAX_TERRAIN_INFLATION;
                }
                if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
                    surface = Math.min(surface, chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz));
                }
                float mask = carver.getCarvingMask(seed, sampleX, sampleZ, false);
                if (surface > sea || surface < sea - 16) {
                    surface += 9;
                }
                surface -= NoiseUtil.floor(16.0f * mask);
            }
        }
        if (megaGiga && columns.oceanBlocked(dx, dz)) {
            return ColumnProbeResult.skip("SKIP — oceanBlocked (cached surface at/below sea level in mega/giga zone)");
        }
        if (megaGiga && cavern < MIN_MEGA_GIGA_CAVERN) {
            cavern = MIN_MEGA_GIGA_CAVERN;
        } else if (cavern == 0) {
            return ColumnProbeResult.skip("SKIP — cavern radius 0");
        }
        int floor = config.getFloorDepth(seed, x, z, cavern);
        float breachMask = carver.getCarvingMask(seed, sampleX, sampleZ, megaGiga);
        boolean underwaterOcean = !megaGiga && NoiseCaveCarver.isUnderwaterOcean(chunk, dx, dz, sea);
        int roofBuffer = NoiseCaveCarver.resolveRoofBuffer(breachMask, sampleX, sampleZ, chunk, generator, config,
                underwaterOcean, seed, columns, dx, dz);
        int ceiling = surface - roofBuffer;
        if (ceiling <= minY) {
            return ColumnProbeResult.skip("SKIP — ceiling=" + ceiling + " <= minY (surface=" + surface + " roofBuffer="
                    + roofBuffer + ")");
        }
        int bottom;
        int top;
        boolean surfaceBreach;
        if (megaGiga) {
            int[] bounds = NoiseCaveCarver.computeMegaGigaBounds(centerY, cavern, surface, roofBuffer, minY, breachMask,
                    columns, dx, dz);
            if (bounds == null) {
                return ColumnProbeResult.skip("SKIP — mega/giga bounds null (river/headroom rejected)");
            }
            bottom = bounds[0];
            top = bounds[1];
            surfaceBreach = bounds[2] != 0;
        } else {
            int[] bounds = NoiseCaveCarver.computeGlobalBounds(centerY, cavern, floor, surface, roofBuffer, minY,
                    breachMask);
            if (bounds == null) {
                return ColumnProbeResult.skip("SKIP — global bounds null");
            }
            bottom = bounds[0];
            top = bounds[1];
            surfaceBreach = bounds[2] != 0;
        }
        if (top - bottom < 3) {
            return ColumnProbeResult.skip("SKIP — vertical span < 3 (bottom=" + bottom + " top=" + top + ")");
        }
        int midY = bottom + top >> 1;
        net.minecraft.core.Holder<Biome> biome = carver.getBiome(x, z, midY, config, generator);
        if (biome == null) {
            return ColumnProbeResult.skip("SKIP — no biome for cave at midY=" + midY);
        }
        int floorBed = surface;
        if (!megaGiga) {
            int stoneFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz) - 1;
            int blockTop = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz) - 1;
            if (stoneFloor < blockTop && stoneFloor >= sea - 4) {
                floorBed = Math.min(surface, stoneFloor);
            }
        }
        int actualSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
        int solidCap;
        int carveCap;
        if (megaGiga) {
            solidCap = surface - (surfaceBreach ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST));
            carveCap = surfaceBreach ? surface + ENTRANCE_AIR_LIFT : solidCap;
        } else {
            solidCap = floorBed - (surfaceBreach ? 0 : Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST));
            carveCap = surfaceBreach ? surface + ENTRANCE_AIR_LIFT : solidCap;
        }
        if (!surfaceBreach) {
            int heightCap = actualSurface - Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
            solidCap = Math.min(solidCap, heightCap);
            carveCap = Math.min(carveCap, heightCap);
            int waterTop = CaveOceanFilter.findWaterSurfaceY(chunk, dx, dz);
            if (waterTop >= 0) {
                int waterCap = waterTop - 1 - Math.min(roofBuffer, AGGRESSIVE_SURFACE_CRUST);
                solidCap = Math.min(solidCap, waterCap);
                carveCap = Math.min(carveCap, waterCap);
            }
        }
        if (probeY < bottom || probeY > top) {
            return ColumnProbeResult.skip(String.format(java.util.Locale.ROOT,
                    "SKIP — probe Y=%d outside carve band Y=%d..%d (surface=%d actual=%d)", probeY, bottom, top,
                    surface, actualSurface));
        }
        if (probeY > carveCap) {
            return ColumnProbeResult.skip(String.format(java.util.Locale.ROOT,
                    "SKIP — probe Y=%d above carveCap=%d (surfaceBreach=%s surface=%d actual=%d)", probeY, carveCap,
                    surfaceBreach, surface, actualSurface));
        }
        return new ColumnProbeResult(true, null, bottom, top, surface, cavern, centerY, roofBuffer, carveCap, value,
                breachMask);
    }

}
