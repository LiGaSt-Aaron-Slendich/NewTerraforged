package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public final class CavePatchPlacer {
    private static final int GRID_BLOCKS = 80;

    private CavePatchPlacer() {
    }

    public static void apply(int seed, ChunkAccess chunk, Generator generator, NoiseCave config, CarverChunk carver, CaveBiomeRegistry registry) {
        if (registry == null || registry.isVanillaFallback()) {
            return;
        }
        if (!config.getType().isMegaOrGiga()) {
            return;
        }
        List<CaveBiomeEntry> islandSpecials = registry.getSpecial(CavePlacementType.ISLAND_PATCH);
        List<CaveBiomeEntry> ceilingSpecials = registry.getSpecial(CavePlacementType.CEILING_PATCH);
        if (islandSpecials.isEmpty() && ceilingSpecials.isEmpty()) {
            return;
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = config.getMinY();
        int maxY = Math.min(config.getMaxY(), chunk.getHighestSectionPosition() + 15);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                int floorY = CavePatchPlacer.findFloorAir(chunk, lx, lz, minY, maxY);
                if (floorY < 0 || !CaveUndergroundGuard.mayPlaceAnchor(chunk, lx, floorY, lz, true)) continue;
                int ceilY = CavePatchPlacer.findCeilingAir(chunk, lx, lz, floorY, maxY);
                if (ceilY <= floorY + 2) continue;
                int caveHeight = ceilY - floorY;
                CavePatchPlacer.applyIsland(seed, chunk, carver, registry, islandSpecials, lx, lz, wx, wz, floorY, ceilY, pos);
                CavePatchPlacer.applyCeiling(seed, chunk, carver, registry, ceilingSpecials, lx, lz, wx, wz, floorY, ceilY, caveHeight, pos);
            }
        }
    }

    public static CaveBiomeEntry previewCeilingPatch(int seed, int wx, int wz, int floorY, int ceilY, CaveBiomeRegistry registry) {
        if (registry == null || ceilY <= floorY + 2) {
            return null;
        }
        List<CaveBiomeEntry> ceilingSpecials = registry.getSpecial(CavePlacementType.CEILING_PATCH);
        if (ceilingSpecials.isEmpty()) {
            return null;
        }
        PatchHit hit = CavePatchPlacer.findPatch(seed, wx, wz, ceilingSpecials, CavePatchPlacer.totalWeight(ceilingSpecials));
        if (hit == null) {
            return null;
        }
        int radius = NoiseCave.calcIslandRadius(hit.entry().islandMaxRadius());
        if (hit.distance() > (float)radius) {
            return null;
        }
        return hit.entry();
    }

    private static void applyIsland(int seed, ChunkAccess chunk, CarverChunk carver, CaveBiomeRegistry registry, List<CaveBiomeEntry> islandSpecials, int lx, int lz, int wx, int wz, int floorY, int ceilY, BlockPos.MutableBlockPos pos) {
        Holder<Biome> holder;
        if (islandSpecials.isEmpty()) {
            return;
        }
        PatchHit hit = CavePatchPlacer.findPatch(seed, wx, wz, islandSpecials, CavePatchPlacer.totalWeight(islandSpecials));
        if (hit == null || (holder = registry.getHolder(hit.entry()).orElse(null)) == null) {
            return;
        }
        int radius = NoiseCave.calcIslandRadius(hit.entry().islandMaxRadius());
        if (hit.distance() > (float)radius) {
            return;
        }
        int islandTop = Math.min(ceilY, floorY + NoiseCave.calcIslandHeight(radius));
        CavePatchPlacer.paintColumn(chunk, carver, lx, lz, floorY, islandTop, holder, pos);
    }

    private static void applyCeiling(int seed, ChunkAccess chunk, CarverChunk carver, CaveBiomeRegistry registry, List<CaveBiomeEntry> ceilingSpecials, int lx, int lz, int wx, int wz, int floorY, int ceilY, int caveHeight, BlockPos.MutableBlockPos pos) {
        Holder<Biome> holder;
        if (ceilingSpecials.isEmpty()) {
            return;
        }
        PatchHit hit = CavePatchPlacer.findPatch(seed ^ 0x6C62272E, wx, wz, ceilingSpecials, CavePatchPlacer.totalWeight(ceilingSpecials));
        if (hit == null || (holder = registry.getHolder(hit.entry()).orElse(null)) == null) {
            return;
        }
        CaveBiomeEntry patch = hit.entry();
        int radius = NoiseCave.calcIslandRadius(patch.islandMaxRadius());
        if (hit.distance() > (float)radius) {
            return;
        }
        float factor = NoiseUtil.clamp((NoiseUtil.valCoord2D(seed, wx, wz) + 1.0f) * 0.5f, 0.0f, 1.0f);
        int band = NoiseCave.calcCeilingPatchHeight(caveHeight, patch.ceilingPatchMin(), patch.ceilingPatchMax(), factor);
        int bandBottom = Math.max(floorY + 2, ceilY - band);
        CavePatchPlacer.paintColumn(chunk, carver, lx, lz, bandBottom, ceilY, holder, pos);
    }

    private static float totalWeight(List<CaveBiomeEntry> entries) {
        float total = 0.0f;
        for (CaveBiomeEntry entry : entries) {
            total += entry.weight();
        }
        return total;
    }

    private static PatchHit findPatch(int seed, int wx, int wz, List<CaveBiomeEntry> specials, float totalWeight) {
        if (specials.isEmpty() || totalWeight <= 0.0f) {
            return null;
        }
        int cellX = Math.floorDiv(wx, GRID_BLOCKS);
        int cellZ = Math.floorDiv(wz, GRID_BLOCKS);
        PatchHit best = null;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                int pickHash = NoiseUtil.hash2D(seed ^ 0x5DEECE66, cx, cz);
                float pick = (float)(pickHash & 0xFFFF) / 65535.0f;
                CaveBiomeEntry candidate = CavePatchPlacer.pickWeighted(specials, totalWeight, pick);
                if (candidate == null) continue;
                float spawn = (NoiseUtil.valCoord2D(seed + candidate.biome().hashCode(), cx, cz) + 1.0f) * 0.5f;
                float threshold = 0.05f + candidate.weight() * 0.08f;
                if (CaveBiomeIds.isCrystalCaveBiome(candidate.biome()) && !CaveBiomeIds.isPrismachasmBiome(candidate.biome())) {
                    threshold += 0.12f;
                }
                if (spawn > threshold) continue;
                float jx = (float)(cx * GRID_BLOCKS) + 40.0f + CavePatchPlacer.jitter(seed, cx, cz, 0) * (float)GRID_BLOCKS * 0.35f;
                float jz = (float)(cz * GRID_BLOCKS) + 40.0f + CavePatchPlacer.jitter(seed, cx, cz, 1) * (float)GRID_BLOCKS * 0.35f;
                float dist = NoiseUtil.sqrt(((float)wx - jx) * ((float)wx - jx) + ((float)wz - jz) * ((float)wz - jz));
                if (best != null && !(dist < best.distance())) continue;
                best = new PatchHit(candidate, dist);
            }
        }
        return best;
    }

    private static void paintColumn(ChunkAccess chunk, CarverChunk carver, int lx, int lz, int fromY, int toY, Holder<Biome> biome, BlockPos.MutableBlockPos pos) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        for (int y = fromY; y <= toY; ++y) {
            pos.set(lx, y, lz);
            if (!chunk.getBlockState(pos).isAir()) continue;
            CavePatchPlacer.setBiomeQuart(chunk, lx, y, lz, biome);
            carver.setPatchBiome(lx, y, lz, biome, chunkMinX, chunkMinZ);
        }
    }

    private static CaveBiomeEntry pickWeighted(List<CaveBiomeEntry> specials, float total, float pick) {
        float target = pick * total;
        float cumulative = 0.0f;
        for (CaveBiomeEntry entry : specials) {
            if (!(target <= (cumulative += entry.weight()))) continue;
            return entry;
        }
        return specials.get(specials.size() - 1);
    }

    private static float jitter(int seed, int cx, int cz, int axis) {
        return NoiseUtil.valCoord2D(seed + axis * 131, cx, cz);
    }

    private static int findFloorAir(ChunkAccess chunk, int lx, int lz, int minY, int maxY) {
        return CaveColumnScan.findLowestFloor(chunk, lx, lz, minY, maxY);
    }

    private static int findCeilingAir(ChunkAccess chunk, int lx, int lz, int floorY, int maxY) {
        return CaveColumnScan.findCeilingAboveFloor(chunk, lx, lz, floorY, maxY);
    }

    private static void setBiomeQuart(ChunkAccess chunk, int lx, int ly, int lz, Holder<Biome> biome) {
        int biomeX = lx >> 2;
        int biomeZ = lz >> 2;
        int biomeY = (ly & 0xF) >> 2;
        int sectionIndex = chunk.getSectionIndex(ly);
        LevelChunkSection section = chunk.getSection(sectionIndex);
        PalettedContainer container = section.getBiomes();
        container.set(biomeX, biomeY, biomeZ, biome);
    }

    private record PatchHit(CaveBiomeEntry entry, float distance) {
    }
}
