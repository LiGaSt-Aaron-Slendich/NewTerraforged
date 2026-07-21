package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Shared island / archipelago placement math.
 * Density follows continent falloff (moves when continents move): densest near shores
 * and in channels between landmasses; sparse mid-ocean (volcanic / rare archipelago only).
 * Non-volcano islands use elongated organic blobs, not radial circles.
 */
public final class IslandScatter {
    public static final int LAGUNA_MAX_DEPTH = 15;
    /** Coarse cluster spacing (world blocks); acceptance still gated by coast proximity. */
    public static final int ARCH_CELL = 3600;
    public static final int VOLC_CELL = 10000;
    private static final int MAX_SATELLITES = 9;

    private IslandScatter() {
    }

    /**
     * 0..1 — how strongly this ocean sample should receive coastal islands.
     * Peaks around shallow-ocean / near-shore continentNoise; scales width with continentScale.
     */
    public static float shoreProximity(float continentNoise, int continentScale) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        float scale = Math.max(500.0F, continentScale);
        // Larger continents → island belt extends farther into ocean (wider peak in noise space).
        float width = 0.10F + 0.22F * NoiseUtil.clamp(scale / 3000.0F, 0.35F, 2.5F);
        float peak = 0.30F;
        float d = (cn - peak) / width;
        float bell = (float) Math.exp(-0.5 * d * d);
        // Channels between continents sit in the shallow band — boost mid values.
        float channel = cn > 0.12F && cn < 0.42F ? 0.25F * (1.0F - Math.abs(cn - 0.28F) / 0.20F) : 0.0F;
        return NoiseUtil.clamp(bell + Math.max(0.0F, channel), 0.0F, 1.0F);
    }

    /** Mid-ocean allowance for rare volcanic / archipelago only (not coastal freckles). */
    public static float midOceanAllow(float continentNoise) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        if (cn >= 0.18F) {
            return 0.0F;
        }
        return (0.18F - cn) / 0.18F;
    }

    public static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    public static float valueNoise2(int seed, float x, float z) {
        int x0 = NoiseUtil.floor(x);
        int z0 = NoiseUtil.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fz * fz * (3.0F - 2.0F * fz);
        float a = hash01(seed, x0, z0);
        float b = hash01(seed, x0 + 1, z0);
        float c = hash01(seed, x0, z0 + 1);
        float d = hash01(seed, x0 + 1, z0 + 1);
        float ab = a + (b - a) * u;
        float cd = c + (d - c) * u;
        return ab + (cd - ab) * v;
    }

    /**
     * Organic elongated island mask. Returns &lt;0 outside, 0..1 inside (1 = centre).
     */
    public static float organicIslandMask(float dx, float dz, float rx, float rz, float angle, int seed, int id) {
        float c = NoiseUtil.cos(angle);
        float s = NoiseUtil.sin(angle);
        float u = dx * c - dz * s;
        float v = dx * s + dz * c;
        float warp = (valueNoise2(seed ^ (id * 31), u * 0.018F, v * 0.018F) - 0.5F) * 0.55F;
        float warp2 = (valueNoise2(seed ^ (id * 17 + 3), u * 0.04F, v * 0.04F) - 0.5F) * 0.25F;
        float rxw = rx * (1.0F + warp);
        float rzw = rz * (1.0F + warp2);
        if (rxw < 8.0F) {
            rxw = 8.0F;
        }
        if (rzw < 6.0F) {
            rzw = 6.0F;
        }
        float e = (u * u) / (rxw * rxw) + (v * v) / (rzw * rzw);
        if (e >= 1.0F) {
            return -1.0F;
        }
        return 1.0F - e;
    }

    public record ClusterEval(boolean land, boolean laguna, float heightBoost, boolean volcano, boolean pipe) {
        public static final ClusterEval NONE = new ClusterEval(false, false, 0.0F, false, false);
    }

    /**
     * Scattered archipelago: large organic core + irregular satellite islands + laguna between.
     */
    public static ClusterEval evalArchipelago(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float proximity,
            float midOcean
    ) {
        // Must be near shore/channel, OR rare mid-ocean.
        float gate = proximity + midOcean * 0.12F;
        if (gate < 0.08F) {
            return ClusterEval.NONE;
        }

        int cell = ARCH_CELL;
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);

        // Jittered cell centres so clusters aren't on a rigid lattice.
        float jx = (hash01(seed ^ 11, cx, cz) - 0.5F) * cell * 0.55F;
        float jz = (hash01(seed ^ 13, cx, cz) - 0.5F) * cell * 0.55F;
        float centerX = (cx + 0.5F) * cell + jx;
        float centerZ = (cz + 0.5F) * cell + jz;

        float place = hash01(seed ^ 99, cx, cz);
        float need = 1.0F - archipelagoChance * NoiseUtil.clamp(0.35F + gate * 0.85F, 0.0F, 1.0F);
        if (place < need) {
            return ClusterEval.NONE;
        }

        // Prefer clusters when proximity is high.
        if (hash01(seed ^ 77, cx, cz) > 0.15F + proximity * 0.85F + midOcean * 0.05F) {
            return ClusterEval.NONE;
        }

        float coreRx = 140.0F + hash01(seed, cx, cz) * 320.0F;
        float coreRz = 90.0F + hash01(seed ^ 5, cx, cz) * 260.0F;
        float coreAngle = hash01(seed ^ 19, cx, cz) * NoiseUtil.PI2;
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;

        float core = organicIslandMask(dx, dz, coreRx, coreRz, coreAngle, seed, 0);
        if (core >= 0.0F) {
            return new ClusterEval(true, false, 0.35F + core * 0.35F, false, false);
        }

        float bestSat = -1.0F;
        float bestLag = Float.MAX_VALUE;
        int sats = 4 + NoiseUtil.floor(hash01(seed ^ 23, cx, cz) * (MAX_SATELLITES - 3));
        for (int i = 0; i < sats; i++) {
            float a = hash01(seed ^ (40 + i), cx, cz) * NoiseUtil.PI2;
            float dist = coreRx * 0.55F + hash01(seed ^ (60 + i), cx, cz) * (coreRx * 1.1F + 180.0F);
            // Irregular scatter — not a ring.
            dist *= 0.65F + hash01(seed ^ (80 + i), cx, cz) * 0.7F;
            float ox = NoiseUtil.cos(a) * dist + (hash01(seed ^ (100 + i), cx, cz) - 0.5F) * 120.0F;
            float oz = NoiseUtil.sin(a) * dist + (hash01(seed ^ (120 + i), cx, cz) - 0.5F) * 120.0F;
            float srx = 25.0F + hash01(seed ^ (140 + i), cx, cz) * 95.0F;
            float srz = 18.0F + hash01(seed ^ (160 + i), cx, cz) * 80.0F;
            float sang = hash01(seed ^ (180 + i), cx, cz) * NoiseUtil.PI2;
            float mask = organicIslandMask(dx - ox, dz - oz, srx, srz, sang, seed, i + 1);
            if (mask > bestSat) {
                bestSat = mask;
            }
            // Distance to satellite ellipse approx for laguna.
            float c = NoiseUtil.cos(sang);
            float s = NoiseUtil.sin(sang);
            float u = (dx - ox) * c - (dz - oz) * s;
            float v = (dx - ox) * s + (dz - oz) * c;
            float el = NoiseUtil.sqrt((u * u) / (srx * srx) + (v * v) / (srz * srz));
            if (el < bestLag) {
                bestLag = el;
            }
        }

        if (bestSat >= 0.0F) {
            return new ClusterEval(true, false, 0.30F + bestSat * 0.30F, false, false);
        }

        // Laguna: shallow water near the cluster, between islands — not a solid rim.
        float coreElApprox = NoiseUtil.sqrt((dx * dx) / (coreRx * coreRx) + (dz * dz) / (coreRz * coreRz));
        float nearCluster = Math.min(coreElApprox, bestLag);
        if (nearCluster > 1.0F && nearCluster < 1.85F) {
            float gapNoise = valueNoise2(seed ^ 9, worldX * 0.01F, worldZ * 0.01F);
            if (gapNoise > 0.28F && gapNoise < 0.78F) {
                return new ClusterEval(false, true, 0.0F, false, false);
            }
        }
        return ClusterEval.NONE;
    }

    public static ClusterEval evalOceanVolcano(
            float worldX,
            float worldZ,
            int seed,
            float volcanicChance,
            float proximity,
            float midOcean
    ) {
        // Prefer near shore; allow sparse mid-ocean.
        float gate = proximity * 0.85F + midOcean * 0.35F;
        if (gate < 0.05F || volcanicChance <= 0.0F) {
            return ClusterEval.NONE;
        }
        int cell = VOLC_CELL;
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);
        float place = hash01(seed ^ 0xB01C, cx, cz);
        float need = 1.0F - volcanicChance * (0.12F + gate * 0.35F);
        if (place < need) {
            return ClusterEval.NONE;
        }
        float centerX = (cx + 0.5F) * cell + (hash01(seed, cx, cz) - 0.5F) * cell * 0.4F;
        float centerZ = (cz + 0.5F) * cell + (hash01(seed ^ 3, cx, cz) - 0.5F) * cell * 0.4F;
        float radius = 70.0F + hash01(seed ^ 9, cx, cz) * 140.0F;
        return evalVolcanoCone(worldX, worldZ, centerX, centerZ, radius);
    }

    public static ClusterEval evalCoastalFreckle(
            float worldX,
            float worldZ,
            int seed,
            float coastalChance,
            float volcanicChance,
            float continentNoise
    ) {
        // Only on near-shore band of existing land/coast samples.
        if (continentNoise < 0.28F || continentNoise > 0.70F) {
            return ClusterEval.NONE;
        }
        int ix = NoiseUtil.floor(worldX);
        int iz = NoiseUtil.floor(worldZ);
        float roll = hash01(seed, ix >> 2, iz >> 2);
        float volcanicBand = volcanicChance * 0.10F;
        if (volcanicChance > 0.0F && roll < volcanicBand) {
            float local = hash01(seed ^ 31, ix >> 1, iz >> 1);
            if (local < 0.18F) {
                float centerX = (ix >> 4 << 4) + 8.0F;
                float centerZ = (iz >> 4 << 4) + 8.0F;
                float radius = 50.0F + local * 80.0F;
                return evalVolcanoCone(worldX, worldZ, centerX, centerZ, radius);
            }
            return ClusterEval.NONE;
        }
        if (coastalChance > 0.0F && roll < volcanicBand + coastalChance * 0.45F) {
            float local = hash01(seed ^ 17, ix >> 1, iz >> 1);
            if (local < 0.28F) {
                // Small elongated coastal island freckle.
                float ang = hash01(seed ^ 41, ix >> 3, iz >> 3) * NoiseUtil.PI2;
                float rx = 18.0F + local * 40.0F;
                float rz = 10.0F + local * 28.0F;
                float centerX = (ix >> 3 << 3) + 4.0F;
                float centerZ = (iz >> 3 << 3) + 4.0F;
                float mask = organicIslandMask(worldX - centerX, worldZ - centerZ, rx, rz, ang, seed, 99);
                if (mask >= 0.0F) {
                    return new ClusterEval(true, false, 0.25F + mask * 0.25F, false, false);
                }
            }
        }
        return ClusterEval.NONE;
    }

    /** Radial volcano with real height: outer cone + crater rim + pipe (throat). */
    public static ClusterEval evalVolcanoCone(float worldX, float worldZ, float centerX, float centerZ, float radius) {
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (dist > radius) {
            return ClusterEval.NONE;
        }
        float t = 1.0F - dist / radius;
        float craterR = radius * 0.22F;
        float rimR = radius * 0.38F;
        if (dist <= craterR) {
            float inner = dist / Math.max(1.0F, craterR);
            return new ClusterEval(true, false, 0.40F + inner * 0.10F, true, true);
        }
        if (dist <= rimR) {
            float rim = (dist - craterR) / Math.max(1.0E-3F, rimR - craterR);
            return new ClusterEval(true, false, 0.55F + rim * 0.20F, true, false);
        }
        return new ClusterEval(true, false, 0.38F + t * 0.35F, true, false);
    }
}
