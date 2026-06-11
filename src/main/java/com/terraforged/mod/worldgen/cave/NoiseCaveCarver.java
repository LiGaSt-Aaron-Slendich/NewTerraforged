package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveEntranceCarver;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

public class NoiseCaveCarver {
    private static final int CHUNK_AREA = 256;
    private static final int SURFACE_BUFFER = 22;
    private static final int MEGA_GIGA_SURFACE_BUFFER = 26;
    private static final int MIN_BIOME_DEPTH = 12;
    private static final int MIN_GLOBAL_CAVERN = 4;
    private static final float MEGA_ZONE_THRESHOLD = 0.15f;
    private static final float GIGA_ZONE_THRESHOLD = 0.15f;
    private static final int BIOME_HALO = 2;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static void carve(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config, boolean carve) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = generator.getMinY();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        boolean megaGigaConfig = config.getType().isMegaOrGiga();
        if (megaGigaConfig && carve) {
            CaveEntranceCarver.ensureMassifTunnelAxis(generator, generator.getCaveEntranceClaims(), seed, startX + 8, startZ + 8);
            CaveEntranceCarver.tryCarveTunnelAnchors(seed, chunk, carver, generator, config, generator.getCaveEntranceClaims());
        }
        for (int i = 0; i < 256; ++i) {
            int midY;
            Holder<Biome> biome;
            int bottom;
            int top;
            int dx = i & 0xF;
            int dz = i >> 4;
            int x = startX + dx;
            int z = startZ + dz;
            int surface = NoiseCaveCarver.resolveColumnSurface(dx, dz, chunk, generator, carver, config, seed, x, z);
            boolean megaGiga = config.getType().isMegaOrGiga();
            int y = config.getHeight(seed, x, z);
            float value = megaGiga ? CaveNoise.sampleMerged(carver.modifier, seed, x, z) : CaveNoise.sample(carver.modifier, seed, x, z);
            int cavern = NoiseCaveCarver.resolveCavernSize(seed, x, z, value, config, carver);
            if (megaGiga && CaveOceanFilter.isBlockedForMegaGiga(generator, config.getType(), x, z) || cavern == 0 || config.getType() == CaveType.GLOBAL && (cavern < 4 || NoiseCaveCarver.isMegaGigaZone(seed, x, z, carver))) continue;
            int floor = config.getFloorDepth(seed, x, z, cavern);
            float breachMask = carver.getCarvingMask(seed, x, z, megaGiga);
            int roofBuffer = NoiseCaveCarver.resolveRoofBuffer(surface, breachMask, dx, dz, chunk, generator, carver, config, x, z);
            int ceiling = surface - roofBuffer;
            if (megaGiga && !carver.hasTunnelRiver()) {
                CaveType systemType = CaveSystemGrid.dominantType(seed, x, z);
                long systemKey = CaveSystemGrid.systemKey(x, z, systemType);
                CaveEntranceClaims.TunnelAxis axis = generator.getCaveEntranceClaims().tunnelAxis(systemKey);
                if (axis != null) {
                    carver.restoreTunnel(axis);
                }
            }
            if (megaGiga && carve && CaveEntranceCarver.isEntranceCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
                CaveEntranceCarver.carveSlopeEntrance(chunk, carver, generator, generator.getCaveEntranceClaims(), config, seed, x, z, dx, dz, cavern, breachMask);
            } else if (megaGiga && carve && CaveEntranceCarver.isTunnelExitCandidate(generator, carver, generator.getCaveEntranceClaims(), seed, x, z, cavern, breachMask, true)) {
                CaveEntranceCarver.carveTunnelExit(chunk, carver, generator, generator.getCaveEntranceClaims(), config, seed, x, z, dx, dz, cavern, breachMask);
            }
            if (ceiling <= minY) continue;
            if (megaGiga) {
                int maxDown;
                int maxUp = Math.max(0, ceiling - y);
                int vertRadius = Math.min(cavern, Math.min(maxUp, maxDown = Math.max(0, y - minY)));
                if (vertRadius < 3) continue;
                top = y + vertRadius;
                bottom = y - vertRadius;
            } else {
                top = Math.min(y + cavern, ceiling);
                bottom = Math.max(y - floor, minY);
            }
            int[] bounds = NoiseCaveCarver.adaptColumnBounds(bottom, top, ceiling, minY);
            if (bounds == null || (top = bounds[1]) - (bottom = bounds[0]) < 3 || (biome = carver.getBiome(x, z, midY = bottom + top >> 1, config, generator)) == null || !carve) continue;
            NoiseCaveCarver.carveColumn(chunk, carver, config, generator, biome, x, z, dx, dz, bottom, top, surface, roofBuffer, breachMask, megaGiga, pos);
        }
    }

    private static int resolveCavernSize(int seed, int x, int z, float value, NoiseCave config, CarverChunk carver) {
        return config.getCavernSize(seed, x, z, value);
    }

    private static boolean isMegaGigaZone(int seed, int x, int z, CarverChunk carver) {
        if (carver.megaModifier != null && CaveNoise.sample(carver.megaModifier, seed, x, z) > 0.15f) {
            return true;
        }
        return carver.gigaModifier != null && CaveNoise.sample(carver.gigaModifier, seed, x, z) > 0.15f;
    }

    private static int[] adaptColumnBounds(int bottom, int top, int ceiling, int minY) {
        int[] nArray;
        if ((top = Math.min(top, ceiling)) - (bottom = Math.max(bottom, minY)) >= 3) {
            return new int[]{bottom, top};
        }
        int available = ceiling - minY;
        if (available < 3) {
            return null;
        }
        int mid = bottom + top >> 1;
        int half = Math.min(Math.max(1, (top - bottom) / 2), available / 2);
        bottom = Math.max(minY, mid - half);
        top = Math.min(ceiling, mid + half);
        if (top - bottom < 3) {
            top = Math.min(ceiling, minY + available);
            bottom = top - 3;
        }
        if (top - bottom >= 3) {
            int[] nArray2 = new int[2];
            nArray2[0] = bottom;
            nArray = nArray2;
            nArray2[1] = top;
        } else {
            nArray = null;
        }
        return nArray;
    }

    private static void carveColumn(ChunkAccess chunk, CarverChunk carver, NoiseCave config, Generator generator, Holder<Biome> defaultBiome, int x, int z, int dx, int dz, int bottom, int top, int surface, int roofBuffer, float breachMask, boolean megaGiga, BlockPos.MutableBlockPos pos) {
        int patchY;
        int maxBiomeY = config.getType() == CaveType.GLOBAL ? config.getMaxY() >> 2 : surface - 12 >> 2;
        int topThird = config.getMaxY() - (config.getMaxY() - config.getMinY()) / 3;
        boolean patchPlacement = config.getPlacementType() == CavePlacementType.CEILING_PATCH || config.getPlacementType() == CavePlacementType.ISLAND_PATCH;
        boolean global = config.getType() == CaveType.GLOBAL;
        int surfaceBiomeSkip = global ? 4 : 12;
        Holder<Biome> patchBiome = defaultBiome;
        if (patchPlacement && (patchBiome = carver.getBiome(x, z, patchY = config.getPlacementType() == CavePlacementType.ISLAND_PATCH ? bottom : top, config, generator)) == null) {
            patchBiome = defaultBiome;
        }
        int columnCeiling = surface - roofBuffer;
        for (int cy = bottom; cy <= top; ++cy) {
            if (cy >= columnCeiling) continue;
            pos.set(dx, cy, dz);
            if (!chunk.getBlockState((BlockPos)pos).getFluidState().isEmpty()) continue;
            chunk.setBlockState((BlockPos)pos, AIR, false);
            if (cy >> 2 >= maxBiomeY || cy >= surface - surfaceBiomeSkip) continue;
            Holder<Biome> biome = defaultBiome;
            if (patchPlacement) {
                boolean topSection;
                topSection = config.getPlacementType() == CavePlacementType.CEILING_PATCH ? cy >= topThird : cy < topThird;
                if (topSection) {
                    biome = patchBiome;
                }
            }
            NoiseCaveCarver.setBiomeQuart(chunk, dx, cy, dz, biome);
            carver.noteDecorateAnchor(biome, new BlockPos(x, cy, z));
            NoiseCaveCarver.paintHalo(chunk, biome, dx, dz, cy, pos, x, z, carver);
        }
    }

    private static void paintHalo(ChunkAccess chunk, Holder<Biome> biome, int dx, int dz, int cy, BlockPos.MutableBlockPos pos, int worldX, int worldZ, CarverChunk carver) {
        for (int ox = -2; ox <= 2; ++ox) {
            for (int oz = -2; oz <= 2; ++oz) {
                if (ox == 0 && oz == 0) continue;
                int px = dx + ox;
                int pz = dz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) continue;
                pos.set(px, cy, pz);
                BlockState state = chunk.getBlockState((BlockPos)pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || !state.isSolidRender((BlockGetter)chunk, (BlockPos)pos)) continue;
                NoiseCaveCarver.setBiomeQuart(chunk, px, cy, pz, biome);
                carver.noteDecorateAnchor(biome, new BlockPos(worldX + ox, cy, worldZ + oz));
            }
        }
    }

    private static void setBiomeQuart(ChunkAccess chunk, int dx, int cy, int dz, Holder<Biome> biome) {
        int biomeX = dx >> 2;
        int biomeZ = dz >> 2;
        int biomeY = (cy & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(cy);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
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

    private static int resolveRoofBuffer(int surface, float breachMask, int localX, int localZ, ChunkAccess chunk, Generator generator, CarverChunk carver, NoiseCave config, int worldX, int worldZ) {
        int sea = generator.getSeaLevel();
        boolean megaGiga = config.getType().isMegaOrGiga();
        if (NoiseCaveCarver.isUnderwaterOcean(chunk, localX, localZ, sea)) {
            return megaGiga ? 26 : 22;
        }
        return megaGiga ? 26 : 22;
    }

    private static boolean isUnderwaterOcean(ChunkAccess chunk, int localX, int localZ, int seaLevel) {
        int oceanFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ) - 1;
        return oceanFloor < seaLevel - 4;
    }
}
