package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.util.ChunkBiomePaint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Paints one layout biome for the full air column in mega/giga caves.
 * Prevents vertical quart stripes from partial carve paint and patch bands in open chambers.
 */
public final class CaveBiomeColumnUnifier {
    private static final int OPEN_CHAMBER_HEIGHT = 28;

    private CaveBiomeColumnUnifier() {
    }

    public static void unifyMegaGigaChunk(int seed, ChunkAccess chunk, CarverChunk carver, Generator generator, NoiseCave config) {
        if (config == null || !config.getType().isMegaOrGiga()) {
            return;
        }
        CarverColumnCache columns = carver.columnCache();
        if (!columns.anyMegaGiga()) {
            return;
        }
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (!columns.matches(config.getType(), lx, lz)) {
                    continue;
                }
                int floorY = CaveColumnScan.findFloorNearSurface(chunk, lx, lz, minY, maxY);
                if (floorY < 0) {
                    continue;
                }
                int ceilY = CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY, maxY);
                if (ceilY <= floorY + 2) {
                    continue;
                }
                int caveHeight = ceilY - floorY;
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int midY = floorY + caveHeight / 2;
                Holder<Biome> biome = carver.getChamberBiome(wx, wz, midY, config, generator, caveHeight, chunk, floorY, ceilY);
                if (biome == null) {
                    continue;
                }
                for (int y = floorY; y <= ceilY; ++y) {
                    pos.set(lx, y, lz);
                    if (!chunk.getBlockState(pos).isAir()) {
                        continue;
                    }
                    CaveBiomeColumnUnifier.setBiomeQuart(chunk, lx, y, lz, biome, generator);
                }
                carver.markBiomeRestoreColumn(lx, lz);
            }
        }
    }

    public static boolean isOpenChamber(int floorY, int ceilY) {
        return ceilY - floorY >= OPEN_CHAMBER_HEIGHT;
    }

    private static void setBiomeQuart(ChunkAccess chunk, int lx, int ly, int lz, Holder<Biome> biome, Generator generator) {
        int sectionIndex = chunk.getSectionIndex(ly);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
            return;
        }
        LevelChunkSection section = chunk.getSection(sectionIndex);
        ChunkBiomePaint.set(section, lx >> 2, (ly & 0xF) >> 2, lz >> 2, biome, generator.getBiomeSource().getRegistry());
    }
}
