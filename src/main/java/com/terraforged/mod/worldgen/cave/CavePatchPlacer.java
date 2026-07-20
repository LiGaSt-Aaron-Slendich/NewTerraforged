package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.util.NoiseUtil;
import java.util.List;

/**
 * SPECIAL patch helpers: ceiling preview (cartography) and island slots inside PRIMARY regions.
 * Island paint is applied via {@link CaveMegaGigaLayout#getBiomeAt} — not via feature density.
 */
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

    /**
     * Returns an island SPECIAL biome if (wx,wz) lies inside one of up to {@code maxIslands}
     * deterministic slots owned by the host PRIMARY sector.
     */
    public static CaveBiomeEntry islandAt(
            int seed,
            int wx,
            int wz,
            float centerX,
            float centerZ,
            CaveMegaGigaLayout.Sector host,
            List<CaveBiomeEntry> islandPool,
            int maxIslands,
            float islandRadiusCapChunks
    ) {
        if (host == null || islandPool == null || islandPool.isEmpty() || maxIslands <= 0 || host.hopFromCore() == 0) {
            return null;
        }
        float totalWeight = CavePatchPlacer.totalWeight(islandPool);
        if (totalWeight <= 0.0F) {
            return null;
        }
        float midAngle = CavePatchPlacer.normalizeAngle((host.angleStart() + host.angleEnd()) * 0.5F);
        float midRadius = (host.innerRadius() + host.outerRadius()) * 0.5F;
        int regionSalt = NoiseUtil.hash2D(seed ^ 0x151A7D, (int)(centerX + midRadius), (int)(centerZ + midAngle * 100.0F))
                ^ host.hopFromCore() * 131
                ^ host.biome().biome().hashCode();

        CaveBiomeEntry best = null;
        float bestDist = Float.MAX_VALUE;
        int slots = Math.min(maxIslands, 4);
        for (int slot = 0; slot < slots; ++slot) {
            float spawn = CavePatchPlacer.noise01(seed, regionSalt + slot * 17);
            // Keep roughly 50–75% of slots occupied so regions average ~2–3 of 4 islands.
            if (spawn > 0.62F) {
                continue;
            }
            float angleJitter = (CavePatchPlacer.noise01(seed, regionSalt + slot * 31 + 3) - 0.5F) * CavePatchPlacer.sectorSpan(host) * 0.7F;
            float radiusJitter = (CavePatchPlacer.noise01(seed, regionSalt + slot * 47 + 5) - 0.5F)
                    * Math.max(16.0F, host.outerRadius() - host.innerRadius()) * 0.55F;
            float angle = CavePatchPlacer.normalizeAngle(midAngle + angleJitter);
            float radius = Math.max(host.innerRadius() + 8.0F, Math.min(host.outerRadius() - 8.0F, midRadius + radiusJitter));
            float ix = centerX + (float)Math.cos(angle) * radius;
            float iz = centerZ + (float)Math.sin(angle) * radius;
            float pick = CavePatchPlacer.noise01(seed, regionSalt + slot * 59 + 7);
            CaveBiomeEntry candidate = CavePatchPlacer.pickWeighted(islandPool, totalWeight, pick);
            if (candidate == null) {
                continue;
            }
            float cap = Math.min(candidate.islandMaxRadius(), Math.max(0.5F, islandRadiusCapChunks));
            int islandRadius = NoiseCave.calcIslandRadius(cap);
            float dist = NoiseUtil.sqrt((wx - ix) * (wx - ix) + (wz - iz) * (wz - iz));
            if (dist > islandRadius || !(dist < bestDist)) {
                continue;
            }
            bestDist = dist;
            best = candidate;
        }
        return best;
    }

    private static float sectorSpan(CaveMegaGigaLayout.Sector sector) {
        float start = CavePatchPlacer.normalizeAngle(sector.angleStart());
        float end = CavePatchPlacer.normalizeAngle(sector.angleEnd());
        if (start <= end) {
            return Math.max(0.2F, end - start);
        }
        return Math.max(0.2F, (float)(Math.PI * 2) - start + end);
    }

    private static float normalizeAngle(float angle) {
        float twoPi = (float)Math.PI * 2;
        if ((angle %= twoPi) < 0.0F) {
            angle += twoPi;
        }
        return angle;
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

    private static float noise01(int seed, int salt) {
        return (NoiseUtil.valCoord2D(seed, salt, salt ^ 0x5A) + 1.0F) * 0.5F;
    }

    private record PatchHit(CaveBiomeEntry entry, float distance) {
    }
}
