package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CaveSurfaceBiomeRestorer {
    private static final int RESTORE_BELOW_SURFACE = 64;

    private CaveSurfaceBiomeRestorer() {
    }

    public static void restore(ChunkAccess chunk, Generator generator) {
        CaveSurfaceBiomeRestorer.restore(chunk, generator, null);
    }

    public static void restore(ChunkAccess chunk, Generator generator, CarverChunk carver) {
        if (carver != null && !carver.hasAnyBiomeRestoreColumn()) {
            return;
        }
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && !carver.needsBiomeRestore(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int restoreFrom = surface - 64;
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                if (surfaceBiome == null) continue;
                int minY = chunk.getMinBuildHeight();
                int maxY = chunk.getMaxBuildHeight() - 1;
                int yStart = Math.max(minY, restoreFrom);
                int yEnd = Math.min(maxY, surface + 16);
                for (int y = yStart; y <= yEnd; ++y) {
                    if (carver != null && carver.isEntranceColumn(lx, lz) && y >= surface - CaveUndergroundGuard.ENTRANCE_BIOME_DEPTH) {
                        continue;
                    }
                    Holder<Biome> existing = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
                    if (existing != null && (CaveBiomeIds.isUndergroundBiome(existing) || CaveBiomeIds.isPatchPaintedBiome(existing))) continue;
                    CaveSurfaceBiomeRestorer.setBiomeQuart(chunk, lx, y, lz, surfaceBiome);
                }
            }
        }
    }

    public static Holder<Biome> resolveSurfaceBiome(Source source, int climateSeed, int wx, int wz, int surfaceY) {
        Holder<Biome> sampled = source.getNoiseBiome(QuartPos.fromBlock((int)wx), QuartPos.fromBlock((int)(surfaceY - 8)), QuartPos.fromBlock((int)wz), Source.NOOP_CLIMATE_SAMPLER);
        if (CaveSurfaceBiomeRestorer.isRestorableSurfaceBiome(sampled)) {
            return sampled;
        }
        sampled = source.getBiomeSampler().sampleBiome(climateSeed, wx, wz);
        if (CaveSurfaceBiomeRestorer.isRestorableSurfaceBiome(sampled)) {
            return sampled;
        }
        return source.getRegistry().getHolder(Biomes.PLAINS).orElse(null);
    }

    private static boolean isRestorableSurfaceBiome(Holder<Biome> biome) {
        if (!BiomeUtil.isOverworldSurfaceBiome(biome)) {
            return false;
        }
        return !biome.is(BiomeTags.IS_RIVER) && !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH);
    }

    static void setBiomeQuart(ChunkAccess chunk, int lx, int ly, int lz, Holder<Biome> biome) {
        int biomeX = lx >> 2;
        int biomeZ = lz >> 2;
        int biomeY = (ly & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(ly);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
    }

    /**
     * Overwrites cave-biome paint in the surface band — used after integrity repair when
     * {@link #restore} intentionally skips underground-painted quarts.
     */
    public static void forceRestoreSurfaceColumns(ChunkAccess chunk, Generator generator, CarverChunk carver) {
        Source source = generator.getBiomeSource();
        int climateSeed = Seeds.get((int)generator.getSeed());
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                Holder<Biome> surfaceBiome = CaveSurfaceBiomeRestorer.resolveSurfaceBiome(source, climateSeed, wx, wz, surface);
                if (surfaceBiome == null) {
                    continue;
                }
                int yStart = Math.max(minY, surface - 3);
                int yEnd = Math.min(maxY, surface + 12);
                for (int y = yStart; y <= yEnd; ++y) {
                    if (!CaveChunkSurfaceBounds.mayModify(chunk, carver, lx, y, lz)) {
                        continue;
                    }
                    CaveSurfaceBiomeRestorer.setBiomeQuart(chunk, lx, y, lz, surfaceBiome);
                }
            }
        }
    }
}
