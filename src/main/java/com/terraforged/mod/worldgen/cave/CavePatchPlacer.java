package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import java.util.List;

/** Cartography-only preview of ceiling patch biomes (runtime apply lives in layer port). */
public final class CavePatchPlacer {
    private static final int GRID_BLOCKS = 80;

    private CavePatchPlacer() {
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
        if (hit.distance() > radius) {
            return null;
        }
        return hit.entry();
    }

    private static float totalWeight(List<CaveBiomeEntry> entries) {
        float total = 0.0F;
        for (CaveBiomeEntry entry : entries) {
            total += entry.weight();
        }
        return total;
    }

    private static PatchHit findPatch(int seed, int wx, int wz, List<CaveBiomeEntry> specials, float totalWeight) {
        if (specials.isEmpty() || totalWeight <= 0.0F) {
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
                float pick = (pickHash & 0xFFFF) / 65535.0F;
                CaveBiomeEntry candidate = CavePatchPlacer.pickWeighted(specials, totalWeight, pick);
                if (candidate == null) {
                    continue;
                }
                float spawn = (NoiseUtil.valCoord2D(seed + candidate.biome().hashCode(), cx, cz) + 1.0F) * 0.5F;
                float threshold = 0.05F + candidate.weight() * 0.08F;
                if (CaveBiomeIds.isCrystalCaveBiome(candidate.biome()) && !CaveBiomeIds.isPrismachasmBiome(candidate.biome())) {
                    threshold += 0.12F;
                }
                if (spawn > threshold) {
                    continue;
                }
                float jx = cx * GRID_BLOCKS + 40.0F + CavePatchPlacer.jitter(seed, cx, cz, 0) * GRID_BLOCKS * 0.35F;
                float jz = cz * GRID_BLOCKS + 40.0F + CavePatchPlacer.jitter(seed, cx, cz, 1) * GRID_BLOCKS * 0.35F;
                float dist = NoiseUtil.sqrt((wx - jx) * (wx - jx) + (wz - jz) * (wz - jz));
                if (best != null && !(dist < best.distance())) {
                    continue;
                }
                best = new PatchHit(candidate, dist);
            }
        }
        return best;
    }

    private static CaveBiomeEntry pickWeighted(List<CaveBiomeEntry> specials, float total, float pick) {
        float target = pick * total;
        float cumulative = 0.0F;
        for (CaveBiomeEntry entry : specials) {
            cumulative += entry.weight();
            if (target <= cumulative) {
                return entry;
            }
        }
        return specials.get(specials.size() - 1);
    }

    private static float jitter(int seed, int cx, int cz, int axis) {
        return NoiseUtil.valCoord2D(seed + axis * 131, cx, cz);
    }

    private record PatchHit(CaveBiomeEntry entry, float distance) {
    }
}
