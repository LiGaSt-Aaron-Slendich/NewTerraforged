package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Post-decoration surface-only pass: strips cave-biome blocks and leaked cave features
 * from the overworld surface column. Does not scan underground cave volumes.
 */
public final class CaveDecorationSanitizer {
    /** Scan above heightmap for tall leaked features (mushrooms, columns, trees). */
    private static final int SURFACE_LIFT = 12;
    /** Thin band below the top solid block — cover paint only, not deep stone. */
    private static final int SURFACE_CRUST = 3;

    private CaveDecorationSanitizer() {
    }

    public static void sanitize(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null) {
            return;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                int yTop = CaveDecorationSanitizer.findSurfaceColumnTop(chunk, lx, lz, surface);
                int yBottom = Math.max(minY, surface - SURFACE_CRUST);
                for (int y = yTop; y >= yBottom; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (!CaveDecorationSanitizer.shouldRemoveFromSurface(state, chunk, lx, y, lz, surfaceBiome)) {
                        continue;
                    }
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
        CaveSurfaceBiomeRestorer.restore(chunk, generator, carver);
    }

    /** Surface-band cleanup for columns flagged during carving as crust-risk only. */
    public static void sanitizeRiskColumns(ChunkAccess chunk, CarverChunk carver, Generator generator) {
        if (carver == null || !carver.hasSurfaceRisk()) {
            return;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!carver.isSurfaceRiskColumn(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                int yTop = CaveDecorationSanitizer.findSurfaceColumnTop(chunk, lx, lz, surface);
                int yBottom = Math.max(minY, surface - SURFACE_CRUST);
                for (int y = yTop; y >= yBottom; --y) {
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (!CaveDecorationSanitizer.isCorruptedSurfaceBlock(chunk, lx, y, lz, state, surfaceBiome)) {
                        continue;
                    }
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
        CaveSurfaceBiomeRestorer.restoreRiskColumns(chunk, generator, carver);
    }

    private static int findSurfaceColumnTop(ChunkAccess chunk, int lx, int lz, int surface) {
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, surface + SURFACE_LIFT);
        for (int y = maxY; y >= surface; --y) {
            if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
                return y;
            }
        }
        return surface;
    }

    private static boolean shouldRemoveFromSurface(BlockState state, ChunkAccess chunk, int lx, int y, int lz, Holder<Biome> surfaceBiome) {
        return CaveDecorationSanitizer.isCorruptedSurfaceBlock(chunk, lx, y, lz, state, surfaceBiome);
    }

    /** Broad detection: cave-painted leaks only — never bare mod blocks or vanilla vegetation. */
    static boolean isCorruptedSurfaceBlock(ChunkAccess chunk, int lx, int y, int lz, BlockState state, Holder<Biome> surfaceBiome) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted == null || !(CaveBiomeIds.isUndergroundBiome(painted) || CaveBiomeIds.isModCaveBiome(painted))) {
            return false;
        }
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y < surface) {
            return false;
        }
        if (!CaveDecorationSanitizer.isCaveLeakBlock(state)) {
            return false;
        }
        return surfaceBiome != null && CaveDecorationSanitizer.isOverworldSurface(surfaceBiome);
    }

    /** Strict post-restore check: only blocks still tagged with cave biome paint or mod cave blocks. */
    static boolean isUnresolvedSurfaceDefect(ChunkAccess chunk, int lx, int y, int lz, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (CaveDecorationSanitizer.isModCaveBlock(state)) {
            return true;
        }
        if (!CaveDecorationSanitizer.isCaveLeakBlock(state)) {
            return false;
        }
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        return painted != null && (CaveBiomeIds.isUndergroundBiome(painted) || CaveBiomeIds.isModCaveBiome(painted));
    }

    private static boolean isOverworldSurface(Holder<Biome> biome) {
        if (!BiomeUtil.isOverworldSurfaceBiome(biome)) {
            return false;
        }
        return !biome.is(BiomeTags.IS_RIVER) && !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH);
    }

    static boolean isCaveLeakBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.COBBLESTONE)) {
            return false;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        if ("minecraft".equals(id.getNamespace())) {
            String path = id.getPath();
            return path.contains("mushroom") || path.contains("vine") || path.contains("lichen")
                    || path.contains("moss") && !path.equals("moss_block") || path.contains("cobweb") || path.contains("fungus")
                    || path.contains("roots") || path.contains("sprouts") || path.contains("bush")
                    || path.contains("grass") && !path.equals("grass_block");
        }
        return CaveDecorationSanitizer.isModCaveBlock(state);
    }

    static boolean isModCaveBlock(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || "minecraft".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("stone") || path.contains("deepslate") || path.contains("ore") || path.contains("tuff")
                || path.contains("calcite") && path.contains("block")) {
            return false;
        }
        return path.contains("prismoss") || path.contains("scorch") || path.contains("charred")
                || path.contains("mycel") || path.contains("bioshroom") || path.contains("glowshroom")
                || path.contains("fungal") || path.contains("shroom") || path.contains("mushroom")
                || path.contains("cobweb") || path.contains("lichen") || path.contains("vine")
                || path.contains("crystal") || path.contains("geode") || path.contains("basalt") && !path.contains("pillar");
    }
}
