package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Strips dead-forest / glowing-forest leaks from heat cave columns (scorching, mantle, brimstone, magma).
 */
public final class CaveHeatBlockSanitizer {
    private static final int MIN_CAVE_AIR = 10;
    private static final int SURFACE_CRUST = 2;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private CaveHeatBlockSanitizer() {
    }

    public static boolean chunkMayNeedSanitize(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null || generator == null) {
            return false;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < 256; i += 8) {
            int lx = i & 0xF;
            int lz = i >> 4;
            int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz) - 24;
            Holder<Biome> biome = CaveHeatBlockSanitizer.sampleCaveBiome(generator, chunkX + lx, y, chunkZ + lz);
            if (CaveBiomeIds.isVolcanicCaveBiome(biome) || CaveBiomeIds.isScorchingCaveBiome(biome)) {
                return true;
            }
        }
        return false;
    }

    public static int sanitize(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null || generator == null) {
            return 0;
        }
        int[][] ranges = CaveHeatBlockSanitizer.buildScanRanges(chunk);
        int removed = 0;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int[] range = ranges[lx | lz << 4];
                if (range == null) {
                    continue;
                }
                int sampleY = (range[0] + range[1]) / 2;
                Holder<Biome> biome = CaveHeatBlockSanitizer.sampleCaveBiome(generator, chunkX + lx, sampleY, chunkZ + lz);
                if (!CaveBiomeIds.isVolcanicCaveBiome(biome)) {
                    continue;
                }
                for (int y = range[0]; y <= range[1]; ++y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!CaveHeatBlockSanitizer.isHeatIncompatibleBlock(state)) {
                        continue;
                    }
                    chunk.setBlockState(pos, AIR, false);
                    ++removed;
                }
            }
        }
        return removed;
    }

    static boolean isHeatIncompatibleBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (CaveHeatBlockSanitizer.isNativeHeatBlock(path)) {
            return false;
        }
        if ("minecraft".equals(id.getNamespace())) {
            return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                    || path.contains("glow_lichen") || path.contains("vine") || path.contains("mushroom");
        }
        if (CaveHeatBlockSanitizer.isDeadForestBlockLeak(path) || path.contains("dead_bush")) {
            return true;
        }
        if (path.contains("ash_vent") && !CaveFeatureFilters.isScorchingCaveVentFeature(path)) {
            return true;
        }
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || path.contains("leaf") && !path.contains("charred") && !path.contains("scorch")
                || path.contains("log") && !path.contains("charred") && !path.contains("scorch");
    }

    private static boolean isNativeHeatBlock(String path) {
        return path.contains("scorch") || path.contains("charred") || path.contains("brimstone")
                || path.contains("basalt") && !path.contains("pillar") || path.contains("magma")
                || path.contains("volcanic") || path.contains("yellowstone")
                || path.contains("ash") && (path.contains("scorch") || path.contains("vent"));
    }

    private static boolean isDeadForestBlockLeak(String path) {
        if (CaveFeatureFilters.isDeadWoodFeature(path)) {
            return true;
        }
        return path.contains("dead_") && (path.contains("leav") || path.contains("log") || path.contains("wood") || path.contains("tree") || path.contains("branch"));
    }

    private static Holder<Biome> sampleCaveBiome(Generator generator, int x, int y, int z) {
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int surface = generator.getOceanFloorHeight(x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        return source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome, y, surface, x, z, 256);
    }

    private static int[][] buildScanRanges(ChunkAccess chunk) {
        int[][] ranges = new int[256][];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int minY = Math.max(chunk.getMinBuildHeight() + 8, surface - 96);
                int topAir = -1;
                int bottomAir = Integer.MAX_VALUE;
                int air = 0;
                for (int y = surface; y >= minY; --y) {
                    if (!chunk.getBlockState(pos.set(lx, y, lz)).isAir()) {
                        continue;
                    }
                    ++air;
                    topAir = y;
                    bottomAir = Math.min(bottomAir, y);
                }
                if (air < MIN_CAVE_AIR || topAir < 0) {
                    continue;
                }
                int scanTop = Math.min(topAir, surface - SURFACE_CRUST);
                if (scanTop < bottomAir) {
                    scanTop = surface - SURFACE_CRUST;
                }
                if (scanTop >= bottomAir) {
                    ranges[lx | lz << 4] = new int[]{bottomAir, scanTop};
                }
            }
        }
        return ranges;
    }
}
