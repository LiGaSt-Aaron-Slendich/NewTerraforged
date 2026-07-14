package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * River biome is sampled in 2D quarts — dry shore blocks inherit {@code minecraft:river}.
 * Re-paint solid/air-above-land columns back to the surrounding land biome.
 */
public final class RiverShoreBiomeClip {
    private static final int BAND_BELOW = 4;
    private static final int BAND_ABOVE = 2;

    private RiverShoreBiomeClip() {
    }

    public static void clip(ChunkAccess chunk, Generator generator, TerrainData terrain) {
        if (terrain == null) {
            return;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int yStart = Math.max(minY, surface - BAND_BELOW);
                int yEnd = Math.min(maxY, surface + BAND_ABOVE);
                for (int y = yStart; y <= yEnd; ++y) {
                    pos.set(lx, y, lz);
                    Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock((int)pos.getX()),
                            QuartPos.fromBlock((int)y), QuartPos.fromBlock((int)pos.getZ()));
                    if (!RiverShoreBiomeClip.isRiverBiome(biome)) {
                        continue;
                    }
                    BlockState state = chunk.getBlockState(pos);
                    if (RiverShoreBiomeClip.isOpenRiverWater(state)) {
                        continue;
                    }
                    if (state.isAir() && RiverShoreBiomeClip.columnHasOpenWaterBelow(chunk, lx, y, lz)) {
                        continue;
                    }
                    Holder<Biome> land = source.getBiomeSampler().sampleLandBiome(climateSeed, wx, wz);
                    if (land == null || RiverShoreBiomeClip.isRiverBiome(land)) {
                        continue;
                    }
                    CaveSurfaceBiomeRestorer.setBiomeQuart(chunk, lx, y, lz, land);
                }
            }
        }
    }

    private static boolean isRiverBiome(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_RIVER);
    }

    private static boolean isOpenRiverWater(BlockState state) {
        return !state.getFluidState().isEmpty() && state.getFluidState().is(Fluids.WATER);
    }

    private static boolean columnHasOpenWaterBelow(ChunkAccess chunk, int lx, int y, int lz) {
        int minY = Math.max(chunk.getMinBuildHeight(), y - 6);
        for (int cy = y - 1; cy >= minY; --cy) {
            BlockState below = chunk.getBlockState(new BlockPos(lx, cy, lz));
            if (RiverShoreBiomeClip.isOpenRiverWater(below)) {
                return true;
            }
            if (!below.isAir() && below.getFluidState().isEmpty()) {
                return false;
            }
        }
        return false;
    }
}
