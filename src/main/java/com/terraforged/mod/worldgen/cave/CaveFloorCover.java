package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Direct floor cover before scatter features — prevents mushrooms/features floating above bare stone.
 */
public final class CaveFloorCover {
    private static final int[][] PATCH = new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}};

    private CaveFloorCover() {
    }

    public static boolean appliesTo(Holder<Biome> biome) {
        return CaveBiomeIds.isModCaveBiome(biome) || CaveBiomeIds.isCoverDenseCaveBiome(biome) && !CaveBiomeIds.isCrystalCaveBiome(biome) || CaveBiomeIds.isPrismachasmBiome(biome) || CaveBiomeIds.isScorchingCaveBiome(biome) || CaveBiomeIds.isVolcanicCaveBiome(biome) || CaveBiomeIds.isFungalCaveBiome(biome);
    }

    /** Normalizes floor height, paints themed cover, returns air anchor for feature placement. */
    public static BlockPos prepare(ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, BlockPos airAnchor) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        int y = airAnchor.getY();
        boolean mega = carver != null && carver.isColumnCacheReady() && carver.columnCache().isMegaGigaZone(lx, lz);
        boolean entrance = carver != null && carver.isEntranceColumn(lx, lz);
        if (carver == null || !CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, biome, mega, entrance)) {
            return CaveFloorCover.normalizeAirAnchor(chunk, airAnchor);
        }
        if (!CaveFloorCover.appliesTo(biome)) {
            return CaveFloorCover.normalizeAirAnchor(chunk, airAnchor);
        }
        BlockState cover = CaveFloorCover.coverBlock(biome);
        if (cover == null) {
            return CaveFloorCover.normalizeAirAnchor(chunk, airAnchor);
        }
        int cx = airAnchor.getX();
        int cz = airAnchor.getZ();
        int bestSolid = -1;
        int bestAir = airAnchor.getY();
        for (int[] offset : PATCH) {
            int plx = (cx + offset[0]) & 0xF;
            int plz = (cz + offset[1]) & 0xF;
            int solidY = CaveFloorCover.findTopSolid(chunk, plx, plz, airAnchor.getY() + 2);
            if (solidY < 0) {
                continue;
            }
            int airY = solidY + 1;
            if (bestSolid < 0 || airY < bestAir) {
                bestSolid = solidY;
                bestAir = airY;
            }
            if (CaveFloorCover.shouldPaint(chunk, carver, plx, solidY, plz)) {
                chunk.setBlockState(new BlockPos(plx, solidY, plz), cover, false);
            }
        }
        if (bestSolid < 0) {
            return airAnchor;
        }
        return new BlockPos(cx, bestAir, cz);
    }

    private static BlockPos normalizeAirAnchor(ChunkAccess chunk, BlockPos airAnchor) {
        int lx = airAnchor.getX() & 0xF;
        int lz = airAnchor.getZ() & 0xF;
        int solidY = CaveFloorCover.findTopSolid(chunk, lx, lz, airAnchor.getY() + 2);
        if (solidY < 0) {
            return airAnchor;
        }
        return new BlockPos(airAnchor.getX(), solidY + 1, airAnchor.getZ());
    }

    private static boolean shouldPaint(ChunkAccess chunk, CarverChunk carver, int lx, int solidY, int lz) {
        BlockState current = chunk.getBlockState(new BlockPos(lx, solidY, lz));
        if (current.isAir() || !current.getFluidState().isEmpty()) {
            return false;
        }
        if (current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.PODZOL) || current.is(Blocks.MYCELIUM)) {
            return false;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int margin = carver != null && carver.isColumnCacheReady() && carver.columnCache().isMegaGigaZone(lx, lz) ? 2 : 4;
        if (solidY >= surface - margin) {
            return false;
        }
        if (carver != null && carver.isColumnCacheReady()) {
            boolean mega = carver.columnCache().isMegaGigaZone(lx, lz);
            if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, solidY, lz, mega)) {
                return false;
            }
        }
        return current.is(BlockTags.BASE_STONE_OVERWORLD) || current.is(Blocks.DIRT) || current.is(Blocks.STONE) || current.is(Blocks.DEEPSLATE) || current.is(Blocks.TUFF) || current.is(Blocks.GRANITE) || current.is(Blocks.DIORITE) || current.is(Blocks.ANDESITE) || current.is(Blocks.CALCITE) || current.is(Blocks.GRAVEL) || current.is(Blocks.COBBLESTONE);
    }

    private static int findTopSolid(ChunkAccess chunk, int lx, int lz, int startY) {
        int minY = chunk.getMinBuildHeight();
        for (int y = Math.min(startY, chunk.getHighestSectionPosition() + 15); y >= minY; --y) {
            BlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (y + 1 <= chunk.getHighestSectionPosition() + 15 && !chunk.getBlockState(new BlockPos(lx, y + 1, lz)).isAir()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    private static BlockState coverBlock(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(key -> key.location().getPath().toLowerCase()).orElse("");
        String ns = biome.unwrapKey().map(key -> key.location().getNamespace()).orElse("minecraft");
        if (path.contains("mycotoxic") || path.contains("fungal") || path.contains("bioshroom") || path.contains("glowshroom")) {
            Block fungal = CaveFloorCover.firstBlock(ns, "mycelium", "fungal_moss", "glow_mycelium", "bioshroom_block");
            return fungal != null ? fungal.defaultBlockState() : Blocks.MYCELIUM.defaultBlockState();
        }
        if (path.contains("scorching") || path.contains("brimstone") || path.contains("volcanic") || path.contains("mantle")) {
            Block scorch = CaveFloorCover.firstBlock("regions_unexplored", "scorched_grass", "scorched_dirt", "charred_grass");
            if (scorch == null) {
                scorch = CaveFloorCover.firstBlock("terralith", "scorched_grass", "charred_grass");
            }
            if (scorch == null) {
                scorch = CaveFloorCover.firstBlock(ns, "scorched_grass", "charred_grass", "scorch");
            }
            return scorch != null ? scorch.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        }
        if (path.contains("prismachasm") || path.contains("prisma")) {
            Block prismoss = CaveFloorCover.firstBlock("regions_unexplored", "prismoss", "prismoss_block");
            if (prismoss == null) {
                prismoss = CaveFloorCover.firstBlock("terralith", "prismoss", "prismoss_block");
            }
            return prismoss != null ? prismoss.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
        }
        if (CaveBiomeIds.isModCaveBiome(biome)) {
            return Blocks.MOSS_BLOCK.defaultBlockState();
        }
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }

    private static Block firstBlock(String namespace, String... names) {
        for (String name : names) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, name));
            if (block != null && block != Blocks.AIR) {
                return block;
            }
        }
        return null;
    }
}
